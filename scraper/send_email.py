#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Gửi email thông báo kết quả scraping
Sử dụng: python send_email.py [success|failed] [optional_message]
"""

import sys
import smtplib
import configparser
from pathlib import Path
from email.mime.text import MIMEText
from email.mime.multipart import MIMEMultipart
from datetime import datetime


def load_config():
    """Đọc cấu hình từ config.ini"""
    config = configparser.ConfigParser()
    config_file = Path(__file__).parent / 'config.ini'
    
    if not config_file.exists():
        print(f"❌ Không tìm thấy file config.ini")
        return None
    
    config.read(config_file, encoding='utf-8')
    return config


def create_email_body(status, stats=None, error_message=None):
    """Tạo nội dung email HTML"""
    
    if status == 'success':
        icon = '✅'
        title = 'CÀO DỮ LIỆU THÀNH CÔNG'
        color = '#4CAF50'
        message = f"""
        <p>Hệ thống đã hoàn thành việc cào dữ liệu giá sản phẩm từ Tiki.</p>
        
        <h3>📊 Thống kê:</h3>
        <ul>
            <li><strong>Tổng số sản phẩm:</strong> {stats.get('total', 0)}</li>
            <li><strong>Thành công:</strong> {stats.get('success', 0)} ({stats.get('success', 0)/stats.get('total', 1)*100:.1f}%)</li>
            <li><strong>Thất bại:</strong> {stats.get('failed', 0)}</li>
            <li><strong>Bỏ qua:</strong> {stats.get('skipped', 0)}</li>
        </ul>
        
        <p>Dữ liệu đã được cập nhật vào database <code>price_insight</code>.</p>
        """
    else:
        icon = '❌'
        title = 'CÀO DỮ LIỆU THẤT BẠI'
        color = '#F44336'
        message = f"""
        <p>Hệ thống gặp lỗi khi cào dữ liệu từ Tiki.</p>
        
        <h3>⚠️ Chi tiết lỗi:</h3>
        <div style="background-color: #fff3cd; padding: 10px; border-left: 4px solid #ffc107;">
            <code>{error_message or 'Không có thông tin chi tiết'}</code>
        </div>
        
        <p>Vui lòng kiểm tra:</p>
        <ul>
            <li>XAMPP/MySQL đang chạy</li>
            <li>Kết nối Internet ổn định</li>
            <li>Log file trong thư mục <code>logs/</code></li>
        </ul>
        """
    
    html = f"""
    <!DOCTYPE html>
    <html>
    <head>
        <meta charset="utf-8">
        <style>
            body {{ font-family: 'Segoe UI', Arial, sans-serif; line-height: 1.6; color: #333; }}
            .container {{ max-width: 600px; margin: 0 auto; padding: 20px; }}
            .header {{ background: {color}; color: white; padding: 20px; text-align: center; border-radius: 5px 5px 0 0; }}
            .content {{ background: #f9f9f9; padding: 20px; border: 1px solid #ddd; border-top: none; }}
            .footer {{ background: #333; color: #aaa; padding: 15px; text-align: center; font-size: 12px; border-radius: 0 0 5px 5px; }}
            code {{ background: #f4f4f4; padding: 2px 6px; border-radius: 3px; font-family: 'Courier New', monospace; }}
            h3 {{ color: {color}; margin-top: 20px; }}
            ul {{ background: white; padding: 15px 30px; border-radius: 5px; }}
        </style>
    </head>
    <body>
        <div class="container">
            <div class="header">
                <h1>{icon} {title}</h1>
            </div>
            <div class="content">
                {message}
                <p style="margin-top: 30px; color: #666;">
                    <strong>Thời gian:</strong> {datetime.now().strftime('%d/%m/%Y %H:%M:%S')}<br>
                    <strong>Hệ thống:</strong> Price Tracker - Automated Scraper
                </p>
            </div>
            <div class="footer">
                <p>Email tự động từ hệ thống Price Tracker<br>
                BTL Lập trình mạng - Nhóm 19</p>
            </div>
        </div>
    </body>
    </html>
    """
    
    return html


def send_email(config, status, stats=None, error_message=None):
    """Gửi email thông báo"""
    
    # Kiểm tra email có được bật không
    if not config.getboolean('EMAIL', 'enabled', fallback=False):
        print("ℹ️  Email notification bị tắt trong config")
        return True
    
    try:
        # Thông tin email
        sender_email = config.get('EMAIL', 'sender_email')
        sender_password = config.get('EMAIL', 'sender_password')
        sender_name = config.get('EMAIL', 'sender_name', fallback='Price Tracker')
        recipients = [email.strip() for email in config.get('EMAIL', 'recipients').split(',')]
        
        # Tạo subject
        if status == 'success':
            subject = config.get('EMAIL', 'subject_success')
        else:
            subject = config.get('EMAIL', 'subject_failed')
        
        # Tạo message
        msg = MIMEMultipart('alternative')
        msg['From'] = f"{sender_name} <{sender_email}>"
        msg['To'] = ', '.join(recipients)
        msg['Subject'] = subject
        
        # Tạo nội dung email
        html_body = create_email_body(status, stats, error_message)
        msg.attach(MIMEText(html_body, 'html', 'utf-8'))
        
        # Gửi email
        print(f"📧 Đang gửi email đến {len(recipients)} người nhận...")
        
        smtp_server = config.get('EMAIL', 'smtp_server')
        smtp_port = config.getint('EMAIL', 'smtp_port')
        
        with smtplib.SMTP(smtp_server, smtp_port) as server:
            server.starttls()
            server.login(sender_email, sender_password)
            server.send_message(msg)
        
        print(f"✅ Đã gửi email thành công đến: {', '.join(recipients)}")
        return True
        
    except Exception as e:
        print(f"❌ Lỗi gửi email: {e}")
        print(f"   Kiểm tra:")
        print(f"   1. Email/password đúng chưa?")
        print(f"   2. Kết nối Internet ổn định chưa?")
        print(f"   3. SMTP server/port đúng chưa?")
        return False


def main():
    """Main function"""
    config = load_config()
    if not config:
        sys.exit(1)
    
    # Lấy status từ argument
    if len(sys.argv) < 2:
        print("Usage: python send_email.py [success|failed] [optional_message]")
        sys.exit(1)
    
    status = sys.argv[1].lower()
    error_message = sys.argv[2] if len(sys.argv) > 2 else None
    
    # Đọc stats từ log file mới nhất (nếu có)
    stats = None
    try:
        from pathlib import Path
        log_dir = Path(__file__).parent / config.get('LOGGING', 'log_dir', fallback='logs')
        
        # Tìm file log mới nhất
        log_files = sorted(log_dir.glob('scraper_*.log'), key=lambda x: x.stat().st_mtime, reverse=True)
        
        if log_files:
            latest_log = log_files[0]
            
            # Parse stats từ log
            with open(latest_log, 'r', encoding='utf-8') as f:
                content = f.read()
                
                # Tìm dòng thống kê
                import re
                total_match = re.search(r'Tổng số sản phẩm:\s+(\d+)', content)
                success_match = re.search(r'✓ Thành công:\s+(\d+)', content)
                failed_match = re.search(r'✗ Thất bại:\s+(\d+)', content)
                skipped_match = re.search(r'! Bỏ qua:\s+(\d+)', content)
                
                if total_match and success_match:
                    stats = {
                        'total': int(total_match.group(1)),
                        'success': int(success_match.group(1)),
                        'failed': int(failed_match.group(1)) if failed_match else 0,
                        'skipped': int(skipped_match.group(1)) if skipped_match else 0
                    }
    except Exception as e:
        print(f"⚠️  Không đọc được stats từ log: {e}")
    
    # Fallback stats nếu không đọc được
    if not stats:
        stats = {
            'total': 0,
            'success': 0,
            'failed': 0,
            'skipped': 0
        }
    
    # Gửi email
    if send_email(config, status, stats, error_message):
        sys.exit(0)
    else:
        sys.exit(1)


if __name__ == "__main__":
    main()
