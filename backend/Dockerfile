FROM python:3.11-slim

ENV PIP_DISABLE_PIP_VERSION_CHECK=1 \
    PIP_NO_CACHE_DIR=1 \
    PIP_DEFAULT_TIMEOUT=300 \
    PIP_RETRIES=10 \
    PIP_ROOT_USER_ACTION=ignore

WORKDIR /app
COPY requirements.txt .
# Preinstall a CPU-only torch wheel so downstream ML deps do not pull the
# massive default CUDA-enabled Linux wheel during image builds.
RUN pip install --index-url https://download.pytorch.org/whl/cpu "torch==2.5.1+cpu"
RUN pip install -r requirements.txt

COPY app ./app
COPY .env.example ./.env.example

EXPOSE 8000
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
