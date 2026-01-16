# TÀI LIỆU BACKEND API - SHOPAPP

## TỔNG QUAN
Đây là backend e-commerce (cửa hàng online) được xây dựng bằng Spring Boot với các chức năng:
- Quản lý người dùng và phân quyền
- Quản lý sản phẩm và danh mục
- Quản lý đơn hàng
- Đánh giá và bình luận sản phẩm
- Thanh toán qua VNPay
- Mã giảm giá (Coupon)
- Yêu thích sản phẩm
- Đăng nhập bằng OAuth2 (Google, Facebook)

## BASE URL
```
http://localhost:8088/api/v1
```

---

## PHÂN QUYỀN (ROLES)

Backend có 2 quyền chính:
1. **ROLE_ADMIN**: Quản trị viên - Toàn quyền
2. **ROLE_USER**: Người dùng thường - Quyền hạn chế

---

## 1. AUTHENTICATION & USER MANAGEMENT

### 1.1. Đăng ký tài khoản
```
POST /api/v1/users/register
```
**Quyền**: Public (không cần đăng nhập)

**Body**:
```json
{
  "fullname": "Nguyễn Văn A",
  "phoneNumber": "0987654321",
  "email": "user@example.com",
  "password": "password123",
  "retypePassword": "password123"
}
```
**Lưu ý**: Cần ít nhất email HOẶC phoneNumber

**Response**:
```json
{
  "message": "Account registration successful",
  "status": 201,
  "data": {
    "id": 1,
    "fullName": "Nguyễn Văn A",
    "phoneNumber": "0987654321",
    "email": "user@example.com",
    "address": "",
    "profileImage": "",
    "dateOfBirth": null,
    "facebookAccountId": null,
    "googleAccountId": null,
    "role": {
      "id": 1,
      "name": "USER"
    },
    "active": true
  }
}
```

### 1.2. Đăng nhập
```
POST /api/v1/users/login
```
**Quyền**: Public

**Body**:
```json
{
  "phoneNumber": "0987654321",  // hoặc email
  "password": "password123"
}
```

**Response**:
```json
{
  "message": "Login successfully",
  "status": 200,
  "data": {
    "message": "Login successfully",
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "refreshToken": "refresh_token_here",
    "username": "0987654321",
    "roles": ["ROLE_USER"],
    "id": 1
  }
}
```

### 1.3. Lấy thông tin user hiện tại
```
POST /api/v1/users/details
```
**Quyền**: ROLE_ADMIN hoặc ROLE_USER

**Headers**:
```
Authorization: Bearer {token}
```

**Response**: Thông tin user (giống đăng ký)

### 1.4. Cập nhật thông tin user
```
PUT /api/v1/users/details/{userId}
```
**Quyền**: ROLE_ADMIN hoặc ROLE_USER (chỉ update chính mình)

**Headers**:
```
Authorization: Bearer {token}
```

**Body**:
```json
{
  "fullname": "Nguyễn Văn B",
  "address": "123 Đường ABC",
  "dateOfBirth": "1990-01-01"
}
```

### 1.5. Upload ảnh đại diện
```
POST /api/v1/users/upload-profile-image
Content-Type: multipart/form-data
```
**Quyền**: ROLE_ADMIN hoặc ROLE_USER

**Headers**:
```
Authorization: Bearer {token}
```

**Body**: FormData với field `file` (max 10MB, chỉ image)

### 1.6. Xem ảnh đại diện
```
GET /api/v1/users/profile-images/{imageName}
```
**Quyền**: Public

### 1.7. Lấy danh sách tất cả user (Admin)
```
GET /api/v1/users?keyword=&page=0&limit=10
```
**Quyền**: ROLE_ADMIN

**Headers**:
```
Authorization: Bearer {token}
```

### 1.8. Reset mật khẩu user (Admin)
```
PUT /api/v1/users/reset-password/{userId}
```
**Quyền**: ROLE_ADMIN

### 1.9. Khóa/Mở khóa user (Admin)
```
PUT /api/v1/users/block/{userId}/{active}
```
**Quyền**: ROLE_ADMIN

**Params**: 
- `active`: 1 (mở khóa) hoặc 0 (khóa)

---

## 2. OAUTH2 LOGIN (Google/Facebook)

### 2.1. Lấy URL đăng nhập Google/Facebook
```
GET /api/v1/users/auth/social-login?login_type=google
```
**Quyền**: Public

**Response**: URL để redirect user đến trang đăng nhập Google/Facebook

### 2.2. Callback sau khi đăng nhập OAuth2
```
GET /api/v1/users/auth/social/callback?code={code}&login_type=google
```
**Quyền**: Public (được gọi tự động sau khi đăng nhập thành công)

**Response**: Giống response đăng nhập thường (có token)

---

## 3. CATEGORIES (Danh mục sản phẩm)

### 3.1. Lấy danh sách categories
```
GET /api/v1/categories?page=0&limit=10
```
**Quyền**: Public

**Response**:
```json
{
  "message": "Get list of categories successfully",
  "status": 200,
  "data": [
    {
      "id": 1,
      "name": "Điện thoại"
    },
    {
      "id": 2,
      "name": "Laptop"
    }
  ]
}
```

### 3.2. Lấy category theo ID
```
GET /api/v1/categories/{id}
```
**Quyền**: Public

### 3.3. Tạo category mới (Admin)
```
POST /api/v1/categories
```
**Quyền**: ROLE_ADMIN

**Headers**:
```
Authorization: Bearer {token}
```

**Body**:
```json
{
  "name": "Điện thoại"
}
```

### 3.4. Cập nhật category (Admin)
```
PUT /api/v1/categories/{id}
```
**Quyền**: ROLE_ADMIN

**Body**: Giống tạo mới

### 3.5. Xóa category (Admin)
```
DELETE /api/v1/categories/{id}
```
**Quyền**: ROLE_ADMIN

---

## 4. PRODUCTS (Sản phẩm)

### 4.1. Lấy danh sách sản phẩm
```
GET /api/v1/products?keyword=&category_id=0&page=0&limit=10
```
**Quyền**: Public

**Query params**:
- `keyword`: Tìm kiếm theo tên
- `category_id`: Lọc theo category (0 = tất cả)
- `page`: Số trang (bắt đầu từ 0)
- `limit`: Số lượng/trang

**Response**:
```json
{
  "message": "Get products successfully",
  "status": 200,
  "data": {
    "products": [
      {
        "id": 1,
        "name": "iPhone 15 Pro Max",
        "price": 30000000,
        "thumbnail": "image_url",
        "description": "Mô tả sản phẩm",
        "categoryId": 1,
        "categoryName": "Điện thoại",
        "productImages": [
          {
            "id": 1,
            "imageUrl": "image1.jpg"
          }
        ]
      }
    ],
    "totalPages": 10
  }
}
```

### 4.2. Lấy chi tiết sản phẩm
```
GET /api/v1/products/{id}
```
**Quyền**: Public

### 4.3. Lấy sản phẩm theo danh sách IDs
```
GET /api/v1/products/by-ids?ids=1,3,5,7
```
**Quyền**: Public

### 4.4. Tạo sản phẩm mới (Admin)
```
POST /api/v1/products
```
**Quyền**: ROLE_ADMIN

**Headers**:
```
Authorization: Bearer {token}
```

**Body**:
```json
{
  "name": "iPhone 15 Pro Max",
  "price": 30000000,
  "description": "Mô tả sản phẩm",
  "thumbnail": "",
  "categoryId": 1
}
```

### 4.5. Cập nhật sản phẩm (Admin)
```
PUT /api/v1/products/{id}
```
**Quyền**: ROLE_ADMIN

**Body**: Giống tạo mới

### 4.6. Xóa sản phẩm (Admin)
```
DELETE /api/v1/products/{id}
```
**Quyền**: ROLE_ADMIN

### 4.7. Upload ảnh sản phẩm (Admin)
```
POST /api/v1/products/uploads/{id}
Content-Type: multipart/form-data
```
**Quyền**: ROLE_ADMIN

**Body**: FormData với field `files` (mảng file, max 5 ảnh, mỗi ảnh max 10MB)

**Response**:
```json
{
  "message": "Upload image successfully",
  "status": 201,
  "data": [
    {
      "id": 1,
      "imageUrl": "image1.jpg"
    }
  ]
}
```

### 4.8. Xem ảnh sản phẩm
```
GET /api/v1/products/images/{imageName}
```
**Quyền**: Public

### 4.9. Like sản phẩm (User/Admin)
```
POST /api/v1/products/like/{productId}
```
**Quyền**: ROLE_ADMIN hoặc ROLE_USER

**Headers**:
```
Authorization: Bearer {token}
```

### 4.10. Unlike sản phẩm (User/Admin)
```
POST /api/v1/products/unlike/{productId}
```
**Quyền**: ROLE_ADMIN hoặc ROLE_USER

### 4.11. Lấy danh sách sản phẩm yêu thích (User/Admin)
```
POST /api/v1/products/favorite-products
```
**Quyền**: ROLE_ADMIN hoặc ROLE_USER

**Headers**:
```
Authorization: Bearer {token}
```

**Response**: Danh sách sản phẩm đã like

---

## 5. PRODUCT IMAGES

### 5.1. Xóa ảnh sản phẩm (Admin)
```
DELETE /api/v1/product_images/{id}
```
**Quyền**: ROLE_ADMIN

---

## 6. COMMENTS (Bình luận/Đánh giá)

### 6.1. Lấy danh sách bình luận
```
GET /api/v1/comments?product_id=1&user_id=1
```
**Quyền**: Public

**Query params**:
- `product_id`: Bắt buộc - ID sản phẩm
- `user_id`: Tùy chọn - Lọc theo user

**Response**:
```json
{
  "message": "Get comments successfully",
  "status": 200,
  "data": [
    {
      "id": 1,
      "content": "Sản phẩm rất tốt!",
      "userId": 1,
      "productId": 1,
      "createdAt": "2024-01-01T10:00:00"
    }
  ]
}
```

### 6.2. Tạo bình luận mới (User/Admin)
```
POST /api/v1/comments
```
**Quyền**: ROLE_ADMIN hoặc ROLE_USER

**Headers**:
```
Authorization: Bearer {token}
```

**Body**:
```json
{
  "productId": 1,
  "userId": 1,
  "content": "Sản phẩm rất tốt!"
}
```
**Lưu ý**: `userId` phải trùng với user đăng nhập

### 6.3. Cập nhật bình luận (User/Admin)
```
PUT /api/v1/comments/{id}
```
**Quyền**: ROLE_ADMIN hoặc ROLE_USER (chỉ update comment của mình)

**Body**: Giống tạo mới

---

## 7. ORDERS (Đơn hàng)

### 7.1. Tạo đơn hàng mới
```
POST /api/v1/orders
```
**Quyền**: ROLE_ADMIN hoặc ROLE_USER

**Headers**:
```
Authorization: Bearer {token}
```

**Body**:
```json
{
  "userId": 1,
  "fullName": "Nguyễn Văn A",
  "email": "user@example.com",
  "phoneNumber": "0987654321",
  "address": "123 Đường ABC",
  "note": "Giao hàng vào buổi sáng",
  "totalMoney": 30000000,
  "couponCode": "DISCOUNT10",
  "paymentMethod": "VNPay",
  "shippingMethod": "Standard",
  "shippingAddress": "123 Đường ABC",
  "shippingDate": "2024-01-15",
  "orderDetails": [
    {
      "productId": 1,
      "quantity": 2,
      "price": 15000000
    }
  ]
}
```

**Lưu ý**: 
- `userId` có thể bỏ qua (tự động lấy từ token)
- `orderDetails`: Danh sách sản phẩm trong đơn hàng

**Response**:
```json
{
  "message": "Insert order successfully",
  "status": 200,
  "data": {
    "id": 1,
    "userId": 1,
    "fullName": "Nguyễn Văn A",
    "phoneNumber": "0987654321",
    "email": "user@example.com",
    "address": "123 Đường ABC",
    "note": "Giao hàng vào buổi sáng",
    "orderDate": "2024-01-01T10:00:00",
    "status": "pending",
    "totalMoney": 30000000,
    "couponId": 1,
    "orderDetails": [...]
  }
}
```

### 7.2. Lấy danh sách đơn hàng của user
```
GET /api/v1/orders/user/{user_id}
```
**Quyền**: ROLE_ADMIN hoặc ROLE_USER (chỉ xem của mình hoặc Admin xem tất cả)

**Headers**:
```
Authorization: Bearer {token}
```

**Response**: Mảng các OrderResponse

### 7.3. Lấy chi tiết đơn hàng
```
GET /api/v1/orders/{id}
```
**Quyền**: Public (nhưng thực tế nên check quyền)

### 7.4. Cập nhật đơn hàng (Admin)
```
PUT /api/v1/orders/{id}
```
**Quyền**: ROLE_ADMIN

**Body**: Giống tạo mới

### 7.5. Hủy đơn hàng (User)
```
PUT /api/v1/orders/cancel/{id}
```
**Quyền**: ROLE_USER (chỉ hủy đơn của mình)

**Lưu ý**: Chỉ hủy được khi status = "pending" hoặc "cancelled"

### 7.6. Cập nhật trạng thái đơn hàng
```
PUT /api/v1/orders/{id}/status?status=processing
```
**Quyền**: ROLE_ADMIN hoặc ROLE_USER

**Status hợp lệ**:
- `pending`: Chờ xử lý
- `processing`: Đang xử lý
- `shipped`: Đã gửi hàng
- `delivered`: Đã giao hàng
- `cancelled`: Đã hủy

### 7.7. Tìm kiếm đơn hàng (Admin)
```
GET /api/v1/orders/get-orders-by-keyword?keyword=&page=0&limit=10
```
**Quyền**: Public (nên chỉ Admin)

### 7.8. Xóa đơn hàng (Admin - Soft Delete)
```
DELETE /api/v1/orders/{id}
```
**Quyền**: ROLE_ADMIN

---

## 8. ORDER DETAILS (Chi tiết đơn hàng)

### 8.1. Tạo order detail
```
POST /api/v1/order_details
```
**Quyền**: ROLE_ADMIN hoặc ROLE_USER

**Body**:
```json
{
  "orderId": 1,
  "productId": 1,
  "quantity": 2,
  "price": 15000000
}
```

### 8.2. Lấy chi tiết order detail
```
GET /api/v1/order_details/{id}
```
**Quyền**: Public

### 8.3. Lấy danh sách order details theo order
```
GET /api/v1/order_details/order/{orderId}
```
**Quyền**: Public

**Response**:
```json
{
  "message": "Get order details by orderId successfully",
  "status": 200,
  "data": [
    {
      "id": 1,
      "orderId": 1,
      "productId": 1,
      "productName": "iPhone 15 Pro Max",
      "quantity": 2,
      "price": 15000000,
      "totalMoney": 30000000
    }
  ]
}
```

### 8.4. Cập nhật order detail
```
PUT /api/v1/order_details/{id}
```
**Quyền**: ROLE_ADMIN hoặc ROLE_USER

### 8.5. Xóa order detail
```
DELETE /api/v1/order_details/{id}
```
**Quyền**: ROLE_ADMIN hoặc ROLE_USER

---

## 9. COUPONS (Mã giảm giá)

### 9.1. Tính toán giá sau khi áp dụng coupon
```
GET /api/v1/coupons/calculate?couponCode=DISCOUNT10&totalAmount=30000000
```
**Quyền**: Public

**Response**:
```json
{
  "message": "Calculate coupon successfully",
  "status": 200,
  "data": {
    "result": 27000000
  }
}
```

**Lưu ý**: Backend chỉ có API tính toán, không có API CRUD coupon (có thể được quản lý trực tiếp trong database)

---

## 10. PAYMENTS (Thanh toán VNPay)

### 10.1. Tạo URL thanh toán VNPay
```
POST /api/v1/payments/create_payment_url
```
**Quyền**: Public

**Body**:
```json
{
  "orderId": 1,
  "amount": 30000000,
  "orderDescription": "Thanh toán đơn hàng #1",
  "orderType": "other",
  "language": "vn"
}
```

**Response**:
```json
{
  "message": "Payment URL generated successfully.",
  "status": 200,
  "data": "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?..."
}
```

**Lưu ý**: Frontend cần redirect user đến URL này để thanh toán. Sau khi thanh toán xong, VNPay sẽ redirect về `return-url` đã cấu hình (mặc định: `http://localhost:4200/payments/payment-callback`)

### 10.2. Query giao dịch
```
POST /api/v1/payments/query
```
**Quyền**: Public

**Body**:
```json
{
  "orderId": 1,
  "transactionDate": "20240101"
}
```

### 10.3. Hoàn tiền (Refund)
```
POST /api/v1/payments/refund
```
**Quyền**: Public

**Body**:
```json
{
  "orderId": 1,
  "transactionDate": "20240101",
  "amount": 30000000,
  "transactionType": "03",
  "createDate": "20240101100000"
}
```

---

## 11. ROLES

### 11.1. Lấy danh sách roles
```
GET /api/v1/roles
```
**Quyền**: Public

**Response**:
```json
{
  "message": "Get roles successfully",
  "status": 200,
  "data": [
    {
      "id": 1,
      "name": "USER"
    },
    {
      "id": 2,
      "name": "ADMIN"
    }
  ]
}
```

---

## 12. POLICIES (Chính sách)

### 12.1. Lấy trang chính sách
```
GET /api/v1/policies/privacy-policy
GET /api/v1/policies/terms-of-service
```
**Quyền**: Public

**Response**: HTML content

---

## RESPONSE FORMAT CHUNG

Tất cả API trả về format:
```json
{
  "message": "Success message",
  "status": 200,
  "data": { ... }
}
```

**Status codes**:
- `200`: Success
- `201`: Created
- `400`: Bad Request
- `401`: Unauthorized (chưa đăng nhập hoặc token hết hạn)
- `403`: Forbidden (không có quyền)
- `404`: Not Found
- `500`: Internal Server Error

---

## AUTHENTICATION

Hầu hết các API (trừ public) yêu cầu JWT token trong header:
```
Authorization: Bearer {token}
```

Token có thời hạn: **30 ngày**
Refresh token có thời hạn: **60 ngày**

---

## LƯU Ý QUAN TRỌNG CHO FRONTEND ANGULAR

1. **Base URL**: `http://localhost:8088/api/v1`

2. **Interceptors**: Nên tạo HTTP Interceptor để:
   - Thêm `Authorization: Bearer {token}` vào mọi request
   - Handle 401 để redirect đến trang login
   - Handle lỗi chung

3. **Storage**: Lưu token và user info vào localStorage hoặc sessionStorage

4. **CORS**: Backend đã config CORS, nhưng cần đảm bảo frontend chạy trên port 4200 (hoặc cấu hình lại)

5. **File Upload**: 
   - Sử dụng FormData cho upload ảnh
   - Max file size: 10MB
   - Chỉ chấp nhận image files

6. **Pagination**: 
   - Page bắt đầu từ 0
   - Limit mặc định: 10

7. **Images**: 
   - Product images: `GET /api/v1/products/images/{imageName}`
   - Profile images: `GET /api/v1/users/profile-images/{imageName}`

8. **Order Status Flow**:
   - `pending` → `processing` → `shipped` → `delivered`
   - Có thể cancel khi `pending`

9. **Payment Flow**:
   1. User tạo order
   2. Gọi API tạo payment URL
   3. Redirect user đến URL VNPay
   4. User thanh toán trên VNPay
   5. VNPay redirect về callback URL với thông tin giao dịch
   6. Frontend xử lý kết quả và cập nhật order status

10. **OAuth2 Flow**:
    1. User click "Đăng nhập bằng Google/Facebook"
    2. Gọi API lấy auth URL
    3. Redirect user đến URL
    4. User đăng nhập trên OAuth provider
    5. OAuth provider redirect về callback với code
    6. Backend xử lý và trả về token

---

## CẤU TRÚC DỮ LIỆU QUAN TRỌNG

### User
```typescript
interface User {
  id: number;
  fullName: string;
  phoneNumber?: string;
  email?: string;
  address?: string;
  profileImage?: string;
  dateOfBirth?: string;
  role: {
    id: number;
    name: 'USER' | 'ADMIN';
  };
  active: boolean;
}
```

### Product
```typescript
interface Product {
  id: number;
  name: string;
  price: number;
  thumbnail?: string;
  description?: string;
  categoryId: number;
  categoryName?: string;
  productImages?: ProductImage[];
}

interface ProductImage {
  id: number;
  imageUrl: string;
}
```

### Order
```typescript
interface Order {
  id: number;
  userId: number;
  fullName: string;
  phoneNumber: string;
  email?: string;
  address: string;
  note?: string;
  orderDate: string;
  status: 'pending' | 'processing' | 'shipped' | 'delivered' | 'cancelled';
  totalMoney: number;
  couponId?: number;
  orderDetails: OrderDetail[];
}

interface OrderDetail {
  id: number;
  orderId: number;
  productId: number;
  productName: string;
  quantity: number;
  price: number;
  totalMoney: number;
}
```

### Category
```typescript
interface Category {
  id: number;
  name: string;
}
```

### Comment
```typescript
interface Comment {
  id: number;
  productId: number;
  userId: number;
  content: string;
  createdAt: string;
}
```

---

Chúc bạn code frontend thành công! 🚀

