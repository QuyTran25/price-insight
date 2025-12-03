const CONFIG = {
    // Auto-detect environment
    isProduction: window.location.hostname !== 'localhost' && window.location.hostname !== '127.0.0.1',
    
    // Backend URLs
    get API_BASE_URL() {
        // If on production (deployed frontend), use the Railway backend URL from environment
        // Railway will inject the backend URL or we can detect from current domain
        if (this.isProduction) {
            // Option 1: Use environment variable (if you have it)
            // Option 2: Use same domain as frontend (if backend is on same Railway service)
            // Option 3: Hardcode Railway backend URL
            return 'https://web-production-1107a3.up.railway.app';
        }
        // Local development
        return 'http://localhost:8080';
    },
    
    get WS_URL() {
        // SSE is always used in production, WebSocket in dev
        // This is handled in websocket.js based on isProduction flag
        if (this.isProduction) {
            return this.API_BASE_URL + '/events'; // SSE endpoint
        }
        return 'ws://localhost:8081'; // WebSocket for local dev
    },
    
    // API Endpoints
    ENDPOINTS: {
        SEARCH: '/search',
        DEALS: '/deals',
        PRODUCT_DETAIL: '/product-detail',
        REFRESH_PRICE: '/refresh-price',
        CATEGORIES: '/categories',
        METRICS: '/metrics',
        EVENTS: '/events' // SSE endpoint
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