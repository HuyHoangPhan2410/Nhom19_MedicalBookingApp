# Medical Booking System Backend (HospitalApp)

Dự án Backend cung cấp hệ thống API RESTful phục vụ ứng dụng Đặt Lịch Khám Bệnh Bác Sĩ Trực Tuyến (MedBooking). Được phát triển trên nền tảng **Java 21**, **Spring Boot 3.4.1**, **Spring Data JPA (Hibernate 7)** và CSDL **MySQL 8.0/9.2**.

---

## 1. Kiến Trúc & Công Nghệ

- **Framework**: Spring Boot 3.4.1 (Java 21)
- **Database**: MySQL Server 8.0+ / 9.2 (Database Name: `MedicalBookingDB`)
- **Persistence**: Spring Data JPA / Hibernate 7 (`EntityManager` hỗ trợ Stored Procedures)
- **Stored Procedures**:
  - `sp_RegisterDoctor`: Đăng ký bác sĩ mới
  - `sp_BookAppointment`: Đặt lịch khám bệnh nhân
  - `sp_ConfirmPayment`: Xác nhận thanh toán hóa đơn
  - `sp_CancelAppointment`: Hủy đơn hẹn khám
  - `sp_RescheduleAppointment`: Đổi lịch khám bệnh
  - `sp_CancelSchedule`: Hủy ca làm việc của bác sĩ
- **Testing Framework**: JUnit 5 + Mockito (**21 Unit Tests tự động đạt 100%**)

---

## 2. Hướng Dẫn Khởi Chạy

### 2.1. Cấu hình CSDL MySQL
Chạy script SQL để tạo bảng và nạp dữ liệu mẫu:
```bash
cmd /c "chcp 65001 >NUL && mysql -u root -pTu1den10 --default-character-set=utf8mb4 < database.sql"
```

### 2.2. Chạy Ứng Dụng Backend
```bash
.\mvnw spring-boot:run
```
Ứng dụng sẽ chạy tại cổng **`http://localhost:8082`**.

---

## 3. Danh Sách Chi Tiết Các Module Và API (Full CRUD)

### 3.1. Module Xác Thực & Tài Khoản (`/api/auth`)
- **[Create] Đăng ký Bệnh nhân mới**: `POST /api/auth/register-patient`
- **[Read] Đăng nhập hệ thống**: `POST /api/auth/login` (Tự động nhận diện vai trò `patient` / `doctor` / `admin` và trả về họ tên chuẩn).

### 3.2. Module Chuyên Khoa Khám (`/api/specialties`)
- **[Read] Lấy danh sách chuyên khoa**: `GET /api/specialties`
- **[Read] Xem chi tiết chuyên khoa**: `GET /api/specialties/{id}`

### 3.3. Module Quản Lý Bác Sĩ (`/api/doctors`)
- **[Read] Lấy danh sách tất cả bác sĩ**: `GET /api/doctors`
- **[Read] Xem chi tiết thông tin bác sĩ**: `GET /api/doctors/{id}`
- **[Read] Lọc danh sách bác sĩ theo chuyên khoa**: `GET /api/doctors/specialty/{specialtyId}`
- **[Create] Đăng ký hồ sơ bác sĩ mới**: `POST /api/doctors/register`

### 3.4. Module Quản Lý Ca Khám Bác Sĩ (`/api/schedules` - Full CRUD)
- **[Read] Xem ca làm việc của bác sĩ**: `GET /api/schedules/doctor/{doctorId}?date=YYYY-MM-DD`
- **[Create] Đăng ký ca khám mới**: `POST /api/schedules`
- **[Update] Cập nhật ca khám**: `PUT /api/schedules/{id}`
- **[Delete] Hủy ca làm việc**: `DELETE /api/schedules/{id}?doctorId={doctorId}`

### 3.5. Module Lịch Hẹn Khám Bệnh (`/api/appointments` - Full CRUD)
- **[Create] Tạo đơn đặt lịch khám**: `POST /api/appointments`
- **[Read] Xem chi tiết lịch hẹn**: `GET /api/appointments/{id}`
- **[Read] Xem danh sách lịch hẹn của bệnh nhân**: `GET /api/appointments/patient/{patientId}`
- **[Read] Xem danh sách lịch hẹn của bác sĩ**: `GET /api/appointments/doctor/{doctorId}`
- **[Delete] Hủy lịch hẹn khám**: `PUT /api/appointments/{id}/cancel`
- **[Update] Đổi ca lịch khám**: `PUT /api/appointments/{id}/reschedule?newScheduleId={newScheduleId}`

### 3.6. Module Thanh Toán Hóa Đơn (`/api/payments`)
- **[Create] Xác nhận thanh toán đơn khám**: `POST /api/payments/confirm`
- **[Read] Xem lịch sử thanh toán bệnh nhân**: `GET /api/payments/patient/{patientId}`
