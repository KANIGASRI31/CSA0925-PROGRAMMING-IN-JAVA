# Smart Campus Parking & Traffic Management System
### CSA09 – Programming in Java | AWT/Swing + JDBC (MySQL)

---

## Prerequisites
| Tool | Version |
|------|---------|
| Java JDK | 17+ (uses text blocks) |
| MySQL Server | 8.0+ |
| MySQL Connector/J | 8.x |

---

## Setup Steps

### 1. Download MySQL JDBC Driver
- Go to: https://dev.mysql.com/downloads/connector/j/
- Download **Platform Independent** ZIP
- Extract and copy `mysql-connector-j-*.jar` to the `lib\` folder
- Rename it to `mysql-connector-j.jar`

### 2. Create Database
Open MySQL Workbench or MySQL CLI and run:
```sql
source C:/Users/MAKESH S/OneDrive/Desktop/parking/database.sql
```
Or paste the contents of `database.sql` directly.

### 3. Configure DB Password
Open `src\parking\db\DBConnection.java` and update line:
```java
private static final String PASSWORD = "";   // ← put your MySQL root password here
```

### 4. Compile & Run
Double-click `compile.bat`  
OR open a terminal in the project root and run:
```
compile.bat
```

---

## Project Structure
```
parking/
├── compile.bat              ← Build + Run
├── run.bat                  ← Run only (after compile)
├── database.sql             ← MySQL schema + sample data
├── lib/
│   └── mysql-connector-j.jar   ← place JDBC driver here
├── src/parking/
│   ├── db/
│   │   └── DBConnection.java
│   ├── model/               ← 9 entity classes
│   │   ├── User.java
│   │   ├── Vehicle.java
│   │   ├── ParkingZone.java
│   │   ├── ParkingSlot.java
│   │   ├── Reservation.java
│   │   ├── ParkingSession.java
│   │   ├── ParkingPass.java
│   │   ├── Payment.java
│   │   └── Violation.java
│   ├── dao/                 ← 9 DAO classes (JDBC operations)
│   │   ├── UserDAO.java
│   │   ├── VehicleDAO.java
│   │   ├── ParkingZoneDAO.java
│   │   ├── ParkingSlotDAO.java
│   │   ├── ReservationDAO.java
│   │   ├── SessionDAO.java       ← uses CallableStatement
│   │   ├── PassDAO.java
│   │   ├── PaymentDAO.java
│   │   └── ViolationDAO.java
│   └── ui/                  ← 7 Swing UI classes
│       ├── MainFrame.java        ← main window + menu bar
│       ├── UserVehiclePanel.java
│       ├── ZoneSlotPanel.java
│       ├── ReservationPanel.java
│       ├── EntryExitPanel.java
│       ├── PassPaymentPanel.java
│       ├── ViolationPanel.java
│       └── ReportsPanel.java
└── out/                     ← compiled .class files (auto-generated)
```

---

## Features

| Module | Functionality |
|--------|--------------|
| **Users & Vehicles** | Register, update, delete, search users and vehicles |
| **Zones & Slots** | Add/edit parking zones, manage slots with colour-coded status |
| **Reservations** | Book slots with conflict detection, cancel reservations |
| **Entry / Exit** | Record vehicle entry via stored procedure, exit with auto fee calculation |
| **Passes & Payments** | Issue monthly/semester/annual passes, track all payments |
| **Violations** | Record violations, mark paid/waived, search by plate |
| **Reports (8 types)** | Zone occupancy, vehicle history, revenue, active sessions, utilisation %, violation summary, pass status, daily revenue |

---

## JDBC Statement Types Used
| Type | Where Used |
|------|-----------|
| `Statement` | SELECT all queries (UserDAO, ZoneDAO, etc.) |
| `PreparedStatement` | All INSERT, UPDATE, DELETE, filtered SELECT |
| `CallableStatement` | `sp_vehicle_entry` and `sp_vehicle_exit` stored procedures |

---

## SDG Alignment
- **SDG 9** – Digitises campus infrastructure management
- **SDG 11** – Reduces parking congestion in campus communities  
- **SDG 13** – Encourages EV slot allocation and efficient space usage
