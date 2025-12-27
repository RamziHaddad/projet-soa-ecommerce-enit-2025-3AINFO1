#!/bin/bash
# Quick development setup script for Unix-like systems

echo "🚀 Starting Feedback Service Development Setup..."
echo ""

# Check if uv is installed
if command -v uv &> /dev/null; then
    echo "✅ uv is installed"
    USE_UV=true
else
    echo "⚠️  uv not found, using pip instead"
    echo "   Install uv for faster dependency management: curl -LsSf https://astral.sh/uv/install.sh | sh"
    USE_UV=false
fi

# Create virtual environment
if [ "$USE_UV" = true ]; then
    echo "📦 Creating virtual environment with uv..."
    uv venv
    source .venv/bin/activate
    echo "📥 Installing dependencies with uv..."
    uv pip install -r requirements.txt
else
    echo "📦 Creating virtual environment with venv..."
    python -m venv venv
    source venv/bin/activate
    echo "📥 Installing dependencies with pip..."
    pip install -r requirements.txt
fi

# Create .env if it doesn't exist
if [ ! -f .env ]; then
    echo "📝 Creating .env file from template..."
    cp .env.example .env
    echo "⚠️  Please edit .env with your configuration"
fi

# Start Docker services
echo "🐳 Starting PostgreSQL and Redis..."
docker-compose up -d db redis

# Wait for services
echo "⏳ Waiting for services to be ready..."
sleep 5

# Run migrations
echo "🔄 Running database migrations..."
alembic upgrade head

echo ""
echo "✅ Setup complete!"
echo ""
echo "Next steps:"
echo "  1. Edit .env with your configuration (if needed)"
echo "  2. Start the app: uvicorn app.main:app --reload"
echo "  3. Open http://localhost:8000/docs"
echo ""
echo "Generate test tokens: python scripts/generate_token.py"
echo ""
