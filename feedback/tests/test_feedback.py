"""
Basic tests for Feedback API
Run with: pytest tests/
"""
import pytest
from fastapi.testclient import TestClient
from app.main import app

client = TestClient(app)


def test_health_check():
    """Test health check endpoint"""
    response = client.get("/health")
    assert response.status_code == 200
    assert response.json()["status"] == "healthy"


def test_root_endpoint():
    """Test root endpoint"""
    response = client.get("/")
    assert response.status_code == 200
    assert "service" in response.json()
    assert "version" in response.json()


def test_get_rating_summary_no_auth():
    """Test that rating summary endpoint works without auth"""
    # This will fail if product doesn't exist, but tests the endpoint is accessible
    response = client.get("/api/v1/feedback/products/999/summary")
    # Should return 200 even for non-existent product (empty summary)
    assert response.status_code == 200


def test_create_feedback_without_auth():
    """Test that creating feedback requires authentication"""
    response = client.post(
        "/api/v1/feedback",
        json={
            "product_id": 123,
            "rating": 5,
            "comment": "Test comment"
        }
    )
    # Should return 403 (no auth header) or 401 (invalid token)
    assert response.status_code in [401, 403]


def test_get_product_feedback_no_auth():
    """Test that getting product feedback works without auth"""
    response = client.get("/api/v1/feedback/products/123?page=1&size=20")
    # Should return 200 even if no feedback exists
    assert response.status_code == 200


# Add more tests as needed
# - Test with valid JWT tokens
# - Test upsert logic
# - Test validation errors
# - Test cache behavior
