# 📚 自习室座位预约管理系统 · 前端（study-room-front）

自习室座位预约管理系统的前端，基于 **Vue 3 + TypeScript + Vite 8 + Element Plus** 构建（原为纯 JavaScript，已完成 TypeScript 迁移与工程化升级）。

## 🧰 技术栈

| 分类 | 选型 |
| --- | --- |
| 框架 | Vue 3.5（`<script setup>` 组合式 API） |
| 语言 | TypeScript |
| 构建工具 | Vite 8 |
| 路由 / 状态 | Vue Router 4 · Pinia |
| UI 组件库 | Element Plus 2 |
| 楼层绘图 | 自研 SVG 编辑器（无第三方画布库，全量 TypeScript） |
| HTTP | Axios（封装于 `src/utils/request.ts`） |

## 🛠️ 工程化

- **类型检查**：`vue-tsc`（见 `tsconfig.json`），构建前自动执行
- **代码规范**：ESLint 9（flat config，`eslint.config.js`）+ Prettier（`.prettierrc.json`）
- **单元测试**：Vitest + @vue/test-utils + jsdom（示例：`src/components/__tests__/TimeWheel.spec.ts`）
- **共享类型**：`src/types/api.ts`（后端接口模型）、`src/types/layout.ts`（画布 JSON 结构）


## 📦 常用命令

| 命令 | 说明 |
| --- | --- |
| `npm run dev` | 启动开发服务器（端口 5137，代理 /api → localhost:8080） |
| `npm run build` | 类型检查 + 生产构建（= `type-check` + `vite build`） |
| `npm run type-check` | 运行 `vue-tsc` 类型检查 |
| `npm run lint` / `npm run lint:fix` | ESLint 检查 / 自动修复 |
| `npm run format` / `npm run format:check` | Prettier 格式化 / 校验 |
| `npm test` / `npm run test:watch` | Vitest 单元测试 / 监听模式 |

## 📁 目录结构

```
src
├── api/            # 后端接口封装（TypeScript 类型化，对应 study-room-back 接口）
├── components/     # 业务组件（floor-editor/ = 楼层绘图编辑器与属性面板）
├── router/         # Vue Router 配置
├── types/          # 共享类型（api.ts / layout.ts）
├── utils/floorEditor.ts # 楼层绘图纯逻辑（含单元测试）
├── views/SeatMap.vue # 主页面
├── env.d.ts        # Vite 客户端类型声明
├── main.ts         # 应用入口
└── style.css       # 全局样式
```
