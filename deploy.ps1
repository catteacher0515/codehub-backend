if (-not (Test-Path .env)) {
    Write-Host "Creating .env from .env.example..."
    Copy-Item .env.example .env
    Write-Host "⚠️  Please open .env and update CODEHUB_DASH_SCOPE_KEY with your API Key."
    Write-Host "   After updating, run this script again."
    exit
}

Write-Host "🚀 Starting Codehub Backend..."
docker-compose up -d --build

Write-Host "✅ Services started!"
Write-Host "   App: http://localhost:8125"
Write-Host "   Docs: http://localhost:8125/doc.html"
Write-Host "   Logs are streaming below (Ctrl+C to exit logs)..."
Write-Host "---------------------------------------------------"

docker-compose logs -f
