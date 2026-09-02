@echo off
REM ============================================================
REM  Smart Campus Parking & Traffic Management System
REM  Build & Run Script
REM ============================================================

SET "ROOT=%~dp0"
SET "LIB=%ROOT%lib\mysql-connector-j.jar"
SET "OUT=%ROOT%out"
SET "MAIN=parking.ui.MainFrame"

REM ── Check JDBC driver ──────────────────────────────────────
IF NOT EXIST "%LIB%" (
    echo  ERROR: JDBC driver not found at lib\mysql-connector-j.jar
    pause & exit /b 1
)

REM ── Create output dir ──────────────────────────────────────
IF NOT EXIST "%OUT%" mkdir "%OUT%"

REM ── Compile using wildcard per package (avoids space issues) ──
echo Compiling...
javac -cp "%LIB%" -d "%OUT%" ^
  "%ROOT%src\parking\db\DBConnection.java" ^
  "%ROOT%src\parking\model\User.java" ^
  "%ROOT%src\parking\model\Vehicle.java" ^
  "%ROOT%src\parking\model\ParkingZone.java" ^
  "%ROOT%src\parking\model\ParkingSlot.java" ^
  "%ROOT%src\parking\model\Reservation.java" ^
  "%ROOT%src\parking\model\ParkingSession.java" ^
  "%ROOT%src\parking\model\ParkingPass.java" ^
  "%ROOT%src\parking\model\Payment.java" ^
  "%ROOT%src\parking\model\Violation.java" ^
  "%ROOT%src\parking\dao\UserDAO.java" ^
  "%ROOT%src\parking\dao\VehicleDAO.java" ^
  "%ROOT%src\parking\dao\ParkingZoneDAO.java" ^
  "%ROOT%src\parking\dao\ParkingSlotDAO.java" ^
  "%ROOT%src\parking\dao\ReservationDAO.java" ^
  "%ROOT%src\parking\dao\SessionDAO.java" ^
  "%ROOT%src\parking\dao\PassDAO.java" ^
  "%ROOT%src\parking\dao\PaymentDAO.java" ^
  "%ROOT%src\parking\dao\ViolationDAO.java" ^
  "%ROOT%src\parking\ui\MainFrame.java" ^
  "%ROOT%src\parking\ui\UserVehiclePanel.java" ^
  "%ROOT%src\parking\ui\ZoneSlotPanel.java" ^
  "%ROOT%src\parking\ui\ReservationPanel.java" ^
  "%ROOT%src\parking\ui\EntryExitPanel.java" ^
  "%ROOT%src\parking\ui\PassPaymentPanel.java" ^
  "%ROOT%src\parking\ui\ViolationPanel.java" ^
  "%ROOT%src\parking\ui\ReportsPanel.java"

IF %ERRORLEVEL% NEQ 0 (
    echo.
    echo  COMPILATION FAILED.
    pause & exit /b 1
)

echo  Compilation successful!
echo.
echo  Launching application...
java -cp "%OUT%;%LIB%" %MAIN%
