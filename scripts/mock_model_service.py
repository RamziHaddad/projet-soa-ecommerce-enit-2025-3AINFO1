"""
Very small mock model-serving HTTP server used for integration in docker-compose.
It exposes POST /predict and returns a simple deterministic top-k mapping.
This is intentionally minimal and *not* a real model server.
"""
from http.server import BaseHTTPRequestHandler, HTTPServer
import json

class Handler(BaseHTTPRequestHandler):
    def _set_headers(self, status=200):
        self.send_response(status)
        self.send_header('Content-Type', 'application/json')
        self.end_headers()

    def do_POST(self):
        if self.path != '/predict':
            self._set_headers(404)
            self.wfile.write(json.dumps({'error': 'not found'}).encode())
            return
        length = int(self.headers.get('content-length', 0))
        body = self.rfile.read(length)
        try:
            payload = json.loads(body)
            top_k = payload.get('top_k', 10)
        except Exception:
            self._set_headers(400)
            self.wfile.write(json.dumps({'error': 'bad request'}).encode())
            return
        # Build deterministic scores
        resp = {f"item_{i}": 1.0/(i+1) for i in range(min(100, top_k))}
        self._set_headers(200)
        self.wfile.write(json.dumps(resp).encode())

if __name__ == '__main__':
    server = HTTPServer(('0.0.0.0', 5001), Handler)
    print('Mock model server listening on 5001')
    server.serve_forever()
