/**
 * timKiem.js - Search results page logic
 * Handles communication with backend server and product card rendering
 */

const API_BASE_URL = (typeof CONFIG !== 'undefined' && CONFIG.API_BASE_URL) ? CONFIG.API_BASE_URL : window.location.origin;

/**
 * Perform search query to backend server
 * @param {string} query - Search query (URL or name)
 * @returns {Promise<Object>} JSON response from server
 */
async function searchProducts(query) {
    // Determine if query is Tiki URL or name search
    const isTikiUrl = query.includes('tiki.vn');
    const action = isTikiUrl ? 'SEARCH_BY_URL' : 'SEARCH_BY_NAME';
    
    console.log('🔍 Searching:', action, query);
    
    try {
        const response = await fetch(`${API_BASE_URL}/search`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                action: action,
                query: query
            })
        });
        
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        
        const data = await response.json();
        console.log('✅ Server response:', data);
        return data;
        
    } catch (error) {
        console.error('❌ Search error:', error);
        throw error;
    }
}

/**
 * Search products by category (group_id)
 * @param {number} groupId - Product group ID
 * @returns {Promise<Object>} JSON response from server
 */
async function searchProductsByCategory(groupId) {
    console.log('🔍 Searching by category:', groupId);
    
    try {
        const response = await fetch(`${API_BASE_URL}/search`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                action: 'SEARCH_BY_CATEGORY',
                group_id: groupId
            })
        });
        
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        
        const data = await response.json();
        console.log('✅ Category search response:', data);
        return data;
        
    } catch (error) {
        console.error('❌ Category search error:', error);
        throw error;
    }
}


/**
 * Render single product card
 * @param {Object} product - Product data from server
 * @param {boolean} isNew - Whether this is a newly scraped product
 * @returns {string} HTML string for product card
 */
function renderProductCard(product, isNew = false) {
    // Deal badge logic
    let dealBadgeHTML = '';
    if (product.deal_type && product.deal_type !== 'Normal') {
        if (product.deal_type === 'Flash Sale') {
            dealBadgeHTML = '<i class="fa-solid fa-bolt-lightning"></i><span>Flash Sale</span>';
        } else {
            dealBadgeHTML = `<span>${product.deal_type}</span>`;
        }
    }
    
    // New product badge
    const newBadgeHTML = isNew ? 
        '<div class="nhan_deal new-product" style="background-color: #10B981;"><span>SẢN PHẨM MỚI</span></div>' : '';
    
    // Discount percentage
    const discountPercentHTML = product.discount_percent > 0 ? 
        `<div class="phan_tram">-${product.discount_percent}%</div>` : '';
    
    // Calculate savings
    const savings = product.original_price - product.price;
    
    return `
        <div class="mathang" 
             data-product-id="${product.product_id}" 
             data-group-id="${product.group_id}"
             data-tiki-url="${product.url}">
            <div class="hinh">
                <img src="${product.image_url}" alt="${product.name}">
                <div class="tren_hinh">
                    ${dealBadgeHTML ? `<div class="nhan_deal deal">${dealBadgeHTML}</div>` : ''}
                    ${newBadgeHTML}
                    ${discountPercentHTML}
                </div>
            </div>
            <div class="thong_tin">
                <div class="nhom_sp">${product.group_name}</div>
                <div class="ten_sp">${product.name}</div>
                ${product.brand ? `<div class="thuong_hieu">${product.brand}</div>` : ''}
                <div class="gia_sp"><span>${formatPriceVND(product.price)}</span> đ</div>
                ${product.original_price > product.price ? `
                <div class="tiet_kiem">
                    <span class="gia_goc">${formatPriceVND(product.original_price)} đ</span>
                    <p class="khau_tru">Tiết kiệm <span>${formatPriceVND(savings)}</span> đ</p>
                </div>
                ` : ''}
                <button class="chi_tiet" onclick="viewProductDetail(${product.product_id})">
                    Xem chi tiết
                </button>
            </div>
        </div>
    `;
}

/**
 * Format price in VND (without currency symbol, with thousand separators)
 */
function formatPriceVND(price) {
    return new Intl.NumberFormat('vi-VN').format(price);
}

/**
 * Format price with currency symbol
 */
function formatPrice(price) {
    return new Intl.NumberFormat('vi-VN', {
        style: 'currency',
        currency: 'VND'
    }).format(price);
}

/**
 * Navigate to product detail page
 */
function viewProductDetail(productId) {
    window.location.href = `Trangchitiet.html?id=${productId}`;
}

/**
 * Get product data from card element (for Trangchitiet.html to use)
 * This function can be used to extract product info from clicked card
 */
function getProductDataFromCard(cardElement) {
    return {
        productId: cardElement.getAttribute('data-product-id'),
        groupId: cardElement.getAttribute('data-group-id'),
        tikiUrl: cardElement.getAttribute('data-tiki-url')
    };
}

/**
 * Alternative: Navigate to detail with all data in URL
 * Use this if you want to pass more info without extra API call
 */
function viewProductDetailWithData(productId, groupId, tikiUrl) {
    const params = new URLSearchParams({
        id: productId,
        group: groupId,
        url: encodeURIComponent(tikiUrl)
    });
    window.location.href = `Trangchitiet.html?${params.toString()}`;
}

/**
 * Display search results
 * @param {Object} response - Response from server
 */
function displayResults(response) {
    const resultsContainer = document.getElementById('frame_sp');
    const resultsInfo = document.getElementById('ket_qua');
    
    // Always show results section
    if (resultsInfo) {
        resultsInfo.style.display = 'block';
    }
    
    if (!response.success) {
        // Show error message with better styling
        if (resultsInfo) {
            resultsInfo.innerHTML = `<p class="error-message">${response.error}</p>`;
            resultsInfo.style.display = 'block';
        }
        if (resultsContainer) {
            resultsContainer.innerHTML = '';
            resultsContainer.style.display = 'none';
        }
        return;
    }
    
    // Handle single product response (URL search)
    if (response.product) {
        const product = response.product;
        
        // Show message for new product
        if (response.isNew) {
            if (resultsInfo) {
                resultsInfo.innerHTML = `
                    <h2>Kết quả tìm kiếm</h2>
                    <div class="new-product-message">
                        <p><strong>Sản phẩm bạn tìm hiện chưa có trong cơ sở dữ liệu của chúng tôi.</strong></p>
                        <p>Hệ thống đã bắt đầu theo dõi sản phẩm này và sẽ cập nhật biến động giá trong những lần tiếp theo.</p>
                        <p>Hiện tại, bạn có thể xem giá mới nhất bên dưới nhé! 😊</p>
                    </div>
                `;
                resultsInfo.style.display = 'block';
            }
        } else {
            if (resultsInfo) {
                resultsInfo.innerHTML = `<h2>Kết quả tìm kiếm</h2><p>Tìm thấy 1 sản phẩm</p>`;
                resultsInfo.style.display = 'block';
            }
        }
        
        if (resultsContainer) {
            resultsContainer.innerHTML = `<div class="hang">${renderProductCard(product, response.isNew)}</div>`;
            resultsContainer.style.display = 'flex';
        }
        return;
    }
    
    // Handle multiple products response (name search)
    if (response.products) {
        const count = response.count;
        
        if (resultsInfo) {
            resultsInfo.innerHTML = `<h2>Kết quả tìm kiếm</h2><p>Tìm thấy ${count} sản phẩm</p>`;
            resultsInfo.style.display = 'block';
        }
        
        // Group products into rows of 4
        let rowsHTML = '';
        for (let i = 0; i < response.products.length; i += 4) {
            const rowProducts = response.products.slice(i, i + 4);
            const cardsHTML = rowProducts.map(product => 
                renderProductCard(product, false)
            ).join('');
            rowsHTML += `<div class="hang">${cardsHTML}</div>`;
        }
        
        if (resultsContainer) {
            resultsContainer.innerHTML = rowsHTML;
            resultsContainer.style.display = 'flex';
        }
        return;
    }
    
    // Fallback error
    if (resultsInfo) {
        resultsInfo.innerHTML = `
            <p style="font-size:2rem; font-weight:bolder;color:#EC4899;text-align: center;font-family: 'roboto', sans-serif;margin: 1.25rem 0;">Kết quả tìm kiếm</p>
            <p class="error-message">Không có kết quả. Vui lòng thử lại!</p>
        `;
        resultsInfo.style.display = 'block';
    }
    if (resultsContainer) {
        resultsContainer.innerHTML = '';
        resultsContainer.style.display = 'none';
    }
}

/**
 * Show notification toast
 */
function showNotification(message) {
    const notification = document.createElement('div');
    notification.className = 'notification';
    notification.textContent = message;
    notification.style.cssText = `
        position: fixed;
        top: 20px;
        right: 20px;
        background: #4CAF50;
        color: white;
        padding: 15px 20px;
        border-radius: 5px;
        box-shadow: 0 2px 10px rgba(0,0,0,0.2);
        z-index: 9999;
        opacity: 0;
        transition: opacity 0.3s;
    `;
    document.body.appendChild(notification);
    
    setTimeout(() => {
        notification.style.opacity = '1';
    }, 100);
    
    setTimeout(() => {
        notification.style.opacity = '0';
        setTimeout(() => notification.remove(), 300);
    }, 3000);
}

/**
 * Show loading spinner
 */
function showLoading() {
    const resultsContainer = document.getElementById('frame_sp');
    const resultsInfo = document.getElementById('ket_qua');
    
    if (resultsInfo) {
        resultsInfo.innerHTML = '<p>Đang tìm kiếm...</p>';
        resultsInfo.style.display = 'block';
    }
    
    if (resultsContainer) {
        resultsContainer.innerHTML = `
            <div class="loading-spinner" style="
                border: 4px solid #f3f3f3;
                border-top: 4px solid #3498db;
                border-radius: 50%;
                width: 50px;
                height: 50px;
                animation: spin 1s linear infinite;
                margin: 50px auto;
            "></div>
            <style>
                @keyframes spin {
                    0% { transform: rotate(0deg); }
                    100% { transform: rotate(360deg); }
                }
            </style>
        `;
        resultsContainer.style.display = 'flex';
    }
}

/**
 * Initialize search on page load
 */
document.addEventListener('DOMContentLoaded', function() {
    // Get query parameters from URL
    const urlParams = new URLSearchParams(window.location.search);
    const query = urlParams.get('q');
    const groupId = urlParams.get('group_id');
    const categoryName = urlParams.get('category');
    
    // Update search box with query if exists
    const searchInput = document.getElementById('search_input');
    if (searchInput && query) {
        searchInput.value = query;
    }
    
    // Check if searching by category
    if (groupId) {
        console.log('Searching by category:', groupId, categoryName);
        showLoading();
        
        // Update header to show category name
        const resultsInfo = document.getElementById('ket_qua');
        if (resultsInfo && categoryName) {
            resultsInfo.innerHTML = `<h2>Danh mục: ${decodeURIComponent(categoryName)}</h2><p>Đang tải sản phẩm...</p>`;
            resultsInfo.style.display = 'block';
        }
        
        // Search by category
        searchProductsByCategory(parseInt(groupId))
            .then(response => {
                displayResults(response);
            })
            .catch(error => {
                console.error('Category search error:', error);
                if (resultsInfo) {
                    resultsInfo.innerHTML = 
                        `<p class="error-message">Lỗi kết nối: ${error.message}</p>`;
                }
            });
        
        return;
    }
    
    // If no query and no category, hide results
    if (!query) {
        const ketQuaSection = document.getElementById('ket_qua');
        const framesp = document.getElementById('frame_sp');
        
        if (ketQuaSection) {
            ketQuaSection.style.display = 'none';
        }
        if (framesp) {
            framesp.style.display = 'none';
        }
    } else {
        // Perform search if query exists
        showLoading();
        
        searchProducts(query)
            .then(response => {
                displayResults(response);
            })
            .catch(error => {
                console.error('Search error:', error);
                const resultsInfo = document.getElementById('ket_qua');
                if (resultsInfo) {
                    resultsInfo.innerHTML = 
                        `<p class="error-message">Lỗi kết nối: ${error.message}<br>
                            <small>Vui lòng đảm bảo server đang chạy tại ${API_BASE_URL}</small></p>`;
                    resultsInfo.style.display = 'block';
                }
            });
    }
    
    // Handle search button click
    const searchButton = document.getElementById('search_button');
    if (searchButton) {
        searchButton.addEventListener('click', function() {
            performSearchNow();
        });
    }
    
    // Handle Enter key in search input
    if (searchInput) {
        searchInput.addEventListener('keypress', function(e) {
            if (e.key === 'Enter') {
                performSearchNow();
            }
        });
    }
    
    // Handle suggestion tag clicks
    const suggestionTags = document.querySelectorAll('#loai_sp p');
    suggestionTags.forEach(tag => {
        tag.addEventListener('click', function() {
            // Use data-keyword for optimized search
            const keyword = this.getAttribute('data-keyword') || this.textContent.trim();
            
            searchInput.value = keyword;
            performSearchNow();
        });
        
        // Add hover effect cursor
        tag.style.cursor = 'pointer';
    });
});

/**
 * Handle new search from search box
 */
function handleSearch() {
    const searchInput = document.getElementById('search_input');
    const searchValue = searchInput.value.trim();
    
    if (searchValue === '') {
        alert('Vui lòng nhập từ khóa tìm kiếm!');
        return;
    }
    
    // Reload page with new query parameter
    window.location.href = `timKiem.html?q=${encodeURIComponent(searchValue)}`;
}

/**
 * Perform search immediately without page reload
 * This provides better UX - search happens on the same page
 */
function performSearchNow() {
    const searchInput = document.getElementById('search_input');
    const searchValue = searchInput.value.trim();
    
    if (searchValue === '') {
        alert('Vui lòng nhập từ khóa tìm kiếm!');
        return;
    }
    
    // Update URL without reload
    const newUrl = `${window.location.pathname}?q=${encodeURIComponent(searchValue)}`;
    window.history.pushState({query: searchValue}, '', newUrl);
    
    // Show loading and perform search
    showLoading();
    
    searchProducts(searchValue)
        .then(response => {
            displayResults(response);
        })
        .catch(error => {
            console.error('Search error:', error);
            const resultsInfo = document.getElementById('ket_qua');
            if (resultsInfo) {
                resultsInfo.innerHTML = 
                    `<p class="error-message">Lỗi kết nối: ${error.message}<br>
                        <small>Vui lòng đảm bảo server đang chạy tại ${API_BASE_URL}</small></p>`;
                resultsInfo.style.display = 'block';
            }
            
            const resultsContainer = document.getElementById('frame_sp');
            if (resultsContainer) {
                resultsContainer.innerHTML = '';
            }
        });
}
