// File loadFrame.js - Tải header và footer cho tất cả các trang

function loadFrame() {
    // Load Header
    fetch('Header.html')
        .then(response => {
            if (!response.ok) {
                throw new Error('Cannot load header');
            }
            return response.text();
        })
        .then(data => {
            document.getElementById('header').innerHTML = data;
            // Set active menu item based on current page
            setActiveMenuItem();
        })
        .catch(error => console.error('Error loading header:', error));

    // Load Footer
    fetch('footer.html')
        .then(response => {
            if (!response.ok) {
                throw new Error('Cannot load footer');
            }
            return response.text();
        })
        .then(data => {
            document.getElementById('footer').innerHTML = data;
        })
        .catch(error => console.error('Error loading footer:', error));
}

/**
 * Set active class for current menu item
 */
function setActiveMenuItem() {
    // Get current page filename
    const currentPage = window.location.pathname.split('/').pop().toLowerCase();
    
    // Map page names to menu items
    const pageMap = {
        'trangchu.html': 0,
        '': 0, // Default to Trang Chủ for root
        'index.html': 0,
        'giamgia.html': 1,
        'timkiem.html': 2,
        'danhmuc.html': 3
    };
    
    // Get menu items
    const menuItems = document.querySelectorAll('#menu .header_text');
    
    // Remove any existing active class
    menuItems.forEach(item => item.classList.remove('active'));
    
    // Add active class to current page
    const activeIndex = pageMap[currentPage];
    if (activeIndex !== undefined && menuItems[activeIndex]) {
        menuItems[activeIndex].classList.add('active');
    }
}

// Tự động load khi trang được tải
document.addEventListener('DOMContentLoaded', loadFrame);
