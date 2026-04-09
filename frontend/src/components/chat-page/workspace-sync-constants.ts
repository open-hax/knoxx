export const DEFAULT_SYNC_INTERVAL_MINUTES = 30;

export const DEFAULT_FILE_TYPES = [
  '.md', '.markdown', '.txt', '.rst', '.org', '.adoc',
  '.json', '.jsonl', '.yaml', '.yml', '.toml', '.ini', '.cfg', '.conf', '.env',
  '.xml', '.csv', '.tsv', '.html', '.htm', '.css',
  '.js', '.jsx', '.ts', '.tsx', '.py', '.rb', '.php', '.java', '.kt', '.go', '.rs',
  '.c', '.cc', '.cpp', '.h', '.hpp', '.clj', '.cljs', '.cljc', '.edn', '.sql', '.sh',
  '.bash', '.zsh', '.fish', '.tex', '.bib', '.nix', '.dockerfile', '.gradle', '.properties',
];

export const DEFAULT_EXCLUDE_PATTERNS = [
  '**/.git/**',
  '**/node_modules/**',
  '**/dist/**',
  '**/coverage/**',
  '**/.next/**',
  '**/.venv/**',
  '**/venv/**',
  '**/__pycache__/**',
  '**/*.png',
  '**/*.jpg',
  '**/*.jpeg',
  '**/*.gif',
  '**/*.pdf',
  '**/*.zip',
  '**/*.tar.gz',
];
