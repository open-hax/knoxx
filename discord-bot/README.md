# Knoxx Discord Bot

A Discord bot that provides intelligent answers from your documentation using the Knoxx platform APIs.

## Features

- `/ask` - Slash command for asking questions
- `/status` - Check bot and backend health
- `/help` - Display help information
- `!docs` - Message command (customizable prefix)
- Rich embeds with source links
- Per-channel conversation context
- Configurable rate limiting

## Quick Setup

### 1. Discord Application Setup

1. Go to [Discord Developer Portal](https://discord.com/developers/applications)
2. Click "New Application" and name it
3. Go to "Bot" section and click "Add Bot"
4. **Enable "Message Content Intent"** (required for message commands)
5. Copy the **Bot Token** and **Client ID**

### 2. Bot Permissions & Invite

1. In Developer Portal → "OAuth2" → "URL Generator"
2. Select scopes: `bot`, `applications.commands`
3. Select permissions: `Send Messages`, `Embed Links`, `Use Slash Commands`
4. Use generated URL to invite bot to your server

### 3. Configuration

Run the setup wizard:

```bash
cd discord-bot
npm install
npm run setup
```

Or manually create `.env`:

```env
DISCORD_BOT_TOKEN=your-bot-token
DISCORD_CLIENT_ID=your-client-id
DISCORD_GUILD_ID=optional-for-testing
RAG_API_URL=http://localhost:8000/api/knoxx
RAG_API_KEY=
BOT_NAME=Docs Bot
BOT_COMMAND_PREFIX=!docs
BOT_EMBED_COLOR=0x7c3aed
COOLDOWN_SECONDS=5
LOG_LEVEL=info
```

API target modes:

- Recommended proxy mode: `RAG_API_URL=http://localhost:8000/api/knoxx` (no API key needed; backend handles Knoxx auth)
- Direct Knoxx mode: `RAG_API_URL=http://localhost:3001/api` and set `RAG_API_KEY`

### 4. Run

```bash
# Register slash commands (one-time)
npm run register

# Development
npm run dev

# Production
npm run build && npm start
```

## Docker

```bash
docker build -t knoxx-discord-bot .
docker run -d --env-file .env knoxx-discord-bot
```

Or use with docker-compose (from knoxx root):

```bash
docker compose --profile with-discord up -d
```

## Commands

| Command | Description |
|---------|-------------|
| `/ask <question>` | Ask a question about the docs |
| `/status` | Check bot health |
| `/help` | Show help |
| `!docs <question>` | Message command alternative |

## Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `DISCORD_BOT_TOKEN` | Bot token from Discord | Required |
| `DISCORD_CLIENT_ID` | Application client ID | Required |
| `DISCORD_GUILD_ID` | Guild ID for testing | Optional |
| `RAG_API_URL` | Knoxx API endpoint | `http://localhost:8000/api/knoxx` |
| `RAG_API_KEY` | Knoxx API key | Optional |
| `BOT_NAME` | Bot display name | `Docs Bot` |
| `BOT_COMMAND_PREFIX` | Message command prefix | `!docs` |
| `BOT_EMBED_COLOR` | Embed color (hex) | `0x7c3aed` |
| `COOLDOWN_SECONDS` | Rate limit cooldown | `5` |
| `LOG_LEVEL` | Logging level | `info` |

## License

MIT
