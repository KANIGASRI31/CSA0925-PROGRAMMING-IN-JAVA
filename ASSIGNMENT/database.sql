-- ============================================================
-- Smart Campus Parking and Traffic Management System
-- MySQL Database Schema
-- ============================================================

CREATE DATABASE IF NOT EXISTS campus_parking;
USE campus_parking;

-- ============================================================
-- 1. USERS TABLE
-- ============================================================
CREATE TABLE IF NOT EXISTS users (
    user_id       INT AUTO_INCREMENT PRIMARY KEY,
    full_name     VARCHAR(100) NOT NULL,
    email         VARCHAR(100) NOT NULL UNIQUE,
    phone         VARCHAR(15)  NOT NULL,
    user_type     ENUM('Student','Faculty','Staff','Visitor','Service') NOT NULL,
    id_number     VARCHAR(50)  NOT NULL UNIQUE,
    address       VARCHAR(255),
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- 2. VEHICLES TABLE
-- ============================================================
CREATE TABLE IF NOT EXISTS vehicles (
    vehicle_id    INT AUTO_INCREMENT PRIMARY KEY,
    user_id       INT NOT NULL,
    license_plate VARCHAR(20) NOT NULL UNIQUE,
    vehicle_type  ENUM('Car','Motorcycle','Truck','Bus','Bicycle','Other') NOT NULL,
    make          VARCHAR(50),
    model         VARCHAR(50),
    color         VARCHAR(30),
    registered_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- ============================================================
-- 3. PARKING ZONES TABLE
-- ============================================================
CREATE TABLE IF NOT EXISTS parking_zones (
    zone_id       INT AUTO_INCREMENT PRIMARY KEY,
    zone_name     VARCHAR(50) NOT NULL UNIQUE,
    zone_type     ENUM('Student','Faculty','Staff','Visitor','Service','General') NOT NULL,
    location      VARCHAR(100),
    total_slots   INT NOT NULL DEFAULT 0,
    hourly_rate   DECIMAL(8,2) NOT NULL DEFAULT 0.00,
    description   VARCHAR(255)
);

-- ============================================================
-- 4. PARKING SLOTS TABLE
-- ============================================================
CREATE TABLE IF NOT EXISTS parking_slots (
    slot_id       INT AUTO_INCREMENT PRIMARY KEY,
    zone_id       INT NOT NULL,
    slot_number   VARCHAR(10) NOT NULL,
    slot_type     ENUM('Regular','Handicapped','EV','Reserved') DEFAULT 'Regular',
    status        ENUM('Available','Occupied','Reserved','Maintenance') DEFAULT 'Available',
    UNIQUE KEY uq_zone_slot (zone_id, slot_number),
    FOREIGN KEY (zone_id) REFERENCES parking_zones(zone_id) ON DELETE CASCADE
);

-- ============================================================
-- 5. RESERVATIONS TABLE
-- ============================================================
CREATE TABLE IF NOT EXISTS reservations (
    reservation_id   INT AUTO_INCREMENT PRIMARY KEY,
    user_id          INT NOT NULL,
    vehicle_id       INT NOT NULL,
    slot_id          INT NOT NULL,
    reserved_from    DATETIME NOT NULL,
    reserved_until   DATETIME NOT NULL,
    status           ENUM('Active','Completed','Cancelled','Expired') DEFAULT 'Active',
    created_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id)    REFERENCES users(user_id)    ON DELETE CASCADE,
    FOREIGN KEY (vehicle_id) REFERENCES vehicles(vehicle_id) ON DELETE CASCADE,
    FOREIGN KEY (slot_id)    REFERENCES parking_slots(slot_id) ON DELETE CASCADE
);

-- ============================================================
-- 6. PARKING SESSIONS TABLE
-- ============================================================
CREATE TABLE IF NOT EXISTS parking_sessions (
    session_id    INT AUTO_INCREMENT PRIMARY KEY,
    vehicle_id    INT NOT NULL,
    slot_id       INT NOT NULL,
    user_id       INT NOT NULL,
    entry_time    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    exit_time     DATETIME,
    duration_hrs  DECIMAL(6,2),
    fee_amount    DECIMAL(8,2) DEFAULT 0.00,
    status        ENUM('Active','Completed') DEFAULT 'Active',
    FOREIGN KEY (vehicle_id) REFERENCES vehicles(vehicle_id) ON DELETE CASCADE,
    FOREIGN KEY (slot_id)    REFERENCES parking_slots(slot_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id)    REFERENCES users(user_id)    ON DELETE CASCADE
);

-- ============================================================
-- 7. PARKING PASSES TABLE
-- ============================================================
CREATE TABLE IF NOT EXISTS parking_passes (
    pass_id       INT AUTO_INCREMENT PRIMARY KEY,
    user_id       INT NOT NULL,
    vehicle_id    INT NOT NULL,
    zone_id       INT NOT NULL,
    pass_type     ENUM('Monthly','Semester','Annual') NOT NULL,
    start_date    DATE NOT NULL,
    end_date      DATE NOT NULL,
    pass_fee      DECIMAL(8,2) NOT NULL,
    status        ENUM('Active','Expired','Cancelled') DEFAULT 'Active',
    issued_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id)    REFERENCES users(user_id)    ON DELETE CASCADE,
    FOREIGN KEY (vehicle_id) REFERENCES vehicles(vehicle_id) ON DELETE CASCADE,
    FOREIGN KEY (zone_id)    REFERENCES parking_zones(zone_id) ON DELETE CASCADE
);

-- ============================================================
-- 8. PAYMENTS TABLE
-- ============================================================
CREATE TABLE IF NOT EXISTS payments (
    payment_id    INT AUTO_INCREMENT PRIMARY KEY,
    user_id       INT NOT NULL,
    session_id    INT,
    pass_id       INT,
    amount        DECIMAL(8,2) NOT NULL,
    payment_mode  ENUM('Cash','Card','UPI','Online') NOT NULL,
    payment_date  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status        ENUM('Paid','Pending','Failed') DEFAULT 'Paid',
    FOREIGN KEY (user_id)    REFERENCES users(user_id)    ON DELETE CASCADE,
    FOREIGN KEY (session_id) REFERENCES parking_sessions(session_id) ON DELETE SET NULL,
    FOREIGN KEY (pass_id)    REFERENCES parking_passes(pass_id) ON DELETE SET NULL
);

-- ============================================================
-- 9. VIOLATIONS TABLE
-- ============================================================
CREATE TABLE IF NOT EXISTS violations (
    violation_id      INT AUTO_INCREMENT PRIMARY KEY,
    vehicle_id        INT NOT NULL,
    slot_id           INT,
    violation_type    VARCHAR(100) NOT NULL,
    description       VARCHAR(255),
    fine_amount       DECIMAL(8,2) NOT NULL DEFAULT 0.00,
    violation_date    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status            ENUM('Pending','Paid','Waived') DEFAULT 'Pending',
    FOREIGN KEY (vehicle_id) REFERENCES vehicles(vehicle_id) ON DELETE CASCADE,
    FOREIGN KEY (slot_id)    REFERENCES parking_slots(slot_id) ON DELETE SET NULL
);

-- ============================================================
-- STORED PROCEDURE: Record Vehicle Entry
-- ============================================================
DELIMITER $$
CREATE PROCEDURE IF NOT EXISTS sp_vehicle_entry(
    IN p_vehicle_id  INT,
    IN p_slot_id     INT,
    IN p_user_id     INT,
    OUT p_session_id INT
)
BEGIN
    DECLARE slot_status VARCHAR(20);
    SELECT status INTO slot_status FROM parking_slots WHERE slot_id = p_slot_id;
    IF slot_status <> 'Available' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Slot is not available';
    ELSE
        INSERT INTO parking_sessions(vehicle_id, slot_id, user_id, entry_time, status)
        VALUES(p_vehicle_id, p_slot_id, p_user_id, NOW(), 'Active');
        SET p_session_id = LAST_INSERT_ID();
        UPDATE parking_slots SET status = 'Occupied' WHERE slot_id = p_slot_id;
    END IF;
END$$
DELIMITER ;

-- ============================================================
-- STORED PROCEDURE: Record Vehicle Exit & Calculate Fee
-- ============================================================
DELIMITER $$
CREATE PROCEDURE IF NOT EXISTS sp_vehicle_exit(
    IN  p_session_id INT,
    OUT p_fee        DECIMAL(8,2)
)
BEGIN
    DECLARE v_entry    DATETIME;
    DECLARE v_slot     INT;
    DECLARE v_rate     DECIMAL(8,2);
    DECLARE v_hours    DECIMAL(6,2);

    SELECT s.entry_time, s.slot_id, z.hourly_rate
    INTO   v_entry, v_slot, v_rate
    FROM   parking_sessions s
    JOIN   parking_slots sl ON s.slot_id = sl.slot_id
    JOIN   parking_zones  z  ON sl.zone_id = z.zone_id
    WHERE  s.session_id = p_session_id;

    SET v_hours = GREATEST(CEIL(TIMESTAMPDIFF(MINUTE, v_entry, NOW()) / 60.0), 1);
    SET p_fee   = v_hours * v_rate;

    UPDATE parking_sessions
    SET    exit_time   = NOW(),
           duration_hrs = v_hours,
           fee_amount  = p_fee,
           status      = 'Completed'
    WHERE  session_id  = p_session_id;

    UPDATE parking_slots SET status = 'Available' WHERE slot_id = v_slot;
END$$
DELIMITER ;

-- ============================================================
-- SAMPLE DATA
-- ============================================================
INSERT INTO parking_zones(zone_name, zone_type, location, total_slots, hourly_rate, description) VALUES
('Zone-A', 'Student',  'Block A - North',   50, 10.00, 'Student parking near hostel'),
('Zone-B', 'Faculty',  'Block B - Admin',   30, 0.00,  'Free faculty parking'),
('Zone-C', 'Visitor',  'Main Gate',         20, 20.00, 'Visitor parking'),
('Zone-D', 'Staff',    'Block D - East',    40, 5.00,  'Staff parking'),
('Zone-E', 'Service',  'Service Entrance',  10, 0.00,  'Service vehicles only'),
('Zone-F', 'General',  'Central Campus',    60, 15.00, 'General purpose parking');

-- Create slots for each zone
INSERT INTO parking_slots(zone_id, slot_number, slot_type, status)
SELECT z.zone_id,
       CONCAT(SUBSTRING(z.zone_name,6,1), LPAD(n.n, 2, '0')),
       CASE WHEN n.n % 10 = 1 THEN 'Handicapped'
            WHEN n.n % 10 = 2 THEN 'EV'
            ELSE 'Regular' END,
       'Available'
FROM   parking_zones z
JOIN   (SELECT 1 n UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5
        UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10) n
WHERE  z.total_slots >= n.n;
