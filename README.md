# 自习室座位预约管理系统 (Study Room System)

> 📖 **在线接口文档（GitHub Pages）：** [https://2005wang123.github.io/study-room-system/](https://2005wang123.github.io/study-room-system/)
>
> 无需本地启动后端即可查看，基于源码整理的全部接口说明（含请求参数与返回结构）。

基于 **Spring Boot 3 + MyBatis-Plus + MySQL + Vue 3 + Vite** 的前后端分离自习室座位预约系统。

## 功能特性

- 用户注册 / 登录（JWT 认证，BCrypt 密码加密）
- 楼层 → 区域(房间) → 座位 三级座位地图，可按楼层 / 区域 / 日期筛选
- 楼层结构图：管理员用可视化绘制工具（区域/墙体/自由画笔/座位/文字/底图）绘制每层布局，支持草稿与发布，发布时自动同步 seat 表；普通用户可直接在结构图上点击座位预约
- 座位状态颜色：绿色=全天空闲，半红半绿=当天部分时段可约，红色=当天已约满，灰色=维修中
- 在线预约座位（同一座位一天内可分多个时间段预约，时间段冲突校验、并发防超卖、单次最长 4 小时、开放时间 08:00-22:00）
- 预约弹窗自动标记已占用时段，不可选冲突时间
- 签到 / 签退（待签到 -> 使用中 -> 已完成）
- 取消预约 / 管理员强制取消
- 预约记录查询（个人）与分页管理（管理员）
- 公告管理（管理员：新增、编辑、下架/发布、删除）
- 公告评论与点赞（普通用户：查看、发表评论、删除自己的评论、点赞）
- 用户管理（管理员：新增、启停、重置密码）
- 登录防爆破（失败锁定 + IP 限流，基于 Caffeine）
- Knife4j 接口文档（`http://localhost:8080/doc.html`）

## 技术栈

| 模块   | 技术 |
| ------ | ---- |
| 后端   | Java 17, Spring Boot 3.5.5, Spring Security, MyBatis-Plus 3.5.7, MySQL, JWT (jjwt), Caffeine, Knife4j |
| 前端   | Vue 3, Vite, Vue Router, Pinia, Axios, Fabric.js（楼层结构图绘制） |

## 目录结构

```
study-room-system
├── study-room-back               # Spring Boot 后端
│   ├── src/main/java/com/example/studyroom
│   │   ├── common        # 统一响应、全局异常、常量、登录限流
│   │   ├── config        # 安全/跨域/拦截器/定时任务/MyBatis-Plus 配置
│   │   ├── controller    # 接口层
│   │   ├── dto           # 请求/响应对象
│   │   ├── entity        # 实体
│   │   ├── mapper        # MyBatis-Plus Mapper
│   │   ├── service       # 业务层
│   │   └── utils         # JWT、密码校验、身份证工具
│   └── src/main/resources
│       ├── mapper        # MyBatis XML
│       └── sql           # 建表脚本 schema.sql、公告脚本 announcement.sql、评论脚本 announcement_comment.sql、区域升级脚本 upgrade_area.sql
└── study-room-front               # Vue 3 前端
```

## 快速开始

### 1. 初始化数据库

```sql
CREATE DATABASE study_room_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE study_room_db;
SOURCE study-room-back/src/main/resources/sql/schema.sql;        -- 建表 + 楼层/区域/座位示例数据
SOURCE study-room-back/src/main/resources/sql/announcement.sql;         -- 公告表 + 示例公告
SOURCE study-room-back/src/main/resources/sql/announcement_comment.sql; -- 公告评论表 + 评论点赞表
SOURCE study-room-back/src/main/resources/sql/floor_layout.sql;      -- 楼层结构图表
```

> 已有旧数据库（没有 `area` 表）时，请先执行升级脚本：
> `SOURCE study-room-back/src/main/resources/sql/upgrade_area.sql;`（幂等，可为 `seat` 增加 `area_id` 并初始化区域）

数据库连接配置见 `study-room-back/src/main/resources/application.yml`（可通过环境变量 `DB_USERNAME` / `DB_PASSWORD` 覆盖）。

### 2. 启动后端

使用 JDK 17+（推荐 17/21，Lombok 暂不支持 JDK 26），在 IntelliJ 中直接运行 `StudyRoomApplication`，
或命令行：

```bash
cd study-room-back
mvn spring-boot:run
```

接口文档：<http://localhost:8080/doc.html>

### 3. 启动前端

```bash
cd study-room-front
npm install
npm run dev        # 默认 http://localhost:5137，/api 代理到后端 8080
```

生产构建：`npm run build`（可通过环境变量 `VITE_API_BASE_URL` 指定后端地址，默认 `/api`）。

### 4. 创建管理员账号

系统没有内置管理员，请按以下任一方式创建：

1. 先注册一个学生账号，然后执行：
   ```sql
   UPDATE sys_user SET role = 1 WHERE username = '你的学号';
   ```
2. 或使用管理员后台「用户管理」页面创建管理员（需先有 1 个管理员）。

## 环境变量

| 变量 | 说明 | 默认值 |
| ---- | ---- | ------ |
| `JWT_SECRET` | JWT 签名密钥（生产必改，建议 ≥64 字节） | 内置开发密钥 |
| `JWT_EXPIRATION` | Token 有效期（毫秒） | 86400000 (24h) |
| `DB_USERNAME` / `DB_PASSWORD` | 数据库账号密码 | root / root |
| `CORS_ALLOWED_ORIGINS` | 允许的前端来源（逗号分隔） | http://localhost:5137 |
| `VITE_API_BASE_URL` | 前端请求的后端地址（前端构建时） | /api |

## 层级结构 / 座位颜色说明

- 层级：楼层 `floor` -> 区域(房间) `area` -> 座位 `seat`（`seat.area_id` 关联 `area.id`）
- 座位地图可按「楼层 + 区域 + 日期」筛选，同一楼层座位按区域分组展示
- 座位颜色规则（针对所选日期）：
  - 🟢 绿色：当天所有时段均空闲
  - 🔴🟢 半红半绿：当天已有部分时段被预约，仍有其他时段可约
  - 🔴 红色：当天开放时段(08:00-22:00)已全部约满
  - ⚪ 灰色：维修中
- 预约弹窗会显示该座位当天已占用时段，冲突时段不可选

## 座位 / 预约状态说明

- 座位 `seat.status`：0-空闲，1-已预约(旧数据兼容)，2-使用中，3-维修中
  - 说明：预约成功不再把座位整体置为「已预约」，座位状态颜色由「当天已预约时长占比」实时计算，
    因此同一座位一天内可被多个用户分时段预约
- 预约 `reservation.status`：0-待签到，1-使用中，2-已完成，3-违约（到期未签到），4-已取消
- 定时任务每小时自动处理：待签到超时 -> 违约；使用中超时 -> 自动完成并释放座位

## 安全说明

- 所有接口默认要求登录（Spring Security deny-by-default + JWT 过滤器），公开接口显式放行
- 角色/权限通过 `@RequireRole` / `@RequirePermission` 注解在拦截器二次校验
- 登录防爆破：同一账号连续失败 5 次锁定 15 分钟；同一 IP 每分钟最多 20 次登录请求
- 初始密码为身份证号后 6 位，首次登录强制提示修改；生产环境建议改为随机初始密码
- 生产部署请务必通过环境变量覆盖 `JWT_SECRET`，并将 `CORS_ALLOWED_ORIGINS` 改为正式域名
