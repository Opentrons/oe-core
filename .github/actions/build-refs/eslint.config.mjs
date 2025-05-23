// eslint.config.mjs for ESLint v9+
import love from 'eslint-config-love';
import eslintConfigPrettier from 'eslint-config-prettier/flat';

export default [
  {
    ...love,
    files: ['**/*.js', '**/*.ts'],
  },
  eslintConfigPrettier,
  {
    ignores: [
      'dist',
      'node_modules',
      '**/*.d.ts',
      '**/build/**',
      '**/coverage/**',
      '**/scripts/**',
    ],
  },
];
