#!/bin/bash

# 检查 .env 是否存在
if [ ! -f .env ]; then
    echo "Creating .env from .env.example..."
    cp .env.example .env
    echo "⚠️  Please open .env and update CODEHUB_DASH_SCOPE_KEY with your API Key."
    echo "   After updating, run this script again."
    exit 1
fi

echo "🚀 Starting Codehub Backend..."
docker-compose up -d --build

echo "✅ Services started!"
echo "   App: http://localhost:8125"
echo "   Docs: http://localhost:8125/doc.html"
echo "   Logs are streaming below (Ctrl+C to exit logs)..."
echo "---------------------------------------------------"

docker-compose logs -f
