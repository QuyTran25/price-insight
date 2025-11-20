# Kiểm tra kết nối MySQL Database
import sys
import configparser
from pathlib import Path

try:
    import mysql.connector
except ImportError:
    print("❌ ERROR: Chưa cài đặt mysql-connector-python")
    print("   Chạy lệnh: pip install mysql-connector-python")
    sys.exit(1)


def load_config():
    """Đọc cấu hình từ config.ini"""
    config = configparser.ConfigParser()
    config_file = Path(__file__).parent / 'config.ini'
    
    if not config_file.exists():
        print(f"❌ ERROR: Không tìm thấy file config.ini")
        print(f"   Tạo file config.ini từ config.ini.example")
        sys.exit(1)
    
    config.read(config_file, encoding='utf-8')
    return config


def check_mysql_connection(config):
    """Kiểm tra kết nối MySQL"""
    print("🔍 Đang kiểm tra kết nối MySQL...")
    
    try:
        db_config = {
            'host': config.get('DATABASE', 'host'),
            'port': config.getint('DATABASE', 'port'),
            'database': config.get('DATABASE', 'database'),
            'user': config.get('DATABASE', 'user'),
            'password': config.get('DATABASE', 'password'),
            'charset': config.get('DATABASE', 'charset')
        }
        
        # Thử kết nối
        connection = mysql.connector.connect(**db_config)
        cursor = connection.cursor()
        
        # Kiểm tra database
        cursor.execute("SELECT DATABASE()")
        db_name = cursor.fetchone()[0]
        
        # Kiểm tra bảng product
        cursor.execute("SELECT COUNT(*) FROM product")
        product_count = cursor.fetchone()[0]
        
        # Kiểm tra bảng price_history
        cursor.execute("SELECT COUNT(*) FROM price_history")
        history_count = cursor.fetchone()[0]
        
        cursor.close()
        connection.close()
        
        print("✅ Kết nối MySQL thành công!")
        print(f"   Database: {db_name}")
        print(f"   Sản phẩm: {product_count}")
        print(f"   Lịch sử giá: {history_count}")
        
        return True
        
    except mysql.connector.Error as err:
        print(f"❌ Lỗi kết nối MySQL: {err}")
        print(f"   Kiểm tra:")
        print(f"   1. XAMPP đã bật MySQL chưa?")
        print(f"   2. Database '{config.get('DATABASE', 'database')}' đã tạo chưa?")
        print(f"   3. Username/password đúng chưa?")
        return False
    
    except Exception as e:
        print(f"❌ Lỗi không xác định: {e}")
        return False


def main():
    """Main function"""
    config = load_config()
    
    if check_mysql_connection(config):
        sys.exit(0)  # Success
    else:
        sys.exit(1)  # Failed


if __name__ == "__main__":
    main()
