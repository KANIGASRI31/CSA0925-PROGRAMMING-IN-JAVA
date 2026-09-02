# Smart Campus Parking - Launch Script
Set-Location "c:\Users\MAKESH S\OneDrive\Desktop\parking"
Write-Host "Launching Smart Campus Parking System..." -ForegroundColor Green
java -cp "out;lib/mysql-connector-j.jar" parking.ui.MainFrame
