# Todo 后端讲解（费曼版 · 按当前项目重写）

> 目标：让你即使没学过后端，也能看懂这个项目“为什么这样写、请求怎么走、数据存在哪、改功能要动哪些文件”。
>
> 项目路径：`d:\Aproject\practice\todo-app\todo-backend`
> 技术栈：Java 17 · Spring Boot 3.3.4 · Spring Data JPA · MySQL · JWT

---

## 先说结论：只记三件事

如果这篇文档太长，你只要带走这三句话：

1. **后端是一个分层餐厅**：`Controller` 接单，`Service` 做菜，`Repository` 取食材，MySQL 是冰库。
2. **后端必须自己判断“你是谁、这数据是不是你的”**：所以有 JWT Filter、`CurrentUser`、`userId` 过滤。
3. **你改一个字段，要按固定路线走**：`Entity → DTO → Service → 重启看数据库自动加列`。

---

## 1. 一张图看懂整个项目

```text
浏览器 Vue
   │  Axios: GET/POST/PUT/DELETE + Authorization: Bearer <JWT>
   ▼
JwtAuthFilter（门卫）
   │  验 token → 把 userId 放进 CurrentUser（ThreadLocal）
   ▼
Controller（服务员）
   │  决定 URL、HTTP 方法、参数怎么取
   ▼
Service（厨房）
   │  业务规则：是谁的？能不能改？重复任务要不要生成下一期？
   ▼
Repository（仓库管理员）
   │  Spring Data JPA 自动生成 SQL
   ▼
MySQL（冰库）
   users / todos / pomodoro_sessions / user_ai_configs
```

每一层只和相邻层说话。服务员不会自己冲进冰库，后厨也不直接跟顾客吵架。这就是分层架构（Layered Architecture）的意义。

---

## 2. 用一个真实场景串起来：添加一条任务

假设你在前端输入“买牛奶”，选今天，点添加。

### 第 1 步：前端发请求

```text
POST /api/todos
Authorization: Bearer eyJhbGci...
Content-Type: application/json

{
  "title": "买牛奶",
  "dueDate": "2026-11-25",
  "category": "生活",
  "priority": 1,
  "repeatRule": "none"
}
```

### 第 2 步：门卫查证件

`JwtAuthFilter` 拦住请求：

- `/api/login` 和 `/api/register` 是白名单，不查 token
- 其他 `/api/*` 必须带 `Authorization: Bearer xxx`
- 解析 JWT，得到 `userId`
- `CurrentUser.set(userId)` 放进本次请求的临时口袋

如果 token 没有或无效，直接返回 `401 Unauthorized`，请求到不了 Controller。

### 第 3 步：服务员接单

`TodoController.create()`：

```java
@PostMapping
public Todo create(@Valid @RequestBody TodoDTO dto) {
    return todoService.create(dto);
}
```

它做三件很小的事：

- `@PostMapping`：说明这个方法处理 POST
- `@RequestBody`：把 JSON 转成 `TodoDTO`
- `@Valid`：让校验注解先跑一遍，比如标题不能为空

然后立刻转给 `TodoService`。Controller 不写业务，这叫**薄控制器**。

### 第 4 步：厨房做菜

`TodoService.create()`：

```java
Todo todo = new Todo();
todo.setUserId(CurrentUser.get());
apply(todo, dto);
aiService.embedTodo(todo);
return todoRepository.save(todo);
```

人话解释：

1. 造一个空任务对象
2. 把当前登录用户 id 塞进去
3. 把 DTO 里的字段拷到 Entity 上
4. 如果配了 embedding 模型，顺便算一个向量（用于语义搜索）
5. 保存进数据库

### 第 5 步：仓库管理员入库

`TodoRepository.save(todo)` 来自 Spring Data JPA。

你没写 SQL，但 Hibernate 会生成类似：

```sql
INSERT INTO todos (user_id, title, due_date, ...)
VALUES (?, ?, ?, ...)
```

这叫 **ORM（Object-Relational Mapping，对象关系映射）**：你操作 Java 对象，框架翻译成 SQL。

### 第 6 步：原路返回

数据库返回保存后的 `Todo`，Controller 收到后，Spring 自动把它序列化成 JSON，前端拿到新任务。

---

## 3. 文件地图：每个包到底干什么

不要按字母顺序记，按**职责**记：

| 层 | 文件 | 人话职责 | 专业术语 |
|---|---|---|---|
| 启动 | `TodoApplication.java` | 点火开关 | Spring Boot Application |
| 配置 | `config/SecurityConfig.java` | 门卫怎么挂、跨域怎么开、图片怎么访问 | Filter Registration / CORS / Resource Handler |
| 认证 | `auth/JwtUtil.java` | 签发和解析 JWT | JWT Sign / Verify |
| 认证 | `auth/JwtAuthFilter.java` | 每个请求先查 token | Servlet Filter |
| 认证 | `auth/CurrentUser.java` | 本次请求里“当前是谁” | ThreadLocal |
| 接口 | `controller/UserController.java` | `/api/register`、`/api/login` | RESTful Controller |
| 接口 | `controller/TodoController.java` | 任务增删改查、排序、番茄 | RESTful Controller |
| 接口 | `controller/AiController.java` | AI 解析、搜索、拆解、设置 | REST Controller / SSE |
| 接口 | `controller/UploadController.java` | 上传图片 | Multipart Upload |
| 业务 | `service/UserService.java` | 注册、登录、密码校验 | Business Logic |
| 业务 | `service/TodoService.java` | 归属校验、重复任务、排序、完成时间 | Business Logic |
| 业务 | `service/AiService.java` | 调大模型的 HTTP 客户端 | HTTP Client / SSE / JSON Mode |
| 业务 | `service/AiSettingsService.java` | 保存用户 AI 配置，加密 Key | AES-GCM Encryption |
| 数据 | `entity/*.java` | 数据表在 Java 里的样子 | Entity / ORM |
| 数据 | `repository/*.java` | 只写接口，Spring 生成 SQL | Repository / JPA |
| 传输 | `dto/*.java` | API 收什么、返回什么 | Data Transfer Object |
| 兜底 | `exception/GlobalExceptionHandler.java` | 统一错误格式 | @RestControllerAdvice / AOP |

一句口诀：

> **Entity 是数据表长什么样，DTO 是前后端约定怎么传，Repository 管查，Service 管规则，Controller 管门口。**

---

## 4. 数据库里到底存了什么

当前项目主要有 4 张表。

### 4.1 `users`：用户表

| 字段 | 作用 |
|---|---|
| `id` | 用户主键 |
| `username` | 用户名，唯一 |
| `password` | BCrypt 哈希后的密码，不是明文 |
| `email` | 邮箱 |

### 4.2 `todos`：任务表

这张表最重要。字段可以按用途分组理解：

**基础信息**

- `title`：标题
- `description`：Markdown 详情
- `completed`：是否完成
- `created_at` / `updated_at`：创建/更新时间

**时间管理**

- `dueDate`：安排到哪一天
- `remindAt`：提醒时间
- `repeatRule`：`none` / `daily` / `weekly`
- `parentId`：父任务 id，用来表示子任务或重复生成的下一期

**Ivy Lee / 排序**

- `sortOrder`：重要性顺序，越小越靠前
- `priority`：0 普通、1 重要、2 紧急
- `category`：清单/分类

**番茄与统计**

- `pomodoroCount`：累计番茄数
- `lastPomodoroAt`：最近一次番茄时间
- `completedAt`：完成时间，用于昨日小结和近 7 天统计

**AI**

- `embedding`：标题向量的 JSON 数组，用于语义搜索

### 4.3 `pomodoro_sessions`：番茄流水表

每完成一个番茄，插一条记录。

> 专业说法：这是事件记录（Event Log）。汇总数放 `todos`，流水放这张表，统计时按时间聚合。

### 4.4 `user_ai_configs`：用户 AI 配置表

- `user_id`：属于哪个用户
- `base_url`：自定义 API 地址
- `api_key_encrypted`：加密后的 API Key
- `chat_model` / `embed_model`：模型名

API Key 不是明文入库，而是用 AES-GCM 加密。加密密钥来自环境变量 `AI_CONFIG_SECRET`。

> 注意：前端设置页只能看到脱敏值，比如 `sk-a****mnop`。留空保存代表保留原 Key。

---

## 5. 认证：后端怎么知道“你是谁”

HTTP 本身是**无状态（Stateless）**的。服务器不会天然记得你刚登录过，所以每次请求都要带凭证。

### 5.1 登录时发 JWT

`UserService.login()`：

1. 按用户名查用户
2. 用 `BCryptPasswordEncoder.matches(明文, 哈希)` 比密码
3. 成功后 `JwtUtil.generate(userId, username)` 签发 JWT

JWT 三段：`Header.Payload.Signature`。

- Payload 里有 userId、过期时间
- 它只是 Base64 编码，**不是加密**
- 签名保证不能被篡改

所以 JWT 里不能放密码。

### 5.2 每次请求查 JWT

`JwtAuthFilter` 是进入 Controller 前的安检门：

```text
没带 Bearer token → 401
token 无效/过期 → 401
token 有效 → CurrentUser.set(userId) → 放行
```

### 5.3 `CurrentUser` 为什么用 ThreadLocal

`ThreadLocal` 可以理解成：

> 每个请求线程自己的私人储物柜。

这样 `TodoService` 里任何地方都能写：

```java
Long userId = CurrentUser.get();
```

而不用把 `userId` 当参数一层层传下去。

但是线程会复用！处理完用户 A 的请求后，同一个线程可能处理用户 B。如果请求结束时不清空，用户 B 就可能读到用户 A 的 id。

所以 `JwtAuthFilter` 的 `finally { CurrentUser.clear(); }` 是**必须的安全动作**，不是可选的清理代码。

---

## 6. 任务模块：真正复杂的业务都在这里

### 6.1 查询永远只查自己的

`TodoService.list()` 第一件事就是：

```java
Long userId = CurrentUser.get();
```

然后调用类似：

```java
findByUserIdOrderBySortOrderAscCreatedAtDesc(userId)
```

绝对不能写 `findAll()`，否则所有人都能看到所有任务。

### 6.2 修改/删除前先确认所有权

`TodoService` 里有一个私有方法 `owned(id)`：

```java
todoRepository.findById(id)
    .filter(t -> t.getUserId().equals(CurrentUser.get()))
    .orElseThrow(...);
```

这就是**水平越权防护**。

如果用户 A 把 URL 改成 `/api/todos/99`，想改用户 B 的任务，`filter` 会失败，返回 404。

为什么返回 404 而不是 403？

> 403 等于告诉对方“这条数据存在，但你没权限”；404 让“不存在”和“不是你的”长得一样，减少信息泄露。

### 6.3 完成时发生了什么

`apply()` 会检查完成状态变化：

- 从未完成 → 完成：写 `completedAt = now`
- 从完成 → 未完成：清空 `completedAt`

如果 `repeatRule` 是 `daily` 或 `weekly`，还会调用 `generateNextOccurrence()` 生成下一期任务。

### 6.4 拖拽排序怎么保存

前端拖拽后调用：

```text
PUT /api/todos/reorder
Body: [id3, id1, id2, ...]
```

后端 `TodoService.reorder()`：

1. 只取当前用户的任务
2. 按传来的 id 顺序重新设置 `sortOrder`
3. 没出现在数组里的任务排在后面
4. 一次性保存

### 6.5 子任务和重复任务

两者都用 `parentId`：

- AI 拆解出来的子任务：`parentId = 父任务.id`
- 重复任务生成的下一期：`parentId = 上一期.id`

前端通过 `todos.filter(t => t.parentId === todo.id)` 找到子任务。

### 6.6 番茄记录

`POST /api/todos/{id}/pomodoro` 会：

1. `pomodoroCount + 1`
2. 更新 `lastPomodoroAt`
3. 插入一条 `PomodoroSession` 流水

统计页就能据此画柱状图。

---

## 7. AI 模块：没有 magic，只是 HTTP 请求

### 7.1 `AiService`：大模型客户端

它用 JDK 自带的 `HttpClient`，请求 OpenAI 兼容接口：

```text
POST {baseUrl}/chat/completions
Authorization: Bearer <API_KEY>
```

三个能力：

| 方法 | 用途 | 前端功能 |
|---|---|---|
| `chatJson()` | 要求模型返回 JSON | AI 添加 |
| `chatStream()` | 流式输出 | AI 拆解 |
| `embed()` | 文本转向量 | AI 语义搜索 |

`chatStream()` 用 `SseEmitter`，专业叫 **SSE（Server-Sent Events）**：服务器一边生成一边推送，所以前端能看到打字机效果。

### 7.2 `AiConfig`：单次调用的配置对象

```java
record AiConfig(String baseUrl, String apiKey, String chatModel, String embedModel)
```

记录类（record）就是“只装数据的小对象”。

配置优先级：

```text
环境变量默认值 → 数据库用户配置 → 单次请求覆盖
```

### 7.3 `AiSettingsService`：用户配置与加密

前端设置页调用：

- `GET /api/ai/settings`
- `PUT /api/ai/settings`
- `DELETE /api/ai/settings`

后端把 API Key 用 AES-GCM 加密后存入 `user_ai_configs.api_key_encrypted`。

加密流程：

```text
AI_CONFIG_SECRET → SHA-256 → AES 密钥
随机 12 字节 IV
AES-GCM 加密 API Key
存 Base64(IV + ciphertext)
```

返回给前端时只给脱敏值，避免明文 Key 在浏览器里裸奔。

### 7.4 优雅降级（Graceful Degradation）

没配置 API Key 时，AI 功能不可用，但普通增删改查完全正常。

AI 搜索没配 embedding 模型时，回退到普通关键字匹配。

这叫**优雅降级**：高级功能可选，核心功能不受影响。

---

## 8. Spring 帮你做了什么（不懂 Java 也能理解）

### 8.1 IoC / DI：为什么看不到 `new`

你翻遍项目，很难看到：

```java
new TodoService()
```

因为 Spring 会启动时扫描注解，自动创建对象并管理。这叫：

- **IoC（Inversion of Control，控制反转）**：对象的创建权交给框架
- **DI（Dependency Injection，依赖注入）**：对象需要的依赖由框架“塞”进来

```java
public TodoService(TodoRepository todoRepo, PomodoroSessionRepository sessionRepo, AiService ai) { ... }
```

这不是你 new 的，是 Spring 从容器里取出来传进来的。

### 8.2 `@Bean`

`SecurityConfig` 里：

```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```

意思是：这个方法造出来的对象交给 Spring 管，其他地方只要声明 `PasswordEncoder` 参数就能拿到。

### 8.3 注解其实只是“标签”

- `@RestController`：这是个 HTTP 接口类
- `@Service`：这是业务逻辑类
- `@Entity`：这是数据库表对象
- `@GetMapping`：这个方法处理 GET
- `@Valid`：先校验参数
- `@PreUpdate`：更新前自动执行

注解本身不干活，Spring 启动时读标签，然后干活。

### 8.4 DTO 为什么存在

DTO 是前后端之间的“快递盒”，不是数据库对象。

好处：

1. 不把 `password` 这种敏感字段返回前端
2. 可以在字段上写校验规则
3. 数据库结构变化时，API 不一定跟着变

### 8.5 AOP：全局异常处理

`@RestControllerAdvice` 像一张网，Controller 抛出的异常都会被它接住，统一变成：

```json
{ "message": "错误信息" }
```

前端只需要读 `message`，不用每个接口写一套错误解析。

---

## 9. 配置与启动

### 9.1 `application.yml` 管什么

```yaml
spring:
  datasource:       # 连哪个 MySQL
  jpa:
    hibernate:
      ddl-auto: update   # 自动建表/加字段
jwt:
  secret: ...            # JWT 签名密钥
ai:
  base-url: ...          # 默认大模型地址
  api-key: ...           # 默认 Key
  config-secret: ...     # 加密用户 API Key 的密钥
server:
  port: 8080
```

所有 `${ENV_VAR:默认值}` 都支持环境变量覆盖。

### 9.2 `ddl-auto: update`

开发时非常方便：

- 你给 Entity 加字段
- 重启后端
- Hibernate 自动给表加列

但生产环境不该用 `update`，应该用 `validate` 加 Flyway/Liquibase 迁移脚本。

### 9.3 启动命令

```powershell
cd d:\Aproject\practice\todo-app\todo-backend
mvn spring-boot:run
```

`mvn` 是构建工具 Maven，类似前端的 `npm`。`pom.xml` 是它的 `package.json`。

Spring Boot 内嵌 Tomcat，所以不需要单独安装服务器。

> 重要：改了 Java 代码、新增 Entity、新增接口以后，**必须重启后端**。否则前端会请求到旧接口，出现 404 或 500。

---

## 10. 错误码约定

| 情况 | HTTP 状态 | 返回 |
|---|---|---|
| 参数校验失败 | 400 | `{"message":"title: 不能为空"}` |
| 业务错误 | 400 | `{"message":"Username already exists"}` |
| 没带 token / token 无效 | 401 | Filter 直接返回 |
| 数据不存在或不是你的 | 404 | `{"message":"Todo not found"}` |
| AI 没配置 Key | 503 | `{"message":"AI 功能未启用..."}` |
| AI 服务连接/解析失败 | 502 | `{"message":"AI 服务连接失败"}` |
| 未知程序错误 | 500 | `{"message":"Internal error"}` |

500 故意不暴露具体堆栈，避免泄露内部信息。

---

## 11. 给前端人的心智转换表

| 前端 | 后端（本项目） |
|---|---|
| `main.js` | `TodoApplication.java` |
| `package.json` | `pom.xml` |
| `.env` | `application.yml` + 环境变量 |
| axios 请求 | Controller + Service |
| vue-router | `@RequestMapping` |
| axios 拦截器 | `JwtAuthFilter` |
| Pinia / state | MySQL 表 |
| localStorage | 数据库 / JWT |
| TS type + zod | DTO + `@Valid` |
| `window.onerror` | `@RestControllerAdvice` |
| `useContext` 注入 | Spring IoC / DI |
| `Array.filter/map` | Java Stream API |

---

## 12. 怎么读这个项目

### 推荐阅读顺序

1. `TodoApplication.java`：看程序入口
2. `entity/Todo.java`：看数据长什么样
3. `TodoDTO.java`：看 API 收什么
4. `TodoRepository.java`：看能查什么
5. `TodoService.java`：看业务规则
6. `TodoController.java`：看对外接口
7. `JwtAuthFilter.java` + `CurrentUser.java`：看认证
8. `application.yml` + `pom.xml`：看配置和依赖
9. `AiService.java` + `AiSettingsService.java`：最后看 AI

### 如果要加一个字段

比如给任务加 `deadlineWarning`：

1. `Todo.java` 加字段和 getter/setter
2. `TodoDTO.java` 加字段
3. `TodoService.apply()` 里加 `todo.setDeadlineWarning(dto...)`
4. 重启后端，`ddl-auto: update` 会自动加列
5. 前端表单和列表同步加字段

这就是这个项目的“改动路线图”。

---

## 13. 当前 API 一览

| 方法 | 路径 | 说明 | 需要 token |
|---|---|---|---|
| POST | `/api/register` | 注册 | 否 |
| POST | `/api/login` | 登录，返回 JWT | 否 |
| GET | `/api/todos` | 查询任务 | 是 |
| POST | `/api/todos` | 创建任务 | 是 |
| POST | `/api/todos/batch` | 批量创建 | 是 |
| PUT | `/api/todos/reorder` | 拖拽排序 | 是 |
| PUT | `/api/todos/{id}` | 更新任务 | 是 |
| DELETE | `/api/todos/{id}` | 删除任务 | 是 |
| POST | `/api/todos/{id}/pomodoro` | 记录番茄 | 是 |
| GET | `/api/pomodoros` | 番茄历史 | 是 |
| POST | `/api/uploads` | 上传图片 | 是 |
| POST | `/api/ai/parse` | 自然语言转任务 | 是 |
| POST | `/api/ai/search` | 搜索任务 | 是 |
| POST | `/api/ai/breakdown` | 拆解子任务（SSE） | 是 |
| GET | `/api/ai/settings` | 读取 AI 配置 | 是 |
| PUT | `/api/ai/settings` | 保存 AI 配置 | 是 |
| DELETE | `/api/ai/settings` | 清空 AI 配置 | 是 |

测试账号：`alice` / `secret123`
后端地址：`http://localhost:8080`

---

## 附录：遇到 Java 语法再查这张表

| Java | 人话 | 前端对应 |
|---|---|---|
| `class` | 图纸 | `class` |
| `new X()` | 按图纸造对象 | `new X()` |
| `private` / `public` | 可见范围 | `#private` / `export` |
| `final` | 赋值后不能换 | `const` |
| `Long` / `String` | 对象类型 | `number` / `string` |
| `List<Todo>` | 只能装 Todo 的数组 | `Todo[]` |
| `Optional<T>` | 可能有值的盒子 | `T \| undefined` |
| `->` | 小函数 | `=>` |
| `A::b` | 方法引用 | 直接传函数 |
| `.stream().filter().map()` | 集合处理链 | 数组链式方法 |
| `@注解` | 给框架的标签 | 装饰器 |
| `record` | 只装数据的简写类 | `type X = { ... }` |

不用背。读到看不懂时再回来查。
