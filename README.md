# Todo App

带登录功能的 Todo 任务管理应用：Vue 3 + Vite 前端，Spring Boot + MySQL 后端，液态玻璃主题。

## 项目结构

```
todo-app/
├── todo-frontend/   # Vue 3 + Vite 前端（端口 5173）
├── todo-backend/    # Spring Boot + JPA 后端（端口 8080）
└── todo-backend/.mysql-data/   # MySQL 数据目录（勿提交到 Git）
```

## 环境要求

- Node.js 18+ / npm 9+
- JDK 17+、Maven 3.9+
- MySQL（本机安装于 `D:\Atools\mysql`，配置文件 `D:\Atools\mysql\my.ini` 已指向本项目数据目录）

## 启动步骤（按顺序）

### 1. 启动 MySQL

```powershell
# 若 undo 表空间报错，需显式指定 undo 目录
& "D:\Atools\mysql\bin\mysqld.exe" --defaults-file="D:\Atools\mysql\my.ini" --innodb-undo-directory="D:/Aproject/practice/todo-app/todo-backend/.mysql-data" --console
```

窗口保持打开即为运行中（或注册为 Windows 服务）。验证：`Test-NetConnection localhost -Port 3306`。

### 2. 启动后端（Spring Boot，端口 8080）

```powershell
cd todo-backend
mvn spring-boot:run
```

看到 `Started TodoApplication` 即成功。JPA `ddl-auto: update` 会自动建表。

### 3. 启动前端（Vite，端口 5173）

```powershell
cd todo-frontend
npm install   # 首次运行
npm run dev
```

浏览器打开 **http://localhost:5173**。`/api` 请求由 Vite 代理转发到后端 8080，无需关心跨域。

## 测试账号

- 用户名：`alice`
- 密码：`secret123`

## 注意事项

- MySQL 数据存放在 `todo-backend/.mysql-data/`，**不要提交到 GitHub**
- 后端 JWT 密钥可通过环境变量 `JWT_SECRET` 覆盖（默认值仅用于开发）
- 前端主题支持浅色/深色切换（页面右上角按钮），偏好存在 localStorage

更多前端细节见 [todo-frontend/README.md](todo-frontend/README.md)。
