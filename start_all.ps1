# start_all.ps1
Write-Host "=== 1. Démarrage PostgreSQL ===" -ForegroundColor Cyan
docker start postgres-test
Start-Sleep -Seconds 3

Write-Host "=== 2. Démarrage Service IA ===" -ForegroundColor Cyan
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$PSScriptRoot\ai-service'; .\.venv\Scripts\python.exe -m uvicorn main:app --host 127.0.0.1 --port 8000"

Start-Sleep -Seconds 5

Write-Host "=== 3. Démarrage Backend ===" -ForegroundColor Cyan
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$PSScriptRoot\backend'; mvn spring-boot:run"

Write-Host "=== 4. Démarrage Frontend ===" -ForegroundColor Cyan
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$PSScriptRoot\frontend'; npm start"

Write-Host "=== 5. Ouverture du navigateur ===" -ForegroundColor Cyan
Start-Sleep -Seconds 15
Start-Process "http://localhost:4200"

Write-Host " Application lancée !" -ForegroundColor Green