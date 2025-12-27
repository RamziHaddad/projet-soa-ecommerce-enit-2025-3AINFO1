"""
Create a small offline SQLite DB with a `user_features` table used by the
feature store fallback. This script is idempotent.
"""
import sqlite3
import json
import os

DB_PATH = os.getenv('OFFLINE_DB', './offline.db')

conn = sqlite3.connect(DB_PATH)
cur = conn.cursor()
cur.execute('''
CREATE TABLE IF NOT EXISTS user_features (
    user_id TEXT PRIMARY KEY,
    features_json TEXT NOT NULL
)
''')

# Insert a small example user
cur.execute('REPLACE INTO user_features (user_id, features_json) VALUES (?,?)', (
    'example_user', json.dumps({'history': ['item_1','item_2'], 'pref_vector': [0.1,0.9]})
))
conn.commit()
conn.close()
print('Created', DB_PATH)
