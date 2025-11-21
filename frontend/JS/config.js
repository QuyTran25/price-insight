const CONFIG = {
    // Bỏ qua phần detect, chúng ta sửa trực tiếp ở dưới
    isProduction: true, 
    
    // Backend URLs
    get API_BASE_URL() {
        return 'https://web-production-1107a3.up.railway.app';
    },
    
    get WS_URL() {
        return 'wss://web-production-1107a3.up.railway.app/ws';
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