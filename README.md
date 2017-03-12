xresloader
==========

快速实现一个简单暴力功能强大的导表工具

构建环境     | Linux (Oracle JDK 8) |
-------------|----------------------|
当前构建状态 | [![Build Status](https://travis-ci.org/xresloader/xresloader.svg?branch=master)](https://travis-ci.org/xresloader/xresloader) |


Gitter
------
[![Gitter](https://badges.gitter.im/xresloader/xresloader.svg)](https://gitter.im/xresloader/xresloader?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge)

License
------
[MIT License](LICENSE)

编译和打包
======

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

更新依赖包
------
编译和打包见 [安装说明](doc/INSTALL.md)


工具命令行参数
======
执行方式    java -jar xresloader.jar [参数...]

比如：（生成源和结果在sample目录下, xresloader的路径为 ../target/xresloader-1.3.0.2.jar）

```bash
cd sample;

# Excel=>二进制（按协议） 
java -client -jar ../target/xresloader-1.3.0.2.jar -t bin -p protobuf -f proto_v3/kind.pb -s 资源转换示例.xlsx -m scheme_kind -o proto_v3

# Excel=>Lua，并重命名输出文件 
java -client -jar ../target/xresloader-1.3.0.2.jar -t lua -p protobuf -f proto_v3/kind.pb --pretty 4 -s 资源转换示例.xlsx -m scheme_kind -n "/(?i)\.bin$/\.lua/" -o proto_v3

# Excel=>Javascript，并重命名输出文件， 并把数据都导入到全局变量sample
java -client -jar ../target/xresloader-1.3.0.2.jar -t js -p protobuf -f proto_v3/kind.pb --pretty 2 -s 资源转换示例.xlsx -m scheme_kind -n "/(?i)\.bin$/\.js/" --javascript-global sample -o proto_v3

# Excel=>MsgPack二进制，并重命名输出文件 
java -client -jar ../target/xresloader-1.3.0.2.jar -t msgpack -p protobuf -f proto_v3/kind.pb -s 资源转换示例.xlsx -m scheme_kind -n "/(?i)\.bin$/\.msgpack.bin/" -o proto_v3

# 输出文件重命名+输出json格式+多次转表（多个-m参数）
java -client -jar ../target/xresloader-1.3.0.2.jar -t json -p protobuf -f proto_v3/kind.pb -n "/(?i)\.bin$/\.json/" -s 资源转换示例.xlsx -m scheme_kind -m scheme_upgrade -o proto_v3

# Excel=>Xml，并重命名输出文件 
# Excel=>Xml，数据源是ini文件, 并重命名输出文件 
# Excel=>Xml，数据源是json文件, 并重命名输出文件
# Protobuf=>lua代码，输出所有的枚举类型  
echo "
-t xml -p protobuf -f proto_v3/kind.pb -s 资源转换示例.xlsx -m scheme_kind -n \"/(?i)\.bin$/\.xml/\" -o proto_v3
-t xml -p protobuf -f proto_v3/kind.pb --pretty 2 -s 资源转换示例.scheme.ini -m scheme_kind -n \"/(?i)\.bin$/\.xml/\" -o proto_v3
-t xml -p protobuf -f proto_v3/kind.pb -s 资源转换示例.scheme.json -m scheme_kind -n \"/(?i)\.bin$/\.xml/\" -o proto_v3
-t lua -p protobuf -f proto_v3/kind.pb --pretty 2 -c kind_const.lua --lua-global -o proto_v3
" | java -Dfile.encoding=UTF-8 -client -jar ../target/xresloader-1.3.0.2.jar --stdin

# 注意这个命令必须使用bash或sh
# 如果bash的编码是UTF-8在Windows下会因为编码错误而找不到文件,所以需要加-Dfile.encoding=UTF-8
# 如果bash的编码和系统编码一致（一般是GBK），则不用加这个选项

# Excel => 二进制(按协议,内嵌数组)+命令行输入meta
# Excel => Lua(按协议,内嵌数组)+命令行输入meta
echo "
-t lua -p protobuf -f proto_v3/kind.pb --pretty 2 -m \"DataSource=资源转换示例.xlsx|arr_in_arr|3,1\" -m \"MacroSource=资源转换示例.xlsx|macro|2,1\" -m \"ProtoName=arr_in_arr_cfg\" -m \"OutputFile=arr_in_arr_cfg.lua\" -m \"KeyRow=2\" -o proto_v3
-t bin -p protobuf -f proto_v3/kind.pb -m \"DataSource=资源转换示例.xlsx|arr_in_arr|3,1\" -m \"MacroSource=资源转换示例.xlsx|macro|2,1\" -m \"ProtoName=arr_in_arr_cfg\" -m \"OutputFile=arr_in_arr_cfg.bin\" -m \"KeyRow=2\" -o proto_v3
" | java -Dfile.encoding=UTF-8 -client -jar ../target/xresloader-1.3.0.2.jar --stdin
```

可用参数列表
------

|          参数选项           |         描述        |                   说明                                                                                     |
|-----------------------------|---------------------|------------------------------------------------------------------------------------------------------------|
|-h --help                    | 帮助信息            | 显示帮助和支持的参数列表                                                                                   |
|-t --output-type             | 输出类型            | bin（默认值）,lua,msgpack,json,xml,javascript,js                                                           |
|-p --proto                   | 协议描述类型        | protobuf(默认值),capnproto(暂未实现),flatbuffer(暂未实现)                                                  |
|-f --proto-file              | 协议描述文件        |                                                                                                            |
|-o --output-dir              | 输出目录            | 默认为当前目录                                                                                             |
|-d --data-src-dir            | 数据源根目录        | 默认为当前目录                                                                                             |
|-s --src-file                | 数据源描述文件      | 后缀可以是 .xls, .xlsx, .cvs, .xlsm, .ods, .ini, .cfg, .conf, .json                                        |
|-m --src-meta                | 数据源描述表        | 可多个                                                                                                     |
|-v --version                 | 打印版本号          |                                                                                                            |
|-n --rename                  | 重命名输出文件名    | 正则表达式 （如：/(?i)\\.bin$/\\.lua/）                                                                    |
|-c --const-print             | 输出协议描述中的常量| 参数为字符串，表示输出的文件名                                                                             |
|--pretty                     | 格式化输出          | 参数为整数，0代表关闭美化输出功能，大于0表示格式化时的缩进量                                               |
|--enable-excel-formular      | 开启Excel公式支持   | 默认开启，使用公式会大幅减慢转表速度                                                                       |
|--disable-excel-formular     | 关闭Excel公式支持   | 关闭公式会大幅加快转表速度                                                                                 |
|--disable-empty-list         | 禁止空列表项        | 默认开启，禁止空列表项，自动删除Excel中的未填充数据，不会转出到输出文件中                                  |
|--enable-empty-list          | 开启空列表项        | 开启空列表项，未填充数据将使用默认的空值来填充，并转出到输出文件中                                         |     
|--stdin                      | 通过标准输入批量转表| 通过标准输入批量转表，参数可上面的一样,每行一项，字符串参数可以用单引号或双引号包裹，但是都不支持转义      |
|--lua-global                 | lua输出写到全局表   | 输出协议描述中的常量到Lua脚本时，同时导入符号到全局表_G中（仅对常量导出有效）                              |    
|--xml-root                   | xml输出的根节点tag  | 输出格式为xml时的根节点的TagName                                                                           |
|--javascript-export          | 导出javascript数据的模式| 可选项(nodejs: 使用兼容nodejs的exports, amd: 使用兼容amd的define, 其他: 写入全局(window或global))      |
|--javascript-global          | 导出javascript全局空间| 导出数据到全局时，可以指定写入的名字空间                                                                 |

 
协议类型
------
1. **protobuf**     （已实现）
2. **capnproto**    （暂未支持:目前capnproto还没有提供java下通过反射打解包数据的方法，暂时放弃支持）
3. **flatbuffer**   （暂未支持:目前flatbuffers还没有提供java下通过反射打解包数据的方法，暂时放弃支持）
                
                
数据源描述文件说明(根据后缀判断类型有不同读取方式)
------
|     数据源描述文件后缀      |                                  数据源描述表                                  |           说明           |
|-----------------------------|--------------------------------------------------------------------------------|--------------------------|
|         .xls,.xlsx          | 视作Excel文件，数据源描述表为Excel内的Sheet名称                                |已实现, 非微软格式尚未测试|
|     .ini,.conf,.cfg         | 视作ini文件，数据源描述表为ini内的Section名称                                  |已实现(不支持自动合表)   |
|          .json              | 视作json文件，数据源描述表为json内的第一层子节点名称                              |已实现(必须是UTF-8编码,不支持自动合表)   |
|          .xml               | 视作xml文件，数据源描述表为xml内的根节点下的子节点TagName，并且只取第一个            |(暂未支持)                |


数据源描述表配置项及示例
======
|     字段     |                        简介                                                            |           主配置           |     次配置   |   补充配置   |     说明     |
|--------------|----------------------------------------------------------------------------------------|----------------------------|--------------|--------------|--------------|
|DataSource    | 配置数据源(主配置:文件路径,次配置:表名,补充配置:起始行号，列号)                        |  ./资源转换示例.xlsx       | kind         |  3,1         |   **必须**，可多个。多个则表示把多个Excel表数据合并再生成配置输出，这意味着这多个Excel表的描述Key的顺序和个数必须相同   |
|MacroSource   | 元数据数据源(主配置:文件路径,次配置:表名,补充配置:起始行号，列号)                      |  ./资源转换示例.xlsx       | macro        |  2,1         |    *可选*    |
|编程接口配置  |
|ProtoName     | 协议描述名称                                                                           |   role_cfg                 |              |              |   **必须**, 这个名字可以直接是类型名称[MessageName]，也可以是[PackageName].[MessageName]   |
|OutputFile    | 输出文件                                                                               |   role_cfg.bin             |              |              |   **必须**   |
|KeyRow        | 字段名描述行                                                                           |  2                         |              |              |   **必须**   |
|KeyCase       | 字段名大小写                                                                           | 不变                       |              |              |大写/小写/不变(大小写转换，如果不需要则留空或小写)|
|KeyWordSplit  | 字段名分词字符                                                                         | _                          |              |              |    *可选*,字段名映射时单词之间填充的字符串,不需要请留空 |
|KeyPrefix     | 字段名固定前缀                                                                         |                            |              |              |    *可选*,字段名映射时附加的前缀,不需要请留空 |
|KeySuffix     | 字段名固定后缀                                                                         |                            |              |              |    *可选*,字段名映射时附加的后缀,不需要请留空 |
|KeyWordRegex  | 分词规则(判断规则,移除分词符号规则,前缀过滤规则)                                       | [A-Z_\$ \t\r\n]            | [_\$ \t\r\n] | [a-zA-Z_\$]  | *(可选)*,字段名映射时单词的分词规则,正则表达式,不需要请留空 |
|Encoding      | 编码转换                                                                               | UTF-8                      |              |              |注：Google的protobuf库的代码里写死了UTF-8，故而该选项对Protobuf的二进制输出无效|


数据源描述的特别说明
------

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
> + 主配置（分词匹配符）：[A-Z_\$ \t\r\n]是指，碰到大写字母、下划线、$符号、空格和打印符、换行符都认为是新单词
> + 次配置（过滤匹配符）：[_\$ \t\r\n]是指，碰到分词符号后下划线、$符号、空格和打印符、换行符时都要移除
> + 补充配置（起始匹配符）：[a-zA-Z_\$]是指，第一次碰到字母、下划线、$符号后才开始认为是字段名，前面的都视为无效字符
11. Encoding指明输出的字符串内容都是UTF-8编码。（目前最好只用UTF-8，因为protobuf里写死了UTF-8编码，其他编码不保证完全正常）

上面的配置中，数据从第3行读取，Key从第2行读取。那么第一行可以用来写一些说明或描述性数据。

以上6-10是都为了兼容某些情况下的名称和规范的适配问题。

比如：资源转换示例.xlsx的kind表中，0CostType这一项

1. KeyWordRegex的补充配置会过滤掉前缀0（不匹配[a-zA-Z_\$]），即CostType
2. KeyWordRegex的主配置会在大写字母处做分词，次配置指明保留字母，即分割成Cost Type
3. KeyCase指明转换成小写，即cost type
4. KeyWordSplit指明分词符号是*_*，即cost_type
5. 最后匹配到*kind.proto*中*role_cfg*的*cost_type*字段.

如果不需要字段名转换，直接把KeyWordRegex的配置全部配成空即可。（注意这样大小写转换也不会执行）

输出格式说明
======
|          输出格式参数       |                                  输出格式说明                                                                                       |           说明                                    |
|-----------------------------|-------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------|
|           bin               | 基于协议的二进制文件,不同的协议类型(-p参数)输出的二进制不一样,一般是header+body,body中有转出的数据列表, 协议格式见header文件夹      | 示例见 [sample](sample/role_cfg.bin) (protobuf)                |
|           lua               | 转出为格式化的lua文件(易读), 一般格式为 return {\[1\] = 转表头信息, \[协议结构名称\] = {数据列表} }                                 | 示例见[sample](sample/role_cfg.lua)          |
|          msgpack            | 转出为使用[MsgPack](http://msgpack.org/)打包的二进制文件,内含的第一个message是转表头信息，后面紧跟数据，可以用任何支持得客户端解包  | 示例见[sample](sample/role_cfg.msgpack.bin)  |
|           json              | 转出为json文件,一般格式为 \[ {转表头信息}, {协议结构名称 : \[ 转出的数据 \] } \]                                             | 示例见[sample](sample/role_cfg.json)         |
|           xml               | 转出为xml文件,一般格式为&lt;root&gt;&lt;header&gt;转表头信息&lt;/header&gt;&lt;body&gt;&lt;协议结构名称&gt;数据内容&lt;/协议结构名称&gt;&lt;/body&gt;&lt;/root&gt; | 示例见[sample](sample/role_cfg.xml)  |


**注意：** Xml输出格式中，列表元素的结构是*&lt;配置名称 for="0"&gt;数据1&lt;/配置名称&gt;&lt;配置名称 for="1"&gt;数据2...&lt;/配置名称&gt;* 属性字段for表示数组索引，目的是方便通过xpath查找。

关于加载导出的数据
======

转出的数据都采用header+data_block的形式。本工具并不规定怎么读取转表导出的数据，开发者可以按照转出的数据规则自由操作。

> 建议项目中使用导出的协议二进制或者msgpack。协议二进制可以用任意语言加载protobuf或者其他类似工具（如：[pbc](https://github.com/cloudwu/pbc)）加载。
> 
> 而其他导出类型可以用于一些外部工具的集成，比如基于Web的GM工具，基于Lua的远程调试工具等等。

但是为了使用方便，在[loader-binding](loader-binding)里提供了几种基本的读表方式。

protobuf协议
------
+ protobuf的协议数据解包主要流程:
> 1. 二进制文件使用[header/pb_header.proto](header/pb_header.proto)中的*xresloader_datablocks*结构解包
> 2. xresloader_datablocks.data_block内保存的二进制使用指定的配置协议解包
> 3. xresloader_datablocks.data_block的个数就是Excel转出的数据行数，注意包含空行

+ 如果你使用官方的protobuf或protobuf-lite，可以使用[loader-binding/cxx](loader-binding/cxx)来加载配置
> sample 参见: [sample/cxx/read_kind_sample.cpp](sample/cxx/read_kind_sample.cpp)

+ 如果你使用云风的[pbc](https://github.com/cloudwu/pbc)，可以使用[loader-binding/pbc](loader-binding/pbc)来加载配置
> 这个加载器会依赖 [https://github.com/owent-utils/lua/tree/master/src](https://github.com/owent-utils/lua/tree/master/src) 里的部分内容。
> 
> 需要使用pbc先加载[header/pb_header.pb](header/pb_header.pb)文件
> 
> **proto v3请注意: pbc不支持[packed=true]属性。在proto v3中，所有的*repeated*整数都默认是[packed=true]，要使用pbc解码请注意这些field要显示申明为[packed=false]**
> 
> pbc_config_manager:load_buffer_kv(协议名, 二进制, function(序号, 转出的lua table) return key的值 end) -- 读取key-value型数据接口
> 
> pbc_config_manager:load_buffer_kl(协议名, 二进制, function(序号, 转出的lua table) return key的值 end) -- 读取key-list型数据接口

其他输出格式
------

> **注意：所有导出非二进制的数据都是不带包名的（package），但是使用-c --const-print选项导出协议常量除外。因为数据加载一般有manager统一管理，而协议常量一般直接用于代码中。**
> 
> 导出的常量都很简单易懂，直接看生成的文件很容易理解，这里不再额外作说明了。

1. **lua**格式输出可以按[loader-binding/lua](loader-binding/lua)的说明读取。
> 这个加载器会依赖 [https://github.com/owent-utils/lua/tree/master/src](https://github.com/owent-utils/lua/tree/master/src) 里的部分内容。
> 
> conf_manager:load_kv(require的路径, function(序号, 转出的lua table) return key的值 end) -- 读取key-value型数据接口
> 
> conf_manager:load_kl(require的路径, function(序号, 转出的lua table) return key的值 end) -- 读取key-list型数据接口

2. [MsgPack](http://msgpack.org/)的读取的语言和工具很多，任意工具都能比较简单地读出数据，[loader-binding/msgpack](loader-binding/msgpack)里有一些读取示例(Python和Node.js)
3. **Json**的读取的语言和工具很多，任意工具都能比较简单地读出数据，故而不再提供读取工具
4. **Xml**的读取的语言和工具很多，任意工具都能比较简单地读出数据，故而不再提供读取工具
5. **Javascript**的读取的语言和工具很多，任意工具都能比较简单地读出数据，[loader-binding/javascript](loader-binding/javascript)里有相关说明

高级功能
======
验证器
------
Excel里的Key使用@后缀的字段名，@后面的部分都属于验证器。如果一个字段使用了验证器，那么可以直接填写验证器类型的内部名字或者ID

详见 [sample/资源转换示例.xlsx](sample/资源转换示例.xlsx) 的upgrade_10001和upgrade_10002表

使用注意事项
======
1. Excel里编辑过的单元格即便删除了也会留下不可见的样式配置，这时候会导致转出的数据有空行。可以通过在Excel里删除行解决
2. Excel里的日期时间类型转成协议里整数时会转为Unix时间戳，但是Excel的时间是以1900年1月0号为基准的，这意味着如果时间格式是hh:mm:dd的话，49:30:01会被转为1900-1-2 1:31:01。时间戳会是一个很大的负数
3. 介于上一条，不建议在Excel中使用时间类型
