import { z } from 'zod';
import dotenv from 'dotenv';

dotenv.config();

const envSchema = z.object({
  // Discord credentials
  DISCORD_BOT_TOKEN: z.string().min(1, 'Discord bot token is required'),
  DISCORD_CLIENT_ID: z.string().min(1, 'Discord client ID is required'),
  DISCORD_GUILD_ID: z.string().optional(),

  // RAG Backend
  RAG_API_URL: z.string().url().default('http://localhost:8000/api/knoxx'),
  RAG_API_KEY: z.string().default(''),

  // Bot customization
  BOT_NAME: z.string().default('Forum Bot'),
  BOT_COMMAND_PREFIX: z.string().default('!ask'),
  BOT_EMBED_COLOR: z.string().default('0x7c3aed').transform(val => parseInt(val, 16)),
  
  // Forum mode settings
  FORUM_MODE: z.string().default('true').transform(val => val === 'true'),
  BOT_PERSONALITY: z.string().default('casual'), // casual, formal, friendly
  MAX_IMAGES_PER_RESPONSE: z.coerce.number().default(4),

  // Rate limiting
  COOLDOWN_SECONDS: z.coerce.number().default(5),

  // Logging
  LOG_LEVEL: z.enum(['debug', 'info', 'warn', 'error']).default('info'),
});

export type Env = z.infer<typeof envSchema>;

function validateEnv(): Env {
  const result = envSchema.safeParse(process.env);

  if (!result.success) {
    console.error('❌ Environment validation failed:');
    for (const issue of result.error.issues) {
      console.error(`   - ${issue.path.join('.')}: ${issue.message}`);
    }
    console.error('\n💡 Run `npm run setup` to configure your Discord bot.');
    process.exit(1);
  }

  return result.data;
}

export const env = validateEnv();
