
import pytest

class DummyFeatureStore:
    @staticmethod
    def get_features_for_user(user_id: str):
        return {"user_id": user_id, "history": ["item_1", "item_2"]}

class DummyModelClient:
    @staticmethod
    async def get_prediction(features, top_k=10):
        return {f"item_{i}": 1.0/(i+1) for i in range(min(top_k, 5))}

@pytest.fixture
def client(monkeypatch):
    import app.feature_store as fs
    import app.model_client as mc
    monkeypatch.setattr(fs, 'get_features_for_user', DummyFeatureStore.get_features_for_user)
    monkeypatch.setattr(mc, 'get_prediction', DummyModelClient.get_prediction)
    from fastapi.testclient import TestClient
    from app.main import app
    return TestClient(app)


def test_recommend_ok(client):
    r = client.post('/recommend', json={'user_id': 'u1', 'top_k': 3})
    assert r.status_code == 200
    data = r.json()
    assert data['user_id'] == 'u1'
    assert len(data['items']) == 3


def test_recommend_missing_user(client, monkeypatch):
    import app.feature_store as fs
    monkeypatch.setattr(fs, 'get_features_for_user', lambda uid: {})
    r = client.post('/recommend', json={'user_id': 'missing'})
    assert r.status_code == 404
