FROM python:3.11-slim

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    PIP_DISABLE_PIP_VERSION_CHECK=1

WORKDIR /app

# ffmpeg is used to decode browser-recorded formats (e.g. audio/webm) into 16k mono float32 PCM.
RUN apt-get update \
  && apt-get install -y --no-install-recommends ffmpeg \
  && rm -rf /var/lib/apt/lists/*

COPY requirements.txt ./requirements.txt
RUN pip install --no-cache-dir -r requirements.txt

COPY server.py ./server.py

RUN useradd -m -u 1000 app \
  && mkdir -p /data/models \
  && chown -R app:app /app /data

USER app

ENV PORT=8010 \
    WHISPER_DEVICE=NPU \
    WHISPER_MODEL_ID=anubhav200/openai-whisper-small-openvino-int4 \
    WHISPER_NPU_COMPILER_TYPE=DRIVER \
    MODEL_DIR=/data/models

EXPOSE 8010

CMD ["python", "server.py"]
