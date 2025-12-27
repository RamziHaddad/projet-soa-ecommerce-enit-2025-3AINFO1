import pytest
import asyncio

from app.recommender import score_items_for_user, recommend_similar_product

class DummyModel:
    @staticmethod
    async def get_prediction(features, top_k=10):
        # echo candidate items or return deterministic mapping
        candidates = features.get('candidate_items')
        if candidates:
            return {c: float(len(c)) for c in candidates[:top_k]}
        base = features.get('base_item')
        if base:
            return {f"{base}_sim_{i}": 1.0/(i+1) for i in range(top_k)}
        return {f"item_{i}": 1.0/(i+1) for i in range(top_k)}

@pytest.fixture(autouse=True)
def patch_model(monkeypatch):
    import app.model_client as mc
    monkeypatch.setattr(mc, 'get_prediction', DummyModel.get_prediction)


def test_score_items_for_user_model():
    features = {'history': ['a','b']}
    res = asyncio.get_event_loop().run_until_complete(score_items_for_user(features, candidate_items=['c','dd','eee'], top_k=3))
    assert set(res.keys()) == {'c','dd','eee'}


def test_recommend_similar_model():
    # seed offline features by monkeypatching feature_store
    import app.feature_store as fs
    monkeypatch = pytest.MonkeyPatch()
    monkeypatch.setattr(fs, 'get_features_for_user', lambda uid: {'history': ['p1','p2']})
    try:
        res = asyncio.get_event_loop().run_until_complete(recommend_similar_product('user1', top_k=3))
        assert any('p2_sim' in k for k in res.keys())
    finally:
        monkeypatch.undo()
