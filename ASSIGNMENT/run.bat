@echo off
REM  Run only (assumes already compiled)
SET PROJECT_ROOT=%~dp0
java -cp "%PROJECT_ROOT%out;%PROJECT_ROOT%lib\mysql-connector-j.jar" parking.ui.MainFrame
