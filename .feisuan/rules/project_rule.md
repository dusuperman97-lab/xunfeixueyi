
# 讯飞学意 (Xunfei Xueyi) 项目开发规范指南

本项目为基于 Spring Boot 3.x 与 LangChain4j 的 AI 教育平台应用。为保证代码质量、可维护性、安全性与可扩展性，请在开发过程中严格遵循以下规范。

## 一、项目基础信息

- **项目名称**：xun-fei-xue-yi
- **工作区路径**：`D:\idea_workspace\xun-fei-xue-yi`
- **代码作者**：DU
- **构建工具**：Maven 3.x
- **主框架**：Spring Boot 3.2.10
- **语言版本**：JDK 21.0.8 (注意：pom.xml中配置为17，但环境为21，建议统一升级至21以支持最新特性)
- **第一语言**：中文（注释、文档均使用中文）

## 二、目录结构规范

项目采用前后端分离架构，后端核心代码位于 `src/main/java/com/project/platform`，前端位于 `web` 目录。

### 1. 后端目录树 (Java)

```text
src/main/java/com/project/platform
├── agents                  # AI 智能体核心模块
│   ├── ai                  # AI 模型交互逻辑
│   ├── config              # AI 相关配置 (RAG, Spark, MCP等)
│   ├── entry               # 入口处理
│   ├── guardrail           # 安全护栏/过滤
│   └── tools               # AI 工具集
├── aspect                  # AOP 切面 (日志、权限等)
├── config                  # 全局配置类 (CORS, 拦截器等)
├── controller              # REST API 控制器
├── dto                     # 数据传输对象 (Data Transfer Object)
├── entity                  # 数据库实体对象
│   └── ppt                 # PPT 相关实体
├── exception               # 全局异常处理
├── interceptor             # 拦截器
├── mapper                  # MyBatis Mapper 接口
├── properties              # 配置属性类
├── rag                     # RAG (检索增强生成) 业务逻辑
├── service                 # 服务层接口
│   └── impl                # 服务层实现类
├── utils                   # 工具类
└── vo                      # 视图展示对象
```

### 2. 资源目录 (Resources)

```text
src/main/resources
├── docs                    # 静态文档资源
├── knowledge               # RAG 知识库文件
├── mapper                  # MyBatis XML 映射文件
└── application.yaml        # 主配置文件
```

### 3. 前端目录结构 (Web)

```text
web
├── public
├── src
│   ├── api                 # 接口请求封装
│   ├── assets              # 静态资源
│   ├── components          # 通用组件
│   ├── router              # 路由配置
│   ├── styles              # 全局样式
│   ├── utils               # 前端工具函数
│   └── views               # 页面视图
│       ├── admin           # 管理员模块
│       ├── layout          # 布局组件
│       └── student         # 学生模块
│           ├── Agent       # 智能体对话
│           ├── course      # 课程相关
│           └── ...         # 其他学生功能页面
```

### 4. 其他重要目录

- `sql/`：数据库 SQL 脚本存放目录。
- `uploads/`：本地文件上传存储目录（知识库、思维导图、多媒体等），建议生产环境迁移至阿里云 OSS。

## 三、技术栈与依赖规范

### 1. 核心依赖

| 类别 | 依赖名称 | 版本/说明 | 用途 |
|------|----------|-----------|------|
| **Web** | spring-boot-starter-web | 3.2.10 | REST API 开发 |
| **Web** | spring-boot-starter-webflux | 3.2.10 | 流式输出支持 |
| **ORM** | mybatis-spring-boot-starter | 3.0.4 | 持久层框架 |
| **DB** | mysql-connector-j | 8.0.31 | MySQL 驱动 |
| **DB** | spring-boot-starter-data-mongodb | 3.2.10 | MongoDB 驱动 (用于聊天记忆) |
| **AI** | langchain4j-spring-boot-starter | 1.0.0-beta4 | AI 模型集成核心 |
| **AI** | langchain4j-community-dashscope-spring-boot-starter | 1.0.0-beta4 | 阿里云通义千问集成 |
| **AI** | langchain4j-ollama-spring-boot-starter | 1.0.0-beta4 | Ollama 本地模型集成 |
| **AI** | langchain4j-mcp | 1.0.0-beta4 | MCP 协议支持 |
| **AI** | langchain4j-reactor | 1.0.0-beta4 | 响应式编程支持 |
| **Utils** | lombok | 可选 | 简化实体类代码 |
| **Utils** | fastjson2 | 2.0.53 | JSON 处理 |
| **Utils** | hutool-all | 5.7.20 | 通用工具集 |
| **Utils** | commons-lang3 | 3.12.0 | Apache 通用工具 |
| **Utils** | jjwt | 0.9.1 | JWT 令牌处理 |
| **File** | aliyun-sdk-oss | 3.10.2 | 阿里云对象存储 |
| **File** | poi/poi-ooxml/pdfbox | 5.2.3 / 2.0.29 | 文档解析 (Word/PDF) |
| **Net** | okhttp | 4.12.0 | HTTP 客户端 |
| **Net** | jsoup | 1.20.1 | 网页抓取与解析 |

### 2. 通用依赖规则

- **版本管理**：所有 `langchain4j` 相关依赖必须通过 `<dependencyManagement>` 中的 `langchain4j-bom` 统一管理版本，避免版本冲突。
- **日志实现**：MyBatis 默认使用 `StdOutImpl` 打印 SQL 到控制台（调试阶段），生产环境请切换为 `Slf4jImpl`。
- **循环引用**：Spring Boot 3.2.x 默认禁用循环引用，本项目在配置中显式开启了 `spring.main.allow-circular-references: true`，但在代码设计中应尽量避免循环依赖。

## 四、分层架构规范

| 层级 | 职责说明 | 开发约束与注意事项 |
|------|----------|-------------------|
| **Controller** | 处理 HTTP 请求，定义 API 接口 | 不得直接访问数据库或 AI 模型；必须通过 Service 层调用；支持流式响应 (SSE) |
| **Service** | 实现业务逻辑、事务管理、AI 调用编排 | 必须通过 Repository/Mapper 访问数据库；AI 调用逻辑封装在此层；返回 DTO 而非 Entity |
| **Agents/Config** | AI 智能体配置与工具定义 | 负责 LangChain4j 的 ChatModel, EmbeddingModel, Tool 等 Bean 的初始化与配置 |
| **Mapper** | 数据库访问接口 | 继承 `BaseMapper` 或使用 `@Mapper` 注解；SQL 复杂查询写在 XML 中 |
| **Entity** | 映射数据库表结构 | 包名统一为 `entity`；不得直接返回给前端 |

### 接口与实现分离

- 所有 Service 接口需放在 `service` 包下，具体实现类需放在 `service.impl` 子包中。

## 五、安全与性能规范

### 1. 输入校验

- 使用 `jakarta.validation.constraints.*` 注解（如 `@NotBlank`, `@Size`）。
- 禁止手动拼接 SQL 字符串，防止 SQL 注入攻击。
- 文件上传需校验文件类型和大小，限制最大上传大小为 100MB。

### 2. 事务管理

- `@Transactional` 注解仅用于 **Service 层** 方法。
- 避免在循环中频繁提交事务，影响性能。
- AI 模型调用通常耗时较长，建议不要在长事务中调用 AI 接口，以免占用数据库连接。

### 3. 敏感信息保护

- **严禁**将 API Key、Secret、数据库密码等敏感信息硬编码在代码中。
- 敏感信息应配置在 `application.yaml` 中，并通过 `@Value` 或 `@ConfigurationProperties` 注入。
- **注意**：当前配置文件中包含明文 AK/SK，**上线前必须**使用环境变量或配置中心（如 Nacos/Apollo）替换，并加入 `.gitignore`。

## 六、代码风格规范

### 1. 命名规范

| 类型 | 命名方式 | 示例 |
|------|----------|------|
| 类名 | UpperCamelCase | `UserServiceImpl`, `AgentConfig` |
| 方法/变量 | lowerCamelCase | `saveUser()`, `chatMemory` |
| 常量 | UPPER_SNAKE_CASE | `MAX_LOGIN_ATTEMPTS` |
| 包名 | 全小写 | `com.project.platform.agents` |

### 2. 类型命名规范（阿里巴巴风格）

| 后缀 | 用途说明 | 示例 |
|------|----------|------|
| DTO | 数据传输对象 | `ChatRequestDTO` |
| DO | 数据库实体对象 | `UserDO` (若使用 MyBatis-Plus 可省略) |
| VO | 视图展示对象 | `ChatResponseVO` |
| Query | 查询参数封装对象 | `UserQuery` |

### 3. 实体类简化工具

- 使用 Lombok 注解替代手动编写 getter/setter/构造方法：
  - `@Data`
  - `@NoArgsConstructor`
  - `@AllArgsConstructor`

## 七、AI 开发特定规范

### 1. LangChain4j 使用规范

- **模型切换**：通过 `application.yaml` 中的 `langchain4j.community.dashscope` 或 `langchain4j.ollama` 配置切换模型，不要在代码中硬编码模型名称。
- **流式输出**：使用 `SseEmitter` 或 WebFlux 的 `Flux` 实现流式响应，确保网络超时时间（如 `PT5M`）配置合理。
- **工具调用**：在 `agents/tools` 包下定义工具类，并使用 `@Tool` 注解标记。

### 2. RAG (检索增强生成)

- 知识库文件存储在 `src/main/resources/knowledge` 或 `uploads/knowledge`。
- 向量存储使用 Pinecone 或本地实现，需配置 `langchain4j-pinecone`。
- 文档解析使用 `langchain4j-document-parser-apache-pdfbox` 和 `poi`。

## 八、扩展性与日志规范

### 1. 接口优先原则

- 所有业务逻辑通过接口定义（如 `UserService`），具体实现放在 `impl` 包中。
- AI 能力抽象化，便于未来切换不同的 AI 供应商（如从通义千问切换到讯飞星火）。

### 2. 日志记录

- 使用 `@Slf4j` 注解代替 `System.out.println`。
- 日志级别规范：
  - `ERROR`：系统异常、数据库连接失败。
  - `WARN`：业务逻辑异常（如参数校验失败）、第三方 API 调用超时。
  - `INFO`：关键业务流程开始/结束（如用户登录、AI 对话开始）。
  - `DEBUG`：详细调试信息、SQL 执行详情（生产环境关闭）。

## 九、编码原则总结

| 原则 | 说明 |
|------|------|
| **SOLID** | 高内聚、低耦合，增强可维护性与可扩展性 |
| **DRY** | 避免重复代码，提高复用性 |
| **KISS** | 保持代码简洁易懂 |
| **YAGNI** | 不实现当前不需要的功能 |
| **OWASP** | 防范常见安全漏洞，如 SQL 注入、XSS、敏感信息泄露 |
| **AI-Safety** | 对 AI 输出进行校验，防止提示词注入；对用户上传文件进行安全扫描 |
