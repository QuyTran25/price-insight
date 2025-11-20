/**
 * Configuration cho Frontend
 * Tự động detect môi trường (local vs production)
 */

const CONFIG = {
    // Tự động detect environment
    isProduction: window.location.hostname !== 'localhost' && 
                  window.location.hostname !== '127.0.0.1',
    
    // Backend URLs
    get API_BASE_URL() {
        if (this.isProduction) {
            // TODO: Thay YOUR_RAILWAY_APP_NAME bằng tên Railway app thật
            // Ví dụ: https://price-tracker-backend-production.up.railway.app
            return 'https://YOUR_RAILWAY_APP_NAME.up.railway.app';
        }
        return 'http://localhost:8080';
    },
    
    get WS_URL() {
        if (this.isProduction) {
            // WebSocket sẽ dùng cùng domain nhưng port khác (hoặc path /ws)
            // Railway có thể config để route /ws tới WebSocket server
            return 'wss://YOUR_RAILWAY_APP_NAME.up.railway.app';
        }
        return 'ws://localhost:8081';
    },
    
    // API Endpoints
    ENDPOINTS: {
        SEARCH: '/search',
        DEALS: '/deals',
        PRODUCT_DETAIL: '/product-detail',
        REFRESH_PRICE: '/refresh-price',
        CATEGORIES: '/categories',
        METRICS: '/metrics'
    },
    
    // Logging
    DEBUG: !this.isProduction,
    
    log(...args) {
        if (this.DEBUG) {
            console.log('[Config]', ...args);
        }
    }
};

// Log current environment
CONFIG.log('Environment:', CONFIG.isProduction ? 'Production' : 'Development');
CONFIG.log('API Base URL:', CONFIG.API_BASE_URL);
CONFIG.log('WebSocket URL:', CONFIG.WS_URL);

// Export cho các file khác sử dụng
if (typeof module !== 'undefined' && module.exports) {
    module.exports = CONFIG;
}
