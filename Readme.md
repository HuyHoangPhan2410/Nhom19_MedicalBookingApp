# Nhom19 - Medical Booking App (Docker)

## Kiến trúc

  Frontend (Vue3+Nginx)  --->  Backend (Spring Boot)  --->  Database (MySQL 8)
  Port: 8081                   Port: 8082                   Port: 3306

## Khởi chạy nhanh

### 1. Clone repo
git clone https://github.com/HuyHoangPhan2410/Nhom19_MedicalBookingApp.git
cd Nhom19_MedicalBookingApp

### 2. Build & chạy tất cả
docker compose up --build -d

### 3. Truy cập
- Frontend:  http://localhost:8081
- Backend (API):   http://localhost:8082
- Database:  localhost:3306
- Admin:http://localhost:8081/admin/login
Email:  / Password: password123
### 4. Kiểm tra
docker compose ps

## Tài khoản mẫu

| Role | Email | Password |
|---------|-------|----------|
| Bác sĩ | dr.hung@hospital.com | password123 |
| Bác sĩ | dr.mai@hospital.com | password123 |
| Bệnh nhân | patient.an@gmail.com | password123 |
| Bệnh nhân | patient.bich@gmail.com | password123 |
| Admin | admin@medbooking.com | password123 |
