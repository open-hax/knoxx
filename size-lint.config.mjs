export default {
  includePaths: [
    'backend/src/cljs',
    'frontend/src',
    'discord-bot/src',
    'ingestion/src',
    'ingestion/test',
  ],
  ignoreDirectories: [
    'node_modules',
    'dist',
    'build',
    'target',
    '.shadow-cljs',
    '.clj-kondo',
    '.git',
  ],
  ignoreSuffixes: ['.d.ts'],
  thresholdsByExtension: {
    '.clj': { warn: 350, error: 500 },
    '.cljc': { warn: 350, error: 500 },
    '.cljs': { warn: 350, error: 500 },
    '.ts': { warn: 350, error: 500 },
    '.tsx': { warn: 350, error: 500 },
  },
};
