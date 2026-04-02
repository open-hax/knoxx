import {
  SlashCommandBuilder,
  ChatInputCommandInteraction,
  EmbedBuilder,
  ActionRowBuilder,
  ButtonBuilder,
  ButtonStyle,
  AttachmentBuilder,
} from 'discord.js';
import { env, logger } from '../config/index.js';
import { ragApi, type Source, type ImageResult } from '../services/index.js';

// Store conversation IDs per channel for context
const channelConversations = new Map<string, string>();

// Store last response images for "more images" button
const lastResponseImages = new Map<string, { conversationId: string; shown: number; total: number }>();

export const askCommand = {
  data: new SlashCommandBuilder()
    .setName('ask')
    .setDescription('Ask about what people have discussed in the forum')
    .addStringOption(option =>
      option
        .setName('question')
        .setDescription('What do you want to know?')
        .setRequired(true)
        .setMaxLength(2000)
    )
    .addBooleanOption(option =>
      option
        .setName('private')
        .setDescription('Only you will see the response')
        .setRequired(false)
    )
    .addBooleanOption(option =>
      option
        .setName('show_images')
        .setDescription('Include related images in the response')
        .setRequired(false)
    ),

  async execute(interaction: ChatInputCommandInteraction): Promise<void> {
    const question = interaction.options.getString('question', true);
    const isPrivate = interaction.options.getBoolean('private') ?? false;
    const showImages = interaction.options.getBoolean('show_images') ?? true;
    const channelId = interaction.channelId;
    const userId = interaction.user.id;

    logger.info({ userId, channelId, questionLength: question.length, showImages }, 'Processing /ask command');

    await interaction.deferReply({ ephemeral: isPrivate });

    try {
      const existingConversationId = channelConversations.get(channelId);

      const response = await ragApi.chat({
        message: question,
        conversationId: existingConversationId,
      });

      channelConversations.set(channelId, response.conversationId);

      // Create the main response embed
      const embed = createForumResponseEmbed(question, response.answer, response.sources);
      
      // Create source buttons
      const components: ActionRowBuilder<ButtonBuilder>[] = [];
      const sourceButtons = createSourceButtons(response.sources);
      if (sourceButtons.length > 0) {
        components.push(...sourceButtons);
      }

      // Handle images
      const images = response.images || [];
      const totalImages = response.totalImages || images.length;
      
      if (showImages && images.length > 0) {
        // Store for "more images" functionality
        lastResponseImages.set(channelId, {
          conversationId: response.conversationId,
          shown: Math.min(images.length, env.MAX_IMAGES_PER_RESPONSE),
          total: totalImages,
        });

        // Create image embeds (Discord allows up to 10 embeds per message)
        const imageEmbeds = createImageEmbeds(images.slice(0, env.MAX_IMAGES_PER_RESPONSE));
        
        // Add "more images" button if there are more
        if (totalImages > env.MAX_IMAGES_PER_RESPONSE) {
          const moreImagesRow = new ActionRowBuilder<ButtonBuilder>()
            .addComponents(
              new ButtonBuilder()
                .setCustomId(`more_images_${channelId}`)
                .setLabel(`Show more images (${totalImages - env.MAX_IMAGES_PER_RESPONSE} remaining)`)
                .setStyle(ButtonStyle.Secondary)
                .setEmoji('🖼️')
            );
          components.push(moreImagesRow);
        }

        await interaction.editReply({
          embeds: [embed, ...imageEmbeds],
          components: components.length > 0 ? components : undefined,
        });
      } else {
        await interaction.editReply({
          embeds: [embed],
          components: components.length > 0 ? components : undefined,
        });
      }

      logger.info({ 
        userId, 
        channelId, 
        sourceCount: response.sources.length,
        imageCount: images.length,
        totalImages,
      }, '/ask command completed');
    } catch (error) {
      logger.error({ error, userId, channelId }, '/ask command failed');

      const errorEmbed = new EmbedBuilder()
        .setColor(0xff4444)
        .setTitle('Oops! 😅')
        .setDescription("Sorry, I ran into a problem while looking that up. Mind trying again in a sec?")
        .setFooter({ text: env.BOT_NAME })
        .setTimestamp();

      await interaction.editReply({ embeds: [errorEmbed] });
    }
  },
};

/**
 * Create a forum-style response embed with casual tone
 */
function createForumResponseEmbed(question: string, answer: string, sources: Source[]): EmbedBuilder {
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

  // Add sources as forum thread references
  if (sources.length > 0) {
    const sourceList = sources
      .slice(0, 5)
      .map((s, i) => {
        // Format source based on whether it looks like a forum post
        const isForumPost = s.section?.includes('Post by') || s.title?.includes('Thread');
        if (isForumPost) {
          return `${i + 1}. ${s.section || s.title}`;
        }
        return `${i + 1}. [${s.title}](${s.url})`;
      })
      .join('\n');

    embed.addFields({
      name: '💬 Based on discussions from',
      value: sourceList || 'Various forum posts',
      inline: false,
    });
  }

  return embed;
}

/**
 * Create image embeds for Discord
 */
function createImageEmbeds(images: ImageResult[]): EmbedBuilder[] {
  return images.map((img, index) => {
    const embed = new EmbedBuilder()
      .setImage(img.url)
      .setColor(env.BOT_EMBED_COLOR);
    
    // Only add footer to first image to avoid clutter
    if (index === 0 && images.length > 1) {
      embed.setFooter({ text: `📸 ${images.length} related images` });
    }
    
    return embed;
  });
}

/**
 * Create source buttons for forum threads
 */
function createSourceButtons(sources: Source[]): ActionRowBuilder<ButtonBuilder>[] {
  if (sources.length === 0) return [];

  // Filter to only sources with valid URLs
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

// Export for use in button handler
export { lastResponseImages };
