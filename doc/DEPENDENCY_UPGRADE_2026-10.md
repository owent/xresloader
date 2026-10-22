# 2026-10-22 依赖更新与代码整理

## 版本依据与范围

版本来自 Maven Central 的 `maven-metadata.xml`、[Gradle 稳定版接口](https://services.gradle.org/versions/current)、[Maven 官方下载页](https://maven.apache.org/download.cgi) 和 [PyPI Protobuf 元数据](https://pypi.org/pypi/protobuf/json)。仅选择正式稳定版，不采用 Maven 4 RC 等预发布版本。

| 项目 | 修改前 | 修改后 |
| --- | --- | --- |
| Gradle Wrapper | 9.7.1，本地 wrapper 目录被忽略 | 9.8.2，脚本、JAR、属性纳入版本控制 |
| Maven | 依赖本机安装 | Wrapper 3.3.4 固定 Maven 3.10.0 |
| Commons Codec | 1.22.1 | 1.23.0 |
| SpotBugs annotations | 4.9.8 | 4.10.5，仍仅用于编译 |
| SnakeYAML Engine | 3.1.1 | 3.2 |
| Commons IO | 传递依赖 2.21.0 | 显式依赖 2.22.0 |
| Commons Collections | 传递依赖 4.5.0 | 显式依赖 4.6.0 |
| Gson | 2.8.9 | 2.14.0 |
| Error Prone annotations | 2.18.0 | 2.50.0 |
| XMLBeans | 5.3.0 | 5.4.1 |
| Commons Lang | 3.18.0 | 3.21.0 |
| ASM 全部五个模块 | 7.3.1 | 9.11.1 |
| JSpecify | 1.0.0 | 1.0.1 |
| Maven Resources Plugin | 3.4.0 | 3.5.0 |
| Maven JAR Plugin | 3.5.0 | 3.5.1 |
| Maven Clean / Install / Deploy Plugin | Maven 生命周期默认值 | 显式固定 3.5.0 / 3.2.0 / 3.2.0 |
| Gradle JaCoCo | 插件默认值 | 0.8.15，与 Maven 一致 |
| Python Protobuf | `>=3.12.1` | `==7.36.2` |

Maven 的 `dependencyManagement` 与 Gradle 的依赖约束同步管理传递依赖；没有分别替换构建插件内部的依赖。Commons IO 和 Collections 是源码直接使用的库，因此改为显式声明。

以下依赖经核实已为最新稳定版：Protobuf Java 4.36.2、POI 5.5.1、Log4j 2.26.1、Commons CLI 1.11.0、ini4j 0.5.4、MessagePack 0.9.12、JSON 20260814、dom4j 2.2.0、Commons CSV 1.14.1、Nashorn 15.7、JUnit 6.1.3、JaCoCo 0.8.15、Shadow 9.6.1，以及已声明的 Compiler、Surefire/Failsafe、Shade 和 Build Helper 插件。其余运行和测试传递依赖也已逐项查询 Central 元数据。

自带 protoc 为 36.2，与 Java/Python Protobuf 对应。协议子模块 `d0551139c6ffe7003eba6f862662c3adceac006c` 与上游 `main` 一致，未改动生成协议和 sample 输入。Actions 的 `checkout@v7`、`setup-java@v6`、`stale@v11`、`upload-to-github-release@v1` 已跟随各自最新稳定主版本。

两个 Wrapper 均固定发行包 SHA-256；Gradle wrapper JAR 的 SHA-256 与官方接口一致。Maven ZIP 下载后先与 Central 提供的 SHA-512 核对，再生成 Wrapper 使用的 SHA-256。

## 源码约定与兼容性

对 `src` 中的局部变量、参数、内部字段和方法使用 `lowerCamelCase`，内部类型使用 `UpperCamelCase`，不可变常量使用 `UPPER_SNAKE_CASE`。同步调整访问修饰符顺序、引用和 Javadoc 参数名。重命名通过 Java 编译器解析符号进行，不修改字符串中的协议字段、配置项或生成的其他语言代码。

公开接口保留兼容例外：`JAVA_TYPE` 等公开枚举、`DataDstObject`/`DataDstTableContent` 的历史字段名称继续可用。内部调用使用 `getLogger`、`loadScheme`、`buildData`、`setScheme`、`initialize`；旧 `getLoger`、`load_scheme`、`build_data`、`set_scheme`、`Initialize` 入口保留，已存在的 scheme/output 子类覆盖仍能被调用。生成的 `header` Java 文件不做风格重写。

资源管理与实现调整包括：

- 使用 try-with-resources 关闭协议描述、Excel、YAML、文本校验、INI、UE 导入配置和回调脚本输入；JSON 输入完整读取并保留 UTF-8 BOM 处理。
- 数值校验共享不可变 `Pattern`，每次创建独立 `Matcher`。[JDK 文档](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/regex/Pattern.html) 明确允许跨线程共享 Pattern。
- 待初始化校验器使用 `Deque` 表达先进先出语义，去掉 `List` 到 `LinkedList` 的强转，保留重复实例去重规则。
- 日期时间格式器和时区引用固定为私有常量，保留原有时区计算方式。

数值回归以旧 JAR 的行为为准。例如整数路径将 `200%` 转为 `200`，浮点路径转为 `2.0`；本次保持这一既有差异，不同时改变数据格式语义。

## 验证

本地验证环境为 Windows、Microsoft JDK 25.0.4.1。分别以 `--release 17` 和 `--release 21` 编译，测试实际运行在 JDK 25 上，未分别在 JDK 17/21 运行时执行。

| 检查 | 结果 |
| --- | --- |
| 修改前 Maven `clean verify` | 13 个单元测试、2 个打包集成测试通过 |
| 修改后 Maven 默认目标与 Java 21 目标 | 各 21 个单元测试、2 个打包集成测试通过 |
| 重复 Maven 打包 | 通过，保留原有未合并 JAR 重建及服务资源合并规则 |
| Gradle 9.8.2 `--rerun-tasks test shadowJar` | 21 个单元测试通过，打包成功 |
| 源码方法结构与符号绑定检查 | 原有 796 个方法中 779 个除名称外一致；17 个差异为上列资源、队列和正则调整 |
| Maven 与修改前 JAR 的 sample 比较 | 224 个输出文件逐字节一致，新增/缺失/内容变化均为 0 |
| Gradle 与修改前 JAR 的 sample 比较 | 224 个输出文件逐字节一致，新增/缺失/内容变化均为 0 |

新增测试覆盖队列顺序及去重、带/不带 BOM 的配置读取与文件关闭、异常 JSON 文件释放、数值转换与并发匹配、JavaScript 旧 logger/枚举入口、旧 scheme 实现分派。删除原先没有断言的 `SampleTest`。

sample 比较运行真实 PowerShell 生成脚本，使用干净的隔离输入目录，覆盖 proto v2/v3、二进制、Lua、JSON、XML、JavaScript、MessagePack、UE CSV/JSON 及生成的 C++ 文件，包含回调脚本、校验器、别名和转置示例。比较全部生成文件及文件集合，不跳过任何数据差异。脚本现在会在 JAR 缺失或任何转换失败时失败，并检查批处理确实生成了关键输出。

复现步骤见 [sample 输出回归验证](../sample/README.md#输出回归验证)。本次本地基线、JAR、版本查询和构建日志保存在忽略目录 `build/upgrade-audit`，比较工作目录为 `build/sample-regression`。CI 已改用固定 Wrapper，并增加 Maven/Gradle sample 交叉比较；远端 Linux CI 尚未在本次本地修改中执行。
