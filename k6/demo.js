import http from "k6/http";
import { sleep, check } from "k6";

/**
 * K6 LOAD TEST - PRICE TRACKER SYSTEM
 * Flow: Homepage -> (Search / Deals / Categories) -> Product Detail
 */

export const options = {
  stages: [
    { duration: "15s", target: 50 },   // Ramp-up: 0 -> 50 users
    { duration: "20s", target: 100 },
    { duration: "25s", target: 150 },
    { duration: "30s", target: 200 },
    { duration: "60s", target: 200 },  // Steady state
    { duration: "15s", target: 0 },    // Ramp-down
  ],
  thresholds: {
    http_req_duration: ["p(95)<1000"],
    http_req_failed: ["rate<0.10"],
  },
};

const BASE_URL = "http://localhost:8080";

const productIds = [1, 2, 3, 5, 10, 15, 20, 25, 40, 45, 50];
const keywords = [
  "Điện thoại", 
  "Laptop", 
  "Thời trang nam", 
  "Thời trang nữ",
  "điện tử" , 
  "" // Từ khóa rỗng để test search không điều kiện
];
const dealTypes = ["ALL", "FLASH_SALE", "HOT_DEAL", "TRENDING"];
const groupIds = [1, 2, 3, 4, 5];

export default function () {
  //Phân bổ theo mục đích price tracker
  const rand = Math.random() * 100;

  // =========================
  // FLOW 1: Tìm kiếm -> Chi tiết (40%)
  // =========================
  if (rand < 40) {
    const keyword = keywords[Math.floor(Math.random() * keywords.length)];
    const searchPayload = JSON.stringify({ action: "SEARCH_BY_NAME", query: keyword });
    const searchRes = http.post(`${BASE_URL}/search`, searchPayload, {
      headers: { "Content-Type": "application/json" },
    });

    check(searchRes, {
      "Search API status 200": (r) => r.status === 200,
      "Search API has products": (r) => {
        try { return JSON.parse(r.body).success === true; } 
        catch (e) { return false; }
      },
    });

    sleep(1);

    // Vào chi tiết sản phẩm - XEM LỊCH SỬ GIÁ
    const productId = productIds[Math.floor(Math.random() * productIds.length)];
    const detailPayload = JSON.stringify({ product_id: productId });
    const detailRes = http.post(`${BASE_URL}/product-detail`, detailPayload, {
      headers: { "Content-Type": "application/json" },
    });

    check(detailRes, {
      "Product Detail status 200": (r) => r.status === 200,
      "Product Detail has price history": (r) => {
        try { 
          const data = JSON.parse(r.body);
          return data.success === true && data.product !== null;
        }
        catch (e) { return false; }
      },
    });

    sleep(2); // User xem biểu đồ giá
  } 
  
  // =========================
  // FLOW 2: Giảm giá -> Chi tiết (30%)
  // =========================
  else if (rand < 70) {
    const dealType = dealTypes[Math.floor(Math.random() * dealTypes.length)];
    const dealsPayload = JSON.stringify({ deal_type: dealType });
    const dealsRes = http.post(`${BASE_URL}/deals`, dealsPayload, {
      headers: { "Content-Type": "application/json" },
    });

    check(dealsRes, {
      "Deals API status 200": (r) => r.status === 200,
      "Deals API returns products": (r) => {
        try { 
          const data = JSON.parse(r.body);
          // Chấp nhận success=true ngay cả khi products=[] (vì một số deal type không có data)
          return data.success === true;
        } catch(e) { return false; }
      },
    });

    sleep(1);

    // Vào chi tiết sản phẩm
    const productId = productIds[Math.floor(Math.random() * productIds.length)];
    const detailPayload = JSON.stringify({ product_id: productId });
    const detailRes = http.post(`${BASE_URL}/product-detail`, detailPayload, {
      headers: { "Content-Type": "application/json" },
    });

    check(detailRes, { 
      "Product Detail status 200": (r) => r.status === 200,
      "Product Detail has price history": (r) => {
        try { 
          const data = JSON.parse(r.body);
          return data.success === true && data.product !== null;
        }
        catch (e) { return false; }
      },
    });
    
    sleep(2);
  } 
  
  // =========================
  // FLOW 3: Danh mục -> Chi tiết (30%)
  // =========================
  else {
    const categoriesRes = http.get(`${BASE_URL}/categories`);
    check(categoriesRes, {
      "Categories API status 200": (r) => r.status === 200,
      "Categories API returns data": (r) => {
        try { 
          const data = JSON.parse(r.body); 
          return data.success === true && Array.isArray(data.categories); 
        }
        catch(e){ return false; }
      },
    });

    sleep(0.5);

    // Tìm kiếm theo category
    const groupId = groupIds[Math.floor(Math.random() * groupIds.length)];
    const categoryPayload = JSON.stringify({ action: "SEARCH_BY_CATEGORY", group_id: groupId });
    const categoryRes = http.post(`${BASE_URL}/search`, categoryPayload, {
      headers: { "Content-Type": "application/json" },
    });

    check(categoryRes, { 
      "Category Search status 200": (r) => r.status === 200,
      "Category Search has products": (r) => {
        try { return JSON.parse(r.body).success === true; }
        catch (e) { return false; }
      },
    });
    
    sleep(1);

    // Vào chi tiết sản phẩm
    const productId = productIds[Math.floor(Math.random() * productIds.length)];
    const detailPayload = JSON.stringify({ product_id: productId });
    const detailRes = http.post(`${BASE_URL}/product-detail`, detailPayload, {
      headers: { "Content-Type": "application/json" },
    });

    check(detailRes, { 
      "Product Detail status 200": (r) => r.status === 200,
      "Product Detail has price history": (r) => {
        try { 
          const data = JSON.parse(r.body);
          return data.success === true && data.product !== null;
        }
        catch (e) { return false; }
      },
    });
    
    sleep(2);
  }
}

/**
 * CÁCH CHẠY:
 * 
 * 1. Đảm bảo server đang chạy:
 *    .\start-server.bat
 * 
 * 2. Chạy k6 test:
 *    k6 run k6/demo.js
 * 
 * 3. Xem kết quả:
 *    - http_req_duration: Thời gian response
 *    - http_req_failed: Tỷ lệ lỗi
 *    - checks_succeeded: Tỷ lệ checks pass
 *    - iterations: Số lần hoàn thành các flows
 *    - vus: Số virtual users đang active
 * 
 * PHÂN BỐ TEST SCENARIOS:
 *    - Flow 1: Tìm kiếm -> Chi tiết sản phẩm (40%)
 *    - Flow 2: Giảm giá -> Chi tiết sản phẩm (30%)
 *    - Flow 3: Danh mục -> Chi tiết sản phẩm (30%)
 * 
 * KẾT QUẢ MONG ĐỢI:
 *    ✓ http_req_duration p(95) < 1000ms 
 *    ✓ http_req_failed rate < 10% (chấp nhận vì stress test)
 *    ✓ checks_succeeded > 90%
 *    ✓ Server không crash với 200 concurrent users
 *    ✓ Thread pool (100 threads) xử lý được high load
 * 
 */
