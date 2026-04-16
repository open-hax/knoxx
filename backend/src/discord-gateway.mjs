import {
  ChannelType,
  Client,
  Events,
  GatewayIntentBits,
  Partials,
} from 'discord.js';

function mapMessage(message) {
  return {
    id: message.id,
    channelId: message.channelId,
    content: message.content ?? '',
    authorId: message.author?.id ?? '',
    authorUsername: message.author?.username ?? 'unknown',
    authorIsBot: Boolean(message.author?.bot),
    timestamp: message.createdAt?.toISOString?.() ?? new Date().toISOString(),
    attachments: [...message.attachments.values()].map((attachment) => ({
      id: attachment.id,
      filename: attachment.name ?? '',
      contentType: attachment.contentType ?? null,
      size: attachment.size ?? 0,
      url: attachment.url ?? '',
    })),
    embeds: message.embeds.map((embed) => ({
      title: embed.title ?? null,
      description: embed.description ?? null,
      url: embed.url ?? null,
    })),
  };
}

function isReadableTextChannel(channel) {
  if (!channel) return false;
  if (typeof channel.isTextBased === 'function' && channel.isTextBased()) return true;
  return false;
}

function sortNewestFirst(messages) {
  return [...messages].sort((a, b) => String(b.timestamp).localeCompare(String(a.timestamp)));
}

export function createDiscordGatewayManager({ log = console } = {}) {
  let client = null;
  let readyPromise = null;
  let currentToken = null;
  const listeners = new Set();

  const notifyMessage = (message) => {
    const mapped = mapMessage(message);
    for (const listener of listeners) {
      try {
        listener(mapped, message);
      } catch (error) {
        log.error?.('[discord-gateway] listener failed', error);
      }
    }
  };

  const buildClient = () => {
    const next = new Client({
      intents: [
        GatewayIntentBits.Guilds,
        GatewayIntentBits.GuildMessages,
        GatewayIntentBits.DirectMessages,
        GatewayIntentBits.MessageContent,
      ],
      partials: [Partials.Channel],
    });

    next.on(Events.ClientReady, (readyClient) => {
      log.info?.(`[discord-gateway] ready as ${readyClient.user?.tag ?? 'unknown'} in ${readyClient.guilds.cache.size} guilds`);
    });

    next.on(Events.MessageCreate, (message) => {
      notifyMessage(message);
    });

    next.on(Events.Error, (error) => {
      log.error?.('[discord-gateway] client error', error);
    });

    return next;
  };

  const ensureClient = async () => {
    if (!client) {
      throw new Error('Discord gateway client is not started');
    }
    if (readyPromise) {
      await readyPromise;
    }
    return client;
  };

  const splitMessage = (text) => {
    const normalized = String(text ?? '').trim();
    if (normalized.length <= 2000) return [normalized];
    const parts = [];
    let remaining = normalized;
    while (remaining.length > 2000) {
      let splitAt = remaining.lastIndexOf('\n\n', 2000);
      if (splitAt < 1000) splitAt = remaining.lastIndexOf('\n', 2000);
      if (splitAt < 1000) splitAt = remaining.lastIndexOf(' ', 2000);
      if (splitAt <= 0) splitAt = 2000;
      parts.push(remaining.slice(0, splitAt).trimEnd());
      remaining = remaining.slice(splitAt).trimStart();
    }
    if (remaining.length > 0) parts.push(remaining);
    return parts;
  };

  return {
    async start(token) {
      const nextToken = String(token ?? '').trim();
      if (!nextToken) {
        await this.stop();
        return null;
      }
      if (client && currentToken === nextToken) {
        await readyPromise;
        return client;
      }
      await this.stop();
      currentToken = nextToken;
      client = buildClient();
      readyPromise = client.login(nextToken)
        .then(() => client)
        .catch(async (error) => {
          log.error?.('[discord-gateway] login failed', error);
          try {
            await client?.destroy();
          } catch {}
          client = null;
          readyPromise = null;
          currentToken = null;
          throw error;
        });
      return readyPromise;
    },

    async stop() {
      if (client) {
        try {
          await client.destroy();
        } catch {}
      }
      client = null;
      readyPromise = null;
      currentToken = null;
    },

    async restart(token) {
      await this.stop();
      return this.start(token);
    },

    onMessage(listener) {
      listeners.add(listener);
      return () => listeners.delete(listener);
    },

    status() {
      return {
        started: Boolean(client),
        ready: Boolean(client?.isReady?.()),
        userTag: client?.user?.tag ?? null,
        guildCount: client?.guilds?.cache?.size ?? 0,
      };
    },

    async listServers() {
      const activeClient = await ensureClient();
      return [...activeClient.guilds.cache.values()].map((guild) => ({
        id: guild.id,
        name: guild.name,
        memberCount: guild.memberCount ?? null,
      }));
    },

    async listChannels(guildId) {
      const activeClient = await ensureClient();
      const collect = async (guild) => {
        const fetched = await guild.channels.fetch();
        return [...fetched.values()]
          .filter((channel) => channel && isReadableTextChannel(channel) && channel.type !== ChannelType.DM)
          .map((channel) => ({
            id: channel.id,
            name: channel.name ?? '',
            guildId: guild.id,
            type: String(channel.type),
          }));
      };

      if (guildId) {
        const guild = activeClient.guilds.cache.get(guildId);
        if (!guild) throw new Error(`Guild not found: ${guildId}`);
        return collect(guild);
      }

      const all = [];
      for (const guild of activeClient.guilds.cache.values()) {
        try {
          all.push(...await collect(guild));
        } catch (error) {
          log.warn?.('[discord-gateway] listChannels guild failed', guild.id, error);
        }
      }
      return all;
    },

    async fetchChannelMessages(channelId, { limit = 50, before, after, around } = {}) {
      const activeClient = await ensureClient();
      const channel = await activeClient.channels.fetch(channelId);
      if (!channel || !isReadableTextChannel(channel)) {
        throw new Error(`Channel not found or not text-based: ${channelId}`);
      }
      const fetched = await channel.messages.fetch({
        limit: Math.max(1, Math.min(100, limit)),
        before,
        after,
        around,
      });
      return sortNewestFirst([...fetched.values()].map(mapMessage));
    },

    async fetchDmMessages(userId, { limit = 50, before } = {}) {
      const activeClient = await ensureClient();
      const user = await activeClient.users.fetch(userId);
      const dm = await user.createDM();
      const fetched = await dm.messages.fetch({
        limit: Math.max(1, Math.min(100, limit)),
        before,
      });
      return {
        dmChannelId: dm.id,
        messages: sortNewestFirst([...fetched.values()].map(mapMessage)),
      };
    },

    async searchMessages(scope, options = {}) {
      const normalizedScope = String(scope ?? 'channel').toLowerCase();
      if (normalizedScope === 'dm') {
        const result = await this.fetchDmMessages(options.userId, { limit: 100, before: options.before });
        const needle = String(options.query ?? '').toLowerCase();
        const filtered = result.messages.filter((message) => {
          const queryOk = !needle || message.content.toLowerCase().includes(needle);
          const authorOk = !options.userId || message.authorId === options.userId;
          return queryOk && authorOk;
        });
        return {
          ...result,
          messages: filtered.slice(0, options.limit ?? 50),
          count: Math.min(filtered.length, options.limit ?? 50),
          source: 'gateway-cache',
        };
      }

      const messages = await this.fetchChannelMessages(options.channelId, {
        limit: 100,
        before: options.before,
        after: options.after,
      });
      const needle = String(options.query ?? '').toLowerCase();
      const filtered = messages.filter((message) => {
        const queryOk = !needle || message.content.toLowerCase().includes(needle);
        const authorOk = !options.userId || message.authorId === options.userId;
        return queryOk && authorOk;
      });
      return {
        channelId: options.channelId,
        messages: filtered.slice(0, options.limit ?? 50),
        count: Math.min(filtered.length, options.limit ?? 50),
        source: 'gateway-cache',
      };
    },

    async sendMessage(channelId, text, replyTo) {
      const activeClient = await ensureClient();
      const channel = await activeClient.channels.fetch(channelId);
      if (!channel || !isReadableTextChannel(channel)) {
        throw new Error(`Channel not found or not text-based: ${channelId}`);
      }
      const chunks = splitMessage(text);
      let sent = null;
      for (const [index, chunk] of chunks.entries()) {
        const payload = { content: chunk };
        if (index === 0 && replyTo) {
          payload.reply = { messageReference: replyTo };
        }
        sent = await channel.send(payload);
      }
      return {
        channelId,
        messageId: sent?.id ?? '',
        sent: true,
        timestamp: sent?.createdAt?.toISOString?.() ?? new Date().toISOString(),
        chunkCount: chunks.length,
      };
    },
  };
}
