# Todo FrontEND（Vue 3 + Vite）

带登录功能的 Todo 任务管理前端，技术栈：Vue 3 + Vite + Vue Router + Axios。

## 环境要求

- **Node.js 18+**（当前环境：Node v22.23.2）
- **npm 9+**（当前环境：npm 10.9.8）
- **后端服务需已启动**（默认 `http://localhost:8080`）

## 安装与启动

在项目根目录 `todo-frontEND` 下执行：

```powershell
npm install
npm run dev
```

前端启动后默认监听 `http://localhost:5173`，已配置 `/api` 代理转发到后端 `http://localhost:8080`，避免跨域问题。

## 测试账号

- 用户名：`alice`
- 密码：`secret123`

## 使用说明

1. 打开 `http://localhost:5173`，输入账号密码登录。
2. 在任务列表页可新增、编辑、删除、完成/取消完成任务，支持按「全部 / 未完成 / 已完成」筛选。
3. 未登录访问受保护页面会自动跳转至登录页。

## 构建生产版本

```powershell
npm run build
```

## 注意事项

- 请求通过 Vite 代理转发到后端，无需关心跨域配置。
- 登录 Token 及登录状态由前端管理，未登录时自动跳转登录页。
- 修改前端代码后，`npm run dev` 会自动热更新，无需手动重启。
