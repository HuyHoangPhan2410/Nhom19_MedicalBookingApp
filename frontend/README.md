# MedBooking Frontend - Nền Tảng Đặt Lịch Khám Bệnh Trực Tuyến

Ứng dụng Frontend **Single Page Application (SPA)** phục vụ hệ thống đặt lịch khám bệnh MedBooking. Dự án được xây dựng dựa trên framework **Vue 3 (Composition API)** kết hợp công cụ build **Vite 8**, **Pinia State Management**, **Vue Router 4** và **Axios Client**, cung cấp trải nghiệm hiện đại, mượt mà cho cả Bệnh nhân và Bác sĩ.

---

## 1. Tính Năng Nổi Bật (Key Features)

### 1.1. Phân Quyền Giao Diện Tự Động Theo Vai Trò (Role-Based Dynamic Routing)
- Hệ thống tự động nhận diện vai trò người dùng (`patient` hoặc `doctor`) sau khi đăng nhập để hiển thị giao diện và menu phù hợp.

### 1.2. Dành Cho Bệnh Nhân (Patient Portal)
- **Trang chủ & Lọc bác sĩ**: Lọc bác sĩ theo danh mục chuyên khoa (Tim mạch, Nhi khoa, Da liễu, Răng hàm mặt, Thần kinh, Cơ xương khớp) hoặc tìm kiếm theo tên.
- **Chi tiết bác sĩ & Chọn lịch**:
  - Chọn Ngày khám (Khóa tuyệt đối không cho chọn ngày trong quá khứ).
  - Chọn Giờ khám khả dụng hiển thị theo ngày đã chọn (`Còn trống X suất` / `Đã hết chỗ`).
- **Đặt lịch khám**: Điền triệu chứng bệnh và gửi yêu cầu đặt lịch tới hệ thống.
- **Quản lý lịch sử khám**:
  - Xem danh sách toàn bộ các lịch hẹn ở các trạng thái khác nhau (`pending` - Chờ thanh toán, `confirmed` - Đã xác nhận, `cancelled` - Đã hủy, `completed` - Đã hoàn thành).
  - **Thanh toán trực tuyến**: Chọn cổng thanh toán (MoMo, VNPay, Tiền mặt) và xác nhận hóa đơn.
  - **Đổi lịch khám**: Chọn ca khám mới khả dụng của cùng bác sĩ.
  - **Hủy lịch hẹn**: Hủy suất khám khi có thay đổi cá nhân.

### 1.3. Dành Cho Bác Sĩ (Doctor Schedule Dashboard - Full CRUD)
- **Bảng thời khóa biểu tuần**: Giao diện dạng lưới 7 ngày trong tuần (Thứ 2 đến Chủ nhật), tự động highlight nổi bật ngày Hôm Nay, hiển thị tất cả các ca khám bệnh nhân đã đặt.
- **Quản lý ca làm việc (Full CRUD Schedule Management)**:
  - **[Create] Đăng ký ca khám mới**: Bấm nút `+ Đăng Ký Ca Khám Mới`, chọn ngày làm việc (chặn ngày quá khứ), chọn khung giờ mẫu hoặc tự chọn và nhập số suất tối đa.
  - **[Read] Xem danh sách ca làm việc**: Xem toàn bộ các ca làm việc của bác sĩ kèm tỷ lệ số bệnh nhân đã đăng ký.
  - **[Update] Sửa ca khám**: Mở modal `EditScheduleModal.vue` cập nhật ngày, khung giờ làm việc hoặc số suất khám tối đa.
  - **[Delete] Hủy ca khám**: Hủy ca làm việc không thể tiếp tục trực.
- **Thống kê tổng quan**: Số ca khám tuần này và tổng số bệnh nhân được phục vụ.
- **Báo cáo phí khám & doanh thu dự kiến**: Tính toán tự động tổng số tiền phí khám thu được Hôm nay, Tuần này và Tháng này.

---

## 2. Cấu Trúc Mã Nguồn (Project Structure)

```text
frontend/
├── src/
│   ├── api/
│   │   └── axios.js             # Cấu hình Axios Instance kết nối Backend (http://localhost:8082)
│   ├── assets/
│   │   └── main.css             # Design System CSS (Biến màu, Card glassmorphism, Badges, Modals)
│   ├── components/
│   │   ├── Navbar.vue           # Thanh điều hướng header linh hoạt theo vai trò
│   │   ├── DoctorCard.vue       # Thẻ hiển thị thông tin bác sĩ
│   │   ├── AppointmentCard.vue  # Thẻ hiển thị chi tiết đơn khám & các nút thao tác
│   │   ├── PaymentModal.vue     # Modal lựa chọn và xác nhận thanh toán
│   │   ├── RescheduleModal.vue  # Modal lựa chọn ca khám mới để đổi lịch
│   │   ├── CreateScheduleModal.vue # Modal đăng ký ca khám mới cho bác sĩ
│   │   └── EditScheduleModal.vue   # Modal cập nhật chỉnh sửa ca khám cho bác sĩ
│   ├── router/
│   │   └── index.js             # Đăng ký danh sách đường dẫn Vue Router 4
│   ├── stores/
│   │   └── auth.js              # Pinia Store quản lý token và trạng thái đăng nhập
│   ├── views/
│   │   ├── HomeView.vue         # Trang chủ danh sách bác sĩ & lọc chuyên khoa
│   │   ├── DoctorDetailView.vue # Trang chi tiết bác sĩ & chọn ca khám
│   │   ├── PatientProfileView.vue # Trang hồ sơ & lịch sử khám bệnh nhân
│   │   ├── DoctorDashboardView.vue # Bảng điều khiển lịch khám & quản lý ca làm việc bác sĩ
│   │   └── AuthView.vue         # Trang đăng nhập & đăng ký bệnh nhân mới
│   ├── App.vue                  # Root Component chứa Navbar và Router-View
│   └── main.js                  # Entry point khởi tạo Vue App, Pinia và Router
├── index.html
├── vite.config.js               # Cấu hình cổng khởi chạy Vite Server (Port 8081)
└── package.json
```

---

## 3. Công Nghệ Sử Dụng (Tech Stack)

- Framework: Vue.js 3 (Composition API với `<script setup>`)
- Build Tool: Vite 8 (Thời gian khởi chạy dev server ~300ms)
- State Management: Pinia 2
- Router: Vue Router 4
- HTTP Client: Axios (Tự động đính kèm Bearer Token & Interceptor xử lý lỗi)
- Style: Modern Custom CSS (CSS Variables, Responsive Grid, Micro-animations)

---

## 4. Hướng Dẫn Khởi Chạy Dự Án (Getting Started)

### 4.1. Cài Đặt Dependencies
Mở Terminal tại thư mục `frontend` và cài đặt các thư viện:
```bash
npm install
```

### 4.2. Khởi Chạy Môi Trường Phát Triển (Development Server)
```bash
npm run dev
```
Giao diện ứng dụng sẽ sẵn sàng truy cập tại: **`http://localhost:8081/`**.

### 4.3. Biên Dịch Sản Phẩm (Production Build)
```bash
npm run build
```
Mã nguồn sau khi tối ưu hóa sẽ được lưu tại thư mục `dist/`.

---

## 5. Cấu Hình Kết Nối Backend

- Backend Spring Boot đang chạy tại địa chỉ: **`http://localhost:8082`**.
- Toàn bộ kết nối API được tập trung cấu hình tại file `src/api/axios.js`.
