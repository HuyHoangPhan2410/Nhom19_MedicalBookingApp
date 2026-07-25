-- KHỞI TẠO DATABASE CHO DOCKER
SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

USE MedicalBookingDB;

-- 2. BẢNG USERS
CREATE TABLE users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role ENUM('patient', 'doctor', 'admin') NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- 3. BẢNG PATIENTS
CREATE TABLE patients (
    user_id INT PRIMARY KEY,
    full_name VARCHAR(100) NOT NULL,
    dob DATE NOT NULL,
    gender ENUM('male', 'female', 'other') NOT NULL,
    phone VARCHAR(15) UNIQUE NOT NULL,
    address VARCHAR(255),
    blood_type VARCHAR(5),
    CONSTRAINT fk_patients_users FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;

-- 4. BẢNG SPECIALTIES
CREATE TABLE specialties (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT
) ENGINE=InnoDB;

-- 5. BẢNG DOCTORS
CREATE TABLE doctors (
    user_id INT PRIMARY KEY,
    specialty_id INT NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    experience_years INT NOT NULL DEFAULT 0 CHECK (experience_years >= 0),
    consultation_fee DECIMAL(12, 2) NOT NULL CHECK (consultation_fee >= 0),
    bio TEXT,
    CONSTRAINT fk_doctors_users FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_doctors_specialties FOREIGN KEY (specialty_id)
        REFERENCES specialties(id) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB;

-- 6. BẢNG SCHEDULES
CREATE TABLE schedules (
    id INT AUTO_INCREMENT PRIMARY KEY,
    doctor_id INT NOT NULL,
    work_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    max_patients INT NOT NULL DEFAULT 1 CHECK (max_patients > 0),
    CONSTRAINT fk_schedules_doctors FOREIGN KEY (doctor_id)
        REFERENCES doctors(user_id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT chk_schedule_time CHECK (end_time > start_time)
) ENGINE=InnoDB;

-- 7. BẢNG APPOINTMENTS
CREATE TABLE appointments (
    id INT AUTO_INCREMENT PRIMARY KEY,
    patient_id INT NOT NULL,
    doctor_id INT NOT NULL,
    schedule_id INT NOT NULL,
    status ENUM('pending', 'confirmed', 'cancelled', 'completed') NOT NULL DEFAULT 'pending',
    symptoms TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_appointments_patients FOREIGN KEY (patient_id)
        REFERENCES patients(user_id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_appointments_doctors FOREIGN KEY (doctor_id)
        REFERENCES doctors(user_id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_appointments_schedules FOREIGN KEY (schedule_id)
        REFERENCES schedules(id) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB;

-- 8. BẢNG PAYMENTS (✅ đổi enum, thêm stripe column)
CREATE TABLE payments (
    id INT AUTO_INCREMENT PRIMARY KEY,
    appointment_id INT NOT NULL UNIQUE,
    amount DECIMAL(12, 2) NOT NULL CHECK (amount >= 0),
    payment_method ENUM('cash', 'card') NOT NULL,
    status ENUM('pending', 'success', 'failed') NOT NULL DEFAULT 'pending',
    paid_at TIMESTAMP NULL DEFAULT NULL,
    stripe_payment_intent_id VARCHAR(255) NULL,
    CONSTRAINT fk_payments_appointments FOREIGN KEY (appointment_id)
        REFERENCES appointments(id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;

-- 9. INDEX
CREATE INDEX idx_appointments_patient ON appointments(patient_id);
CREATE INDEX idx_appointments_doctor ON appointments(doctor_id);
CREATE INDEX idx_appointments_status ON appointments(status);
CREATE INDEX idx_schedules_doctor_date ON schedules(doctor_id, work_date);

-- =========================================================
-- STORED PROCEDURES
-- =========================================================

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

    SELECT COUNT(*) INTO v_patient_exists FROM patients WHERE user_id = p_patient_id;
    IF v_patient_exists = 0 THEN
        ROLLBACK;
        SET p_message = 'Thất bại: Bệnh nhân không tồn tại.';
        LEAVE proc_label;
    END IF;

    SELECT max_patients INTO v_max_patients
    FROM schedules WHERE id = p_schedule_id AND doctor_id = p_doctor_id FOR UPDATE;

    IF v_max_patients IS NULL OR v_max_patients = 0 THEN
        ROLLBACK;
        SET p_message = 'Thất bại: Ca làm việc của bác sĩ không tồn tại.';
        LEAVE proc_label;
    END IF;

    SELECT COUNT(*) INTO v_current_booked
    FROM appointments WHERE schedule_id = p_schedule_id AND status IN ('pending', 'confirmed');

    IF v_current_booked >= v_max_patients THEN
        ROLLBACK;
        SET p_message = 'Thất bại: Ca khám này đã hết chỗ.';
        LEAVE proc_label;
    END IF;

    IF EXISTS (
        SELECT 1 FROM appointments
        WHERE patient_id = p_patient_id AND schedule_id = p_schedule_id AND status IN ('pending', 'confirmed')
    ) THEN
        ROLLBACK;
        SET p_message = 'Thất bại: Bạn đã đặt lịch trong khung giờ này rồi.';
        LEAVE proc_label;
    END IF;

    INSERT INTO appointments (patient_id, doctor_id, schedule_id, status, symptoms)
    VALUES (p_patient_id, p_doctor_id, p_schedule_id, 'pending', p_symptoms);

    SET p_appointment_id = LAST_INSERT_ID();

    -- ✅ Đổi 'momo' → 'cash'
    INSERT INTO payments (appointment_id, amount, payment_method, status)
    SELECT p_appointment_id, consultation_fee, 'cash', 'pending'
    FROM doctors WHERE user_id = p_doctor_id;

    COMMIT;
    SET p_message = 'Đặt lịch hẹn thành công!';
END //

DELIMITER ;

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

    SELECT id, status INTO v_payment_id, v_payment_status
    FROM payments WHERE appointment_id = p_appointment_id FOR UPDATE;

    IF v_payment_id IS NULL THEN
        ROLLBACK;
        SET p_is_success = FALSE;
        SET p_message = 'Thất bại: Không tìm thấy hóa đơn thanh toán.';
        LEAVE proc_label;
    END IF;

    IF v_payment_status = 'success' THEN
        ROLLBACK;
        SET p_is_success = TRUE;
        SET p_message = 'Giao dịch này đã được thanh toán trước đó.';
        LEAVE proc_label;
    END IF;

    SELECT status INTO v_appointment_status
    FROM appointments WHERE id = p_appointment_id FOR UPDATE;

    IF v_appointment_status = 'cancelled' THEN
        ROLLBACK;
        SET p_is_success = FALSE;
        SET p_message = 'Thất bại: Lịch hẹn này đã bị hủy trước đó.';
        LEAVE proc_label;
    END IF;

    UPDATE payments
    SET status = 'success', payment_method = IFNULL(p_payment_method, payment_method), paid_at = CURRENT_TIMESTAMP
    WHERE id = v_payment_id;

    UPDATE appointments SET status = 'confirmed' WHERE id = p_appointment_id;

    COMMIT;
    SET p_is_success = TRUE;
    SET p_message = 'Thanh toán thành công!';
END //

DELIMITER ;

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

    SELECT status INTO v_appointment_status
    FROM appointments WHERE id = p_appointment_id FOR UPDATE;

    IF v_appointment_status IS NULL THEN
        ROLLBACK;
        SET p_is_success = FALSE;
        SET p_message = 'Thất bại: Không tìm thấy lịch hẹn.';
        LEAVE proc_label;
    ELSEIF v_appointment_status IN ('cancelled', 'completed') THEN
        ROLLBACK;
        SET p_is_success = FALSE;
        SET p_message = 'Thất bại: Lịch hẹn đã hủy hoặc hoàn thành.';
        LEAVE proc_label;
    END IF;

    UPDATE appointments SET status = 'cancelled' WHERE id = p_appointment_id;

    SELECT status INTO v_payment_status
    FROM payments WHERE appointment_id = p_appointment_id FOR UPDATE;

    IF v_payment_status IS NOT NULL THEN
        UPDATE payments SET status = 'failed' WHERE appointment_id = p_appointment_id;
    END IF;

    COMMIT;
    SET p_is_success = TRUE;
    SET p_message = 'Hủy lịch hẹn thành công!';
END //

DELIMITER ;

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

    IF EXISTS (SELECT 1 FROM users WHERE email = p_email) THEN
        ROLLBACK;
        SET p_doctor_id = NULL;
        SET p_message = 'Thất bại: Email này đã được đăng ký.';
        LEAVE proc_label;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM specialties WHERE id = p_specialty_id) THEN
        ROLLBACK;
        SET p_doctor_id = NULL;
        SET p_message = 'Thất bại: Chuyên khoa không hợp lệ.';
        LEAVE proc_label;
    END IF;

    INSERT INTO users (email, password_hash, role, is_active)
    VALUES (p_email, p_password_hash, 'doctor', TRUE);

    SET v_new_user_id = LAST_INSERT_ID();

    INSERT INTO doctors (user_id, specialty_id, full_name, experience_years, consultation_fee, bio)
    VALUES (v_new_user_id, p_specialty_id, p_full_name, p_experience_years, p_consultation_fee, p_bio);

    COMMIT;
    SET p_doctor_id = v_new_user_id;
    SET p_message = 'Đăng ký Bác sĩ thành công!';
END //

DELIMITER ;

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

    SELECT patient_id, doctor_id, status INTO v_patient_id, v_doctor_id, v_app_status
    FROM appointments WHERE id = p_appointment_id FOR UPDATE;

    IF v_app_status IS NULL OR v_app_status IN ('cancelled', 'completed') THEN
        ROLLBACK;
        SET p_is_success = FALSE;
        SET p_message = 'Thất bại: Lịch hẹn không hợp lệ để đổi lịch.';
        LEAVE proc_label;
    END IF;

    SELECT max_patients INTO v_max_patients
    FROM schedules WHERE id = p_new_schedule_id AND doctor_id = v_doctor_id FOR UPDATE;

    IF v_max_patients IS NULL THEN
        ROLLBACK;
        SET p_is_success = FALSE;
        SET p_message = 'Thất bại: Ca khám mới không khả dụng.';
        LEAVE proc_label;
    END IF;

    SELECT COUNT(*) INTO v_current_booked
    FROM appointments WHERE schedule_id = p_new_schedule_id AND status IN ('pending', 'confirmed');

    IF v_current_booked >= v_max_patients THEN
        ROLLBACK;
        SET p_is_success = FALSE;
        SET p_message = 'Thất bại: Ca khám mới đã hết chỗ.';
        LEAVE proc_label;
    END IF;

    UPDATE appointments SET schedule_id = p_new_schedule_id WHERE id = p_appointment_id;

    COMMIT;
    SET p_is_success = TRUE;
    SET p_message = 'Đổi lịch hẹn thành công!';
END //

DELIMITER ;

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

    SELECT COUNT(*) INTO v_schedule_exists
    FROM schedules WHERE id = p_schedule_id AND doctor_id = p_doctor_id FOR UPDATE;

    IF v_schedule_exists = 0 THEN
        ROLLBACK;
        SET p_is_success = FALSE;
        SET p_message = 'Thất bại: Ca làm việc không tồn tại.';
        LEAVE proc_label;
    END IF;

    UPDATE appointments SET status = 'cancelled'
    WHERE schedule_id = p_schedule_id AND status IN ('pending', 'confirmed');

    UPDATE payments SET status = 'failed'
    WHERE appointment_id IN (SELECT id FROM appointments WHERE schedule_id = p_schedule_id);

    DELETE FROM schedules WHERE id = p_schedule_id;

    COMMIT;
    SET p_is_success = TRUE;
    SET p_message = 'Hủy ca làm việc thành công!';
END //

DELIMITER ;

-- Event Scheduler (chạy qua docker-compose command --event-scheduler=ON)
DELIMITER //

CREATE EVENT IF NOT EXISTS evt_CleanupPendingAppointments
ON SCHEDULE EVERY 5 MINUTE
DO
BEGIN
    DECLARE DONE INT DEFAULT FALSE;
    DECLARE v_app_id INT;

    DECLARE app_cursor CURSOR FOR
        SELECT id FROM appointments
        WHERE status = 'pending'
        AND created_at < DATE_SUB(NOW(), INTERVAL 15 MINUTE);

    DECLARE CONTINUE HANDLER FOR NOT FOUND SET DONE = TRUE;

    OPEN app_cursor;
    read_loop: LOOP
        FETCH app_cursor INTO v_app_id;
        IF DONE THEN LEAVE read_loop; END IF;
        CALL sp_CancelAppointment(v_app_id, 'admin', @success, @msg);
    END LOOP;
    CLOSE app_cursor;
END //

DELIMITER ;

-- =========================================================
-- 10. DỮ LIỆU MẪU (✅ BCrypt hash cho password123)
-- =========================================================

-- Chuyên khoa
INSERT INTO specialties (id, name, description) VALUES
(1, 'Tim Mạch', 'Chuyên khoa chẩn đoán và điều trị các bệnh lý tim mạch, huyết áp và mạch máu.'),
(2, 'Nhi Khoa', 'Chuyên khoa chăm sóc sức khỏe toàn diện và điều trị bệnh lý cho trẻ em.'),
(3, 'Da Liễu', 'Chuyên khoa điều trị các bệnh lý về da, tóc, móng và thẩm mỹ da liễu.'),
(4, 'Răng Hàm Mặt', 'Chuyên khoa khám, điều trị bệnh lý răng miệng và nha khoa thẩm mỹ.'),
(5, 'Thần Kinh', 'Chuyên khoa điều trị các bệnh lý hệ thần kinh, não bộ và cột sống.'),
(6, 'Cơ Xương Khớp', 'Chuyên khoa điều trị thoái hóa khớp, chấn thương chỉnh hình và viêm khớp.');

-- ✅ Admin account (id=15)
INSERT INTO users (id, email, password_hash, role, is_active) VALUES
(15, 'admin@medbooking.com', '$2a$10$BxzcPZTd/SXkGkDwREk1.eoWldP8SuBVgmYZwYpEJlQ4EUMx3ANI6', 'admin', TRUE);

-- Bác sĩ (✅ BCrypt)
INSERT INTO users (id, email, password_hash, role, is_active) VALUES (1, 'dr.hung@hospital.com', '$2a$10$BxzcPZTd/SXkGkDwREk1.eoWldP8SuBVgmYZwYpEJlQ4EUMx3ANI6', 'doctor', TRUE);
INSERT INTO doctors (user_id, specialty_id, full_name, experience_years, consultation_fee, bio) VALUES
(1, 1, 'BS. CKI Nguyễn Văn Hùng', 15, 500000.00, 'Chuyên gia hàng đầu về tim mạch can thiệp với hơn 15 năm kinh nghiệm.');

INSERT INTO users (id, email, password_hash, role, is_active) VALUES (5, 'dr.thai@hospital.com', '$2a$10$BxzcPZTd/SXkGkDwREk1.eoWldP8SuBVgmYZwYpEJlQ4EUMx3ANI6', 'doctor', TRUE);
INSERT INTO doctors (user_id, specialty_id, full_name, experience_years, consultation_fee, bio) VALUES
(5, 1, 'TS. BS Lê Văn Thái', 18, 550000.00, 'Phó Trưởng khoa Tim mạch, chuyên gia về điều trị cao huyết áp.');

INSERT INTO users (id, email, password_hash, role, is_active) VALUES (2, 'dr.mai@hospital.com', '$2a$10$BxzcPZTd/SXkGkDwREk1.eoWldP8SuBVgmYZwYpEJlQ4EUMx3ANI6', 'doctor', TRUE);
INSERT INTO doctors (user_id, specialty_id, full_name, experience_years, consultation_fee, bio) VALUES
(2, 2, 'TS. BS Trần Thị Mai', 12, 450000.00, 'Trưởng khoa Nhi, giàu kinh nghiệm trong điều trị và chăm sóc sức khỏe trẻ sơ sinh.');

INSERT INTO users (id, email, password_hash, role, is_active) VALUES (6, 'dr.trang@hospital.com', '$2a$10$BxzcPZTd/SXkGkDwREk1.eoWldP8SuBVgmYZwYpEJlQ4EUMx3ANI6', 'doctor', TRUE);
INSERT INTO doctors (user_id, specialty_id, full_name, experience_years, consultation_fee, bio) VALUES
(6, 2, 'BS. CKII Phạm Thu Trang', 14, 480000.00, 'Chuyên gia Nhi khoa tổng quát, dinh dưỡng trẻ em.');

INSERT INTO users (id, email, password_hash, role, is_active) VALUES (3, 'dr.nam@hospital.com', '$2a$10$BxzcPZTd/SXkGkDwREk1.eoWldP8SuBVgmYZwYpEJlQ4EUMx3ANI6', 'doctor', TRUE);
INSERT INTO doctors (user_id, specialty_id, full_name, experience_years, consultation_fee, bio) VALUES
(3, 3, 'BS. CKII Lê Hoàng Nam', 10, 400000.00, 'Chuyên gia điều trị các bệnh lý da liễu mãn tính.');

INSERT INTO users (id, email, password_hash, role, is_active) VALUES (7, 'dr.ha@hospital.com', '$2a$10$BxzcPZTd/SXkGkDwREk1.eoWldP8SuBVgmYZwYpEJlQ4EUMx3ANI6', 'doctor', TRUE);
INSERT INTO doctors (user_id, specialty_id, full_name, experience_years, consultation_fee, bio) VALUES
(7, 3, 'BS. CKI Võ Thanh Hà', 11, 420000.00, 'Chuyên khoa da liễu thẩm mỹ, mụn trứng cá mãn tính.');

INSERT INTO users (id, email, password_hash, role, is_active) VALUES (4, 'dr.tuan@hospital.com', '$2a$10$BxzcPZTd/SXkGkDwREk1.eoWldP8SuBVgmYZwYpEJlQ4EUMx3ANI6', 'doctor', TRUE);
INSERT INTO doctors (user_id, specialty_id, full_name, experience_years, consultation_fee, bio) VALUES
(4, 4, 'BS. Phạm Minh Tuấn', 8, 350000.00, 'Chuyên gia nha khoa thẩm mỹ, niềng răng và cấy ghép Implant.');

INSERT INTO users (id, email, password_hash, role, is_active) VALUES (8, 'dr.tri@hospital.com', '$2a$10$BxzcPZTd/SXkGkDwREk1.eoWldP8SuBVgmYZwYpEJlQ4EUMx3ANI6', 'doctor', TRUE);
INSERT INTO doctors (user_id, specialty_id, full_name, experience_years, consultation_fee, bio) VALUES
(8, 4, 'TS. BS Ngô Đức Trí', 16, 500000.00, 'Chuyên gia phẫu thuật Răng Hàm Mặt.');

INSERT INTO users (id, email, password_hash, role, is_active) VALUES (9, 'dr.long@hospital.com', '$2a$10$BxzcPZTd/SXkGkDwREk1.eoWldP8SuBVgmYZwYpEJlQ4EUMx3ANI6', 'doctor', TRUE);
INSERT INTO doctors (user_id, specialty_id, full_name, experience_years, consultation_fee, bio) VALUES
(9, 5, 'PGS. TS Nguyễn Bảo Long', 20, 600000.00, 'Chuyên gia nội thần kinh, điều trị đau đầu mãn tính.');

INSERT INTO users (id, email, password_hash, role, is_active) VALUES (10, 'dr.thao@hospital.com', '$2a$10$BxzcPZTd/SXkGkDwREk1.eoWldP8SuBVgmYZwYpEJlQ4EUMx3ANI6', 'doctor', TRUE);
INSERT INTO doctors (user_id, specialty_id, full_name, experience_years, consultation_fee, bio) VALUES
(10, 5, 'BS. CKI Đỗ Phương Thảo', 9, 380000.00, 'Bác sĩ chuyên khoa thần kinh.');

INSERT INTO users (id, email, password_hash, role, is_active) VALUES (11, 'dr.huy@hospital.com', '$2a$10$BxzcPZTd/SXkGkDwREk1.eoWldP8SuBVgmYZwYpEJlQ4EUMx3ANI6', 'doctor', TRUE);
INSERT INTO doctors (user_id, specialty_id, full_name, experience_years, consultation_fee, bio) VALUES
(11, 6, 'BS. CKII Đặng Quốc Huy', 13, 450000.00, 'Chuyên gia cơ xương khớp.');

INSERT INTO users (id, email, password_hash, role, is_active) VALUES (12, 'dr.an@hospital.com', '$2a$10$BxzcPZTd/SXkGkDwREk1.eoWldP8SuBVgmYZwYpEJlQ4EUMx3ANI6', 'doctor', TRUE);
INSERT INTO doctors (user_id, specialty_id, full_name, experience_years, consultation_fee, bio) VALUES
(12, 6, 'TS. BS Trịnh Hoài An', 17, 520000.00, 'Chuyên gia chấn thương chỉnh hình.');

-- Bệnh nhân (✅ BCrypt)
INSERT INTO users (id, email, password_hash, role, is_active) VALUES (13, 'patient.an@gmail.com', '$2a$10$BxzcPZTd/SXkGkDwREk1.eoWldP8SuBVgmYZwYpEJlQ4EUMx3ANI6', 'patient', TRUE);
INSERT INTO patients (user_id, full_name, dob, gender, phone, address, blood_type) VALUES
(13, 'Lê Văn An', '1990-03-12', 'male', '0901234567', '123 Nguyễn Trãi, Quận 5, TP.HCM', 'O+');

INSERT INTO users (id, email, password_hash, role, is_active) VALUES (14, 'patient.bich@gmail.com', '$2a$10$BxzcPZTd/SXkGkDwREk1.eoWldP8SuBVgmYZwYpEJlQ4EUMx3ANI6', 'patient', TRUE);
INSERT INTO patients (user_id, full_name, dob, gender, phone, address, blood_type) VALUES
(14, 'Phạm Thị Bích', '1995-08-25', 'female', '0912345678', '456 Lê Đại Hành, Quận 11, TP.HCM', 'A+');

-- Schedules
INSERT INTO schedules (id, doctor_id, work_date, start_time, end_time, max_patients) VALUES
(1, 1, CURDATE(), '08:00:00', '09:30:00', 5),
(2, 1, CURDATE(), '09:30:00', '11:00:00', 5),
(3, 1, DATE_ADD(CURDATE(), INTERVAL 1 DAY), '13:30:00', '15:00:00', 5),
(4, 5, CURDATE(), '08:00:00', '09:30:00', 5),
(5, 5, DATE_ADD(CURDATE(), INTERVAL 1 DAY), '09:30:00', '11:00:00', 5),
(6, 2, CURDATE(), '08:00:00', '09:30:00', 4),
(7, 2, CURDATE(), '09:30:00', '11:00:00', 4),
(8, 2, DATE_ADD(CURDATE(), INTERVAL 1 DAY), '13:30:00', '15:00:00', 4),
(9, 6, CURDATE(), '13:30:00', '15:00:00', 4),
(10, 6, DATE_ADD(CURDATE(), INTERVAL 1 DAY), '08:00:00', '09:30:00', 4),
(11, 3, CURDATE(), '13:30:00', '15:00:00', 5),
(12, 3, DATE_ADD(CURDATE(), INTERVAL 1 DAY), '08:00:00', '09:30:00', 5),
(13, 7, CURDATE(), '08:00:00', '09:30:00', 5),
(14, 7, DATE_ADD(CURDATE(), INTERVAL 1 DAY), '13:30:00', '15:00:00', 5),
(15, 4, CURDATE(), '09:30:00', '11:00:00', 3),
(16, 4, DATE_ADD(CURDATE(), INTERVAL 1 DAY), '14:00:00', '16:00:00', 3),
(17, 8, CURDATE(), '08:00:00', '09:30:00', 4),
(18, 8, DATE_ADD(CURDATE(), INTERVAL 1 DAY), '09:30:00', '11:00:00', 4),
(19, 9, CURDATE(), '08:00:00', '09:30:00', 5),
(20, 9, DATE_ADD(CURDATE(), INTERVAL 1 DAY), '13:30:00', '15:00:00', 5),
(21, 10, CURDATE(), '13:30:00', '15:00:00', 5),
(22, 10, DATE_ADD(CURDATE(), INTERVAL 1 DAY), '08:00:00', '09:30:00', 5),
(23, 11, CURDATE(), '08:00:00', '09:30:00', 4),
(24, 11, DATE_ADD(CURDATE(), INTERVAL 1 DAY), '13:30:00', '15:00:00', 4),
(25, 12, CURDATE(), '13:30:00', '15:00:00', 5),
(26, 12, DATE_ADD(CURDATE(), INTERVAL 1 DAY), '09:30:00', '11:00:00', 5);

-- Appointments mẫu
INSERT INTO appointments (id, patient_id, doctor_id, schedule_id, status, symptoms) VALUES
(1, 13, 1, 1, 'confirmed', 'Đau tức ngực trái khi vận động mạnh'),
(2, 14, 2, 6, 'pending', 'Bé bị sốt nhẹ về đêm, ho kéo dài 2 ngày');

-- ✅ Payments mẫu 
INSERT INTO payments (id, appointment_id, amount, payment_method, status, paid_at) VALUES
(1, 1, 500000.00, 'cash', 'success', CURRENT_TIMESTAMP),
(2, 2, 450000.00, 'card', 'pending', NULL);

-- =========================================================
-- 11. CẤP QUYỀN CHO USER DOCKER
-- =========================================================
GRANT ALL PRIVILEGES ON MedicalBookingDB.* TO 'meduser'@'%';
FLUSH PRIVILEGES;