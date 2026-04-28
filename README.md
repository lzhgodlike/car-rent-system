# 汽车租赁管理系统

本项目为山东建筑大学毕业设计《汽车租赁管理系统的设计与实现》的基础工程，采用 `Spring Boot + Vue 3 + MySQL` 技术栈实现。

## 项目结构

- `backend`：Spring Boot 后端服务
- `frontend`：Vue 3 + Element Plus 前端页面
- `sql`：MySQL 建表与初始化脚本

## 已实现模块

- 登录注册模块
- 用户信息管理模块
- 车辆类型管理模块
- 车辆信息管理模块
- 租车信息管理模块
- 还车信息管理模块
- 故障上报管理模块
- 数据统计分析模块

## 后端启动说明

1. 在 MySQL 中执行 `sql/schema.sql`
2. 再执行 `sql/data.sql`
3. 修改 `backend/src/main/resources/application.yml` 中的数据库账号密码
4. 进入 `backend` 目录执行：

```bash
mvn spring-boot:run
```

后端默认端口：`8080`

## 前端启动说明

1. 进入 `frontend` 目录执行：

```bash
npm install
npm run dev
```

前端默认端口：`5173`

## 默认账号

- 管理员：`admin / admin`
- 普通用户：`zhangsan / 123456`
- 普通用户：`lisi / 123456`

## 说明

当前工程适合作为毕业设计原型与论文配套项目，后续你还可以继续扩展：

- JWT 刷新机制
- 分页查询
- 文件上传与车辆图片管理
- ECharts 图表统计
- 更细粒度的角色权限控制
- 单元测试与接口文档
