import {
  SlashCommandBuilder,
  ChatInputCommandInteraction,
  EmbedBuilder,
  ActionRowBuilder,
  ButtonBuilder,
  ButtonStyle,
} from 'discord.js';
import { env, logger } from '../config/index.js';
import { ragApi, type ImageResult } from '../services/index.js';

// Store for pagination
const imageSearchResults = new Map<string, { 
  conversationId: string; 
  query: string;
  shown: number; 
  total: number;
}>();

export const imagesCommand = {
  data: new SlashCommandBuilder()
    .setName('images')
    .setDescription('Find images that people have shared about a topic')
    .addStringOption(option =>
      option
        .setName('topic')
        .setDescription('What kind of images are you looking for?')
        .setRequired(true)
        .setMaxLength(500)
    )
    .addIntegerOption(option =>
      option
        .setName('count')
        .setDescription('How many images to show (1-8)')
        .setRequired(false)
        .setMinValue(1)
        .setMaxValue(8)
    ),

  async execute(interaction: ChatInputCommandInteraction): Promise<void> {
    const topic = interaction.options.getString('topic', true);
    const count = interaction.options.getInteger('count') ?? 4;
    const channelId = interaction.channelId;
    const userId = interaction.user.id;

    logger.info({ userId, channelId, topic, count }, 'Processing /images command');

    await interaction.deferReply();

    try {
      // Ask the RAG API specifically for images
      const imageQuery = `Show me images related to: ${topic}`;
      
      const response = await ragApi.chat({
        message: imageQuery,
      });

      const images = response.images || [];
      const totalImages = response.totalImages || images.length;

      if (images.length === 0) {
        const noImagesEmbed = new EmbedBuilder()
          .setColor(0xffaa00)
          .setTitle('No images found 🔍')
          .setDescription(`I couldn't find any images related to "${topic}". Try a different search term?`)
          .setFooter({ text: env.BOT_NAME })
          .setTimestamp();

        await interaction.editReply({ embeds: [noImagesEmbed] });
        return;
      }

      // Store for pagination
      imageSearchResults.set(channelId, {
        conversationId: response.conversationId,
        query: topic,
        shown: Math.min(count, images.length),
        total: totalImages,
      });

      // Create header embed
      const headerEmbed = new EmbedBuilder()
        .setColor(env.BOT_EMBED_COLOR)
        .setTitle(`🖼️ Images about "${topic}"`)
        .setDescription(`Found ${totalImages} image${totalImages !== 1 ? 's' : ''} from forum discussions`)
        .setFooter({ text: `Showing ${Math.min(count, images.length)} of ${totalImages}` });

      // Create image embeds
      const imageEmbeds = images.slice(0, count).map((img, index) => {
        const embed = new EmbedBuilder()
          .setImage(img.url)
          .setColor(env.BOT_EMBED_COLOR);
        
        if (img.sourceTitle) {
          embed.setFooter({ text: `From: ${img.sourceTitle.slice(0, 100)}` });
        }
        
        return embed;
      });

      // Add "more images" button if there are more
      const components: ActionRowBuilder<ButtonBuilder>[] = [];
      if (totalImages > count) {
        const moreImagesRow = new ActionRowBuilder<ButtonBuilder>()
          .addComponents(
            new ButtonBuilder()
              .setCustomId(`more_search_images_${channelId}`)
              .setLabel(`Load more (${totalImages - count} remaining)`)
              .setStyle(ButtonStyle.Primary)
              .setEmoji('📸')
          );
        components.push(moreImagesRow);
      }

      await interaction.editReply({
        embeds: [headerEmbed, ...imageEmbeds],
        components: components.length > 0 ? components : undefined,
      });

      logger.info({ 
        userId, 
        channelId, 
        imagesShown: Math.min(count, images.length),
        totalImages,
      }, '/images command completed');
    } catch (error) {
      logger.error({ error, userId, channelId }, '/images command failed');

      const errorEmbed = new EmbedBuilder()
        .setColor(0xff4444)
        .setTitle('Oops! 😅')
        .setDescription("Had trouble searching for images. Want to try again?")
        .setFooter({ text: env.BOT_NAME })
        .setTimestamp();

      await interaction.editReply({ embeds: [errorEmbed] });
    }
  },
};

export { imageSearchResults };
