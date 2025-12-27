#!/usr/bin/env python3
"""
Script to generate a test JWT token for development
Usage: python scripts/generate_token.py
"""
import sys
import os

# Add parent directory to path
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from app.utils.security import create_access_token
from datetime import timedelta


def generate_test_token():
    """Generate a test JWT token for development"""
    
    # Test user data
    test_users = [
        {
            "user_id": 1,
            "username": "alice",
            "roles": ["user"]
        },
        {
            "user_id": 2,
            "username": "bob",
            "roles": ["user"]
        },
        {
            "user_id": 999,
            "username": "admin",
            "roles": ["user", "admin"]
        }
    ]
    
    print("=" * 80)
    print("TEST JWT TOKENS FOR DEVELOPMENT")
    print("=" * 80)
    print()
    
    for user in test_users:
        token = create_access_token(
            data=user,
            expires_delta=timedelta(hours=24)  # 24 hour expiry for testing
        )
        
        print(f"User: {user['username']} (ID: {user['user_id']})")
        print(f"Roles: {', '.join(user['roles'])}")
        print(f"Token: {token}")
        print()
        print(f"curl example:")
        print(f'curl -H "Authorization: Bearer {token}" http://localhost:8000/api/v1/feedback/users/me')
        print()
        print("-" * 80)
        print()
    
    print("⚠️  WARNING: These tokens are for DEVELOPMENT ONLY!")
    print("    Never use these in production.")
    print()


if __name__ == "__main__":
    generate_test_token()
