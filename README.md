# xresloader

[![GitHub Actions status](https://github.com/xresloader/xresloader/workflows/Main%20Building/badge.svg)](https://github.com/xresloader/xresloader/actions)
[![Release](https://github.com/xresloader/xresloader/workflows/Release/badge.svg)](https://github.com/xresloader/xresloader/actions?query=workflow%3ARelease)
[![GitHub release (latest by date)](https://img.shields.io/github/v/release/xresloader/xresloader)](https://github.com/xresloader/xresloader/releases)

![GitHub code size in bytes](https://img.shields.io/github/languages/code-size/xresloader/xresloader)
![GitHub repo size](https://img.shields.io/github/repo-size/xresloader/xresloader)
![GitHub All Releases](https://img.shields.io/github/downloads/xresloader/xresloader/total)
![GitHub forks](https://img.shields.io/github/forks/xresloader/xresloader?style=social)
![GitHub stars](https://img.shields.io/github/stars/xresloader/xresloader?style=social)

文档: <https://xresloader.atframe.work>

## 主要功能

+ 跨平台（java 11 or upper）
+ Excel => protobuf/msgpack/lua/javascript/json/xml
+ 完整支持协议结构，包括嵌套结构和数组嵌套
+ 同时支持protobuf proto v2 和 proto v3
+ 支持导出proto枚举值到lua/javascript代码和json/xml数据
+ 支持导出proto描述信息值到lua/javascript代码和json/xml数据（支持自定义插件，方便用户根据proto描述自定义反射功能）
+ 支持导出 UnrealEngine 支持的json或csv格式，支持自动生成和导出 UnrealEngine 的 DataTable 加载代码
+ 支持别名表，用于给数据内容使用一个易读的名字
+ 支持验证器，可以在数据里直接填写proto字段名或枚举名，或者验证填入数据的是否有效
+ 支持通过protobuf协议插件控制部分输出
+ 支持自动合表，把多个Excel数据表合并成一个输出文件
+ 支持公式
+ 支持空数据压缩（裁剪）或保留定长数组
+ 支持基于正则表达式分词的字段名映射转换规则
+ 支持设置数据版本号
+ Lua输出支持全局导出或导出为 require 模块或导出为 module 模块。
+ Javascript输出支持全局导出或导出为 nodejs 模块或导出为 AMD 模块。
+ 提供CLI批量转换工具（支持python 2.7/python 3 @ Windows、macOS、Linux）
+ 提供GUI批量转换工具（支持Windows、macOS、Linux）
+ CLI/GUI批量转换工具支持include来实现配置复用

本工程只是转表引擎工具，批处理（批量转表）工具的请参见：

+ 批量转表配置规范: <https://github.com/xresloader/xresconv-conf>
+ 跨平台GUI工具(Windows/Linux/macOS): <https://github.com/xresloader/xresconv-gui>
+ 跨平台命令行工具(兼容python2和python3，Windows/Linux/macOS): <https://github.com/xresloader/xresconv-cli>

## v2.11.0-rc2及以前版本更新迁移指引

由于 v2.11.0-rc3 版本变更了默认的索引器，导致对Excel一些内置的数据类型处理和先前有一些差异。比如对于日期时间类型、百分率等。
现在会先转出原始的文本，再根据protocol的目标类型做转换。如果需要回退到老的POI索引，可以使用 `--enable-excel-formular` 选项切换到老的索引器。

新版本开始使用JDK 11打包，如果仍然需要 JDK1.8打包请自行下载源码并修改 `pom.xml` 内 `maven-compiler-plugin` 的 `source` 和 `target` 后使用 `mvn package` 命令打包。

## License

[![MIT License](https://img.shields.io/github/license/xresloader/xresloader)](LICENSE)

## GET START

使用步骤

1. 下载[xresloader][1]
2. 定义protobuf的协议文件(.proto)
3. 使用protobuf官方工具***protoc***把.proto文件转换成pb
4. 编写Excel,并且字段名和protobuf协议字段名和层级关系对应
5. 执行命令 java -jar XRESLOADER.jar [参数...]，传入excel文件名、表名和其他规则。（参数和选项见 [工具命令行参数](#工具命令行参数) ）
6. 使用对应的语言或者工具库加载导出的数据。（编码规则和解析工具见 [关于加载导出的数据](#关于加载导出的数据) ）

> + [sample](sample)目录下有所有功能的示例excel、配置、协议和对sample数据的几种读取方式代码

## 工具命令行参数

执行方式    java -jar xresloader.jar [参数...]

> 生成源和结果在sample目录下, xresloader的路径为 ../target/xresloader-1.4.1.jar

推荐使用--stdin批量输入+多个-m参数指定转表参数的模式,如:

```bash
echo "
-t lua -p protobuf -f proto_v3/kind.pb --pretty 2 -m \"DataSource=资源转换示例.xlsx|arr_in_arr|3,1\" -m \"MacroSource=资源转换示例.xlsx|macro|2,1\" -m \"ProtoName=arr_in_arr_cfg\" -m \"OutputFile=arr_in_arr_cfg.lua\" -m \"KeyRow=2\" -o proto_v3
-t bin -p protobuf -f proto_v3/kind.pb -m \"DataSource=资源转换示例.xlsx|arr_in_arr|3,1\" -m \"MacroSource=资源转换示例.xlsx|macro|2,1\" -m \"ProtoName=arr_in_arr_cfg\" -m \"OutputFile=arr_in_arr_cfg.bin\" -m \"KeyRow=2\" -o proto_v3
" | java -Dfile.encoding=UTF-8 -client -jar ../target/xresloader-1.3.3.0.jar --stdin
```

其他调用形式见[sample](sample)

### 可用参数列表

| 参数选项                       | 描述                            | 说明                                                                                                                            |
| ------------------------------ | ------------------------------- | ------------------------------------------------------------------------------------------------------------------------------- |
| -h --help                      | 帮助信息                        | 显示帮助和支持的参数列表                                                                                                        |
| -t --output-type               | 输出类型                        | bin（默认值）,lua,msgpack,json,xml,javascript,js,ue-csv,ue-json                                                                 |
| -p --proto                     | 协议描述类型                    | protobuf(默认值),capnproto(暂未实现),flatbuffer(暂未实现)                                                                       |
| -f --proto-file                | 协议描述文件                    |                                                                                                                                 |
| -o --output-dir                | 输出目录                        | 默认为当前目录                                                                                                                  |
| -d --data-src-dir              | 数据源根目录                    | 默认为当前目录                                                                                                                  |
| -s --src-file                  | 数据源描述文件                  | 后缀可以是 .xls, .xlsx, .cvs, .xlsm, .ods, .ini, .cfg, .conf, .json                                                             |
| -m --src-meta                  | 数据源描述表                    | 可多个                                                                                                                          |
| --enable-string-macro          | 设置Macro表也对字符串类型生效   | 可以通过全局开启此选项，特定表使用 `--disable-string-macro` 来实现默认开启字符串文本替换，特定表不替换                          |
| --disable-string-macro         | 设置Macro表也对字符串类型不生效 | (默认)                                                                                                                          |
| -v --version                   | 打印版本号                      |                                                                                                                                 |
| -n --rename                    | 重命名输出文件名                | 正则表达式 （如：/(?i)\\.bin$/\\.lua/）                                                                                         |
| --require-mapping-all          | 开启所有字段映射检查            | 开启所有字段映射检查后，转出结构中所有的字段都必须配置映射关系，数组字段至少要有一个元素                                        |
| --enable-alias-mapping         | 开启别名匹配                    | 映射Excel列到目标数据结构，允许使用别名(<2.18.0版本时默认值)                                                                    |
| --disable-alias-mapping        | 关闭别名匹配                    | 映射Excel列到目标数据结构，不允许使用别名(>=2.18.0版本)                                                                         |
| -c --const-print               | 输出协议描述中的常量            | 参数为字符串，表示输出的文件名                                                                                                  |
| -i --option-print              | 输出协议描述中的选项            | 参数为字符串，表示输出的文件名                                                                                                  |
| -r --descriptor-print          | 输出完整协议描述信息            | 参数为字符串，表示输出的文件名                                                                                                  |
| -a --data-version              | 设置数据版本号                  | 参数为字符串，表示输出的数据的data_ver字段。如果不设置将按执行时间自动生成一个                                                  |
| --pretty                       | 格式化输出                      | 参数为整数，0代表关闭美化输出功能，大于0表示格式化时的缩进量                                                                    |
| --enable-excel-formular        | 开启Excel公式实时计算           | 开启公式实时计算会减慢转表速度(2.11-RC3版本后默认关闭)                                                                          |
| --disable-excel-formular       | 关闭Excel公式实时计算           | 关闭公式实时计算，会使用新的流式索引器，大幅加快转表速度，降低内存开销。（注: 也会关闭对日期格式的探测）                        |
| --disable-empty-list           | 移除数组空项                    | (废弃)，请使用 `--list-strip-all-empty`                                                                                         |
| --enable-empty-list            | 保留全部数组空项                | (废弃)，请使用 `--list-keep-empty`                                                                                              |
| --list-strip-all-empty         | 移除数组空项                    | (默认) 移除数组空项，自动删除Excel中的未填充数据，不会转出到输出文件中                                                          |
| --list-keep-empty              | 保留全部数组空项                | 保留全部数组空项，未填充数据将使用默认的空值来填充，并转出到输出文件中                                                          |
| --list-strip-empty-tail        | 移除数组尾部空项                | 移除数组尾部空项，自动删除尾部的未填充数据，其他的未填充数据将使用默认的空值，并转出到输出文件中                                |
| --stdin                        | 通过标准输入批量转表            | 通过标准输入批量转表，参数可上面的一样,每行一项，字符串参数可以用单引号或双引号包裹，但是都不支持转义                           |
| --lua-global                   | lua输出写到全局表               | 输出协议描述中的常量到Lua脚本时，同时导入符号到全局表_G中（仅对常量导出有效）                                                   |
| --lua-module                   | lua输出使用module写出           | 输出Lua脚本时，使用 module(模块名, package.seeall) 导出到全局                                                                   |
| --xml-root                     | xml输出的根节点tag              | 输出格式为xml时的根节点的TagName                                                                                                |
| --javascript-export            | 导出javascript数据的模式        | 可选项(nodejs: 使用兼容nodejs的exports, amd: 使用兼容amd的define, 其他: 写入全局(window或global))                               |
| --javascript-global            | 导出javascript全局空间          | 导出数据到全局时，可以指定写入的名字空间                                                                                        |
| --ignore-unknown-dependency    | 忽略未知的依赖项                | 忽略未知的输入协议的依赖项(>=2.9.0版本)                                                                                         |
| --validator-rules              | 指定自定义验证器配置文件路径    | 指定自定义验证器配置文件路径                                                                                                    |
| --disable-data-validator       | 允许忽略数据验证错误            | (>=2.17.0版本)                                                                                                                  |
| --data-validator-error-version | 设置数据验证错误的起始版本号    | 低于这个版本的验证器才会验证失败，否则仅输出告警。0表示总是错误。(>=2.20.0版本)                                                 |
| --data-source-lru-cache-rows   | 数据源的LRU Cache行数           | 仅缓存流式索引                                                                                                                  |
| --tolerate-max-empty-rows      | 连续空行检测的行数              | 设置连续空行检测的行数(>=2.14.1版本) ，大量的连续空行通常是误操作                                                               |
| --ignore-field-tags            | 字段Tag                         | 忽略指定tag的字段转出(>=2.19.0版本)                                                                                             |
| --default-field-separator      | 默认Plain模式分隔符             | 设置Plain模式分隔符(>=2.21.0版本,默认值: `,;\|` ), 用于使用文本配置配置message,map,list,oneof类型的默认时，内部字段的分隔符检测 |
| --data-source-mapping-file     | 数据源映射输出文件              | (>=2.19.1版本)                                                                                                                  |
| --data-source-mapping-mode     | 数据源映射输出模式              | `none`, `md5`, `sha1`, `sha256` (>=2.19.1版本)                                                                                  |
| --data-source-mapping-seed     | 数据源映射输出Hash Seed         | (>=2.19.1版本)                                                                                                                  |

### 协议类型

1. **protobuf**     （已实现）
2. **capnproto**    （暂未支持:目前capnproto还没有提供java下通过反射打解包数据的方法，暂时放弃支持）
3. **flatbuffer**   （暂未支持:目前flatbuffers还没有提供java下通过反射打解包数据的方法，暂时放弃支持）

## 数据源描述文件说明(根据后缀判断类型有不同读取方式)

| 数据源描述文件后缀 | 数据源描述表                                                              | 说明                                   |
| ------------------ | ------------------------------------------------------------------------- | -------------------------------------- |
| .xls,.xlsx         | 视作Excel文件，数据源描述表为Excel内的Sheet名称                           | 已实现, 非微软格式尚未测试             |
| .ini,.conf,.cfg    | 视作ini文件，数据源描述表为ini内的Section名称                             | 已实现(不支持自动合表)                 |
| .json              | 视作json文件，数据源描述表为json内的第一层子节点名称                      | 已实现(必须是UTF-8编码,不支持自动合表) |
| .xml               | 视作xml文件，数据源描述表为xml内的根节点下的子节点TagName，并且只取第一个 | (暂未支持)                             |

## 数据源描述表配置项及示例

| 字段                        | 简介                                                              | 主配置              | 次配置          | 补充配置        | 说明                                                                                                                |
| --------------------------- | ----------------------------------------------------------------- | ------------------- | --------------- | --------------- | ------------------------------------------------------------------------------------------------------------------- |
| DataSource                  | 配置数据源(主配置:文件路径,次配置:表名,补充配置:起始行号，列号)   | ./资源转换示例.xlsx | kind            | 3,1             | **必须**，可多个。多个则表示把多个Excel表数据合并再生成配置输出，这意味着这多个Excel表的描述Key的顺序和个数必须相同 |
| MacroSource                 | 元数据数据源(主配置:文件路径,次配置:表名,补充配置:起始行号，列号) | ./资源转换示例.xlsx | macro           | 2,1             | *可选*                                                                                                              |
| 编程接口配置                |
| ProtoName                   | 协议描述名称                                                      | role_cfg            |                 |                 | **必须**, 这个名字可以直接是类型名称[MessageName]，也可以是[PackageName].[MessageName]                              |
| OutputFile                  | 输出文件                                                          | role_cfg.bin        |                 |                 | **必须**                                                                                                            |
| KeyRow                      | 字段名描述行                                                      | 2                   |                 |                 | **必须**                                                                                                            |
| KeyCase                     | 字段名大小写                                                      | 小写                |                 |                 | 大写/小写/不变(大小写转换，如果不需要则留空或小写)                                                                  |
| KeyWordSplit                | 字段名分词字符                                                    | _                   |                 |                 | *可选*,字段名映射时单词之间填充的字符串,不需要请留空                                                                |
| KeyPrefix                   | 字段名固定前缀                                                    |                     |                 |                 | *可选*,字段名映射时附加的前缀,不需要请留空                                                                          |
| KeySuffix                   | 字段名固定后缀                                                    |                     |                 |                 | *可选*,字段名映射时附加的后缀,不需要请留空                                                                          |
| KeyWordRegex                | 分词规则(判断规则,移除分词符号规则,前缀过滤规则)                  | `[A-Z_\$ \t\r\n]`   | `[_\$ \t\r\n]`  | `[a-zA-Z_\$]`   | *(可选)*,字段名映射时单词的分词规则,正则表达式,不需要请留空                                                         |
| Encoding                    | 编码转换                                                          | UTF-8               |                 |                 | 注：Google的protobuf库的代码里写死了UTF-8，故而该选项对Protobuf的二进制输出无效                                     |
| JsonCfg-LargeNumberAsString | 是否把大数字转换成字符串                                          | true                |                 |                 | 控制是否把大数字转换成字符串（Json和Javascript，2.16.0版本开始支持）                                                |
| UeCfg-UProperty             | UnrealEngine配置支持的字段属性                                    | 字段分组            | 蓝图权限        | 编辑权限        | *可选*,默认值: XResConfig\|BlueprintReadOnly\|EditAnywhere                                                          |
| UeCfg-CaseConvert           | 是否开启驼峰命名转换（默认开启）                                  | `true/false`        |                 |                 | *可选*,开启后将使用首字母大写的驼峰命名法生成字段名和类名                                                           |
| UeCfg-CodeOutput            | 代码输出目录                                                      | 代码输出根目录      | Publich目录前缀 | Private目录前缀 | *可选*                                                                                                              |
| UeCfg-DestinationPath       | 资源输出目录（uassert目录，默认会根据代码输出目录猜测）           | 左包裹字符          | 右包裹字符      |                 | *可选*                                                                                                              |
| UeCfg-CsvObjectWrapper      | 指定 `Ue-Csv` 模式输出时，map和array的包裹字符                    | 资源输出目录        |                 |                 | *可选*                                                                                                              |
| UeCfg-EnableDefaultLoader   | 是否启用UE默认的Loader                                            | `true/false`        |                 |                 | *可选*,默认值: `true`                                                                                               |
| UeCfg-IncludeHeader         | UE代码额外的自定义包含头文件                                      | 头文件路径          | 头文件路径      | 头文件路径      | *可选*,三个路径都可选，此选项可以多次出现                                                                           |
| CallbackScript              | 使用Javascript脚本处理输出的数据                                  | Javascript脚本路径  |                 |                 | *可选*, （2.13.0版本开始支持）                                                                                      |

### 数据源描述的特别说明

比如在**资源转换示例.xlsx**中：

1. DataSource指明配置从文件:资源转换示例.xlsx，表:kind，第3行第1列开始读数据
2. MacroSource指明配置从文件:资源转换示例.xlsx，表:macro，第2行第1列开始读数据文本替换的搜索串，并第2列读替换目标
3. ProtoName指明协议中的数据结构名称为role_cfg
4. OutputFile指明输出的文件名
5. KeyRow指明从文件:资源转换示例.xlsx，表:kind（DataSource指定）第2行找字段名称，并转换成协议中的字段名。填充数据到该字段。
6. KeyCase指明从第2行的字段名称转换成协议中的字段名时要转换成小写(如果字段名和第2行的内容一致则填不变即可)
7. KeyWordSplit指明从第2行的字段名称转换成协议中的字段名时分词符号是*_*。(如果字段名和第2行的内容一致则留空)
8. KeyPrefix指明从第2行的字段名称转换成协议中的字段名后，字段名加固定前缀
9. KeySuffix指明从第2行的字段名称转换成协议中的字段名后，字段名加固定后缀
10. KeyWordRegex指明分词的判定规则，全部是正则表达式
  >
  > + 主配置（分词匹配符）：`[A-Z_\$ \t\r\n]` 是指，碰到大写字母、下划线、$符号、空格和打印符、换行符都认为是新单词
  > + 次配置（过滤匹配符）：`[_\$ \t\r\n]` 是指，碰到分词符号后下划线、$符号、空格和打印符、换行符时都要移除
  > + 补充配置（起始匹配符）：`[a-zA-Z_\$]` 是指，第一次碰到字母、下划线、$符号后才开始认为是字段名，前面的都视为无效字符
  >
11. Encoding指明输出的字符串内容都是UTF-8编码。（目前最好只用UTF-8，因为protobuf里写死了UTF-8编码，其他编码不保证完全正常）
12. CallbackScript指向的脚本中，需要满足已下条件:

+ 可使用 `gOurInstance` 访问数据源接口（ `DataSrcImpl.getOurInstance()` ）
+ 可使用 `gSchemeConf` 访问数据转换配置接口（ `SchemeConf.getInstance()` ）
+ 提供 `function initDataSource()` 函数，将在切换数据源时触发（文件名或sheet名）。
+ 提供 `function currentMessageCallback(originMsg, typeDesc)` 函数，将在切换数据源时触发（文件名或sheet名）。
  + `originMsg` 为原始数据结构的 `HashMap` 结构
  + `typeDesc` 为数据类型描述信息, `org.xresloader.core.data.dst.DataDstWriterNode.DataDstTypeDescriptor` 结构

上面的配置中，数据从第3行读取，Key从第2行读取。那么第一行可以用来写一些说明或描述性数据。

以上6-10是都为了兼容某些情况下的名称和规范的适配问题。

比如：资源转换示例.xlsx的kind表中，0CostType这一项

1. KeyWordRegex的补充配置会过滤掉前缀0（不匹配[a-zA-Z_\$]），即CostType
2. KeyWordRegex的主配置会在大写字母处做分词，次配置指明保留字母，即分割成Cost Type
3. KeyCase指明转换成小写，即cost type
4. KeyWordSplit指明分词符号是_，即cost_type
5. 最后匹配到*kind.proto*中*role_cfg*的*cost_type*字段.

如果不需要字段名转换，直接把KeyWordRegex的配置全部配成空即可。（注意这样大小写转换也不会执行）

## 输出格式说明

| 输出格式参数 | 输出格式说明                                                                                                                                                       | 说明                                        |
| ------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------ | ------------------------------------------- |
| bin          | 基于协议的二进制文件,不同的协议类型(-p参数)输出的二进制不一样,一般是header+body,body中有转出的数据列表, 协议格式见header文件夹                                     | 示例见 [sample](sample/proto_v3) (protobuf) |
| lua          | 转出为的lua代码文件(可选是否要pretty格式化), 一般格式为 `return {[1] = 转表头信息, [协议结构名称] = {数据列表} }`                                                  | 示例见[sample](sample/proto_v3)             |
| msgpack      | 转出为使用[MsgPack](http://msgpack.org/)打包的二进制文件,内含的第一个message是转表头信息，后面紧跟数据，可以用任何支持得客户端解包                                 | 示例见[sample](sample/proto_v3)             |
| json         | 转出为json文件,一般格式为 `[ {转表头信息}, {协议结构名称 : [ 转出的数据 ] } ]`                                                                                     | 示例见[sample](sample/proto_v3)             |
| xml          | 转出为xml文件,一般格式为&lt;root&gt;&lt;header&gt;转表头信息&lt;/header&gt;&lt;body&gt;&lt;协议结构名称&gt;数据内容&lt;/协议结构名称&gt;&lt;/body&gt;&lt;/root&gt; | 示例见[sample](sample/proto_v3)             |
| js           | 转出为js代码文件(可选是否要pretty格式化)                                                                                                                           | 示例见[sample](sample/proto_v3)             |

**注意：** Xml输出格式中，列表元素的结构是&lt;配置名称 for="0"&gt;数据1&lt;/配置名称&gt;&lt;配置名称 for="1"&gt;数据2...&lt;/配置名称&gt; 属性字段for表示数组索引，目的是方便通过xpath查找。

## 关于加载导出的数据

转出的数据都采用header+data_block的形式。本工具并不规定怎么读取转表导出的数据，开发者可以按照转出的数据规则自由操作。

> 建议项目中使用导出的协议二进制或者msgpack。协议二进制可以用任意语言加载protobuf或者其他类似工具（如：[pbc][3] 或 [upb][2] ）加载。
>
> 而其他导出类型可以用于一些外部工具的集成，比如基于Web的GM工具，基于Lua的远程调试工具等等。

但是为了使用方便，在[loader-binding](loader-binding)里提供了几种基本的读表方式。

### protobuf协议

+ protobuf的协议数据解包主要流程:
  >
  > 1. 二进制文件使用[third_party/xresloader-protocol/core/pb_header.proto][8] 中的 `xresloader_datablocks` 结构解包
  > 2. xresloader_datablocks.data_block内保存的二进制使用指定的配置协议解包
  > 3. xresloader_datablocks.data_block的个数就是Excel转出的数据行数，注意包含空行

+ 如果你使用官方的protobuf或protobuf-lite，可以使用[loader-binding/cxx](loader-binding/cxx)来加载配置
  > sample 参见: [sample/cxx/read_kind_sample.cpp](sample/cxx/read_kind_sample.cpp)

+ 如果你使用云风的[pbc][3]，可以使用[loader-binding/pbc](loader-binding/pbc)来加载配置
  > 这个加载器会依赖 [https://github.com/owent-utils/lua/tree/master/src](https://github.com/owent-utils/lua/tree/master/src) 里的部分内容。
  >
  > 需要使用pbc先加载[header/pb_header.pb](header/pb_header.pb)文件。
  >
  > **proto v3请注意: [pbc][3] 不支持[packed=true]属性。在proto v3中，所有的*repeated*整数都默认是[packed=true]，要使用pbc解码请注意这些field要显示申明为[packed=false]**
  >
  > 或者使用我修改过的[pbc的proto_v3分支](https://github.com/owent-contrib/pbc/tree/proto_v3)
  >
  > pbc_config_manager:load_buffer_kv(协议名, 二进制, function(序号, 转出的lua table) return key的值 end) -- 读取key-value型数据接口
  >
  > pbc_config_manager:load_buffer_kl(协议名, 二进制, function(序号, 转出的lua table) return key的值 end) -- 读取key-list型数据接口

+ 如果你使用protobuf的 [upb][2]和 [upb][2] 的Lua binding加载配置，可以使用 [xres-code-generator][4] 子项目 来生成加载配置的代码
+ UE支持:
  + 可以直接输出支持UE DataTable的 `Ue-Csv` 或者 `Ue-Json` 格式，同时也会输出对应结构的代码和导入设置。
  + 也可以通过 [xres-code-generator][4] 子项目来生成C++接口，再通过 `template/UE*` 的UE模板来生成蓝图支持的Wrapper接口。通过这种方式加载数据支持多版本并存和支持复杂的多级索引和多个索引。

## 其他输出格式

> **注意：所有导出非二进制的数据都是不带包名的（package），但是使用-c --const-print选项导出协议常量除外。因为数据加载一般有manager统一管理，而协议常量一般直接用于代码中。**
>
> 导出的常量都很简单易懂，直接看生成的文件很容易理解，这里不再额外作说明了。

1. **lua**格式输出可以按[loader-binding/lua](loader-binding/lua)的说明读取。
  >
  > 这个加载器会依赖 [https://github.com/owent-utils/lua/tree/master/src](https://github.com/owent-utils/lua/tree/master/src) 里的部分内容。
  >
  > conf_manager:load_kv(require的路径, function(序号, 转出的lua table) return key的值 end) -- 读取key-value型数据接口
  >
  > conf_manager:load_kl(require的路径, function(序号, 转出的lua table) return key的值 end) -- 读取key-list型数据接口
  >
2. [MsgPack](http://msgpack.org/)的读取的语言和工具很多，任意工具都能比较简单地读出数据，[loader-binding/msgpack](loader-binding/msgpack)里有一些读取示例(Python和Node.js)
3. **Json**的读取的语言和工具很多，任意工具都能比较简单地读出数据，故而不再提供读取工具
4. **Xml**的读取的语言和工具很多，任意工具都能比较简单地读出数据，故而不再提供读取工具
5. **Javascript**的读取的语言和工具很多，任意工具都能比较简单地读出数据，[loader-binding/javascript](loader-binding/javascript)里有相关说明

## 高级功能

### 验证器

Excel里的Key使用@后缀的字段名，@后面的部分都属于验证器。或者也可以通过协议插件来设置验证器，如果一个字段使用了验证器，验证器可以使用以下值:

+ 函数: `InText("文件名"[, 第几个字段[, \"字段分隔正则表达式\"]])` : 从文本文件（UTF-8编码）,可以指定读第几个字段和用于字段分隔的正则表达式
+ 函数: `InTableColumn("文件名", "Sheet名", 从第几行开始, 从第几列开始)` : 从Excel数据列读取可用值,指定数据行和数据列
+ 函数: `InTableColumn("文件名", "Sheet名", 从第几行开始, KeyRow, KeyValue)` : 从Excel数据列读取可用值,指定数据行并通过某一行的的值获取数据列
+ 函数: `Regex("正则表达式")` : 验证匹配正则表达式(>=2.21.0版本)
+ 函数: `InMacroTable("文件名", "Sheet名", 从第几行开始, 第几列是映射Key, 第几列是映射Value)` : 从Excel里读取别名映射,指定数据行和别名映射Key和别名映射Value的列号(>=2.20.0版本)
  > 类似于Macro表，但是可以把这个验证器配置在指定字段或Excel列中，仅对指定字段或Excel列生效。
+ 函数: `InMacroTable("文件名", "Sheet名", 从第几行开始, KeyRow, 映射Key字段名, 映射Value字段名)` : 从Excel里读取别名映射,指定数据行并通过某一行的值别名映射Key和别名映射Value的列(>=2.20.0版本)
  > 类似于Macro表，但是可以把这个验证器配置在指定字段或Excel列中，仅对指定字段或Excel列生效。
  > 具体可参考 [sample/custom_validator.yaml](sample/custom_validator.yaml) 内的 `custom_rule6` 和 [sample/proto_v3/kind.proto](sample/proto_v3/kind.proto) 内的 `field_alias_message` 配置。
+ 函数: `And("子验证器", ...)` : 必须同时满足多个验证器(>=2.21.0版本)
+ 函数: `Or("子验证器", ...)` : 必须满足任一验证器(>=2.21.0版本)
+ 自定义验证器名（通过 `--validator-rules` 加载）
+ 协议类型（对应protobuf的message里的每个field，excel里可以填field number或者field name）
+ 枚举类型（对应protobuf的enum里的每个number，excel里可以填enum number或者enum name）
+ 值范围: `A-B`（比如 `0-1234` ）或 `>=A`（比如 `>=1234` ）或 `<=A`（比如 `<=1234` ）或 `>A`（比如 `>1234` ）或 `<A`（比如 `<1234` ）
+ 多个验证器可以使用“或”符号（“|”)来分隔开， 任意一个验证器满足条件则认为时合法数据(如: 100-200|2000-3000|test_msg_verifier)

自定义验证器的配置格式(YAML):

```yaml
validator:
  - name: "validator name"
    description: "（可选）描述"
    version: 0 # 版本，从 2.20.0 版本开始支持
    mode: or # 模式: or, and, not 。从 2.21.1 版本开始支持, 默认为 or
    rules:
      - validator_rule1
      - validator_rule2
      - ...
```

详见 [sample/资源转换示例.xlsx](sample/资源转换示例.xlsx) 的upgrade_10001和upgrade_10002表

### Protobuf插件

项目中可以导入 [header/extensions](header/extensions) 目录， 然后通过导入 [header/v2](header/extensions/v2) 或 [header/v2](header/extensions/v3) 中的相应proto文件来支持额外的插件扩展支持。

#### Protobuf插件 - Message插件

| 插件名称                               | 插件功能                                                                                           |
| :------------------------------------- | :------------------------------------------------------------------------------------------------- |
| org.xresloader.msg_description         | 消息体描述信息，会写入输出的header中和代码中                                                       |
| org.xresloader.msg_require_mapping_all | 设置message的所有字段必须被全部映射                                                                |
| org.xresloader.msg_separator           | Plain模式字段分隔符，可指定多个，用于在一个单元格内配置复杂格式时的分隔符列表，默认值: `,;、       | ` |
| org.xresloader.ue.helper               | 生成UE Utility代码的类名后缀                                                                       |
| org.xresloader.ue.not_data_table       | 不是DataTable，helper类里不生成加载代码                                                            |
| org.xresloader.ue.default_loader       | Message是否开启默认Loader（`EN_LOADER_MODE_DEFAULT,EN_LOADER_MODE_ENABLE,EN_LOADER_MODE_DISABLE`） |
| org.xresloader.ue.include_header       | Message输出的代码额外包含头文件，可多个                                                            |

#### Protobuf插件 - Field插件

| 插件名称                                         | 插件功能                                                                                                                                     |
| :----------------------------------------------- | :------------------------------------------------------------------------------------------------------------------------------------------- |
| org.xresloader.validator                         | 验证器，可填范围(log-high),message名，enum名。多个由 `\|` 分隔。任意验证器通过检查则认为数据有效                                             |
| org.xresloader.field_unique_tag                  | 设置唯一性检测Tag，多个相同tag的字段将合并并在转出数据时检测唯一性（可多个）                                                                 |
| org.xresloader.field_not_null                    | 如果配置了字段映射且某个数据行对应的oneof数据为空，则忽略此行                                                                                |
| org.xresloader.map_key_validator                 | 用于Map类型Key的验证器，可填范围(low-high),message名，enum名。多个由 `\|` 分隔。任意验证器通过检查则认为数据有效                             |
| org.xresloader.map_value_validator               | 用于Map类型Value的验证器，可填范围(low-high),message名，enum名。多个由 `\|` 分隔。任意验证器通过检查则认为数据有效                           |
| org.xresloader.field_description                 | 字段描述信息，会写入输出的header中和代码中                                                                                                   |
| org.xresloader.field_alias                       | 字段别名，配合 **验证器** 功能，允许在数据源中直接填写别名来配置数据                                                                         |
| org.xresloader.field_ratio                       | 字段放大，用于比如配置百分率为 0.12，当 org.xresloader.field_ratio=100时转出的数据为12                                                       |
| org.xresloader.field_separator                   | Plain模式分隔符，可指定多个，用于在一个单元格内配置复杂格式时的分隔符列表，默认值: `,;\| `                                                   |
| org.xresloader.field_required                    | 设置字段为 **required** ，用于向proto3提供，proto2的 **required** 约束                                                                       |
| org.xresloader.field_origin_value                | 写出原始数据到指定字段（ `Timestamp` 和 `Duration` 类型）                                                                                    |
| org.xresloader.field_allow_missing_in_plain_mode | Plain模式下设置此字段可选，如果未设置则使用默认值（版本 2.16.0 版本开始支持）                                                                |
| org.xresloader.field_list_strip_option           | 给单个字段设置数组裁剪，可选值( `LIST_STRIP_DEFAULT\|LIST_STRIP_NOTHING\|LIST_STRIP_TAIL\|LIST_STRIP_ALL` )（版本 2.18.0 版本开始支持）      |
|                                                  | + `LIST_STRIP_DEFAULT`: 默认值，使用 `--list-strip-all-empty/--list-keep-empty/--list-strip-empty-tail` 控制，未设置则是裁剪全部空值 ）      |
|                                                  | + `LIST_STRIP_NOTHING`: 不裁剪数据，相当于 `--list-keep-empty`                                                                               |
|                                                  | + `LIST_STRIP_TAIL`: 裁剪尾部空值，相当于 `--list-strip-empty-tail`                                                                          |
|                                                  | + `LIST_STRIP_ALL`: 裁剪全部空值，相当于 `--list-strip-all-empty`                                                                            |
| org.xresloader.field_list_min_size               | 给单个字段设置数组最小长度，输入字符串：`"<N>\|枚举名"`（版本 2.18.0 版本开始支持）                                                          |
| org.xresloader.field_list_max_size               | 给单个字段设置数组最大长度，输入字符串：`"<N>\|枚举名"`（版本 2.18.0 版本开始支持）                                                          |
| org.xresloader.field_list_strict_size            | 设置单个字段设置数组严格长度要求，即不自动补全最小长度，而是报错。：`false\|true`（默认值: `false` ，版本 2.18.0 版本开始支持）              |
| org.xresloader.field_tag                         | 设置字段Tag，配合 `--ignore-field-tags` 选项可用于跳过某些数据。（版本 2.19.0 版本开始支持）                                                 |
| org.xresloader.ue.key_tag                        | 生成UE代码时，如果需要支持多个Key组合成一个Name，用这个字段指定系数（必须大于0）                                                             |
| org.xresloader.ue.ue_type_name                   | 生成UE代码时，如果指定了这个字段，那么生成的字段类型将是 `TSoftObjectPtr<ue_type_name>` , 并且支持蓝图中直接引用                             |
| org.xresloader.ue.ue_type_is_class               | 生成UE代码时，如果指定了这个字段，那么生成的字段类型将是 `TSoftClassPtr<ue_type_name>` , 并且支持蓝图中直接引用                              |
| org.xresloader.ue.ue_origin_type_name            | 生成UE代码时，如果指定了这个字段，那么生成的字段类型将使用这个类型，需要用户自己确保类型有效且对应的proto类型可以隐式转换到该类型            |
| org.xresloader.ue.ue_origin_type_default_value   | 生成UE代码时，如果指定了这个字段，那么生成的字段的Reset代码将使用这个表达式，需要用户自己确保表达式有效且对应的proto类型可以隐式转换到该类型 |

#### Protobuf插件 - EnumValue插件

| 插件名称                         | 插件功能                                                               |
| :------------------------------- | :--------------------------------------------------------------------- |
| org.xresloader.enumv_description | 枚举值描述信息，可能会写入输出的header中和代码中                       |
| org.xresloader.enum_alias        | 枚举值别名，配合 **验证器** 功能，允许在数据源中直接填写别名来配置数据 |

#### Protobuf插件 - Oneof插件(2.8.0版本及以上)

| 插件名称                                         | 插件功能                                                                                                 |
| :----------------------------------------------- | :------------------------------------------------------------------------------------------------------- |
| org.xresloader.oneof_description                 | oneof描述信息，可能会写入输出的header中和代码中                                                          |
| org.xresloader.oneof_separator                   | Plain模式类型和值字段的分隔符，可指定多个，用于在一个单元格内配置复杂格式时的分隔符列表，默认值: `,;\| ` |
| org.xresloader.oneof_not_null                    | 如果配置了字段映射且某个数据行对应的oneof数据为空，则忽略此行                                            |
| org.xresloader.oneof_allow_missing_in_plain_mode | Plain模式下设置此字段可选，如果未设置则使用默认值（版本 2.16.0 版本开始支持）                            |
| org.xresloader.oneof_tag                         | 设置字段Tag，配合 `--ignore-field-tags` 选项可用于跳过某些数据。（版本 2.19.0 版本开始支持）             |

## 生态和工具

+ [xresconv-gui][5]: GUI批量转表工具。 <https://github.com/xresloader/xresconv-gui>
+ [xresconv-cli][6]: 命令行批量转表工具。 <https://github.com/xresloader/xresconv-cli>
+ [xres-code-generator][4]: 读表代码生成工具。 <https://github.com/xresloader/xres-code-generator>
+ [xresloader-dump-bin][7]: 二进制输出的dump工具。 <https://github.com/xresloader/xresloader-dump-bin>
  > 用于把转表生成的二进制导出为Human-Readable的文本，方便调试。可以直接从 <https://github.com/xresloader/xresloader-dump-bin/releases> 下载对应平台的可执行程序

## 编译和打包（For developer）

+ 本项目使用[apache maven](https://maven.apache.org/)管理包依赖和打包构建流程。
+ JDK 需要1.8或以上版本

```bash
# 编译
mvn compile
# 打包
mvn package
```

以上命令会自动下载依赖文件、包和插件。

编译完成后，输出的结果默认会放在 ***target*** 目录下。

### 更新依赖包

编译和打包见 [安装说明](doc/INSTALL.md)

## FAQ

1. 为什么会读到很多空数据？

Ans: Excel里编辑过的单元格即便删除了也会留下不可见的样式配置，这时候会导致转出的数据有空行。可以通过在Excel里删除行解决

2. 为什么Excel里填的时间，但是转出来是一个负数？

Ans: Excel里的日期时间类型转成协议里整数时会转为Unix时间戳，但是Excel的时间是以1900年1月0号为基准的，这意味着如果时间格式是hh:mm:dd的话，49:30:01会被转为1900-1-2 1:31:01。时间戳会是一个很大的负数

介于这个原因，不建议在Excel中使用时间类型

3. Windows下控制台里执行执行会报文件编码错误？（java.nio.charset.UnsupportedCharsetException: cp65001）

Ans: 这个问题涉及的几个Exception是

```bash
ERROR StatusLogger Unable to inject fields into builder class for plugin type class org.apache.logging.log4j.core.appender.ConsoleAppender, element Console.
 java.nio.charset.UnsupportedCharsetException: cp65001
        at java.nio.charset.Charset.forName(Unknown Source)
        at org.apache.logging.log4j.util.PropertiesUtil.getCharsetProperty(PropertiesUtil.java:146)
        at org.apache.logging.log4j.util.PropertiesUtil.getCharsetProperty(PropertiesUtil.java:134)
        ...
```

和

```bash
ERROR StatusLogger Unable to invoke factory method in class class org.apache.logging.log4j.core.appender.ConsoleAppender for element Console.
 java.lang.IllegalStateException: No factory method found for class org.apache.logging.log4j.core.appender.ConsoleAppender
        at org.apache.logging.log4j.core.config.plugins.util.PluginBuilder.findFactoryMethod(PluginBuilder.java:224)
        at org.apache.logging.log4j.core.config.plugins.util.PluginBuilder.build(PluginBuilder.java:130)
        at org.apache.logging.log4j.core.config.AbstractConfiguration.createPluginObject(AbstractConfiguration.java:952)
        ...
```

这是因为在Windows控制台中，如果编码是UTF-8，java获取编码时会获取到cp65001，而这个编码java本身是不识别的。这种情况可以按下面的方法解决：

+ 第一种: 执行 [xresloader][1] 之前先执行 chcp 936，切换到GBK编码
+ 第二种: 在powershell里执行

4. 为什么在proto里定义的是一个无符号(unsigned)类型(uint32、uint64等)，实际输出的UE代码是有符号(signed)的(int32/int64)？

Ans: 因为有一些语言是没有无符号(unsigned)类型的，为了统一数据类型，我们统一转换为有符号类型，转换方式和protobuf的java版SDK保持一致。如果需要使用大于int32最大值的uint32类型，请用int64代替。

5. 为什么 `UE-Csv` 和 `UE-Json` 输出的代码会多一个 `Name` 字段?

Ans: 因为对 `UE-Json` 输出中， `Name` 是一个特殊字段，也用于UE中内置的接口的查找索引。所以为了统一输出的数据结构（ 这样无论是 `UE-Csv` 还是 `UE-Json` 都可以用相同的代码结构来导入 ），我们对 `UE-Csv` 和 `UE-Json` 统一自动生成 `Name` 字段。但是如果用户自定义了 `Name` 字段， 我们会使用用户自定义的 `Name` 字段。

6. 提示 `Can not reserve enough space for XXX objecct heap`

Ans: 在转换很大的Excel文件时（上万行数据），会需要很高的内存（>=1GB）。所以为了方便我们在批量转表sample的xml中配置了 `<java_option desc="java选项-最大内存限制2GB">-Xmx2048m</java_option>` 。
如果出现这个提示可能是32位jre无法分配这么多地址空间导致的，可以在xml里删除这个配置。但是还是建议使用64位jre。

7. 提示 `Exception in thread "main" java.lang.OutOfMemoryError: Java heap space`

Ans: 这个提示通常是堆内存不足， [xresloader][1] 默认使用的POI的内置缓存机制会消耗大量内存。碰到这种情况，可以和上面的解决方法一样加一个类似 `-Xmx8192m` 来增大最大内存限制。

在 [xresloader][1] **2.10.0** 及以上的版本，可以使用 `--disable-excel-formular` 选项关闭实时公式计算\(仅仅时关闭公式实时计算，还是会读Excel里已经缓存的计算结果的\)。这时候 [xresloader][1] 会使用流式读取并使用 [xresloader][1] 内部实现的缓存机制，同时关闭文件级缓存和表级缓存，能大幅降低内存消耗。

8. 如何设置默认时区

Ans: 可以通过环境变量 `TZ` 或者java运行时属性 `user.timezone` 设置时区。

+ 环境变量 `TZ`: `export TZ=UTC`
+ 运行时属性 `user.timezone`: `java -Duser.timezone=UTC -jar <jar file>`

可用的区域示例(`+HH:mm`,`-HH:mm`,别名,完整时区名): `+08:00`, `UTC`, `GMT`, `Asia/Shanghai`, `America/Los_Angeles`, `Asia/Singapore` .

See <https://en.wikipedia.org/wiki/List_of_tz_database_time_zones> and <https://www.iana.org/time-zones> for details.

[1]: https://github.com/xresloader/xresloader/releases
[2]: https://github.com/protocolbuffers/upb
[3]: https://github.com/cloudwu/pbc
[4]: https://github.com/xresloader/xres-code-generator
[5]: https://github.com/xresloader/xresconv-gui
[6]: https://github.com/xresloader/xresconv-cli
[7]: https://github.com/xresloader/xresloader-dump-bin
[8]: https://github.com/xresloader/xresloader-protocol/blob/main/core/pb_header_v3.proto
