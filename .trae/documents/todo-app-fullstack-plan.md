# Todo 全栈任务管理项目计划

## 1. 项目概述

从零搭建一个带登录功能的全栈 Todo 任务管理项目，支持用户注册、登录（JWT 认证）、任务增删改查。

## 2. 技术选型（用户确认）

| 层面 | 技术 |
|------|------|
| 后端 | Java + Spring Boot + MySQL + JWT |
| 前端 | Vue 3 + Vite + Vue Router + Axios |
| 数据库 | MySQL |
| 认证 | JWT（无状态 Token） |

## 3. 环境约束（重要）

开发执行环境当前状态：

| 工具 | 状态 | 说明 |
|------|------|------|
| Node.js v22.23.2 | ✓ 可用 | 可执行前端开发 |
| Java | ✗ 未安装 | 需用户安装 JDK（建议 JDK 17+） |
| Maven | ✗ 未安装 | 需用户安装 Maven，或切换 Gradle |
| MySQL | ✗ 未安装 | 需用户启动 MySQL 服务，或配置 MySQL 连接 |

**计划中的应对策略**：项目代码按所选技术栈编写，构建与运行依赖用户环境安装对应工具链。前后端通过 HTTP API 通信，后端接口地址通过 Vite 代理转发，不直接暴露后端端口。

---

## 4. 项目结构

```
todo-app/
├── todo-backend/                    # 后端 Spring Boot 项目
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/todo/
│       │   ├── TodoApplication.java         # 启动类
│       │   ├── config/
│       │   │   ├── CorsConfig.java          # 跨域配置
│       │   │   └── SecurityConfig.java       # JWT 过滤器配置
│       │   ├── entity/
│       │   │   ├── User.java                # 用户实体
│       │   │   └── Todo.java                # 任务实体
│       │   ├── repository/
│       │   │   ├── UserRepository.java      # 用户数据访问
│       │   │   └── TodoRepository.java      # 任务数据访问
│       │   ├── service/
│       │   │   ├── UserService.java         # 用户业务逻辑
│       │   │   └── TodoService.java         # 任务业务逻辑
│       │   ├── controller/
│       │   │   ├── UserController.java      # 用户接口（注册/登录）
│       │   │   └── TodoController.java      # 任务接口（CRUD）
│       │   ├── dto/
│       │   │   ├── UserDTO.java             # 用户请求/响应 DTO
│       │   │   └── TodoDTO.java             # 任务请求/响应 DTO
│       │   ├── auth/
│       │   │   ├── JwtUtil.java             # JWT 工具类
│       │   │   ├── JwtAuthFilter.java       # JWT 认证过滤器
│       │   │   └── CurrentUser.java         # 当前用户上下文
│       │   └── exception/
│       │       └── GlobalExceptionHandler.java  # 全局异常处理
│       └── resources/
│           └── application.yml               # 配置文件
└── todo-frontend/                   # 前端 Vue 3 项目
    ├── package.json
    ├── vite.config.js
    └── src/
        ├── main.js
        ├── App.vue
        ├── router/
        │   └── index.js            # 路由配置 + 登录守卫
        ├── views/
        │   ├── Login.vue           # 登录页
        │   └── TodoList.vue        # 任务列表页
        └── api/
            └── request.js          # Axios 实例 + JWT 处理
```

---

## 5. 后端实现细节

### 5.1 基础配置

**pom.xml**：引入 Spring Boot Web、Spring Data JPA、MySQL Driver、JWT (jjwt) 库、Spring Security Crypto（BCrypt 密码加密）。

**application.yml**：
- 服务端口（如 8080）
- MySQL 连接配置（host、port、database、username、password）
- JWT 配置（secret、expires 等）

### 5.2 实体层

**User** 实体：
- `Long id`（主键）
- `String username`（用户名，唯一）
- `String password`（BCrypt 加密后的密码）
- `String email`（邮箱）
- `LocalDateTime createdAt`（创建时间）

**Todo** 实体：
- `Long id`（主键）
- `Long userId`（用户 ID，外键关联 User）
- `String title`（任务标题）
- `String description`（任务描述）
- `boolean completed`（是否完成）
- `LocalDateTime createdAt`（创建时间）
- `LocalDateTime updatedAt`（更新时间）

### 5.3 数据访问层（Repository）

- **UserRepository**：`findByUsername(String username)` 按用户名查询用户
- **TodoRepository**：按用户 ID 查询任务，支持按完成状态筛选

### 5.4 业务逻辑层（Service）

**UserService**：
- 注册：校验用户名唯一性，BCrypt 加密密码后保存
- 登录：校验用户名存在，比对密码，生成 JWT Token 返回

**TodoService**：
- 创建任务：归属当前用户，保存
- 更新任务：更新标题、描述、完成状态
- 删除任务
- 查询任务列表：支持按用户过滤、按完成状态筛选

### 5.5 认证层（JWT）

**JwtUtil**：
- 生成 JWT Token（包含用户 ID 等信息，设置过期时间）
- 解析 JWT Token，提取用户信息

**JwtAuthFilter**：
- 拦截后端受保护接口
- 从请求头提取 JWT Token
- 校验 Token 有效性，通过则放入当前用户上下文，未通过则返回 401

**SecurityConfig**：
- 配置 JwtAuthFilter 注册到 FilterChain
- 对需要认证的接口进行鉴权
- 配置 CORS 跨域

### 5.6 接口层（Controller）

**UserController**：
- `POST /api/register` — 注册（username, password, email）
- `POST /api/login` — 登录（username, password），返回 JWT

**TodoController**：
- `POST /api/todos` — 创建任务（需登录，归属当前用户）
- `PUT /api/todos/{id}` — 更新任务（需登录）
- `DELETE /api/todos/{id}` — 删除任务（需登录）
- `GET /api/todos` — 查询任务列表（需登录，支持分页/筛选）

### 5.7 异常处理

**GlobalExceptionHandler**：统一处理业务异常（如用户名重复、未登录等），返回标准错误格式。

---

## 6. 前端实现细节

### 6.1 项目配置

**package.json**：Vue 3、Vite、Vue Router、Axios。

**vite.config.js**：
- Vite 配置
- 代理配置：将 `/api` 请求代理到后端地址（如 `http://localhost:8080`），解决跨域问题

**main.js**：应用入口，初始化 Vue 实例、路由、Axios 实例。

### 6.2 路由与状态管理

**router/index.js**：
- 路由配置：登录页（`/login`）、任务列表页（`/`）
- 登录守卫（beforeEach）：未登录访问受保护页面时重定向到登录页

**api/request.js**：
- Axios 实例
- 请求拦截器：自动添加 JWT Token 到请求头
- 响应拦截器：根据返回的 Token 状态更新本地存储，处理 401 重定向

### 6.3 页面实现

**Login.vue**：
- 用户名、密码输入框
- 登录按钮
- 注册功能（可选，后端已支持注册）
- 登录成功后跳转到任务列表页

**TodoList.vue**：
- 任务列表展示（标题、描述、完成状态）
- 添加任务表单（标题、描述）
- 任务编辑与删除操作
- 任务完成/取消完成切换
- 登录状态判断，未登录时跳转登录页

---

## 7. 数据库设计（MySQL）

### 7.1 users 表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键，自增 |
| username | VARCHAR(50) | 用户名，唯一约束 |
| password | VARCHAR(100) | BCrypt 加密密码 |
| email | VARCHAR(100) | 邮箱 |
| created_at | DATETIME | 创建时间 |

### 7.2 todos 表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键，自增 |
| user_id | BIGINT | 用户 ID，外键关联 users.id |
| title | VARCHAR(200) | 任务标题 |
| description | TEXT | 任务描述 |
| completed | TINYINT(1) | 是否完成（0/1） |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

### 7.3 索引

- `todos(user_id)` — 按用户查询任务
- `todos(completed)` — 按完成状态筛选

---

## 8. 实施步骤

### 步骤 1：初始化后端项目
- 创建 `todo-backend` 目录及 Maven 工程结构
- 编写 `pom.xml`，配置依赖
- 创建 `TodoApplication.java` 启动类

### 步骤 2：后端配置与实体层
- 编写 `application.yml`（MySQL 连接、JWT 配置）
- 编写实体类 `User.java`、`Todo.java`
- 编写 Repository 层接口

### 步骤 3：后端认证与业务层
- 编写 `JwtUtil`、`JwtAuthFilter`、`SecurityConfig`
- 编写 `UserService`、`TodoService`
- 编写 DTO 类

### 步骤 4：后端接口层与异常处理
- 编写 `UserController`、`TodoController`
- 编写 `GlobalExceptionHandler`

### 步骤 5：初始化数据库
- 创建 MySQL 数据库及表结构（users、todos）
- 执行建表 SQL

### 步骤 6：初始化前端项目
- 创建 `todo-frontend` 目录及 Vite 工程结构
- 编写 `package.json`、`vite.config.js`
- 配置 Axios 实例、路由、登录守卫

### 步骤 7：前端页面实现
- 实现 `Login.vue` 登录页
- 实现 `TodoList.vue` 任务列表页（CRUD）

### 步骤 8：联调与验证
- 启动后端服务、前端服务
- 验证用户注册、登录、任务增删改查全流程
- 验证 JWT 认证与权限控制

---

## 9. 验证步骤

1. **后端构建**：执行 `mvn clean package`，确认无编译错误，生成可执行 JAR。
2. **数据库初始化**：确认 MySQL 中 `users`、`todos` 表及索引已创建。
3. **后端运行**：启动 Spring Boot 服务，确认接口正常响应（注册、登录）。
4. **前端构建**：执行 `npm install`、`npm run build`，确认无构建错误。
5. **前端运行**：启动 Vite 开发服务器，确认页面正常加载。
6. **端到端验证**：
   - 注册新用户 → 登录 → 获取 Token
   - 创建任务 → 查看任务列表 → 编辑任务 → 删除任务 → 完成/取消完成
   - 验证未登录访问受保护接口被拦截（返回 401）

---

## 10. 注意事项与简化说明

- **密码安全**：使用 BCrypt 加密存储密码，登录时比对哈希值，不存储明文。
- **JWT 配置**：JWT Secret 需足够复杂度，建议在 `application.yml` 中配置，生产环境通过环境变量注入。
- **环境配置**：MySQL 连接信息、JWT Secret 需根据实际环境调整 `application.yml` 中的配置。
- **跨域处理**：前端通过 Vite 代理转发 `/api` 请求，不暴露后端端口到公网。
- **环境约束**：Java、Maven、MySQL 需用户在开发环境中安装并配置，项目代码已按所选技术栈编写。
