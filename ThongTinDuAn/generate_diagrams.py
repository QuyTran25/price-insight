#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Generate Architecture Diagrams for Price Tracker Project
Tạo các sơ đồ kiến trúc cho dự án Price Tracker
"""

import matplotlib.pyplot as plt
import matplotlib.patches as mpatches
from matplotlib.patches import FancyBboxPatch, FancyArrowPatch, Circle
import numpy as np

# Set font hỗ trợ tiếng Việt
plt.rcParams['font.family'] = 'DejaVu Sans'
plt.rcParams['font.size'] = 10

def create_tcp_protocol_diagram():
    """Tạo sơ đồ giao thức TCP trong dự án"""
    fig, ax = plt.subplots(1, 1, figsize=(14, 10))
    ax.set_xlim(0, 14)
    ax.set_ylim(0, 10)
    ax.axis('off')
    
    # Title
    ax.text(7, 9.5, 'SO DO GIAO THUC TCP TRONG PRICE TRACKER SYSTEM', 
            ha='center', va='center', fontsize=16, fontweight='bold',
            bbox=dict(boxstyle='round,pad=0.5', facecolor='lightblue', edgecolor='black', linewidth=2))
    
    # Client Box
    client_box = FancyBboxPatch((1, 6.5), 2.5, 1.5, 
                                boxstyle="round,pad=0.1", 
                                facecolor='#E8F5E9', edgecolor='#4CAF50', linewidth=2)
    ax.add_patch(client_box)
    ax.text(2.25, 7.5, 'CLIENT', ha='center', va='center', fontweight='bold', fontsize=11)
    ax.text(2.25, 7.1, 'Java Desktop', ha='center', va='center', fontsize=9)
    ax.text(2.25, 6.8, 'NetworkClient', ha='center', va='center', fontsize=8, style='italic')
    
    # Server Box
    server_box = FancyBboxPatch((10.5, 6.5), 2.5, 1.5,
                                boxstyle="round,pad=0.1",
                                facecolor='#E3F2FD', edgecolor='#2196F3', linewidth=2)
    ax.add_patch(server_box)
    ax.text(11.75, 7.5, 'SERVER', ha='center', va='center', fontweight='bold', fontsize=11)
    ax.text(11.75, 7.1, 'PriceTrackerServer', ha='center', va='center', fontsize=9)
    ax.text(11.75, 6.8, 'Port: 8888 (SSL)', ha='center', va='center', fontsize=8, style='italic')
    
    # TCP Connection Steps
    steps = [
        (5, 'SYN (Seq=x)', '#FF9800', True),
        (4.5, 'SYN-ACK (Seq=y, Ack=x+1)', '#4CAF50', False),
        (4, 'ACK (Ack=y+1)', '#FF9800', True),
        (3.3, '-- TCP CONNECTION ESTABLISHED --', '#9C27B0', None),
        (2.8, 'Encrypted Request (AES-256/GCM)', '#FF5722', True),
        (2.3, 'Encrypted Response (AES-256/GCM)', '#03A9F4', False),
        (1.8, 'Data Transfer...', '#607D8B', None),
        (1.3, 'FIN (Close connection)', '#F44336', True),
        (0.8, 'ACK', '#F44336', False),
    ]
    
    for y, text, color, direction in steps:
        if direction is True:  # Client -> Server
            arrow = FancyArrowPatch((3.6, y), (10.4, y),
                                  arrowstyle='->', mutation_scale=20, 
                                  linewidth=2, color=color)
            ax.add_patch(arrow)
            ax.text(7, y+0.15, text, ha='center', va='bottom', 
                   fontsize=9, bbox=dict(boxstyle='round,pad=0.3', 
                   facecolor='white', edgecolor=color, linewidth=1.5))
        elif direction is False:  # Server -> Client
            arrow = FancyArrowPatch((10.4, y), (3.6, y),
                                  arrowstyle='->', mutation_scale=20,
                                  linewidth=2, color=color)
            ax.add_patch(arrow)
            ax.text(7, y+0.15, text, ha='center', va='bottom',
                   fontsize=9, bbox=dict(boxstyle='round,pad=0.3',
                   facecolor='white', edgecolor=color, linewidth=1.5))
        else:  # Info line
            ax.plot([3.6, 10.4], [y, y], '--', color=color, linewidth=1.5, alpha=0.5)
            ax.text(7, y, text, ha='center', va='center',
                   fontsize=9, fontweight='bold', color=color)
    
    # Legend
    legend_y = 0.3
    legend_elements = [
        mpatches.Patch(facecolor='#FF9800', edgecolor='black', label='Client -> Server'),
        mpatches.Patch(facecolor='#4CAF50', edgecolor='black', label='Server -> Client'),
        mpatches.Patch(facecolor='#9C27B0', edgecolor='black', label='Connection State'),
    ]
    ax.legend(handles=legend_elements, loc='lower center', ncol=3, 
             frameon=True, fancybox=True, shadow=True)
    
    plt.tight_layout()
    plt.savefig('images/tcp_protocol_diagram.png', dpi=300, bbox_inches='tight')
    print("✓ Created: images/tcp_protocol_diagram.png")
    plt.close()


def create_system_architecture_diagram():
    """Tạo sơ đồ kiến trúc tổng thể hệ thống"""
    fig, ax = plt.subplots(1, 1, figsize=(16, 12))
    ax.set_xlim(0, 16)
    ax.set_ylim(0, 12)
    ax.axis('off')
    
    # Title
    ax.text(8, 11.5, 'KIEN TRUC TONG THE - PRICE TRACKER SYSTEM',
            ha='center', va='center', fontsize=18, fontweight='bold',
            bbox=dict(boxstyle='round,pad=0.6', facecolor='#FFEB3B', 
                     edgecolor='black', linewidth=3))
    
    # Layer 1: Frontend
    frontend_box = FancyBboxPatch((1, 9), 6, 1.5,
                                 boxstyle="round,pad=0.15",
                                 facecolor='#FFF3E0', edgecolor='#FF9800', linewidth=2.5)
    ax.add_patch(frontend_box)
    ax.text(4, 10.2, 'FRONTEND LAYER', ha='center', va='center', 
           fontweight='bold', fontsize=13, color='#E65100')
    ax.text(4, 9.7, 'HTML5 | CSS3 | JavaScript (Vanilla)', 
           ha='center', va='center', fontsize=10)
    ax.text(4, 9.3, 'Port: 8080 (HTTP)', ha='center', va='center', 
           fontsize=9, style='italic')
    
    # Layer 2: HTTP Server
    http_box = FancyBboxPatch((9, 9), 6, 1.5,
                             boxstyle="round,pad=0.15",
                             facecolor='#E1F5FE', edgecolor='#03A9F4', linewidth=2.5)
    ax.add_patch(http_box)
    ax.text(12, 10.2, 'HTTP SERVER', ha='center', va='center',
           fontweight='bold', fontsize=13, color='#01579B')
    ax.text(12, 9.7, 'SimpleHttpServer.java', ha='center', va='center', fontsize=10)
    ax.text(12, 9.3, 'API: /search | /deals | /product-detail | /categories',
           ha='center', va='center', fontsize=8, style='italic')
    
    # Arrow Frontend -> HTTP
    arrow1 = FancyArrowPatch((7.1, 9.75), (8.9, 9.75),
                           arrowstyle='<->', mutation_scale=25,
                           linewidth=3, color='#4CAF50')
    ax.add_patch(arrow1)
    ax.text(8, 10.1, 'HTTP\nREST API', ha='center', va='center',
           fontsize=9, fontweight='bold',
           bbox=dict(boxstyle='round,pad=0.3', facecolor='#C8E6C9', 
                    edgecolor='#4CAF50', linewidth=2))
    
    # Layer 3: SSL Server (Optional)
    ssl_box = FancyBboxPatch((1, 6.5), 6, 1.5,
                            boxstyle="round,pad=0.15",
                            facecolor='#F3E5F5', edgecolor='#9C27B0', linewidth=2.5)
    ax.add_patch(ssl_box)
    ax.text(4, 7.75, 'SSL SERVER (Optional)', ha='center', va='center',
           fontweight='bold', fontsize=13, color='#4A148C')
    ax.text(4, 7.3, 'PriceTrackerServer.java', ha='center', va='center', fontsize=10)
    ax.text(4, 6.85, 'Port: 8888 (SSL/TLS) | AES-256/GCM Encryption',
           ha='center', va='center', fontsize=9, style='italic')
    
    # Layer 4: Business Logic
    business_box = FancyBboxPatch((9, 6.5), 6, 1.5,
                                 boxstyle="round,pad=0.15",
                                 facecolor='#FCE4EC', edgecolor='#E91E63', linewidth=2.5)
    ax.add_patch(business_box)
    ax.text(12, 7.75, 'BUSINESS LOGIC LAYER', ha='center', va='center',
           fontweight='bold', fontsize=13, color='#880E4F')
    ax.text(12, 7.3, 'ProductDAO | PriceHistoryDAO | ReviewDAO', 
           ha='center', va='center', fontsize=10)
    ax.text(12, 6.85, 'ClientHandler | Multi-threading (50 threads)',
           ha='center', va='center', fontsize=9, style='italic')
    
    # Arrows to Business Logic
    arrow2 = FancyArrowPatch((12, 8.9), (12, 8.1),
                           arrowstyle='<->', mutation_scale=25,
                           linewidth=3, color='#FF5722')
    ax.add_patch(arrow2)
    
    arrow3 = FancyArrowPatch((7, 7.25), (8.9, 7.25),
                           arrowstyle='<->', mutation_scale=25,
                           linewidth=3, color='#9C27B0')
    ax.add_patch(arrow3)
    
    # Layer 5: Database Layer
    db_box = FancyBboxPatch((4.5, 4), 7, 1.5,
                           boxstyle="round,pad=0.15",
                           facecolor='#E8F5E9', edgecolor='#4CAF50', linewidth=2.5)
    ax.add_patch(db_box)
    ax.text(8, 5.25, 'DATABASE LAYER', ha='center', va='center',
           fontweight='bold', fontsize=13, color='#1B5E20')
    ax.text(8, 4.8, 'MySQL 8.0 | HikariCP Connection Pool', 
           ha='center', va='center', fontsize=10)
    ax.text(8, 4.4, 'Database: price_insight | Port: 3306',
           ha='center', va='center', fontsize=9, style='italic')
    
    # Arrow Business -> Database
    arrow4 = FancyArrowPatch((12, 6.4), (8, 5.6),
                           arrowstyle='<->', mutation_scale=25,
                           linewidth=3, color='#4CAF50')
    ax.add_patch(arrow4)
    ax.text(10.5, 6, 'SQL\nQueries', ha='center', va='center',
           fontsize=9, fontweight='bold',
           bbox=dict(boxstyle='round,pad=0.3', facecolor='#C8E6C9',
                    edgecolor='#4CAF50', linewidth=2))
    
    # Layer 6: Scraper Layer
    scraper_box = FancyBboxPatch((1, 1.5), 6, 1.5,
                                boxstyle="round,pad=0.15",
                                facecolor='#FFF9C4', edgecolor='#FBC02D', linewidth=2.5)
    ax.add_patch(scraper_box)
    ax.text(4, 2.75, 'SCRAPER LAYER (Tier 1)', ha='center', va='center',
           fontweight='bold', fontsize=13, color='#F57F17')
    ax.text(4, 2.3, 'scraper.py (Tiki) | scraper_lazada.py (Lazada)',
           ha='center', va='center', fontsize=10)
    ax.text(4, 1.85, 'Python 3.8+ | Auto: 8h, 16h daily',
           ha='center', va='center', fontsize=9, style='italic')
    
    # Arrow Scraper -> Database
    arrow5 = FancyArrowPatch((6, 3.1), (6.5, 4),
                           arrowstyle='->', mutation_scale=25,
                           linewidth=3, color='#FF9800')
    ax.add_patch(arrow5)
    ax.text(5, 3.5, 'INSERT\nPrice Data', ha='center', va='center',
           fontsize=9, fontweight='bold',
           bbox=dict(boxstyle='round,pad=0.3', facecolor='#FFE0B2',
                    edgecolor='#FF9800', linewidth=2))
    
    # External Services
    tiki_box = FancyBboxPatch((9, 1.5), 2.5, 1,
                             boxstyle="round,pad=0.1",
                             facecolor='#E8EAF6', edgecolor='#3F51B5', linewidth=2)
    ax.add_patch(tiki_box)
    ax.text(10.25, 2.3, 'TIKI.VN', ha='center', va='center',
           fontweight='bold', fontsize=11, color='#1A237E')
    ax.text(10.25, 1.85, 'API', ha='center', va='center', fontsize=9)
    
    lazada_box = FancyBboxPatch((12, 1.5), 2.5, 1,
                               boxstyle="round,pad=0.1",
                               facecolor='#FCE4EC', edgecolor='#E91E63', linewidth=2)
    ax.add_patch(lazada_box)
    ax.text(13.25, 2.3, 'LAZADA.VN', ha='center', va='center',
           fontweight='bold', fontsize=11, color='#880E4F')
    ax.text(13.25, 1.85, 'HTML Scrape', ha='center', va='center', fontsize=9)
    
    # Arrows to External Services
    arrow6 = FancyArrowPatch((7, 2.25), (8.9, 2.25),
                           arrowstyle='->', mutation_scale=20,
                           linewidth=2.5, color='#3F51B5', linestyle='--')
    ax.add_patch(arrow6)
    
    arrow7 = FancyArrowPatch((7, 1.95), (11.9, 1.95),
                           arrowstyle='->', mutation_scale=20,
                           linewidth=2.5, color='#E91E63', linestyle='--')
    ax.add_patch(arrow7)
    
    # Legend
    legend_elements = [
        mpatches.Patch(facecolor='#FFF3E0', edgecolor='#FF9800', label='Presentation Layer'),
        mpatches.Patch(facecolor='#E1F5FE', edgecolor='#03A9F4', label='Application Layer'),
        mpatches.Patch(facecolor='#E8F5E9', edgecolor='#4CAF50', label='Data Layer'),
        mpatches.Patch(facecolor='#FFF9C4', edgecolor='#FBC02D', label='Data Collection'),
    ]
    ax.legend(handles=legend_elements, loc='lower center', ncol=4,
             frameon=True, fancybox=True, shadow=True, fontsize=10)
    
    # Add notes
    ax.text(8, 0.4, 'Note: HikariCP ensures high-performance connection pooling | SSL/TLS for secure communication',
           ha='center', va='center', fontsize=9, style='italic',
           bbox=dict(boxstyle='round,pad=0.4', facecolor='#FFFFCC', 
                    edgecolor='#666666', linewidth=1))
    
    plt.tight_layout()
    plt.savefig('images/system_architecture.png', dpi=300, bbox_inches='tight')
    print("✓ Created: images/system_architecture.png")
    plt.close()


def create_data_flow_diagram():
    """Tạo sơ đồ luồng dữ liệu"""
    fig, ax = plt.subplots(1, 1, figsize=(14, 10))
    ax.set_xlim(0, 14)
    ax.set_ylim(0, 10)
    ax.axis('off')
    
    # Title
    ax.text(7, 9.5, 'LUONG DU LIEU 2-TANG TRONG PRICE TRACKER',
            ha='center', va='center', fontsize=16, fontweight='bold',
            bbox=dict(boxstyle='round,pad=0.5', facecolor='#E1F5FE',
                     edgecolor='black', linewidth=2))
    
    # Tier 1: Background Scraping
    ax.text(7, 8.5, 'TANG 1: BACKGROUND SCRAPING (Python)', 
           ha='center', va='center', fontsize=13, fontweight='bold',
           bbox=dict(boxstyle='round,pad=0.4', facecolor='#FFF9C4',
                    edgecolor='#FBC02D', linewidth=2))
    
    # Tier 1 boxes
    tier1_boxes = [
        (1.5, 6.5, 2, 1, 'Task\nScheduler', '#E3F2FD'),
        (4, 6.5, 2, 1, 'Python\nScraper', '#FFECB3'),
        (6.5, 6.5, 2, 1, 'Tiki/Lazada\nAPI/HTML', '#E8F5E9'),
        (9, 6.5, 2, 1, 'Parse\nData', '#FCE4EC'),
        (11.5, 6.5, 2, 1, 'Save to\nMySQL', '#F3E5F5'),
    ]
    
    for x, y, w, h, text, color in tier1_boxes:
        box = FancyBboxPatch((x, y), w, h, boxstyle="round,pad=0.1",
                            facecolor=color, edgecolor='black', linewidth=2)
        ax.add_patch(box)
        ax.text(x + w/2, y + h/2, text, ha='center', va='center',
               fontsize=10, fontweight='bold')
    
    # Tier 1 arrows
    for i in range(len(tier1_boxes) - 1):
        x1 = tier1_boxes[i][0] + tier1_boxes[i][2]
        x2 = tier1_boxes[i+1][0]
        y = tier1_boxes[i][1] + tier1_boxes[i][3]/2
        arrow = FancyArrowPatch((x1, y), (x2, y),
                              arrowstyle='->', mutation_scale=20,
                              linewidth=2.5, color='#FF9800')
        ax.add_patch(arrow)
    
    ax.text(7, 5.8, '8h, 16h daily | Auto insert price_history',
           ha='center', va='center', fontsize=9, style='italic',
           color='#E65100')
    
    # Tier 2: On-Demand Fetching
    ax.text(7, 5, 'TANG 2: ON-DEMAND FETCHING (Java)', 
           ha='center', va='center', fontsize=13, fontweight='bold',
           bbox=dict(boxstyle='round,pad=0.4', facecolor='#E1F5FE',
                    edgecolor='#03A9F4', linewidth=2))
    
    # Tier 2 flow
    tier2_steps = [
        (1, 3.5, 'User Search'),
        (3, 3.5, 'HTTP Request'),
        (5.5, 3.5, 'Query DB\n(Immediate)'),
        (8, 3.5, 'Return Result'),
        (10.5, 3.5, 'Check Age\n>8h?'),
        (5.5, 2, 'Thread:\nScrape New'),
        (8, 2, 'Update DB'),
        (10.5, 2, 'Auto Refresh\n(5s later)'),
    ]
    
    for x, y, text in tier2_steps[:5]:
        if 'Query' in text or 'Check' in text:
            color = '#C8E6C9'
            edge = '#4CAF50'
        else:
            color = '#BBDEFB'
            edge = '#2196F3'
        
        circle = Circle((x, y), 0.4, facecolor=color, edgecolor=edge, linewidth=2)
        ax.add_patch(circle)
        ax.text(x, y, text, ha='center', va='center', fontsize=8, fontweight='bold')
    
    # Tier 2 arrows (main flow)
    for i in range(len(tier2_steps[:5]) - 1):
        x1, y1 = tier2_steps[i][:2]
        x2, y2 = tier2_steps[i+1][:2]
        arrow = FancyArrowPatch((x1+0.4, y1), (x2-0.4, y2),
                              arrowstyle='->', mutation_scale=15,
                              linewidth=2, color='#2196F3')
        ax.add_patch(arrow)
    
    # Thread flow
    for x, y, text in tier2_steps[5:]:
        circle = Circle((x, y), 0.4, facecolor='#FFCCBC', 
                       edgecolor='#FF5722', linewidth=2)
        ax.add_patch(circle)
        ax.text(x, y, text, ha='center', va='center', fontsize=8, fontweight='bold')
    
    # Thread arrows
    arrow_down = FancyArrowPatch((10.5, 3.1), (5.5, 2.4),
                                arrowstyle='->', mutation_scale=15,
                                linewidth=2, color='#FF5722', linestyle='--')
    ax.add_patch(arrow_down)
    ax.text(8, 2.8, 'If old data', ha='center', va='center',
           fontsize=8, style='italic', color='#FF5722')
    
    arrow_right = FancyArrowPatch((5.9, 2), (7.6, 2),
                                 arrowstyle='->', mutation_scale=15,
                                 linewidth=2, color='#FF5722')
    ax.add_patch(arrow_right)
    
    arrow_right2 = FancyArrowPatch((8.4, 2), (10.1, 2),
                                  arrowstyle='->', mutation_scale=15,
                                  linewidth=2, color='#FF5722')
    ax.add_patch(arrow_right2)
    
    # Info boxes
    info1 = "Tier 1: Xay dung du lieu lich su\n- Chay tu dong 2 lan/ngay\n- ~100+ products"
    ax.text(2, 1, info1, ha='left', va='top', fontsize=9,
           bbox=dict(boxstyle='round,pad=0.5', facecolor='#FFF9C4',
                    edgecolor='#FBC02D', linewidth=2))
    
    info2 = "Tier 2: Du lieu realtime\n- Tra ve ngay lap tuc\n- Update nen neu can"
    ax.text(12, 1, info2, ha='right', va='top', fontsize=9,
           bbox=dict(boxstyle='round,pad=0.5', facecolor='#E1F5FE',
                    edgecolor='#03A9F4', linewidth=2))
    
    plt.tight_layout()
    plt.savefig('images/data_flow_diagram.png', dpi=300, bbox_inches='tight')
    print("✓ Created: images/data_flow_diagram.png")
    plt.close()


def create_database_schema_diagram():
    """Tạo sơ đồ database schema chuyên nghiệp với layout tối ưu"""
    fig, ax = plt.subplots(1, 1, figsize=(18, 12))
    ax.set_xlim(0, 18)
    ax.set_ylim(0, 12)
    ax.axis('off')
    
    # Title với gradient effect
    title_box = FancyBboxPatch((5, 11), 8, 0.7,
                               boxstyle="round,pad=0.1",
                               facecolor='#2E7D32', edgecolor='#1B5E20', linewidth=3)
    ax.add_patch(title_box)
    ax.text(9, 11.35, 'DATABASE SCHEMA - price_insight',
            ha='center', va='center', fontsize=18, fontweight='bold',
            color='white')
    
    # Tables với layout cân đối
    tables = [
        {
            'name': 'PRODUCT',
            'pos': (0.5, 6.2),
            'color': '#4CAF50',
            'fields': [
                ('product_id', 'PK', 'BIGINT'),
                ('group_id', 'FK', 'INT'),
                ('name', '', 'VARCHAR(500)'),
                ('brand', '', 'VARCHAR(100)'),
                ('url', '', 'TEXT'),
                ('image_url', '', 'TEXT'),
                ('description', '', 'TEXT'),
                ('source', '', "VARCHAR(50)"),
                ('is_featured', '', 'BOOLEAN'),
                ('created_at', '', 'DATETIME')
            ]
        },
        {
            'name': 'PRODUCT_GROUP',
            'pos': (0.5, 2.5),
            'color': '#66BB6A',
            'fields': [
                ('group_id', 'PK', 'INT'),
                ('group_name', '', 'VARCHAR(100)'),
                ('description', '', 'TEXT')
            ]
        },
        {
            'name': 'PRICE_HISTORY',
            'pos': (6, 6.2),
            'color': '#FF9800',
            'fields': [
                ('price_id', 'PK', 'BIGINT'),
                ('product_id', 'FK', 'BIGINT'),
                ('price', '', 'DECIMAL(15,2)'),
                ('original_price', '', 'DECIMAL(15,2)'),
                ('currency', '', 'VARCHAR(10)'),
                ('deal_type', '', 'VARCHAR(50)'),
                ('recorded_at', '', 'DATETIME')
            ]
        },
        {
            'name': 'REVIEW',
            'pos': (11.5, 6.2),
            'color': '#2196F3',
            'fields': [
                ('review_id', 'PK', 'BIGINT'),
                ('product_id', 'FK', 'BIGINT'),
                ('reviewer_name', '', 'VARCHAR(255)'),
                ('rating', '', 'INT'),
                ('comment', '', 'TEXT'),
                ('review_date', '', 'DATETIME')
            ]
        },
        {
            'name': 'SCRAPE_LOG',
            'pos': (6, 2.5),
            'color': '#9C27B0',
            'fields': [
                ('log_id', 'PK', 'BIGINT'),
                ('scrape_date', '', 'DATETIME'),
                ('source', '', 'VARCHAR(50)'),
                ('total_products', '', 'INT'),
                ('status', '', 'VARCHAR(50)'),
                ('notes', '', 'TEXT')
            ]
        },
        {
            'name': 'ERROR_LOG',
            'pos': (11.5, 2.5),
            'color': '#F44336',
            'fields': [
                ('error_id', 'PK', 'BIGINT'),
                ('occurred_at', '', 'DATETIME'),
                ('component', '', 'VARCHAR(100)'),
                ('message', '', 'TEXT'),
                ('stacktrace', '', 'TEXT')
            ]
        }
    ]
    
    # Draw tables
    for table in tables:
        x, y = table['pos']
        height = len(table['fields']) * 0.32 + 0.6
        width = 4.5
        
        # Table header với màu riêng
        header = FancyBboxPatch((x, y + height - 0.6), width, 0.6,
                               boxstyle="round,pad=0.05",
                               facecolor=table['color'], 
                               edgecolor='black', linewidth=2.5)
        ax.add_patch(header)
        
        ax.text(x + width/2, y + height - 0.3, table['name'],
               ha='center', va='center', fontsize=13, fontweight='bold',
               color='white')
        
        # Table body
        body = FancyBboxPatch((x, y), width, height - 0.6,
                             boxstyle="square,pad=0",
                             facecolor='#FAFAFA', edgecolor='black', linewidth=2.5)
        ax.add_patch(body)
        
        # Fields với 3 cột: name | key | type
        for i, (field_name, key_type, data_type) in enumerate(table['fields']):
            field_y = y + height - 0.88 - (i * 0.32)
            
            # Column 1: Field name
            if key_type == 'PK':
                name_color = '#C62828'  # Đỏ đậm
                weight = 'bold'
            elif key_type == 'FK':
                name_color = '#1565C0'  # Xanh đậm
                weight = 'bold'
            else:
                name_color = '#212121'
                weight = 'normal'
            
            ax.text(x + 0.12, field_y, field_name, ha='left', va='center',
                   fontsize=9, color=name_color, fontweight=weight)
            
            # Column 2: Key type (PK/FK)
            if key_type:
                key_badge = FancyBboxPatch((x + width - 1.5, field_y - 0.1), 0.5, 0.22,
                                          boxstyle="round,pad=0.02",
                                          facecolor='#FFE082' if key_type == 'PK' else '#81D4FA',
                                          edgecolor='black', linewidth=0.8)
                ax.add_patch(key_badge)
                ax.text(x + width - 1.25, field_y, key_type, ha='center', va='center',
                       fontsize=7, fontweight='bold', color='#000')
            
            # Column 3: Data type
            ax.text(x + width - 0.12, field_y, data_type, ha='right', va='center',
                   fontsize=7, color='#616161', style='italic')
    
    # Foreign key relationships - RÌA ĐẾN RÌA, VÒNG QUA TRÊN BẢNG CAM
    # PRODUCT: rìa phải x=5.0
    # REVIEW: rìa trái x=11.5, top≈8.72
    # PRICE_HISTORY: top=9.04 → Mũi tên phải đi cao hơn
    relationships = [
        # (start_pos, end_pos, label, color, curve_radius)
        ((5.0, 7.8), (6, 7.8), 'product_id', '#FF6F00', 0.1),      # Cam: PRODUCT → PRICE_HISTORY
        ((5.0, 9.6), (11.5, 8.7), 'product_id', '#0277BD', 0.4),   # Xanh: rìa đến rìa, vòng qua trên
        ((1.0, 6.2), (1.0, 4.06), 'group_id', '#388E3C', 0.1),     # Lá: PRODUCT → PRODUCT_GROUP
    ]
    
    for (x1, y1), (x2, y2), label, color, curve_rad in relationships:
        # Vẽ mũi tên với độ cong
        arrow = FancyArrowPatch((x1, y1), (x2, y2),
                              arrowstyle='->', mutation_scale=20,
                              linewidth=3, color=color,
                              connectionstyle=f"arc3,rad={curve_rad}",
                              alpha=0.9)
        ax.add_patch(arrow)
        
        # Vị trí label
        mid_x = (x1 + x2) / 2
        mid_y = (y1 + y2) / 2
        
        # Điều chỉnh offset cho từng mũi tên
        if color == '#0277BD':  # Xanh dương - label ở giữa đường cong
            offset_y = 0.8  # Lên cao để rõ ràng
            offset_x = 0
        elif color == '#FF6F00':  # Cam - label bên dưới
            offset_y = -0.3
            offset_x = 0
        elif color == '#388E3C':  # Lá - label sang trái
            offset_y = 0
            offset_x = -0.7
        else:
            offset_y = 0.25
            offset_x = 0
        
        # Vẽ label
        ax.text(mid_x + offset_x, mid_y + offset_y, label, 
               ha='center', va='center',
               fontsize=9, fontweight='bold', style='italic',
               bbox=dict(boxstyle='round,pad=0.35', facecolor='white',
                        edgecolor=color, linewidth=2, alpha=0.95))
    
    # Legend box với icon
    legend_box = FancyBboxPatch((4.5, 0.5), 9, 1.2,
                               boxstyle="round,pad=0.15",
                               facecolor='#FFF9C4', 
                               edgecolor='#F57F17', linewidth=2)
    ax.add_patch(legend_box)
    
    # Legend content (không dùng emoji)
    legend_items = [
        "[PK] = Primary Key (Khoa chinh)  |  [FK] = Foreign Key (Khoa ngoai)",
        "Database: price_insight  |  Engine: InnoDB  |  Charset: utf8mb4_general_ci",
        "Total: 6 tables | 112 products (86 Tiki + 26 Lazada) | 2,830+ price records"
    ]
    
    for i, text in enumerate(legend_items):
        ax.text(9, 1.4 - (i * 0.28), text, ha='center', va='center',
               fontsize=9, fontweight='normal' if i > 0 else 'bold')
    
    # Corner statistics boxes
    # Left corner - Database info
    db_box = FancyBboxPatch((0.3, 0.05), 3.5, 0.35,
                           boxstyle="round,pad=0.08",
                           facecolor='#E3F2FD', edgecolor='#1976D2', linewidth=2)
    ax.add_patch(db_box)
    ax.text(2.05, 0.225, "MySQL 8.0 | Port: 3306 | Max Connections: 200",
           ha='center', va='center', fontsize=8, fontweight='bold', color='#0D47A1')
    
    # Right corner - Backup info
    backup_box = FancyBboxPatch((14.2, 0.05), 3.5, 0.35,
                               boxstyle="round,pad=0.08",
                               facecolor='#FFF3E0', edgecolor='#F57C00', linewidth=2)
    ax.add_patch(backup_box)
    ax.text(15.95, 0.225, "Backup: Daily 2:00 AM | Retention: 30 days",
           ha='center', va='center', fontsize=8, fontweight='bold', color='#E65100')
    
    plt.tight_layout()
    plt.savefig('images/database_schema.png', dpi=300, bbox_inches='tight', facecolor='white')
    print("✓ Created: images/database_schema.png")
    plt.close()


if __name__ == "__main__":
    print("=" * 60)
    print("GENERATING DIAGRAMS FOR PRICE TRACKER PROJECT")
    print("=" * 60)
    print()
    
    create_tcp_protocol_diagram()
    create_system_architecture_diagram()
    create_data_flow_diagram()
    create_database_schema_diagram()
    
    print()
    print("=" * 60)
    print("ALL DIAGRAMS GENERATED SUCCESSFULLY!")
    print("=" * 60)
    print("\nGenerated files:")
    print("  - images/tcp_protocol_diagram.png")
    print("  - images/system_architecture.png")
    print("  - images/data_flow_diagram.png")
    print("  - images/database_schema.png")
