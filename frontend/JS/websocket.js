/**
 * WebSocket Client cho Real-time Price Updates
 * Kết nối tới server port 8081 (local) hoặc production URL
 */

class PriceWebSocketClient {
    constructor() {
        this.ws = null;
        this.es = null; // EventSource instance (for SSE)
        this.reconnectDelay = 3000; // 3 giây
        this.maxReconnectDelay = 30000; // 30 giây max
        this.reconnectAttempts = 0;
        this.isManualClose = false;
        this.listeners = {
            connected: [],
            disconnected: [],
            priceUpdate: [],
            error: []
        };
    }

    /**
     * Kết nối tới WebSocket server
     */
    connect() {
        try {
            // Sử dụng CONFIG từ config.js
            const isProduction = (typeof CONFIG !== 'undefined') ? CONFIG.isProduction : false;
            const useSSE = isProduction; // Use SSE in production (single-port)

            if (useSSE) {
                const eventsUrl = (typeof CONFIG !== 'undefined') ? (CONFIG.API_BASE_URL + '/events') : '/events';
                console.log('[SSE] Đang kết nối tới ' + eventsUrl + '...');

                this.es = new EventSource(eventsUrl);

                this.es.onopen = (event) => {
                    console.log('[SSE] ✅ Kết nối thành công!');
                    this.reconnectDelay = 3000;
                    this.reconnectAttempts = 0;
                    this.notifyListeners('connected', event);
                };

                this.es.onmessage = (event) => {
                    try {
                        const data = JSON.parse(event.data);
                        console.log('[SSE] 📥 Nhận message:', data);
                        if (data.type === 'price_update') {
                            this.handlePriceUpdate(data);
                        } else if (data.type === 'server_shutdown') {
                            console.warn('[SSE] Server shutting down:', data.message);
                            this.showToast('⚠️ Server đang bảo trì', 'warning');
                        } else {
                            console.log('[SSE] Unknown message type:', data.type);
                        }
                    } catch (err) {
                        console.error('[SSE] Lỗi parse message:', err);
                    }
                };

                this.es.onerror = (err) => {
                    console.error('[SSE] ⚠️ Lỗi kết nối:', err);
                    this.notifyListeners('error', err);
                    // EventSource tự động reconnects but we can schedule fallback reconnect logic
                };
            } else {
                // WebSocket (development/local)
                const wsUrl = (typeof CONFIG !== 'undefined' && CONFIG.WS_URL) ? CONFIG.WS_URL : ((location.protocol === 'https:' ? 'wss://' : 'ws://') + location.host + '/ws');
                console.log('[WebSocket] Đang kết nối tới ' + wsUrl + '...');

                // --- BẮT ĐẦU ĐOẠN CODE THAY THẾ ---
                if (wsUrl.includes("railway.app")) {
                    console.log("🚀 Đang trên Railway: Chuyển sang chế độ SSE");
                    
                    const sseUrl = wsUrl.replace("wss://", "https://").replace("ws://", "http://").replace("/ws", "/events");
                    this.ws = new EventSource(sseUrl);

                } else {
                    // 2. Nếu đang chạy Local -> Dùng WebSocket như cũ
                    console.log("🏠 Đang chạy Local: Dùng WebSocket");
                    this.ws = new WebSocket(wsUrl);
                }
                // --- KẾT THÚC ĐOẠN CODE THAY THẾ ---

                // Khi kết nối thành công
                this.ws.onopen = (event) => {
                    console.log('[WebSocket] ✅ Kết nối thành công!');
                    this.reconnectDelay = 3000; // Reset delay
                    this.reconnectAttempts = 0;
                    this.notifyListeners('connected', event);
                };

                // Khi nhận message từ server
                this.ws.onmessage = (event) => {
                    try {
                        const data = JSON.parse(event.data);
                        console.log('[WebSocket] 📥 Nhận message:', data);
                        
                        // Xử lý theo loại message
                        switch (data.type) {
                            case 'connected':
                                console.log('[WebSocket] Server:', data.message);
                                break;
                                
                            case 'price_update':
                                this.handlePriceUpdate(data);
                                break;
                                
                            case 'server_shutdown':
                                console.warn('[WebSocket] Server đang shutdown:', data.message);
                                this.showToast('⚠️ Server đang bảo trì', 'warning');
                                break;
                                
                            default:
                                console.log('[WebSocket] Unknown message type:', data.type);
                        }
                    } catch (error) {
                        console.error('[WebSocket] Lỗi parse message:', error);
                    }
                };

                // Khi bị ngắt kết nối
                this.ws.onclose = (event) => {
                    console.log('[WebSocket] ❌ Kết nối đã đóng:', event.code, event.reason);
                    this.notifyListeners('disconnected', event);
                    
                    // Tự động reconnect nếu không phải manual close
                    if (!this.isManualClose) {
                        this.scheduleReconnect();
                    }
                };

                // Khi có lỗi
                this.ws.onerror = (error) => {
                    console.error('[WebSocket] ⚠️ Lỗi kết nối:', error);
                    this.notifyListeners('error', error);
                };
            }

        } catch (error) {
            console.error('[WebSocket] Lỗi khởi tạo:', error);
            this.scheduleReconnect();
        }
    }

    /**
     * Xử lý price update message
     */
    handlePriceUpdate(data) {
        console.log('[WebSocket] 💰 Giá thay đổi:', data.product_name);
        
        // Notify listeners
        this.notifyListeners('priceUpdate', data);
        
        // Hiển thị toast notification
        const priceStr = this.formatPrice(data.current_price);
        const discountStr = data.discount_percent > 0 ? ` (-${data.discount_percent}%)` : '';
        
        this.showToast(
            `🔥 ${data.product_name}<br>` +
            `<strong>${priceStr}</strong>${discountStr}`,
            'info',
            5000
        );
        
        // Update UI nếu sản phẩm đang hiển thị trên trang
        this.updateProductOnPage(data);
    }

    /**
     * Update sản phẩm trên trang (nếu đang hiển thị)
     */
    updateProductOnPage(data) {
        // Tìm product card có data-product-id matching
        const productCard = document.querySelector(`[data-product-id="${data.product_id}"]`);
        
        if (productCard) {
            console.log('[WebSocket] ♻️ Updating product on page:', data.product_id);
            
            // Update giá
            const priceElement = productCard.querySelector('.product-price');
            if (priceElement) {
                priceElement.textContent = this.formatPrice(data.current_price);
                this.flashElement(priceElement); // Animation hiệu ứng
            }
            
            // Update discount badge
            if (data.discount_percent > 0) {
                const discountBadge = productCard.querySelector('.discount-badge');
                if (discountBadge) {
                    discountBadge.textContent = `-${data.discount_percent}%`;
                    this.flashElement(discountBadge);
                }
            }
        }
    }

    /**
     * Hiệu ứng flash khi update
     */
    flashElement(element) {
        element.classList.add('price-updated');
        setTimeout(() => {
            element.classList.remove('price-updated');
        }, 2000);
    }

    /**
     * Lên lịch reconnect
     */
    scheduleReconnect() {
        this.reconnectAttempts++;
        const delay = Math.min(this.reconnectDelay * this.reconnectAttempts, this.maxReconnectDelay);
        
        console.log(`[WebSocket] 🔄 Sẽ reconnect sau ${delay/1000}s (lần thử ${this.reconnectAttempts})...`);
        
        setTimeout(() => {
            if (!this.isManualClose) {
                this.connect();
            }
        }, delay);
    }

    /**
     * Ngắt kết nối
     */
    disconnect() {
        console.log('[WebSocket] Đang ngắt kết nối...');
        this.isManualClose = true;
        
        if (this.ws) {
            this.ws.close();
            this.ws = null;
        }
    }

    /**
     * Gửi message tới server
     */
    send(message) {
        if (this.ws && this.ws.readyState === WebSocket.OPEN) {
            this.ws.send(JSON.stringify(message));
        } else {
            console.warn('[WebSocket] Không thể gửi - kết nối chưa mở');
        }
    }

    /**
     * Đăng ký listener
     */
    on(event, callback) {
        if (this.listeners[event]) {
            this.listeners[event].push(callback);
        }
    }

    /**
     * Notify tất cả listeners
     */
    notifyListeners(event, data) {
        if (this.listeners[event]) {
            this.listeners[event].forEach(callback => {
                try {
                    callback(data);
                } catch (error) {
                    console.error(`[WebSocket] Error in ${event} listener:`, error);
                }
            });
        }
    }

    /**
     * Format giá tiền VND
     */
    formatPrice(price) {
        return new Intl.NumberFormat('vi-VN', {
            style: 'currency',
            currency: 'VND'
        }).format(price);
    }

    /**
     * Hiển thị toast notification
     */
    showToast(message, type = 'info', duration = 3000) {
        // Kiểm tra container đã có chưa
        let container = document.getElementById('toast-container');
        if (!container) {
            container = document.createElement('div');
            container.id = 'toast-container';
            container.style.cssText = `
                position: fixed;
                top: 20px;
                right: 20px;
                z-index: 9999;
            `;
            document.body.appendChild(container);
        }

        // Tạo toast element
        const toast = document.createElement('div');
        toast.className = `toast toast-${type}`;
        toast.innerHTML = message;
        
        // Style cho toast
        const bgColors = {
            success: '#4CAF50',
            error: '#f44336',
            warning: '#ff9800',
            info: '#2196F3'
        };
        
        toast.style.cssText = `
            background: ${bgColors[type] || bgColors.info};
            color: white;
            padding: 15px 20px;
            margin-bottom: 10px;
            border-radius: 8px;
            box-shadow: 0 4px 12px rgba(0,0,0,0.3);
            min-width: 300px;
            max-width: 400px;
            animation: slideIn 0.3s ease-out;
            font-size: 14px;
            line-height: 1.5;
        `;

        container.appendChild(toast);

        // Auto remove sau duration
        setTimeout(() => {
            toast.style.animation = 'slideOut 0.3s ease-out';
            setTimeout(() => {
                toast.remove();
            }, 300);
        }, duration);
    }
}

// Thêm CSS animations
const style = document.createElement('style');
style.textContent = `
    @keyframes slideIn {
        from {
            transform: translateX(400px);
            opacity: 0;
        }
        to {
            transform: translateX(0);
            opacity: 1;
        }
    }
    
    @keyframes slideOut {
        from {
            transform: translateX(0);
            opacity: 1;
        }
        to {
            transform: translateX(400px);
            opacity: 0;
        }
    }
    
    .price-updated {
        animation: priceFlash 0.5s ease-in-out 2;
    }
    
    @keyframes priceFlash {
        0%, 100% { background-color: transparent; }
        50% { background-color: #fff3cd; }
    }
`;
document.head.appendChild(style);

// ==================== KHỞI ĐỘNG TỰ ĐỘNG ====================

// Tạo global instance
window.priceWS = new PriceWebSocketClient();

// Auto-connect khi page load
document.addEventListener('DOMContentLoaded', () => {
    console.log('[WebSocket] Initializing...');
    window.priceWS.connect();
    
    // Thêm status indicator
    addConnectionStatusIndicator();
});

// Disconnect khi page unload
window.addEventListener('beforeunload', () => {
    if (window.priceWS) {
        window.priceWS.disconnect();
    }
});

/**
 * Thêm connection status indicator vào page
 */
function addConnectionStatusIndicator() {
    const indicator = document.createElement('div');
    indicator.id = 'ws-status-indicator';
    indicator.title = 'WebSocket Status';
    indicator.style.cssText = `
        position: fixed;
        bottom: 20px;
        right: 20px;
        width: 12px;
        height: 12px;
        border-radius: 50%;
        background: #ccc;
        border: 2px solid white;
        box-shadow: 0 2px 8px rgba(0,0,0,0.2);
        z-index: 9998;
        cursor: pointer;
    `;
    document.body.appendChild(indicator);
    
    // Update color theo trạng thái
    window.priceWS.on('connected', () => {
        indicator.style.background = '#4CAF50';
        indicator.title = 'Real-time: Connected';
    });
    
    window.priceWS.on('disconnected', () => {
        indicator.style.background = '#f44336';
        indicator.title = 'Real-time: Disconnected';
    });
    
    // Click để xem thông tin
    indicator.addEventListener('click', () => {
        const es = window.priceWS.es;
        const ws = window.priceWS.ws;
        let statusText = 'UNKNOWN';
        let info = '';

        if (es) {
            const s = es.readyState;
            statusText = ['CONNECTING', 'OPEN', 'CLOSED'][s] || 'UNKNOWN';
            info = (typeof CONFIG !== 'undefined' ? (CONFIG.API_BASE_URL + '/events') : '/events');
        } else if (ws) {
            const s = ws.readyState;
            statusText = ['CONNECTING', 'OPEN', 'CLOSING', 'CLOSED'][s] || 'UNKNOWN';
            info = (typeof CONFIG !== 'undefined' && CONFIG.WS_URL) ? CONFIG.WS_URL : ((location.protocol === 'https:' ? 'wss://' : 'ws://') + location.host + '/ws');
        }

        alert(`Real-time Status: ${statusText}\nURL: ${info}`);
    });
}

console.log('[WebSocket] Module loaded ✓');
