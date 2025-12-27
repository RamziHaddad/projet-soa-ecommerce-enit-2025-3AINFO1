FROM python:3.11-slim
WORKDIR /app
ENV PYTHONUNBUFFERED=1
COPY requirements.txt ./
RUN set -eux; for i in 1 2 3 4 5; do pip install --no-cache-dir -r requirements.txt && break || echo "pip install failed, retrying ($i)"; sleep 5; done
COPY ./app ./app
COPY ./scripts ./scripts
EXPOSE 8080
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8080"]
