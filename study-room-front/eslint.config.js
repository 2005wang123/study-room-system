import js from '@eslint/js'
import globals from 'globals'
import pluginVue from 'eslint-plugin-vue'
import tseslint from 'typescript-eslint'
import prettier from 'eslint-config-prettier'

export default tseslint.config(
  { ignores: ['dist/**', 'node_modules/**', 'coverage/**'] },
  js.configs.recommended,
  ...tseslint.configs.recommended,
  ...pluginVue.configs['flat/essential'],
  {
    files: ['**/*.{ts,vue}'],
    languageOptions: {
      globals: { ...globals.browser, ...globals.node }
    },
    rules: {
      // TS 已经能识别浏览器/Node 全局变量，关闭 eslint 核心的 no-undef
      'no-undef': 'off',
      // 未使用变量降级为警告，避免历史代码大量报错（catch 的异常参数不算）
      '@typescript-eslint/no-unused-vars': ['warn', { argsIgnorePattern: '^_', varsIgnorePattern: '^_', caughtErrors: 'none' }],
      // 单一单词组件名（如 Announcements.vue）允许
      'vue/multi-word-component-names': 'off',
      // 允许空的 catch 块（localStorage 容错等场景）
      'no-empty': ['error', { allowEmptyCatch: true }]
    }
  },
  {
    files: ['**/*.vue'],
    languageOptions: {
      parserOptions: { parser: tseslint.parser }
    }
  },
  // 放在最后，关闭与 Prettier 冲突的格式规则
  prettier
)