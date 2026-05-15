import { REST, Routes } from 'discord.js';
import { env, logger } from './config/index.js';
import { commands } from './commands/index.js';

const rest = new REST().setToken(env.DISCORD_BOT_TOKEN);

async function registerCommands() {
  try {
    const commandData = Array.from(commands.values()).map(cmd => cmd.data.toJSON());

    logger.info(`Registering ${commandData.length} slash commands...`);

    if (env.DISCORD_GUILD_ID) {
      await rest.put(
        Routes.applicationGuildCommands(env.DISCORD_CLIENT_ID, env.DISCORD_GUILD_ID),
        { body: commandData }
      );
      logger.info(`✅ Registered commands to guild: ${env.DISCORD_GUILD_ID}`);
    } else {
      await rest.put(
        Routes.applicationCommands(env.DISCORD_CLIENT_ID),
        { body: commandData }
      );
      logger.info('✅ Registered commands globally (may take up to 1 hour to propagate)');
    }

    logger.info('Command registration complete!');
  } catch (error) {
    logger.error({ error }, 'Failed to register commands');
    process.exit(1);
  }
}

registerCommands();
