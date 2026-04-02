import {
  SlashCommandBuilder,
  ChatInputCommandInteraction,
  EmbedBuilder,
} from 'discord.js';
import { env } from '../config/index.js';

export const helpCommand = {
  data: new SlashCommandBuilder()
    .setName('help')
    .setDescription('Learn how to chat with me'),

  async execute(interaction: ChatInputCommandInteraction): Promise<void> {
    const embed = new EmbedBuilder()
      .setColor(env.BOT_EMBED_COLOR)
      .setTitle(`Hey! I'm ${env.BOT_NAME} 👋`)
      .setDescription(
        "I've read through tons of forum discussions and can help you find what people have talked about! " +
        "Just ask me anything and I'll dig through the conversations to find relevant info."
      )
      .addFields(
        {
          name: '💬 `/ask <question>`',
          value: 'Ask me anything! I\'ll search through forum discussions and share what people have said. You can also ask me to show images.',
          inline: false,
        },
        {
          name: '🖼️ `/images <topic>`',
          value: 'Want to see pictures? I can find images that people have shared about any topic.',
          inline: false,
        },
        {
          name: '📊 `/status`',
          value: 'Check if I\'m connected and working properly.',
          inline: false,
        },
        {
          name: `⚡ \`${env.BOT_COMMAND_PREFIX} <question>\``,
          value: 'Quick way to ask without using slash commands.',
          inline: false,
        }
      )
      .addFields({
        name: '💡 Pro tips',
        value: [
          '• I remember our conversation in each channel, so feel free to ask follow-ups!',
          '• Add `show_images:True` to see related pictures',
          '• I\'ll tell you who said what when it\'s relevant',
          '• Keep in mind - I\'m sharing what people discussed, not official facts',
        ].join('\n'),
        inline: false,
      })
      .setFooter({ text: `${env.BOT_NAME} • Your friendly forum knowledge bot` })
      .setTimestamp();

    await interaction.reply({ embeds: [embed], ephemeral: true });
  },
};
