


#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Tiki Price Scraper - Tầng 1 Giai đoạn 2
Cào giá sản phẩm từ Tiki và lưu vào database
Hỗ trợ: Logging, Retry, Email notification
Chạy: python scraper.py hoặc run_scraper.bat
"""

import requests
import mysql.connector
from datetime import datetime
import time
import json
import re
import sys
import logging
import configparser
from pathlib import Path

# ===== ĐỌC CẤU HÌNH TỪ FILE =====
def load_config():
    """Đọc cấu hình từ config.ini"""
    config = configparser.ConfigParser()
    config_file = Path(__file__).parent / 'config.ini'
    
    if not config_file.exists():
        print("❌ ERROR: Không tìm thấy config.ini")
        print("   Tạo file config.ini từ config.ini.example")
        sys.exit(1)
    
    config.read(config_file, encoding='utf-8')
    return config

CONFIG = load_config()

# ===== CẤU HÌNH DATABASE =====
DB_CONFIG = {
    'host': CONFIG.get('DATABASE', 'host'),
    'port': CONFIG.getint('DATABASE', 'port'),
    'user': CONFIG.get('DATABASE', 'user'),
    'password': CONFIG.get('DATABASE', 'password'),
    'database': CONFIG.get('DATABASE', 'database'),
    'charset': CONFIG.get('DATABASE', 'charset')
}

# ===== CẤU HÌNH SCRAPER =====
HEADERS = {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
    'Accept': 'application/json, text/plain, */*',
    'Accept-Language': 'vi-VN,vi;q=0.9,en-US;q=0.8,en;q=0.7',
    'Referer': 'https://tiki.vn/',
}

DELAY_BETWEEN_REQUESTS = CONFIG.getint('SCRAPER', 'delay_between_requests', fallback=2)
REQUEST_TIMEOUT = CONFIG.getint('SCRAPER', 'request_timeout', fallback=10)
MAX_RETRIES = CONFIG.getint('SCRAPER', 'max_retries', fallback=3)
RETRY_DELAY = CONFIG.getint('SCRAPER', 'retry_delay', fallback=60)

# ===== CẤU HÌNH LOGGING =====
LOG_DIR = Path(__file__).parent / CONFIG.get('LOGGING', 'log_dir', fallback='logs')
LOG_DIR.mkdir(exist_ok=True)

log_file = LOG_DIR / f"scraper_{datetime.now().strftime('%Y%m%d')}.log"

logging.basicConfig(
    level=CONFIG.get('LOGGING', 'log_level', fallback='INFO'),
    format=CONFIG.get('LOGGING', 'log_format', fallback='%(asctime)s - %(levelname)s - %(message)s'),
    handlers=[
        logging.FileHandler(log_file, encoding='utf-8'),
        logging.StreamHandler(sys.stdout)
    ]
)

logger = logging.getLogger(__name__)


class TikiScraper:
    def __init__(self):
        self.db_connection = None
        self.cursor = None
        self.stats = {
            'total': 0,
            'success': 0,
            'failed': 0,
            'skipped': 0
        }
        logger.info("="*60)
        logger.info("TIKI PRICE SCRAPER - Tầng 1 Giai đoạn 2")
        logger.info("="*60)
    
    def connect_db(self):
        """Kết nối đến MySQL database với retry logic"""
        for attempt in range(MAX_RETRIES):
            try:
                logger.info(f"Đang kết nối database (lần thử {attempt + 1}/{MAX_RETRIES})...")
                self.db_connection = mysql.connector.connect(**DB_CONFIG)
                self.cursor = self.db_connection.cursor(dictionary=True)
                logger.info("✓ Kết nối database thành công!")
                return True
                
            except mysql.connector.Error as err:
                logger.error(f"✗ Lỗi kết nối database: {err}")
                
                if attempt < MAX_RETRIES - 1:
                    logger.warning(f"Đợi {RETRY_DELAY}s trước khi thử lại...")
                    time.sleep(RETRY_DELAY)
                else:
                    logger.error("Đã thử kết nối tối đa, dừng script")
                    return False
        
        return False
    
    def close_db(self):
        """Đóng kết nối database"""
        if self.cursor:
            self.cursor.close()
        if self.db_connection:
            self.db_connection.close()
        logger.info("✓ Đã đóng kết nối database")
    
    def get_all_products(self):
        """Lấy tất cả sản phẩm từ database"""
        try:
            query = "SELECT product_id, name, url, source FROM product"
            self.cursor.execute(query)
            products = self.cursor.fetchall()
            logger.info(f"✓ Tìm thấy {len(products)} sản phẩm trong database")
            return products
        except mysql.connector.Error as err:
            logger.error(f"✗ Lỗi truy vấn database: {err}")
            return []
    
    def extract_product_id_from_url(self, url):
        """Trích xuất product_id từ URL Tiki"""
        # URL format: https://tiki.vn/...-p12345678.html
        match = re.search(r'-p(\d+)\.html', url)
        if match:
            return match.group(1)
        
        # Hoặc format khác: spid=12345678
        match = re.search(r'spid=(\d+)', url)
        if match:
            return match.group(1)
        
        return None
    
    def scrape_tiki_product(self, tiki_product_id):
        """
        Cào thông tin sản phẩm từ Tiki API với retry logic
        Returns: dict với price, original_price, deal_type hoặc None nếu lỗi
        """
        for attempt in range(MAX_RETRIES):
            try:
                # Thử API endpoint chính thức
                api_url = f"https://tiki.vn/api/v2/products/{tiki_product_id}"
                
                response = requests.get(api_url, headers=HEADERS, timeout=REQUEST_TIMEOUT)
                
                if response.status_code == 200:
                    data = response.json()
                    
                    # Lấy thông tin giá
                    price = data.get('price')
                    original_price = data.get('original_price', price)
                    
                    # Xác định deal_type
                    deal_type = 'NORMAL'
                    badges = data.get('badges_new', [])
                    for badge in badges:
                        badge_code = badge.get('code', '').upper()
                        if 'FLASH' in badge_code:
                            deal_type = 'FLASH_SALE'
                            break
                        elif 'HOT' in badge_code or 'DEAL' in badge_code:
                            deal_type = 'HOT_DEAL'
                            break
                        elif 'TREND' in badge_code:
                            deal_type = 'TRENDING'
                            break
                    
                    # Kiểm tra discount
                    if original_price and price and price < original_price:
                        discount_percent = ((original_price - price) / original_price) * 100
                        if discount_percent >= 30 and deal_type == 'NORMAL':
                            deal_type = 'HOT_DEAL'
                    
                    return {
                        'price': price,
                        'original_price': original_price,
                        'currency': 'VND',
                        'deal_type': deal_type
                    }
                
                elif response.status_code == 404:
                    logger.warning(f"  ! Sản phẩm không tồn tại (404)")
                    return None
                
                else:
                    logger.warning(f"  ! API trả về status code: {response.status_code}")
                    
                    if attempt < MAX_RETRIES - 1:
                        logger.warning(f"  Đợi {RETRY_DELAY}s trước khi thử lại...")
                        time.sleep(RETRY_DELAY)
                        continue
                    return None
                    
            except requests.exceptions.Timeout:
                logger.warning(f"  ! Timeout khi gọi API (lần {attempt + 1}/{MAX_RETRIES})")
                if attempt < MAX_RETRIES - 1:
                    logger.warning(f"  Đợi {RETRY_DELAY}s trước khi thử lại...")
                    time.sleep(RETRY_DELAY)
                else:
                    return None
                    
            except requests.exceptions.RequestException as e:
                logger.error(f"  ! Lỗi request: {e}")
                if attempt < MAX_RETRIES - 1:
                    logger.warning(f"  Đợi {RETRY_DELAY}s trước khi thử lại...")
                    time.sleep(RETRY_DELAY)
                else:
                    return None
                    
            except Exception as e:
                logger.error(f"  ! Lỗi không xác định: {e}")
                return None
        
        return None
    
    def save_price_history(self, product_id, price_data):
        """Lưu thông tin giá vào bảng price_history"""
        try:
            query = """
                INSERT INTO price_history 
                (product_id, price, original_price, currency, deal_type, recorded_at)
                VALUES (%s, %s, %s, %s, %s, %s)
            """
            values = (
                product_id,
                price_data['price'],
                price_data['original_price'],
                price_data['currency'],
                price_data['deal_type'],
                datetime.now()
            )
            
            self.cursor.execute(query, values)
            self.db_connection.commit()
            return True
            
        except mysql.connector.Error as err:
            logger.error(f"  ✗ Lỗi lưu database: {err}")
            return False
    
    def log_scrape_session(self):
        """Ghi log vào bảng scrape_log"""
        try:
            query = """
                INSERT INTO scrape_log 
                (scrape_date, source, total_products, status, notes)
                VALUES (%s, %s, %s, %s, %s)
            """
            status = 'SUCCESS' if self.stats['failed'] == 0 else 'PARTIAL_SUCCESS' if self.stats['success'] > 0 else 'FAILED'
            notes = f"Success: {self.stats['success']}, Failed: {self.stats['failed']}, Skipped: {self.stats['skipped']}"
            
            values = (
                datetime.now(),
                'tiki',
                self.stats['total'],
                status,
                notes
            )
            
            self.cursor.execute(query, values)
            self.db_connection.commit()
            logger.info("✓ Đã ghi log vào database")
            
        except mysql.connector.Error as err:
            logger.error(f"✗ Lỗi ghi log: {err}")
    
    def run(self):
        """Chạy scraper chính"""
        start_time = datetime.now()
        
        # Kết nối database
        if not self.connect_db():
            logger.error("Không thể kết nối database, dừng script")
            return False
        
        # Lấy danh sách sản phẩm
        products = self.get_all_products()
        if not products:
            logger.error("Không có sản phẩm nào để cào!")
            self.close_db()
            return False
        
        self.stats['total'] = len(products)
        
        logger.info("─"*60)
        logger.info(f"Bắt đầu cào {len(products)} sản phẩm...")
        logger.info("─"*60)
        
        # Cào từng sản phẩm
        for idx, product in enumerate(products, 1):
            product_id = product['product_id']
            product_name = product['name']
            product_url = product['url']
            
            logger.info(f"[{idx}/{len(products)}] {product_name[:50]}...")
            
            # Trích xuất Tiki product ID từ URL
            tiki_id = self.extract_product_id_from_url(product_url)
            
            if not tiki_id:
                logger.warning(f"  ! Không thể trích xuất Tiki ID từ URL")
                self.stats['skipped'] += 1
                continue
            
            # Cào dữ liệu từ Tiki
            price_data = self.scrape_tiki_product(tiki_id)
            
            if price_data:
                # Lưu vào database
                if self.save_price_history(product_id, price_data):
                    price_str = f"{price_data['price']:,.0f}đ"
                    if price_data['original_price'] != price_data['price']:
                        price_str += f" (gốc: {price_data['original_price']:,.0f}đ)"
                    logger.info(f"  ✓ Giá: {price_str} | Deal: {price_data['deal_type']}")
                    self.stats['success'] += 1
                else:
                    self.stats['failed'] += 1
            else:
                logger.warning(f"  ✗ Không cào được dữ liệu")
                self.stats['failed'] += 1
            
            # Delay để tránh bị block
            if idx < len(products):
                time.sleep(DELAY_BETWEEN_REQUESTS)
        
        # Ghi log
        self.log_scrape_session()
        
        # Thống kê
        end_time = datetime.now()
        duration = (end_time - start_time).total_seconds()
        
        logger.info("="*60)
        logger.info("KẾT QUẢ SCRAPING:")
        logger.info("="*60)
        logger.info(f"  Tổng số sản phẩm:      {self.stats['total']}")
        logger.info(f"  ✓ Thành công:          {self.stats['success']} ({self.stats['success']/self.stats['total']*100:.1f}%)")
        logger.info(f"  ✗ Thất bại:            {self.stats['failed']}")
        logger.info(f"  ! Bỏ qua:              {self.stats['skipped']}")
        logger.info(f"  ⏱ Thời gian:           {duration:.1f}s ({duration/60:.1f} phút)")
        logger.info("="*60)
        
        # Đóng kết nối
        self.close_db()
        
        # Return True nếu thành công hoàn toàn hoặc một phần
        return self.stats['success'] > 0


def main():
    """Main function"""
    scraper = TikiScraper()
    success = scraper.run()
    
    # Exit code: 0 = success, 1 = failed
    sys.exit(0 if success else 1)


if __name__ == "__main__":
    main()

