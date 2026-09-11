# Todo BackEND（Spring Boot）

带登录功能的 Todo 任务管理后端服务，技术栈：Java 17 + Spring Boot 3 + JPA + MySQL + JWT。

## 环境要求

- **JDK 17+**（当前环境：JDK 25，路径 `C:\Program Files\Java\latest\jdk-25`）
- **Maven 3.9+**（当前环境：Maven 3.9.16，路径 `D:\Atools\apache-maven-3.9.16`）
- **MySQL 8.0+**（当前环境：MySQL 9.7.2，本地运行，端口 3306，root 空密码）
- **本地仓库**（可选，若使用自定义路径）：`C:\Users\Lenovo\.m2\repository`

## 数据库准备

1. 启动 MySQL 服务（当前为便携版，通过命令行启动）。
2. 执行建表脚本 `schema.sql`，初始化 `todo_db` 数据库及 `users`、`todos` 表（含索引）：

   ```powershell
   D:\Atools\MySQL\bin\mysql.exe -u root --port=3306 < schema.sql
   ```

3. 确认连接配置：数据库名 `todo_db`，用户 `root`，密码为空。
4. MySQL 数据存储目录：`todo-backEND\.mysql-data`

## 启动后端

在项目根目录 `todo-backEND` 下执行：

```powershell
$env:JAVA_HOME="C:\Program Files\Java\latest\jdk-25"
$env:Path="$env:JAVA_HOME\bin;D:\Atools\apache-maven-3.9.16\bin;$env:Path"
mvn -q "-Dmaven.repo.local=C:\Users\Lenovo\.m2\repository" spring-boot:run
```

后端启动后默认监听 `http://localhost:8080`。

## 测试账号

- 用户名：`alice`
- 密码：`secret123`

## API 接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/register` | 注册（username, password, email） |
| POST | `/api/login` | 登录，返回 JWT Token |
| GET | `/api/todos` | 查询任务列表（支持 `?completed=true/false` 筛选） |
| POST | `/api/todos` | 创建任务（需登录，归属当前用户） |
| PUT | `/api/todos/{id}` | 更新任务（需登录） |
| DELETE | `/api/todos/{id}` | 删除任务（需登录） |

## 注意事项

- 数据库密码需与 `src/main/resources/application.yml` 中的配置一致（当前为空）。
- 生产环境建议通过环境变量 `JWT_SECRET` 覆盖 JWT 密钥，避免使用默认值。
