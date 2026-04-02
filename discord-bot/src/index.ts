import {
  Client,
  GatewayIntentBits,
  Events,
  Interaction,
  ChatInputCommandInteraction,
  Message,
  EmbedBuilder,
  ActionRowBuilder,
  ButtonBuilder,
  ButtonStyle,
  ButtonInteraction,
} from 'discord.js';
import { env, logger } from './config/index.js';
import { commands } from './commands/index.js';
import { ragApi, type Source, type ImageResult } from './services/index.js';
import { lastResponseImages } from './commands/ask.js';
import { imageSearchResults } from './commands/images.js';

const userCooldowns = new Map<string, number>();
const channelConversations = new Map<string, string>();

const client = new Client({
  intents: [GatewayIntentBits.Guilds, GatewayIntentBits.GuildMessages, GatewayIntentBits.MessageContent],
});

client.once(Events.ClientReady, async (readyClient) => {
  logger.info(
    { username: readyClient.user.tag, guilds: readyClient.guilds.cache.size },
    `🤖 ${env.BOT_NAME} is online and ready to chat!`
  );

  try {
    const health = await ragApi.health();
    logger.info({ health }, 'Connected to knowledge base');
  } catch (error) {
    logger.warn({ error }, 'Knowledge base not reachable at startup - will retry on commands');
  }
});

// Handle slash commands
client.on(Events.InteractionCreate, async (interaction: Interaction) => {
  // Handle button interactions
  if (interaction.isButton()) {
    await handleButtonInteraction(interaction);
    return;
  }

  if (!interaction.isChatInputCommand()) return;

  const command = commands.get(interaction.commandName);

  if (!command) {
    logger.warn({ commandName: interaction.commandName }, 'Unknown command received');
    return;
  }

  const userId = interaction.user.id;
  const now = Date.now();
  const lastUsed = userCooldowns.get(userId) || 0;
  const cooldownMs = env.COOLDOWN_SECONDS * 1000;

  if (now - lastUsed < cooldownMs) {
    const remainingSeconds = Math.ceil((cooldownMs - (now - lastUsed)) / 1000);
    await interaction.reply({
      content: `Hold up! ⏳ Give me ${remainingSeconds} more second${remainingSeconds !== 1 ? 's' : ''} before asking again.`,
      ephemeral: true,
    });
    return;
  }

  userCooldowns.set(userId, now);

  // Cleanup old cooldowns
  if (userCooldowns.size > 10000) {
    const cutoff = now - cooldownMs * 2;
    for (const [uid, time] of userCooldowns.entries()) {
      if (time < cutoff) userCooldowns.delete(uid);
    }
  }

  try {
    logger.debug(
      { commandName: interaction.commandName, userId, guildId: interaction.guildId },
      'Executing command'
    );
    await command.execute(interaction as ChatInputCommandInteraction);
  } catch (error) {
    logger.error({ error, commandName: interaction.commandName, userId }, 'Command execution failed');

    const errorMessage = "Something went wrong on my end 😅 Mind trying that again?";

    if (interaction.replied || interaction.deferred) {
      await interaction.followUp({ content: errorMessage, ephemeral: true });
    } else {
      await interaction.reply({ content: errorMessage, ephemeral: true });
    }
  }
});

// Handle button interactions (for "more images" etc.)
async function handleButtonInteraction(interaction: ButtonInteraction): Promise<void> {
  const customId = interaction.customId;

  if (customId.startsWith('more_images_') || customId.startsWith('more_search_images_')) {
    await handleMoreImagesButton(interaction);
  }
}

async function handleMoreImagesButton(interaction: ButtonInteraction): Promise<void> {
  const channelId = interaction.channelId;
  
  // Check which type of image request this is
  const isSearchImages = interaction.customId.startsWith('more_search_images_');
  const imageData = isSearchImages 
    ? imageSearchResults.get(channelId)
    : lastResponseImages.get(channelId);

  if (!imageData) {
    await interaction.reply({
      content: "Sorry, I lost track of those images. Try searching again?",
      ephemeral: true,
    });
    return;
  }

  await interaction.deferUpdate();

  try {
    const moreImages = await ragApi.getMoreImages(
      imageData.conversationId,
      imageData.shown,
      env.MAX_IMAGES_PER_RESPONSE
    );

    if (moreImages.images.length === 0) {
      await interaction.followUp({
        content: "That's all the images I found!",
        ephemeral: true,
      });
      return;
    }

    // Update shown count
    imageData.shown += moreImages.images.length;

    // Create new image embeds
    const imageEmbeds = moreImages.images.map((img) => {
      const embed = new EmbedBuilder()
        .setImage(img.url)
        .setColor(env.BOT_EMBED_COLOR);
      
      if (img.sourceTitle) {
        embed.setFooter({ text: `From: ${img.sourceTitle.slice(0, 100)}` });
      }
      
      return embed;
    });

    // Create new button if there are still more
    const components: ActionRowBuilder<ButtonBuilder>[] = [];
    if (moreImages.hasMore) {
      const remaining = imageData.total - imageData.shown;
      const buttonId = isSearchImages 
        ? `more_search_images_${channelId}`
        : `more_images_${channelId}`;
      
      components.push(
        new ActionRowBuilder<ButtonBuilder>().addComponents(
          new ButtonBuilder()
            .setCustomId(buttonId)
            .setLabel(`Show more (${remaining} remaining)`)
            .setStyle(ButtonStyle.Secondary)
            .setEmoji('🖼️')
        )
      );
    }

    await interaction.followUp({
      embeds: imageEmbeds,
      components: components.length > 0 ? components : undefined,
    });

    logger.info({ 
      channelId, 
      newImages: moreImages.images.length,
      totalShown: imageData.shown,
      remaining: imageData.total - imageData.shown,
    }, 'Loaded more images');
  } catch (error) {
    logger.error({ error, channelId }, 'Failed to load more images');
    await interaction.followUp({
      content: "Had trouble loading more images. Try again?",
      ephemeral: true,
    });
  }
}

// Handle message commands (e.g., !ask)
client.on(Events.MessageCreate, async (message: Message) => {
  if (message.author.bot) return;

  const prefix = env.BOT_COMMAND_PREFIX.toLowerCase();
  if (!message.content.toLowerCase().startsWith(prefix)) return;

  const userId = message.author.id;
  const now = Date.now();
  const lastUsed = userCooldowns.get(userId) || 0;
  const cooldownMs = env.COOLDOWN_SECONDS * 1000;

  if (now - lastUsed < cooldownMs) {
    const remainingSeconds = Math.ceil((cooldownMs - (now - lastUsed)) / 1000);
    await message.reply(`Hold up! ⏳ Give me ${remainingSeconds} more second${remainingSeconds !== 1 ? 's' : ''}.`);
    return;
  }

  userCooldowns.set(userId, now);

  const question = message.content.slice(prefix.length).trim();

  if (!question) {
    await message.reply(
      `Hey! What did you want to ask? Try something like:\n` +
      `\`${env.BOT_COMMAND_PREFIX} what do people think about X?\``
    );
    return;
  }

  if (question.length > 2000) {
    await message.reply("Whoa, that's a lot! 😅 Can you keep it under 2000 characters?");
    return;
  }

  const channelId = message.channelId;

  logger.info({ userId, channelId, questionLength: question.length }, `Processing ${env.BOT_COMMAND_PREFIX} message`);

  // Show typing indicator
  if ('sendTyping' in message.channel && typeof message.channel.sendTyping === 'function') {
    await message.channel.sendTyping();
  }

  try {
    const existingConversationId = channelConversations.get(channelId);

    const response = await ragApi.chat({
      message: question,
      conversationId: existingConversationId,
    });

    channelConversations.set(channelId, response.conversationId);

    const embed = createCasualResponseEmbed(question, response.answer, response.sources);
    const components = createSourceButtons(response.sources);

    // Include images if available
    const images = response.images || [];
    const embeds = [embed];
    
    if (images.length > 0) {
      const imageEmbeds = images.slice(0, 3).map((img) => 
        new EmbedBuilder()
          .setImage(img.url)
          .setColor(env.BOT_EMBED_COLOR)
      );
      embeds.push(...imageEmbeds);

      // Store for "more images" if there are more
      if ((response.totalImages || 0) > 3) {
        lastResponseImages.set(channelId, {
          conversationId: response.conversationId,
          shown: 3,
          total: response.totalImages || images.length,
        });

        const moreButton = new ActionRowBuilder<ButtonBuilder>().addComponents(
          new ButtonBuilder()
            .setCustomId(`more_images_${channelId}`)
            .setLabel(`More images (${(response.totalImages || 0) - 3} more)`)
            .setStyle(ButtonStyle.Secondary)
            .setEmoji('🖼️')
        );
        components.push(moreButton);
      }
    }

    await message.reply({
      embeds,
      components: components.length > 0 ? components : undefined,
    });

    logger.info({ userId, channelId, sourceCount: response.sources.length }, 'Message command completed');
  } catch (error) {
    logger.error({ error, userId, channelId }, 'Message command failed');

    const errorEmbed = new EmbedBuilder()
      .setColor(0xff4444)
      .setTitle('Oops! 😅')
      .setDescription("Something went wrong while I was looking that up. Mind trying again?")
      .setFooter({ text: env.BOT_NAME })
      .setTimestamp();

    await message.reply({ embeds: [errorEmbed] });
  }
});

function createCasualResponseEmbed(question: string, answer: string, sources: Source[]): EmbedBuilder {
  const maxAnswerLength = 3800;
  let truncatedAnswer = answer;

  if (answer.length > maxAnswerLength) {
    truncatedAnswer = answer.slice(0, maxAnswerLength) + '\n\n*...there\'s more but Discord cut me off* 😅';
  }

  const embed = new EmbedBuilder()
    .setColor(env.BOT_EMBED_COLOR)
    .setDescription(truncatedAnswer)
    .setFooter({ text: `Asked: "${question.slice(0, 80)}${question.length > 80 ? '...' : ''}"` })
    .setTimestamp();

  if (sources.length > 0) {
    const sourceList = sources
      .slice(0, 5)
      .map((s, i) => {
        const isForumPost = s.section?.includes('Post by') || s.title?.includes('Thread');
        if (isForumPost) {
          return `${i + 1}. ${s.section || s.title}`;
        }
        return `${i + 1}. [${s.title}](${s.url})`;
      })
      .join('\n');

    embed.addFields({
      name: '💬 From the discussions',
      value: sourceList || 'Various forum posts',
      inline: false,
    });
  }

  return embed;
}

function createSourceButtons(sources: Source[]): ActionRowBuilder<ButtonBuilder>[] {
  if (sources.length === 0) return [];

  const validSources = sources.filter(s => s.url && s.url.startsWith('http'));
  if (validSources.length === 0) return [];

  const buttons = validSources.slice(0, 3).map((source) => {
    const label = source.title.length > 60 
      ? source.title.slice(0, 57) + '...' 
      : source.title;
    
    return new ButtonBuilder()
      .setLabel(label)
      .setURL(source.url)
      .setStyle(ButtonStyle.Link)
      .setEmoji('🔗');
  });

  return [new ActionRowBuilder<ButtonBuilder>().addComponents(...buttons)];
}

process.on('SIGINT', () => {
  logger.info('Shutting down gracefully...');
  client.destroy();
  process.exit(0);
});

process.on('SIGTERM', () => {
  logger.info('Shutting down gracefully...');
  client.destroy();
  process.exit(0);
});

logger.info('Starting Discord bot...');
client.login(env.DISCORD_BOT_TOKEN).catch((error) => {
  logger.error({ error }, 'Failed to login to Discord');
  process.exit(1);
});
