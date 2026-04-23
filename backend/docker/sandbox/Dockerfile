FROM nikolaik/python-nodejs:python3.12-nodejs22-bookworm

USER 0:0
RUN apt-get update \
    && apt-get install -y --no-install-recommends ffmpeg imagemagick git ca-certificates \
    && rm -rf /var/lib/apt/lists/* \
    && mkdir -p /workspace \
    && chown -R 1000:1000 /workspace

USER 1000:1000
WORKDIR /workspace
CMD ["sh", "-lc", "trap 'exit 0' TERM INT; while true; do sleep 3600; done"]
