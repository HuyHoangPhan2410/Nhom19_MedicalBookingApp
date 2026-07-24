-- KHỞI TẠO DATABASE
SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- 1. TẠO DATABASE VÀ SỬ DỤNG
DROP DATABASE IF EXISTS MedicalBookingDB;
CREATE DATABASE MedicalBookingDB 
CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

USE MedicalBookingDB;

-- 2. BẢNG USERS (Quản lý tài khoản)
CREATE TABLE Users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role ENUM('patient', 'doctor', 'admin') NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- 3. BẢNG PATIENTS (Hồ sơ Bệnh nhân - Thừa hưởng từ Users)
CREATE TABLE Patients (
    user_id INT PRIMARY KEY,
    full_name VARCHAR(100) NOT NULL,
    dob DATE NOT NULL,
    gender ENUM('male', 'female', 'other') NOT NULL,
    phone VARCHAR(15) UNIQUE NOT NULL,
    address VARCHAR(255),
    blood_type VARCHAR(5),
    CONSTRAINT fk_patients_users FOREIGN KEY (user_id) 
        REFERENCES Users(id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;

-- 4. BẢNG SPECIALTIES (Chuyên khoa)
CREATE TABLE Specialties (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT
) ENGINE=InnoDB;

-- 5. BẢNG DOCTORS (Hồ sơ Bác sĩ - Thừa hưởng từ Users)
CREATE TABLE Doctors (
    user_id INT PRIMARY KEY,
    specialty_id INT NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    experience_years INT NOT NULL DEFAULT 0 CHECK (experience_years >= 0),
    consultation_fee DECIMAL(12, 2) NOT NULL CHECK (consultation_fee >= 0),
    bio TEXT,
    CONSTRAINT fk_doctors_users FOREIGN KEY (user_id) 
        REFERENCES Users(id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_doctors_specialties FOREIGN KEY (specialty_id) 
        REFERENCES Specialties(id) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB;

-- 6. BẢNG SCHEDULES (Lịch làm việc của Bác sĩ)
CREATE TABLE Schedules (
    id INT AUTO_INCREMENT PRIMARY KEY,
    doctor_id INT NOT NULL,
    work_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    max_patients INT NOT NULL DEFAULT 1 CHECK (max_patients > 0),
    CONSTRAINT fk_schedules_doctors FOREIGN KEY (doctor_id) 
        REFERENCES Doctors(user_id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT chk_schedule_time CHECK (end_time > start_time)
) ENGINE=InnoDB;

-- 7. BẢNG APPOINTMENTS (Lịch hẹn khám)
CREATE TABLE Appointments (
    id INT AUTO_INCREMENT PRIMARY KEY,
    patient_id INT NOT NULL,
    doctor_id INT NOT NULL,
    schedule_id INT NOT NULL,
    status ENUM('pending', 'confirmed', 'cancelled', 'completed') NOT NULL DEFAULT 'pending',
    symptoms TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_appointments_patients FOREIGN KEY (patient_id) 
        REFERENCES Patients(user_id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_appointments_doctors FOREIGN KEY (doctor_id) 
        REFERENCES Doctors(user_id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_appointments_schedules FOREIGN KEY (schedule_id) 
        REFERENCES Schedules(id) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB;

-- 8. BẢNG PAYMENTS (Thông tin Thanh toán)
CREATE TABLE Payments (
    id INT AUTO_INCREMENT PRIMARY KEY,
    appointment_id INT NOT NULL UNIQUE, -- Quan hệ 1-1 với Appointments
    amount DECIMAL(12, 2) NOT NULL CHECK (amount >= 0),
    payment_method ENUM('momo', 'vnpay', 'cash', 'card') NOT NULL,
    status ENUM('pending', 'success', 'failed') NOT NULL DEFAULT 'pending',
    paid_at TIMESTAMP NULL DEFAULT NULL,
    CONSTRAINT fk_payments_appointments FOREIGN KEY (appointment_id) 
        REFERENCES Appointments(id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;

-- =========================================================
-- 9. KHỞI TẠO CÁC INDEX (Tối ưu hóa tốc độ truy vấn)
-- =========================================================

-- Tăng tốc tìm kiếm lịch hẹn theo bệnh nhân hoặc bác sĩ
CREATE INDEX idx_appointments_patient ON Appointments(patient_id);
CREATE INDEX idx_appointments_doctor ON Appointments(doctor_id);
CREATE INDEX idx_appointments_status ON Appointments(status);

-- Tăng tốc tra cứu ca làm việc theo ngày và theo bác sĩ
CREATE INDEX idx_schedules_doctor_date ON Schedules(doctor_id, work_date);

-- THIẾT LẬP TRANSACTION
-- 1. Đặt lịch hẹn mới
DELIMITER //

CREATE PROCEDURE sp_BookAppointment(
    IN p_patient_id INT,
    IN p_doctor_id INT,
    IN p_schedule_id INT,
    IN p_symptoms TEXT,
    OUT p_appointment_id INT,
    OUT p_message VARCHAR(255)
)
proc_label: BEGIN
    DECLARE v_max_patients INT DEFAULT 0;
    DECLARE v_current_booked INT DEFAULT 0;
    DECLARE v_patient_exists INT DEFAULT 0;
    
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        SET p_appointment_id = NULL;
        SET p_message = 'Lỗi hệ thống: Đã rollback giao dịch đặt lịch.';
    END;

    START TRANSACTION;

    -- 1. Kiểm tra bệnh nhân
    SELECT COUNT(*) INTO v_patient_exists FROM Patients WHERE user_id = p_patient_id;
    IF v_patient_exists = 0 THEN
        ROLLBACK;
        SET p_message = 'Thất bại: Bệnh nhân không tồn tại.';
        LEAVE proc_label;
    END IF;

    -- 2. Lock & Check ca làm việc (Chống Overbooking)
    SELECT max_patients INTO v_max_patients 
    FROM Schedules 
    WHERE id = p_schedule_id AND doctor_id = p_doctor_id 
    FOR UPDATE;

    IF v_max_patients IS NULL OR v_max_patients = 0 THEN
        ROLLBACK;
        SET p_message = 'Thất bại: Ca làm việc của bác sĩ không tồn tại.';
        LEAVE proc_label;
    END IF;

    -- 3. Kiểm tra số slot đã đặt
    SELECT COUNT(*) INTO v_current_booked 
    FROM Appointments 
    WHERE schedule_id = p_schedule_id AND status IN ('pending', 'confirmed');

    IF v_current_booked >= v_max_patients THEN
        ROLLBACK;
        SET p_message = 'Thất bại: Ca khám này đã hết chỗ.';
        LEAVE proc_label;
    END IF;

    -- 4. Kiểm tra trùng lịch hẹn của bệnh nhân
    IF EXISTS (
        SELECT 1 FROM Appointments 
        WHERE patient_id = p_patient_id 
          AND schedule_id = p_schedule_id 
          AND status IN ('pending', 'confirmed')
    ) THEN
        ROLLBACK;
        SET p_message = 'Thất bại: Bạn đã đặt lịch trong khung giờ này rồi.';
        LEAVE proc_label;
    END IF;

    -- 5. Tạo Appointment
    INSERT INTO Appointments (patient_id, doctor_id, schedule_id, status, symptoms)
    VALUES (p_patient_id, p_doctor_id, p_schedule_id, 'pending', p_symptoms);

    SET p_appointment_id = LAST_INSERT_ID();

    -- 6. Tạo Payment ở trạng thái pending
    INSERT INTO Payments (appointment_id, amount, payment_method, status)
    SELECT p_appointment_id, consultation_fee, 'momo', 'pending'
    FROM Doctors WHERE user_id = p_doctor_id;

    COMMIT;
    SET p_message = 'Đặt lịch hẹn thành công!';
END //

DELIMITER ;

-- 2. Xác nhận thanh toán
DELIMITER //

CREATE PROCEDURE sp_ConfirmPayment(
    IN p_appointment_id INT,
    IN p_payment_method VARCHAR(20),
    OUT p_is_success BOOLEAN,
    OUT p_message VARCHAR(255)
)
proc_label: BEGIN
    DECLARE v_payment_id INT;
    DECLARE v_payment_status VARCHAR(20);
    DECLARE v_appointment_status VARCHAR(20);

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        SET p_is_success = FALSE;
        SET p_message = 'Lỗi hệ thống: Đã rollback giao dịch xác nhận thanh toán.';
    END;

    START TRANSACTION;

    -- 1. Lock & Check Payment
    SELECT id, status INTO v_payment_id, v_payment_status
    FROM Payments WHERE appointment_id = p_appointment_id FOR UPDATE;

    IF v_payment_id IS NULL THEN
        ROLLBACK;
        SET p_is_success = FALSE;
        SET p_message = 'Thất bại: Không tìm thấy hóa đơn thanh toán.';
        LEAVE proc_label;
    END IF;

    -- 2. Đảm bảo Idempotency (Tránh trùng lặp)
    IF v_payment_status = 'success' THEN
        ROLLBACK;
        SET p_is_success = TRUE;
        SET p_message = 'Giao dịch này đã được thanh toán trước đó.';
        LEAVE proc_label;
    END IF;

    -- 3. Lock & Check Appointment
    SELECT status INTO v_appointment_status
    FROM Appointments WHERE id = p_appointment_id FOR UPDATE;

    IF v_appointment_status = 'cancelled' THEN
        ROLLBACK;
        SET p_is_success = FALSE;
        SET p_message = 'Thất bại: Lịch hẹn này đã bị hủy trước đó.';
        LEAVE proc_label;
    END IF;

    -- 4. Cập nhật Payment & Appointment
    UPDATE Payments
    SET status = 'success', payment_method = IFNULL(p_payment_method, payment_method), paid_at = CURRENT_TIMESTAMP
    WHERE id = v_payment_id;

    UPDATE Appointments SET status = 'confirmed' WHERE id = p_appointment_id;

    COMMIT;
    SET p_is_success = TRUE;
    SET p_message = 'Thanh toán thành công!';
END //

DELIMITER ;

-- 3. Huỷ lịch hẹn
DELIMITER //

CREATE PROCEDURE sp_CancelAppointment(
    IN p_appointment_id INT,
    IN p_cancelled_by_role VARCHAR(20),
    OUT p_is_success BOOLEAN,
    OUT p_message VARCHAR(255)
)
proc_label: BEGIN
    DECLARE v_appointment_status VARCHAR(20);
    DECLARE v_payment_status VARCHAR(20);

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        SET p_is_success = FALSE;
        SET p_message = 'Lỗi hệ thống: Đã rollback giao dịch hủy lịch.';
    END;

    START TRANSACTION;

    -- 1. Lock Appointment
    SELECT status INTO v_appointment_status
    FROM Appointments WHERE id = p_appointment_id FOR UPDATE;

    IF v_appointment_status IS NULL THEN
        ROLLBACK;
        SET p_is_success = FALSE;
        SET p_message = 'Thất bại: Không tìm thấy lịch hẹn.';
        LEAVE proc_label;
    ELSEIF v_appointment_status IN ('cancelled', 'completed') THEN
        ROLLBACK;
        SET p_is_success = FALSE;
        SET p_message = 'Thất bại: Lịch hẹn đã hủy hoặc hoàn thành, không thể hủy lại.';
        LEAVE proc_label;
    END IF;

    -- 2. Hủy Appointment
    UPDATE Appointments SET status = 'cancelled' WHERE id = p_appointment_id;

    -- 3. Cập nhật trạng thái Payment liên quan
    SELECT status INTO v_payment_status
    FROM Payments WHERE appointment_id = p_appointment_id FOR UPDATE;

    IF v_payment_status IS NOT NULL THEN
        UPDATE Payments SET status = 'failed' WHERE appointment_id = p_appointment_id;
    END IF;

    COMMIT;
    SET p_is_success = TRUE;
    SET p_message = 'Hủy lịch hẹn thành công!';
END //

DELIMITER ;

-- 4. Đăng ký Bác sĩ mới
DELIMITER //

CREATE PROCEDURE sp_RegisterDoctor(
    IN p_email VARCHAR(100),
    IN p_password_hash VARCHAR(255),
    IN p_specialty_id INT,
    IN p_full_name VARCHAR(100),
    IN p_experience_years INT,
    IN p_consultation_fee DECIMAL(12,2),
    IN p_bio TEXT,
    OUT p_doctor_id INT,
    OUT p_message VARCHAR(255)
)
proc_label: BEGIN
    DECLARE v_new_user_id INT;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        SET p_doctor_id = NULL;
        SET p_message = 'Lỗi hệ thống: Không thể tạo hồ sơ bác sĩ.';
    END;

    START TRANSACTION;

    -- 1. Kiểm tra Email trùng
    IF EXISTS (SELECT 1 FROM Users WHERE email = p_email) THEN
        ROLLBACK;
        SET p_doctor_id = NULL;
        SET p_message = 'Thất bại: Email này đã được đăng ký.';
        LEAVE proc_label;
    END IF;

    -- 2. Kiểm tra Chuyên khoa
    IF NOT EXISTS (SELECT 1 FROM Specialties WHERE id = p_specialty_id) THEN
        ROLLBACK;
        SET p_doctor_id = NULL;
        SET p_message = 'Thất bại: Chuyên khoa không hợp lệ.';
        LEAVE proc_label;
    END IF;

    -- 3. Tạo User role = 'doctor'
    INSERT INTO Users (email, password_hash, role, is_active)
    VALUES (p_email, p_password_hash, 'doctor', TRUE);

    SET v_new_user_id = LAST_INSERT_ID();

    -- 4. Tạo Hồ sơ Doctor
    INSERT INTO Doctors (user_id, specialty_id, full_name, experience_years, consultation_fee, bio)
    VALUES (v_new_user_id, p_specialty_id, p_full_name, p_experience_years, p_consultation_fee, p_bio);

    COMMIT;
    SET p_doctor_id = v_new_user_id;
    SET p_message = 'Đăng ký Bác sĩ thành công!';
END //

DELIMITER ;

-- 5. Đổi lịch hẹn
DELIMITER //

CREATE PROCEDURE sp_RescheduleAppointment(
    IN p_appointment_id INT,
    IN p_new_schedule_id INT,
    OUT p_is_success BOOLEAN,
    OUT p_message VARCHAR(255)
)
proc_label: BEGIN
    DECLARE v_patient_id INT;
    DECLARE v_doctor_id INT;
    DECLARE v_app_status VARCHAR(20);
    DECLARE v_max_patients INT;
    DECLARE v_current_booked INT;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        SET p_is_success = FALSE;
        SET p_message = 'Lỗi hệ thống: Đã rollback giao dịch đổi lịch.';
    END;

    START TRANSACTION;

    -- 1. Lock & Check Lịch hẹn cũ
    SELECT patient_id, doctor_id, status INTO v_patient_id, v_doctor_id, v_app_status
    FROM Appointments WHERE id = p_appointment_id FOR UPDATE;

    IF v_app_status IS NULL OR v_app_status IN ('cancelled', 'completed') THEN
        ROLLBACK;
        SET p_is_success = FALSE;
        SET p_message = 'Thất bại: Lịch hẹn không hợp lệ để đổi lịch.';
        LEAVE proc_label;
    END IF;

    -- 2. Lock & Check Ca khám mới
    SELECT max_patients INTO v_max_patients 
    FROM Schedules WHERE id = p_new_schedule_id AND doctor_id = v_doctor_id FOR UPDATE;

    IF v_max_patients IS NULL THEN
        ROLLBACK;
        SET p_is_success = FALSE;
        SET p_message = 'Thất bại: Ca khám mới không khả dụng hoặc không thuộc bác sĩ này.';
        LEAVE proc_label;
    END IF;

    -- 3. Kiểm tra slot ca mới
    SELECT COUNT(*) INTO v_current_booked 
    FROM Appointments WHERE schedule_id = p_new_schedule_id AND status IN ('pending', 'confirmed');

    IF v_current_booked >= v_max_patients THEN
        ROLLBACK;
        SET p_is_success = FALSE;
        SET p_message = 'Thất bại: Ca khám mới đã hết chỗ.';
        LEAVE proc_label;
    END IF;

    -- 4. Cập nhật Schedule mới (Slot cũ tự động được giải phóng)
    UPDATE Appointments SET schedule_id = p_new_schedule_id WHERE id = p_appointment_id;

    COMMIT;
    SET p_is_success = TRUE;
    SET p_message = 'Đổi lịch hẹn thành công!';
END //

DELIMITER ;

-- 6. Xoá ca làm việc của Bác sĩ
DELIMITER //

CREATE PROCEDURE sp_CancelSchedule(
    IN p_schedule_id INT,
    IN p_doctor_id INT,
    OUT p_is_success BOOLEAN,
    OUT p_message VARCHAR(255)
)
proc_label: BEGIN
    DECLARE v_schedule_exists INT;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        SET p_is_success = FALSE;
        SET p_message = 'Lỗi hệ thống: Đã rollback giao dịch hủy ca làm việc.';
    END;

    START TRANSACTION;

    -- 1. Lock & Check ca làm việc
    SELECT COUNT(*) INTO v_schedule_exists 
    FROM Schedules WHERE id = p_schedule_id AND doctor_id = p_doctor_id FOR UPDATE;

    IF v_schedule_exists = 0 THEN
        ROLLBACK;
        SET p_is_success = FALSE;
        SET p_message = 'Thất bại: Ca làm việc không tồn tại.';
        LEAVE proc_label;
    END IF;

    -- 2. Chuyển tất cả Appointment trong ca này sang 'cancelled'
    UPDATE Appointments 
    SET status = 'cancelled' 
    WHERE schedule_id = p_schedule_id AND status IN ('pending', 'confirmed');

    -- 3. Cập nhật Payments liên quan sang failed/hoàn tiền
    UPDATE Payments 
    SET status = 'failed' 
    WHERE appointment_id IN (SELECT id FROM Appointments WHERE schedule_id = p_schedule_id);

    -- 4. Xóa Schedule
    DELETE FROM Schedules WHERE id = p_schedule_id;

    COMMIT;
    SET p_is_success = TRUE;
    SET p_message = 'Hủy ca làm việc thành công! Các lịch hẹn liên quan đã được tự động hủy.';
END //

DELIMITER ;

-- 7. Tự động quét huỷ gia hạn
-- Kích hoạt Event Scheduler và chạy Job quét định kỳ mỗi 5 phút để hủy các đơn pending quá 15 phút chưa thanh toán:
-- Kích hoạt Event Scheduler trên Database Server
SET GLOBAL event_scheduler = ON;

DELIMITER //

CREATE EVENT IF NOT EXISTS evt_CleanupPendingAppointments
ON SCHEDULE EVERY 5 MINUTE
DO
BEGIN
    DECLARE DONE INT DEFAULT FALSE;
    DECLARE v_app_id INT;
    
    -- Cursor lấy các Appointment quá 15 phút chưa thanh toán
    DECLARE app_cursor CURSOR FOR 
        SELECT id FROM Appointments 
        WHERE status = 'pending' 
          AND created_at < DATE_SUB(NOW(), INTERVAL 15 MINUTE);

    DECLARE CONTINUE HANDLER FOR NOT FOUND SET DONE = TRUE;

    OPEN app_cursor;

    read_loop: LOOP
        FETCH app_cursor INTO v_app_id;
        IF DONE THEN
            LEAVE read_loop;
        END IF;

        -- Gọi lại Procedure Hủy hẹn TX-03 để đảm bảo nhất quán logic
        CALL sp_CancelAppointment(v_app_id, 'admin', @success, @msg);
    END LOOP;

    CLOSE app_cursor;
END //

DELIMITER ;

-- =========================================================
-- 10. DỮ LIỆU MẪU (SEED DATA FOR TESTING & DEMO)
-- =========================================================

-- 10.1. Thêm Chuyên Khoa (Specialties)
INSERT INTO Specialties (id, name, description) VALUES
(1, 'Tim Mạch', 'Chuyên khoa chẩn đoán và điều trị các bệnh lý tim mạch, huyết áp và mạch máu.'),
(2, 'Nhi Khoa', 'Chuyên khoa chăm sóc sức khỏe toàn diện và điều trị bệnh lý cho trẻ em.'),
(3, 'Da Liễu', 'Chuyên khoa điều trị các bệnh lý về da, tóc, móng và thẩm mỹ da liễu.'),
(4, 'Răng Hàm Mặt', 'Chuyên khoa khám, điều trị bệnh lý răng miệng và nha khoa thẩm mỹ.'),
(5, 'Thần Kinh', 'Chuyên khoa điều trị các bệnh lý hệ thần kinh, não bộ và cột sống.'),
(6, 'Cơ Xương Khớp', 'Chuyên khoa điều trị thoái hóa khớp, chấn thương chỉnh hình và viêm khớp.');

-- 10.2. Thêm Tài Khoản Bác Sĩ (Users & Doctors - Đầy đủ 6 chuyên khoa)
-- 1. Tim Mạch (Specialty ID: 1)
INSERT INTO Users (id, email, password_hash, role, is_active) VALUES (1, 'dr.hung@hospital.com', 'password123', 'doctor', TRUE);
INSERT INTO Doctors (user_id, specialty_id, full_name, experience_years, consultation_fee, bio) VALUES
(1, 1, 'BS. CKI Nguyễn Văn Hùng', 15, 500000.00, 'Chuyên gia hàng đầu về tim mạch can thiệp với hơn 15 năm kinh nghiệm công tác tại các bệnh viện lớn.');

INSERT INTO Users (id, email, password_hash, role, is_active) VALUES (5, 'dr.thai@hospital.com', 'password123', 'doctor', TRUE);
INSERT INTO Doctors (user_id, specialty_id, full_name, experience_years, consultation_fee, bio) VALUES
(5, 1, 'TS. BS Lê Văn Thái', 18, 550000.00, 'Phó Trưởng khoa Tim mạch, chuyên gia về điều trị cao huyết áp và xơ vữa động mạch.');

-- 2. Nhi Khoa (Specialty ID: 2)
INSERT INTO Users (id, email, password_hash, role, is_active) VALUES (2, 'dr.mai@hospital.com', 'password123', 'doctor', TRUE);
INSERT INTO Doctors (user_id, specialty_id, full_name, experience_years, consultation_fee, bio) VALUES
(2, 2, 'TS. BS Trần Thị Mai', 12, 450000.00, 'Trưởng khoa Nhi, giàu kinh nghiệm trong điều trị và chăm sóc sức khỏe trẻ sơ sinh và trẻ nhỏ.');

INSERT INTO Users (id, email, password_hash, role, is_active) VALUES (6, 'dr.trang@hospital.com', 'password123', 'doctor', TRUE);
INSERT INTO Doctors (user_id, specialty_id, full_name, experience_years, consultation_fee, bio) VALUES
(6, 2, 'BS. CKII Phạm Thu Trang', 14, 480000.00, 'Chuyên gia Nhi khoa tổng quát, dinh dưỡng trẻ em và tư vấn tiêm chủng chủng ngừa.');

-- 3. Da Liễu (Specialty ID: 3)
INSERT INTO Users (id, email, password_hash, role, is_active) VALUES (3, 'dr.nam@hospital.com', 'password123', 'doctor', TRUE);
INSERT INTO Doctors (user_id, specialty_id, full_name, experience_years, consultation_fee, bio) VALUES
(3, 3, 'BS. CKII Lê Hoàng Nam', 10, 400000.00, 'Chuyên gia điều trị các bệnh lý da liễu mãn tính và thẩm mỹ da liễu công nghệ cao.');

INSERT INTO Users (id, email, password_hash, role, is_active) VALUES (7, 'dr.ha@hospital.com', 'password123', 'doctor', TRUE);
INSERT INTO Doctors (user_id, specialty_id, full_name, experience_years, consultation_fee, bio) VALUES
(7, 3, 'BS. CKI Võ Thanh Hà', 11, 420000.00, 'Chuyên khoa da liễu thẩm mỹ, mụn trứng cá mãn tính và phục hồi da hư tổn.');

-- 4. Răng Hàm Mặt (Specialty ID: 4)
INSERT INTO Users (id, email, password_hash, role, is_active) VALUES (4, 'dr.tuan@hospital.com', 'password123', 'doctor', TRUE);
INSERT INTO Doctors (user_id, specialty_id, full_name, experience_years, consultation_fee, bio) VALUES
(4, 4, 'BS. Phạm Minh Tuấn', 8, 350000.00, 'Chuyên gia nha khoa thẩm mỹ, niềng răng và cấy ghép Implant.');

INSERT INTO Users (id, email, password_hash, role, is_active) VALUES (8, 'dr.tri@hospital.com', 'password123', 'doctor', TRUE);
INSERT INTO Doctors (user_id, specialty_id, full_name, experience_years, consultation_fee, bio) VALUES
(8, 4, 'TS. BS Ngô Đức Trí', 16, 500000.00, 'Chuyên gia phẫu thuật Răng Hàm Mặt, chỉnh hình xương hàm và phục hình răng sứ.');

-- 5. Thần Kinh (Specialty ID: 5)
INSERT INTO Users (id, email, password_hash, role, is_active) VALUES (9, 'dr.long@hospital.com', 'password123', 'doctor', TRUE);
INSERT INTO Doctors (user_id, specialty_id, full_name, experience_years, consultation_fee, bio) VALUES
(9, 5, 'PGS. TS Nguyễn Bảo Long', 20, 600000.00, 'Chuyên gia nội thần kinh, điều trị đau đầu mãn tính, tai biến và rối loạn giấc ngủ.');

INSERT INTO Users (id, email, password_hash, role, is_active) VALUES (10, 'dr.thao@hospital.com', 'password123', 'doctor', TRUE);
INSERT INTO Doctors (user_id, specialty_id, full_name, experience_years, consultation_fee, bio) VALUES
(10, 5, 'BS. CKI Đỗ Phương Thảo', 9, 380000.00, 'Bác sĩ chuyên khoa thần kinh, chẩn đoán rối loạn tiền đình và thần kinh ngoại biên.');

-- 6. Cơ Xương Khớp (Specialty ID: 6)
INSERT INTO Users (id, email, password_hash, role, is_active) VALUES (11, 'dr.huy@hospital.com', 'password123', 'doctor', TRUE);
INSERT INTO Doctors (user_id, specialty_id, full_name, experience_years, consultation_fee, bio) VALUES
(11, 6, 'BS. CKII Đặng Quốc Huy', 13, 450000.00, 'Chuyên gia cơ xương khớp, điều trị thoái hóa cột sống, thoát vị đĩa đệm và viêm khớp.');

INSERT INTO Users (id, email, password_hash, role, is_active) VALUES (12, 'dr.an@hospital.com', 'password123', 'doctor', TRUE);
INSERT INTO Doctors (user_id, specialty_id, full_name, experience_years, consultation_fee, bio) VALUES
(12, 6, 'TS. BS Trịnh Hoài An', 17, 520000.00, 'Chuyên gia chấn thương chỉnh hình và y học thể thao.');

-- 10.3. Thêm Tài Khoản Bệnh Nhân (Users & Patients)
INSERT INTO Users (id, email, password_hash, role, is_active) VALUES (13, 'patient.an@gmail.com', 'password123', 'patient', TRUE);
INSERT INTO Patients (user_id, full_name, dob, gender, phone, address, blood_type) VALUES
(13, 'Lê Văn An', '1990-03-12', 'male', '0901234567', '123 Nguyễn Trãi, Phường 2, Quận 5, TP.HCM', 'O+');

INSERT INTO Users (id, email, password_hash, role, is_active) VALUES (14, 'patient.bich@gmail.com', 'password123', 'patient', TRUE);
INSERT INTO Patients (user_id, full_name, dob, gender, phone, address, blood_type) VALUES
(14, 'Phạm Thị Bích', '1995-08-25', 'female', '0912345678', '456 Lê Đại Hành, Phường 11, Quận 11, TP.HCM', 'A+');

-- 10.4. Thêm Ca Làm Việc Cho Bác Sĩ (Schedules - Đầy đủ 26 ca khám)
INSERT INTO Schedules (id, doctor_id, work_date, start_time, end_time, max_patients) VALUES
-- Doctor 1 (Tim Mạch)
(1, 1, CURDATE(), '08:00:00', '09:30:00', 5),
(2, 1, CURDATE(), '09:30:00', '11:00:00', 5),
(3, 1, DATE_ADD(CURDATE(), INTERVAL 1 DAY), '13:30:00', '15:00:00', 5),

-- Doctor 5 (Tim Mạch)
(4, 5, CURDATE(), '08:00:00', '09:30:00', 5),
(5, 5, DATE_ADD(CURDATE(), INTERVAL 1 DAY), '09:30:00', '11:00:00', 5),

-- Doctor 2 (Nhi Khoa)
(6, 2, CURDATE(), '08:00:00', '09:30:00', 4),
(7, 2, CURDATE(), '09:30:00', '11:00:00', 4),
(8, 2, DATE_ADD(CURDATE(), INTERVAL 1 DAY), '13:30:00', '15:00:00', 4),

-- Doctor 6 (Nhi Khoa)
(9, 6, CURDATE(), '13:30:00', '15:00:00', 4),
(10, 6, DATE_ADD(CURDATE(), INTERVAL 1 DAY), '08:00:00', '09:30:00', 4),

-- Doctor 3 (Da Liễu)
(11, 3, CURDATE(), '13:30:00', '15:00:00', 5),
(12, 3, DATE_ADD(CURDATE(), INTERVAL 1 DAY), '08:00:00', '09:30:00', 5),

-- Doctor 7 (Da Liễu)
(13, 7, CURDATE(), '08:00:00', '09:30:00', 5),
(14, 7, DATE_ADD(CURDATE(), INTERVAL 1 DAY), '13:30:00', '15:00:00', 5),

-- Doctor 4 (Răng Hàm Mặt)
(15, 4, CURDATE(), '09:30:00', '11:00:00', 3),
(16, 4, DATE_ADD(CURDATE(), INTERVAL 1 DAY), '14:00:00', '16:00:00', 3),

-- Doctor 8 (Răng Hàm Mặt)
(17, 8, CURDATE(), '08:00:00', '09:30:00', 4),
(18, 8, DATE_ADD(CURDATE(), INTERVAL 1 DAY), '09:30:00', '11:00:00', 4),

-- Doctor 9 (Thần Kinh)
(19, 9, CURDATE(), '08:00:00', '09:30:00', 5),
(20, 9, DATE_ADD(CURDATE(), INTERVAL 1 DAY), '13:30:00', '15:00:00', 5),

-- Doctor 10 (Thần Kinh)
(21, 10, CURDATE(), '13:30:00', '15:00:00', 5),
(22, 10, DATE_ADD(CURDATE(), INTERVAL 1 DAY), '08:00:00', '09:30:00', 5),

-- Doctor 11 (Cơ Xương Khớp)
(23, 11, CURDATE(), '08:00:00', '09:30:00', 4),
(24, 11, DATE_ADD(CURDATE(), INTERVAL 1 DAY), '13:30:00', '15:00:00', 4),

-- Doctor 12 (Cơ Xương Khớp)
(25, 12, CURDATE(), '13:30:00', '15:00:00', 5),
(26, 12, DATE_ADD(CURDATE(), INTERVAL 1 DAY), '09:30:00', '11:00:00', 5);

-- 10.5. Thêm Mẫu Lịch Hẹn & Hóa Đơn Thanh Toán (Appointments & Payments)
INSERT INTO Appointments (id, patient_id, doctor_id, schedule_id, status, symptoms) VALUES
(1, 13, 1, 1, 'confirmed', 'Đau tức ngực trái khi vận động mạnh, hồi hộp đánh trống ngực'),
(2, 14, 2, 6, 'pending', 'Bé bị sốt nhẹ về đêm, ho hắng kéo dài 2 ngày');

INSERT INTO Payments (id, appointment_id, amount, payment_method, status, paid_at) VALUES
(1, 1, 500000.00, 'momo', 'success', CURRENT_TIMESTAMP),
(2, 2, 450000.00, 'vnpay', 'pending', NULL);