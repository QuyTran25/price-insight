/**
 * giamGia.js - Load dữ liệu thực từ backend vào cấu trúc HTML có sẵn
 */

const API_BASE_URL = (typeof CONFIG !== 'undefined' && CONFIG.API_BASE_URL) ? CONFIG.API_BASE_URL : window.location.origin;
const PRODUCTS_PER_ROW = 4;  // 4 sản phẩm mỗi hàng
const ROWS_TO_SHOW = 2;      // Hiển thị 2 hàng = 8 sản phẩm
const PRODUCTS_PER_PAGE = PRODUCTS_PER_ROW * ROWS_TO_SHOW;  // 8 sản phẩm

// Lưu trữ dữ liệu đầy đủ để xử lý "Xem thêm"
let allProductsData = {
    deal_hot: [],
    flash_sale: [],
    hot_deal: [],
    trending: []
};

// Lưu trữ số lượng đang hiển thị
let displayedCounts = {
    deal_hot: 0,
    flash_sale: 0,
    hot_deal: 0,
    trending: 0
};

// Load dữ liệu khi trang được tải
document.addEventListener('DOMContentLoaded', function() {
    console.log('🎁 Trang giảm giá đã load - bắt đầu kết nối backend...');
    loadAllDeals();
    setupFilterButtons();
});

/**
 * Load tất cả sản phẩm giảm giá và phân loại vào các section
 */
async function loadAllDeals() {
    try {
        // Build endpoint using CONFIG endpoints when available
        const dealsEndpoint = (typeof CONFIG !== 'undefined' && CONFIG.ENDPOINTS && CONFIG.API_BASE_URL)
            ? (CONFIG.API_BASE_URL + CONFIG.ENDPOINTS.DEALS)
            : (API_BASE_URL + '/deals');

        // Load ALL deals (bao gồm tất cả)
        const response = await fetch(dealsEndpoint, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ deal_type: 'ALL' })
        });

        if (!response.ok) {
            const text = await response.text().catch(() => 'No response body');
            throw new Error(`Server không phản hồi (status=${response.status}): ${text}`);
        }

        const data = await response.json();
        console.log('✅ Dữ liệu ALL nhận được:', data);
        // Hiển thị debug panel để người dùng dễ copy khi không thể paste từ DevTools
        try { showDebugPanel(data, response.status); } catch (e) { /* ignore */ }
        
        if (data.success && data.products && data.products.length > 0) {
            const grouped = groupByDealType(data.products);
            
            allProductsData.flash_sale = grouped.FLASH_SALE;
            allProductsData.hot_deal = grouped.HOT_DEAL;
            
            // Load vào từng section (trừ trending)
            loadSection('#flash_sale', 'flash_sale', false);
            loadSection('#hot_deal', 'hot_deal', false);
        } else {
            showNoData();
        }
        
        // Load TRENDING riêng (mỗi danh mục 1 sản phẩm giảm giá sâu nhất)
        await loadTrendingDeals();
        
        // Ghép "Deals Hot" = Flash Sale + Hot Deal + Trending
        allProductsData.deal_hot = [
            ...allProductsData.flash_sale,
            ...allProductsData.hot_deal,
            ...allProductsData.trending
        ];
        loadSection('#deal_hot', 'deal_hot', false);
        
        // Cập nhật số lượng trong button SAU KHI đã load hết
        updateButtonCounts();
        
    } catch (error) {
        console.error('❌ Lỗi:', error);
        showError(error.message);
    }
}

/**
 * Load trending deals riêng (mỗi danh mục 1 sản phẩm tốt nhất)
 */
async function loadTrendingDeals() {
    try {
        const dealsEndpoint = (typeof CONFIG !== 'undefined' && CONFIG.ENDPOINTS && CONFIG.API_BASE_URL)
            ? (CONFIG.API_BASE_URL + CONFIG.ENDPOINTS.DEALS)
            : (API_BASE_URL + '/deals');

        const response = await fetch(dealsEndpoint, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ deal_type: 'TRENDING' })
        });

        if (!response.ok) {
            const text = await response.text().catch(() => 'No response body');
            throw new Error(`Server không phản hồi trending (status=${response.status}): ${text}`);
        }

        const data = await response.json();
        console.log('✅ Dữ liệu TRENDING nhận được:', data);
        try { showDebugPanel(data, response.status); } catch (e) { /* ignore */ }
        
        if (data.success && data.products && data.products.length > 0) {
            allProductsData.trending = data.products;
            loadSection('#trending', 'trending', false);
        } else {
            allProductsData.trending = [];
            loadSection('#trending', 'trending', false);
        }
        
        // Cập nhật count sau khi load trending xong
        updateButtonCounts();
        
    } catch (error) {
        console.error('❌ Lỗi load trending:', error);
        allProductsData.trending = [];
        loadSection('#trending', 'trending', false);
        updateButtonCounts();
    }
}

/**
 * Nhóm sản phẩm theo deal_type
 */
function groupByDealType(products) {
    return {
        FLASH_SALE: products.filter(p => p.deal_type === 'FLASH_SALE'),
        HOT_DEAL: products.filter(p => p.deal_type === 'HOT_DEAL'),
        TRENDING: products.filter(p => p.deal_type === 'TRENDING'),
        NORMAL: products.filter(p => !p.deal_type || p.deal_type === 'NORMAL')
    };
}

/**
 * Load sản phẩm vào một section (giữ nguyên cấu trúc HTML gốc)
 * @param {string} sectionId - ID của section (#deal_hot, #flash_sale, ...)
 * @param {string} dataKey - Key trong allProductsData
 * @param {boolean} isLoadMore - True nếu là load thêm, false nếu là load mới
 */
function loadSection(sectionId, dataKey, isLoadMore = false) {
    const section = document.querySelector(sectionId);
    if (!section) return;
    
    const container = section.querySelector('.sanpham');
    if (!container) return;
    
    const allProducts = allProductsData[dataKey];
    if (!allProducts || allProducts.length === 0) {
        showNoProductsMessage(container, dataKey);
        hideXemThemButton(section);
        return;
    }
    
    // Xác định sản phẩm cần hiển thị
    let startIndex = 0;
    let endIndex = PRODUCTS_PER_PAGE;
    
    if (isLoadMore) {
        startIndex = displayedCounts[dataKey];
        endIndex = startIndex + PRODUCTS_PER_PAGE;
    } else {
        // Load mới - xóa nội dung cũ
        container.innerHTML = '';
        displayedCounts[dataKey] = 0;
    }
    
    const productsToShow = allProducts.slice(startIndex, endIndex);
    const totalProducts = allProducts.length;
    
    if (productsToShow.length === 0) {
        if (!isLoadMore) {
            showNoProductsMessage(container, dataKey);
        }
        hideXemThemButton(section);
        return;
    }
    
    // Tạo các hàng sản phẩm (4 sản phẩm/hàng)
    for (let i = 0; i < productsToShow.length; i += PRODUCTS_PER_ROW) {
        const row = document.createElement('div');
        row.className = 'hang';
        
        // Luôn tạo 4 slot, nếu không đủ sản phẩm thì để trống (để giữ layout)
        for (let j = 0; j < PRODUCTS_PER_ROW; j++) {
            const productIndex = i + j;
            if (productIndex < productsToShow.length) {
                row.innerHTML += createProductHTML(productsToShow[productIndex]);
            } else {
                // Thêm div rỗng để giữ layout
                row.innerHTML += '<div class="mathang" style="visibility:hidden;"></div>';
            }
        }
        
        container.appendChild(row);
    }
    
    // Cập nhật số lượng đã hiển thị
    displayedCounts[dataKey] = endIndex;
    
    // Xử lý nút "Xem thêm"
    if (displayedCounts[dataKey] >= totalProducts) {
        hideXemThemButton(section);
    } else {
        showXemThemButton(section, dataKey);
    }
    
    // Gắn sự kiện click
    attachClickEvents(section);
}

/**
 * Hiển thị thông báo không có sản phẩm với icon và message phù hợp
 */
function showNoProductsMessage(container, dataKey) {
    const messages = {
        'deal_hot': {
            icon: '🔥',
            title: 'Chưa có deals hot!',
            message: 'Hiện tại chưa có sản phẩm giảm giá nào. Vui lòng quay lại sau!'
        },
        'flash_sale': {
            icon: '⚡',
            title: 'Flash Sale chưa bắt đầu!',
            message: 'Chưa có sản phẩm flash sale. Đừng bỏ lỡ những deal hot sắp tới!'
        },
        'hot_deal': {
            icon: '💥',
            title: 'Chưa có hot deals!',
            message: 'Chưa có sản phẩm giảm giá sâu. Hãy theo dõi để cập nhật deal mới!'
        },
        'trending': {
            icon: '📈',
            title: 'Chưa có sản phẩm trending!',
            message: 'Chưa có sản phẩm nổi bật từ các danh mục. Quay lại sau nhé!'
        }
    };
    
    const msg = messages[dataKey] || {
        icon: '🛍️',
        title: 'Không có sản phẩm',
        message: 'Hiện tại chưa có sản phẩm trong mục này.'
    };
    
    container.innerHTML = `
        <div style="text-align:center; padding:60px 20px; color:#666;">
            <div style="font-size:4rem; margin-bottom:20px;">${msg.icon}</div>
            <h3 style="color:#CC0843; font-size:1.5rem; margin-bottom:15px; font-weight:700;">${msg.title}</h3>
            <p style="font-size:1.1rem; color:#888; line-height:1.6;">${msg.message}</p>
        </div>
    `;
}

/**
 * Hiển thị nút "Xem thêm" và gắn sự kiện
 */
function showXemThemButton(section, dataKey) {
    let btnXemThem = section.querySelector('.xem_them');
    
    if (!btnXemThem) {
        // Tạo nút nếu chưa có
        btnXemThem = document.createElement('p');
        btnXemThem.className = 'xem_them';
        btnXemThem.textContent = 'Xem Thêm';
        section.appendChild(btnXemThem);
    }
    
    btnXemThem.style.display = 'block';
    btnXemThem.style.cursor = 'pointer';
    
    // Xóa event cũ và gắn event mới
    const newBtn = btnXemThem.cloneNode(true);
    btnXemThem.parentNode.replaceChild(newBtn, btnXemThem);
    
    newBtn.addEventListener('click', function() {
        const sectionId = section.getAttribute('id');
        loadSection(`#${sectionId}`, dataKey, true); // isLoadMore = true
    });
}

/**
 * Ẩn nút "Xem thêm"
 */
function hideXemThemButton(section) {
    const btnXemThem = section.querySelector('.xem_them');
    if (btnXemThem) {
        btnXemThem.style.display = 'none';
    }
}

/**
 * Tạo HTML cho 1 sản phẩm 
 * Ảnh có kích thước cố định để không bị lệch
 */
function createProductHTML(product) {
    const discount = product.discount_percent || 0;
    const price = formatPrice(product.price);
    const originalPrice = formatPrice(product.original_price);
    const savings = formatPrice(product.original_price - product.price);
    
    // Xác định loại badge dựa trên deal_type - CHỈ hiển thị nếu có deal thật
    let badgeHTML = '';
    
    if (product.deal_type === 'FLASH_SALE') {
        badgeHTML = `
            <div class="nhan_deal Sale">
                <i class="fa-solid fa-bolt-lightning"></i>
                <span>Flash Sale</span>
            </div>`;
    } else if (product.deal_type === 'HOT_DEAL') {
        badgeHTML = `
            <div class="nhan_deal deal">
                <i class="fa-solid fa-fire"></i>
                <span>Hot Deal</span>
            </div>`;
    } else if (product.deal_type === 'TRENDING') {
        badgeHTML = `
            <div class="nhan_deal Trending">
                <i class="fa-solid fa-arrow-trend-up"></i>
                <span>Trending</span>
            </div>`;
    }
    // NORMAL: không hiển thị badge
    
    return `
        <div class="mathang" data-url="${product.url || ''}" data-product-id="${product.product_id}">
            <div class="hinh">
                <img src="${product.image_url || 'https://via.placeholder.com/300x300'}" 
                     alt="${product.name}"
                     style="width: 100%; height: 300px; object-fit: cover; display: block;"
                     onerror="this.src='https://via.placeholder.com/300x300?text=No+Image'">
                <div class="tren_hinh">
                    ${badgeHTML}
                    ${discount > 0 ? `<div class="phan_tram">-${discount}%</div>` : ''}
                </div>
            </div>
            <div class="thong_tin">
                <div class="nhom_sp">${product.group_name || 'Sản phẩm'}</div>
                <div class="ten_sp">${truncate(product.name, 50)}</div>
                <div class="gia_sp"><span>${price}</span> đ</div>
                ${originalPrice && savings ? `
                <div class="tiet_kiem">
                    <span class="gia_goc">${originalPrice} đ</span>
                    <p class="khau_tru">Tiết kiệm <span>${savings}</span> đ</p>
                </div>
                ` : ''}
                <button class="chi_tiet">Xem chi tiết</button>
            </div>
        </div>
    `;
}

/**
 * Format số tiền (VD: 7490000 -> 7.490.000)
 */
function formatPrice(price) {
    if (!price) return '0';
    return Math.round(price).toString().replace(/\B(?=(\d{3})+(?!\d))/g, '.');
}

/**
 * Cắt ngắn text
 */
function truncate(text, maxLen) {
    if (!text) return '';
    return text.length > maxLen ? text.substring(0, maxLen) + '...' : text;
}

/**
 * Gắn sự kiện click cho các nút "Xem chi tiết"
 */
function attachClickEvents(section) {
    section.querySelectorAll('.chi_tiet').forEach(btn => {
        btn.addEventListener('click', function(e) {
            e.preventDefault();
            const mathang = this.closest('.mathang');
            const productId = mathang.getAttribute('data-product-id');
            
            if (productId) {
                // Chuyển đến trang chi tiết với product_id
                window.location.href = `Trangchitiet.html?id=${productId}`;
            } else {
                console.error('Không tìm thấy product_id');
            }
        });
    });
}

/**
 * Setup sự kiện cho các nút filter
 */
function setupFilterButtons() {
    const buttons = document.querySelectorAll('#nut a.chucnang');
    
    buttons.forEach((btn, index) => {
        btn.addEventListener('click', function(e) {
            // Giữ hành vi scroll mặc định
            const href = this.getAttribute('href');
            if (href && href.startsWith('#')) {
                const target = document.querySelector(href);
                if (target) {
                    setTimeout(() => {
                        target.scrollIntoView({ behavior: 'smooth', block: 'start' });
                    }, 100);
                }
            }
        });
    });
}

/**
 * Cập nhật số lượng trong các button filter
 */
function updateButtonCounts() {
    const countAll = document.querySelector('#count-all');
    const countFlash = document.querySelector('#count-flash');
    const countHot = document.querySelector('#count-hot');
    const countTrending = document.querySelector('#count-trending');
    
    // Đếm từ allProductsData (dữ liệu thực tế đã load)
    const flashCount = allProductsData.flash_sale.length;
    const hotCount = allProductsData.hot_deal.length;
    const trendingCount = allProductsData.trending.length;
    
    // "Tất cả" = Tổng của 3 loại (KHÔNG bao gồm NORMAL)
    const totalCount = flashCount + hotCount + trendingCount;
    
    if (countAll) countAll.textContent = `(${totalCount})`;
    if (countFlash) countFlash.textContent = `(${flashCount})`;
    if (countHot) countHot.textContent = `(${hotCount})`;
    if (countTrending) countTrending.textContent = `(${trendingCount})`;
}

/**
 * Hiển thị thông báo không có dữ liệu
 */
function showNoData() {
    const container = document.querySelector('#deal_hot .sanpham');
    if (container) {
        container.innerHTML = `
            <div style="text-align:center; padding:60px 20px; color:#666;">
                <div style="font-size:4rem; margin-bottom:20px;">😔</div>
                <h3 style="font-size:1.5rem; margin-bottom:10px;">Chưa có sản phẩm giảm giá</h3>
                <p>Database chưa có dữ liệu hoặc chưa có sản phẩm nào đang giảm giá</p>
                <p style="margin-top:20px; font-size:0.9rem; color:#999;">
                    Hãy chạy scraper hoặc cập nhật price_history trong database
                </p>
            </div>
        `;
    }
}

/**
 * Hiển thị thông báo lỗi
 */
function showError(message) {
    const container = document.querySelector('#deal_hot .sanpham');
    if (container) {
        container.innerHTML = `
            <div style="text-align:center; padding:60px 20px; color:#e74c3c;">
                <div style="font-size:4rem; margin-bottom:20px;">⚠️</div>
                <h3 style="font-size:1.5rem; margin-bottom:10px;">Không thể kết nối Backend</h3>
                <p style="margin-bottom:20px;">${message}</p>
                <div style="text-align:left; max-width:500px; margin:0 auto; background:#f8f9fa; padding:20px; border-radius:8px;">
                    <p style="font-weight:bold; margin-bottom:10px;">Kiểm tra:</p>
                    <ul style="margin:0; padding-left:20px;">
                        <li>Server đã chạy chưa? (port 8080)</li>
                        <li>MySQL đã bật chưa? (XAMPP)</li>
                        <li>Mở Console (F12) xem lỗi chi tiết</li>
                    </ul>
                </div>
                <button onclick="location.reload()" 
                        style="margin-top:30px; padding:12px 40px; background:#EC4899; color:white; 
                               border:none; border-radius:8px; cursor:pointer; font-size:1rem; font-weight:500;">
                    🔄 Thử lại
                </button>
            </div>
        `;
    }
}

console.log('✅ giamGia.js loaded');
