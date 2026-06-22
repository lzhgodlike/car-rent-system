# 汽车租赁管理系统

山东建筑大学毕业设计项目，基于 Spring Boot + Vue 3 + MySQL 实现的汽车租赁管理系统。支持用户在线浏览车辆、下单租车、归还结算，以及管理员对车辆、订单、用户的全流程管理。

## 技术栈

### 后端

| 技术 | 版本 | 说明 |
|------|------|------|
| Spring Boot | 3.3.8 | 应用框架 |
| MyBatis-Plus | 3.5.7 | 数据库 ORM |
| MySQL | 8.0+ | 数据库 |
| JWT (jjwt) | 0.12.6 | 用户认证 |
| Spring WebSocket | - | 实时通知与客服聊天 |
| Lombok | - | 简化代码 |

### 前端

| 技术 | 版本 | 说明 |
|------|------|------|
| Vue | 3.5 | 前端框架 |
| Vue Router | 4.5 | 路由管理 |
| Element Plus | 2.11 | UI 组件库 |
| ECharts | 5.5 | 数据图表 |
| Axios | 1.8 | HTTP 请求 |
| Vite | 6.2 | 构建工具 |

## 项目结构

```
├── backend/                     # 后端 Spring Boot 项目
│   └── src/main/java/com/sdjzu/carrental/
│       ├── common/              # 公共类（ApiResponse、异常处理、UserContext）
│       ├── config/              # 配置（CORS、WebSocket、MyBatis-Plus、MVC）
│       ├── controller/          # REST 控制器
│       ├── mapper/              # MyBatis-Plus Mapper 接口
│       ├── model/               # 实体、DTO、VO、Request 对象
│       ├── security/            # JWT 工具、认证拦截器、WebSocket 握手认证
│       ├── service/             # 业务逻辑层
│       └── ws/                  # WebSocket 处理器（通知、客服聊天）
├── frontend/                    # 前端 Vue 项目
│   └── src/
│       ├── components/          # 公共组件（客服聊天抽屉）
│       ├── router/              # 路由配置（含权限守卫）
│       ├── utils/               # 工具（auth、request、WebSocket 客户端）
│       └── views/
│           ├── admin/           # 管理后台页面（9 个）
│           └── user/            # 用户端页面（4 个）
└── sql/
    ├── schema.sql               # 建表语句
    └── data.sql                 # 生产数据导出（Navicat 格式）
```

## 功能模块

### 用户端

| 功能 | 说明 |
|------|------|
| 首页浏览 | 车辆列表，按品牌/车型/城市筛选，关键词搜索，排序（价格/里程） |
| 车辆详情 | 查看多图轮播、价格、取车地址等 |
| 收藏车辆 | 收藏/取消收藏 |
| 在线租车 | 选择租期下单，支持支付宝/微信支付 |
| 订单管理 | 查看订单列表，取消订单，申请还车 |
| 故障上报 | 对租赁车辆提交故障报告 |
| 客服聊天 | 与管理员实时在线沟通（WebSocket） |
| 消息通知 | 订单状态变更、还车提醒等实时推送（WebSocket） |
| 个人中心 | 修改头像、手机号、身份证等资料 |

### 管理端

| 功能 | 说明 |
|------|------|
| 控制台 | 数据概览（车辆/订单/营收/用户统计）、趋势图表（ECharts）、待办事项 |
| 车辆管理 | 增删改查，启用/停用，多图上传管理 |
| 车型管理 | 车型分类的增删改查 |
| 订单管理 | 查看所有订单，确认取车，拒绝取车，发送还车提醒 |
| 归还处理 | 确认还车，录入实际里程和损坏信息，收取附加费用 |
| 维保管理 | 处理故障报告：安排维修、完成维修、拒绝报告 |
| 客户管理 | 用户列表，启用/禁用账号，编辑用户信息 |
| 客服会话 | 处理用户咨询，实时聊天（WebSocket） |
| 个人中心 | 管理员资料修改 |

## 快速开始

### 环境要求

- JDK 17+
- Node.js 18+
- MySQL 8.0+
- Maven 3.6+

### 1. 数据库初始化

```bash
# 方式一：分别执行
mysql -u root -p < sql/schema.sql
mysql -u root -p car_rental_system < sql/data.sql

# 方式二：直接导入完整数据（含建表和测试数据）
mysql -u root -p < sql/data.sql
```

### 2. 后端启动

```bash
cd backend

# 按需修改数据库连接配置
# src/main/resources/application.yml

mvn spring-boot:run
```

后端运行在 `http://localhost:8080`。

### 3. 前端启动

```bash
cd frontend
npm install
npm run dev
```

前端运行在 `http://localhost:5173`，开发模式下 API 请求自动代理到后端。

### 4. 访问

- 用户端：`http://localhost:5173/`
- 管理后台：`http://localhost:5173/admin`

## 测试账号

| 角色 | 用户名 | 密码 |
|------|--------|------|
| 管理员 | admin | admin |
| 普通用户 | zhangsan | 123456 |
| 普通用户 | lisi | 123456 |

## 配置说明

### 后端 application.yml

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `server.port` | 服务端口 | 8080 |
| `spring.datasource.url` | 数据库地址 | `localhost:3306/car_rental_system` |
| `spring.datasource.username` | 数据库用户名 | root |
| `spring.datasource.password` | 数据库密码 | 123456 |
| `jwt.secret` | JWT 签名密钥 | 见配置文件 |
| `jwt.expire-minutes` | Token 有效期（分钟） | 60 |
| `app.media.base-dir` | 文件上传存储目录 | `./uploads` |
| `spring.servlet.multipart.max-file-size` | 单文件大小上限 | 5MB |
| `spring.servlet.multipart.max-request-size` | 单次请求大小上限 | 20MB |

### 前端代理 (vite.config.js)

开发模式下自动代理：

| 路径 | 目标 | 说明 |
|------|------|------|
| `/api/*` | `http://localhost:8080` | REST API |
| `/static/*` | `http://localhost:8080` | 静态资源（车辆图片等） |
| `/ws/*` | `ws://localhost:8080` | WebSocket 连接 |

## 数据库设计

系统包含以下 11 张表：

| 表名 | 说明 |
|------|------|
| `sys_user` | 用户表（管理员/普通用户） |
| `car_type` | 车型分类 |
| `car_info` | 车辆信息 |
| `car_image` | 车辆图片（支持多图） |
| `rent_order` | 租车订单 |
| `return_order` | 还车订单 |
| `fault_report` | 故障报告 |
| `user_favorite` | 用户收藏 |
| `message_notice` | 消息通知 |
| `support_conversation` | 客服会话 |
| `support_message` | 客服消息 |

## API 接口

所有接口以 `/api` 为前缀，返回格式：

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

### 公开接口（无需登录）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/cars` | 车辆列表（支持筛选/分页） |
| GET | `/api/cars/{id}` | 车辆详情 |
| GET | `/api/cars/brands` | 品牌列表 |
| GET | `/api/cars/cities` | 城市列表 |
| GET | `/api/car-types` | 车型列表 |
| POST | `/api/auth/login` | 用户登录 |
| POST | `/api/auth/register` | 用户注册 |

### 认证接口（需 Bearer Token）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/auth/me` | 当前用户信息 |
| POST | `/api/auth/refresh` | 刷新 Token |
| POST | `/api/rent-orders` | 创建租车订单 |
| POST | `/api/rent-orders/{id}/pay` | 支付订单 |
| PUT | `/api/rent-orders/{id}/cancel` | 取消订单 |
| POST | `/api/return-orders` | 提交还车申请 |
| POST | `/api/fault-reports` | 上报故障 |
| GET | `/api/favorites` | 收藏列表 |
| POST | `/api/favorites/{carId}` | 添加收藏 |
| DELETE | `/api/favorites/{carId}` | 取消收藏 |
| GET/PUT | `/api/users/profile` | 查看/修改个人资料 |

### 管理员接口（需 ADMIN 角色）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/dashboard/overview` | 数据概览 |
| GET | `/api/dashboard/charts` | 图表数据 |
| GET | `/api/dashboard/pending` | 待办事项 |
| POST/PUT/DELETE | `/api/cars` | 车辆增删改 |
| PUT | `/api/cars/{id}/disable` | 停用车辆 |
| PUT | `/api/cars/{id}/enable` | 启用车辆 |
| PUT | `/api/rent-orders/{id}/pickup` | 确认取车 |
| PUT | `/api/rent-orders/{id}/reject-pickup` | 拒绝取车 |
| PUT | `/api/rent-orders/{id}/remind-return` | 发送还车提醒 |
| PUT | `/api/return-orders/{id}/confirm` | 确认还车 |
| PUT | `/api/fault-reports/{id}/handle` | 安排维修 |
| PUT | `/api/fault-reports/{id}/complete-repair` | 完成维修 |
| PUT | `/api/fault-reports/{id}/reject` | 拒绝故障报告 |
| GET | `/api/users` | 用户列表 |
| PUT | `/api/users/{id}/status` | 启用/禁用用户 |
| DELETE | `/api/users/{id}` | 删除用户 |

## 架构说明

### 认证机制

- 使用 JWT 进行无状态认证，Token 有效期 60 分钟
- 前端路由守卫自动检查登录状态和角色权限
- 页面切换时自动续期 Token，过期则跳转登录
- WebSocket 连接通过 URL 参数传递 Token 完成握手认证

### 实时通信

- **通知推送**：订单创建、状态变更、还车提醒等通过 WebSocket 推送给管理员
- **客服聊天**：用户与管理员之间的实时双向通信
- WebSocket 断线自动降级为轮询模式

### 文件上传

- 车辆图片上传到服务器本地 `./uploads` 目录
- 通过 `/static/**` 路径访问，最大支持 5MB 单文件
