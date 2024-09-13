# 更新记录

## Unrelease

1. 输出的UE代码的默认值也输出到 USTRUCT 申明中。

## 2.19.0

1. 优化整数类型的验证，不允许浮点数转整数。
2. 增加 `org.xresloader.field_tag=<Tag>` 和 `org.xresloader.oneof_tag=<Tag>` 插件，并允许通过 `--ignore-field-tags` 忽略部分数据。
3. 更新依赖库
   + `maven-surefire-plugin` -> 3.5.0
   + `maven-shade-plugin` -> 3.6.0
   + `commons-cli` -> 1.9.0
   + `commons-codec` -> 1.17.1
   + `log4j` -> 2.24.0
   + `com.google.protobuf` -> 4.28.0
   + `org.apache.poi` -> 5.3.0

## 2.18.2

1. 修复 2.18.0-2.18.1 版本中映射可选字段的错误。

## 2.18.1

1. 修复 `%` 后缀和带验证器时Excel对大数字自动附加的 `,` 的适配。

## 2.18.0

1. 增加插件 `org.xresloader.field_list_strip_option=LIST_STRIP_DEFAULT|LIST_STRIP_NOTHING|LIST_STRIP_TAIL|LIST_STRIP_ALL` 用于给单个字段设置数组裁剪。
2. 增加插件 `org.xresloader.field_list_min_size="<N>|枚举名"` 用于给单个字段数组最小长度要求。
3. 增加插件 `org.xresloader.field_list_max_size="<N>|枚举名"` 用于给单个字段数组最大长度要求。
4. 增加插件 `org.xresloader.field_list_strict_size=<true|false>` 用于设置单个字段数组的严格长度要求，即不自动补全最小长度，而是报错。
5. 增加选项 `--disable-alias-mapping` 用于关闭字段映射时使用别名。
6. 修复使用别名时，message结构未被正确映射的问题。
7. 修复对Excel对大数字自动附加的 `,` 的适配。
8. 修复对非裁剪模式下的数组数据自动补全问题。

### BREAKCHANGES

+ 在此版本后，默认启用 `--enable-alias-mapping` 开启字段别名映射。如果出现别名重名出现冲突，请使用 `--disable-alias-mapping` 还原之前的默认行为。

## 2.17.1

1. 修复数组别名丢失字段映射的问题。
2. 修复文本模式输出跨分组转出数据没清理干净的问题。

## 2.17.0

1. 增加 `--disable-data-validator` 允许跳过数据验证。
2. 增加正则表达式验证器 `Regex("正则表达式")` 。

## 2.16.1

1. 修复字段别名的一处错误
2. 更新依赖库
   + `com.google.protobuf` -> 4.27.2
   + `org.apache.poi` -> 5.3.0

## 2.16.0

1. 增加int32和uint32的范围检测。
2. JSON和Javascript类型输出时，超过 `2^53` 和低于 `-2^53` 的值转为字符串类型表示。
3. 修复无效的空行忽略提示(空通过Scheme `JsonCfg-LargeNumberAsString=true/false` 来控制)。
4. 单例增加同步锁和ThreadLocal，预埋下个版本可能会增加内置的多线程并发。
5. 增加 `org.xresloader.field_allow_missing_in_plain_mode` 和 `org.xresloader.oneof_allow_missing_in_plain_mode` 以允许Plain模式下部分字段可选
6. 更新依赖库
   + `jacoco-maven-plugin` -> 0.8.12
   + `maven-compiler-plugin` -> 3.13.0
   + `maven-shade-plugin` -> 3.5.3
   + `build-helper-maven-plugin` -> 3.6.0
   + `commons-cli` -> 1.8.0
   + `commons-codec` -> 1.17.0
   + `log4j` -> 2.23.1
   + `org.json` -> 20240303
   + `com.google.protobuf` -> 4.27.0
   + `commons-csv` -> 1.11.0

## 2.15.1

1. 修复转出bin类型数据时，所有字段都是默认值（转出的数据零字节）时，此行会被裁减掉的BUG。
2. 修复Excel文件压缩率过高时无法打开文件的问题。
3. 更新依赖库
  + `maven-compiler-plugin` -> 3.12.1
  + `maven-surefire-plugin` -> 3.2.5
  + `build-helper-maven-plugin` -> 3.5.0
  + `commons-codec` -> 1.16.1
  + `log4j` -> 2.22.1
  + `msgpack-core` -> 0.9.8
  + `org.json` ->20240205
  + `com.google.protobuf` -> 3.25.3
  + `xmlsec` -> 4.0.1
  + `org.apache.poi` -> 5.2.5
  + `junit-jupiter-api` -> 5.10.2

## 2.15.0

1. 修订 `DataVerifyInTableColumn` 验证器的文件错误输出。
2. 新增 `org.xresloader.map_key_validator` 和 `org.xresloader.map_value_validator` 插件用于设置map类型的内部字段验证器。
3. 更新依赖库
  + `org.jacoco.jacoco-maven-plugin` -> 0.8.11
  + `maven-surefire-plugin` -> 3.2.2
  + `maven-shade-plugin` -> 3.5.1
  + `commons-cli` -> 1.6.0
  + `log4j` -> 2.22.0
  + `msgpack-core` -> 0.9.6
  + `org.json` -> 20231013
  + `com.google.protobuf` -> 3.25.1
  + `org.apache.santuario` -> 4.0.0
  + `org.apache.poi` -> 5.2.4
  + `org.snakeyaml` -> 2.7
  + `org.junit.jupiter` -> 5.10.1

## 2.14.1

1. 增加输出每个数据源的数据个数
2. 修订数据源错误时的返回码
3. 增加连续空行检测（ `--tolerate-max-empty-rows` 可用于设置连续空行检测数量。 ）

## 2.14.0

1. 修订数值类型数据错误导致的验证器Panic问题
2. 从Excel读取浮点数支持 `%` ，读取整数支持 `,` 分隔符。
3. 修订列名输出
4. 包含 2.14.0-rc1 到 2.14.0-rc4 版本的所有内容

## 2.14.0-rc4

1. 修订老Scheme配置模式的输出格式
2. 优化调整内部Validator变量名
3. `InTableColumn()` 和 `InText()` 验证器按需加载文件内容
4. 验证器加载失败现在也返回错误
5. 自定义验证规则改为首次使用时采取检测循环依赖
6. 优化公式错误的输出消息
7. 修订唯一性验证器显示的冲突行号少了1的问题。
8. 自定义验证器增加 `description` 配置，用以支持自定义描述输出。
9. 优化输出的列名
10. 使用LRU算法来缓存缓存流式索引数据源表，增加 `--data-source-lru-cache-rows` 来控制LRU缓存大小。

## 2.14.0-rc3

1. 插件重命名 `verifier` -> `validator`
2. 增加函数验证器: `InText("文件名"[, 第几个字段[, \"字段分隔正则表达式\"]])` : 从文本文件（UTF-8编码）,可以指定读第几个字段和用于字段分隔的正则表达式
3. 增加函数验证器: `InTableColumn("文件名", "Sheet名", 从第几行开始, 从第几列开始)` : 从Excel数据列读取可用值,指定数据行和数据列
4. 增加函数验证器: `InTableColumn("文件名", "Sheet名", 从第几行开始, KeyRow, KeyValue)` : 从Excel数据列读取可用值,指定数据行并通过某一行的的值获取数据列
5. 增加选项 `--validator-rules` 用于指定自定义验证器组合
6. 增加自定义索引器缓存，缓存30000行以下的表格，加快读取速度

## 2.14.0-rc2

1. `org.xresloader.field_alias` 和 `org.xresloader.enum_alias` 允许多个别名
2. 修订repeated字段映射使用别名的问题
3. 增加 `org.xresloader.field_not_null` 插件和 `org.xresloader.oneof_not_null` 用以忽略Excel中指定数据为空的数据行
4. 增加 `org.xresloader.field_unique_tag` 插件用以支持唯一性检测
5. `-f` 支持传入多个pb文件

## 2.14.0-rc1

1. 增加 `org.xresloader.ue.ue_origin_type_name` 来设置输出UE代码的原始类型
2. 增加 `org.xresloader.ue.ue_origin_type_default_value` 来设置输出UE代码的原始类型的默认值
3. 修复打包时 `Discovered module-info.class. Shading will break its strong encapsulation.` 的告警
4. 允许在Excel同一列里配置多个字段，由转表工具自动复制
5. 范围验证器支持 `>数字`, `>=数字`, `<数字`, `<=数字` , 并且现在支持浮点数。
6. 修复一处UE-Csv输出格式问题
7. 更新 `protobuf` 到 3.23.3
8. 更新 `org.json` 到 20230618
9. 更新 `commons-codec` 到 1.16.0
10. 更新 `build-helper-maven-plugin` 到 3.4.0
11. 更新 `maven-shade-plugin` 到 3.4.0
12. 更新 `maven-surefire-plugin` 到 3.1.2

## 2.13.1

1. 修订UE的FObjectFinder只能用于构造函数的问题。
   + 增加 `UeCfg-EnableDefaultLoader` 选项控制默认的Loader是否开启
   + 增加 `org.xresloader.ue.default_loader=EN_LOADER_MODE_DEFAULT|EN_LOADER_MODE_ENABLE|EN_LOADER_MODE_DISABLE` 选项控制单独的Message是否开启默认Loader
2. 增加UE输出代码的额外包含头文件选项
   + 增加 `UeCfg-IncludeHeader` 选项，对所有输出的UE代码额外包含头文件
   + 增加 `org.xresloader.ue.include_header` 插件用于对于特定的Message额外附加包含文件
3. 修复Lua输出 `</` 时追加了冗余的 `\` 导致转移错误的问题。
4. 修复一处在Plain模式中oneof内使用enum类型时，无法导出输出的问题。
5. 更新 `org.jacoco.jacoco-maven-plugin` 到 0.8.10
6. 更新 `org.apache.maven.plugins.maven-compiler-plugin` 到 3.11.0
7. 更新 `maven-surefire-plugin` 到 3.1.0
8. 更新 `org.apache.maven.plugins.maven-shade-plugin` 到 3.4.1
9. 更新 `log4j` 到 2.20.0
10. 更新 `protobuf` 到 3.23.0
11. 更新 `org.apache.pdfbox` 到 2.0.28
12. 更新 `org.apache.santuario.xmlsec` 到 3.0.2
13. 更新 `org.dom4j` 到 2.1.4
14. 更新 `org.apache.commons.commons-csv` 到 1.10.0
15. 更新 `junit` 到 5.9.3

## 2.13.0

1. 修复Excel文件过大时的报错和提示
2. 增加Javascript Patch数据的功能(感谢 [@superwenda](https://github.com/superwenda) )
3. 允许多个数据源查找目录
4. 添加benchmark参考

## 2.12.1

1. 更新 `protobuf` 到 3.21.9
2. Json和UE-Json格式的输出保证有序
3. 修复UE-Csv格式输出map类型时乱序的问题

## 2.12.0

1. 特殊处理 `Timestamp` 和 `Duration` 类型的字符串转换
2. 移除 `UeCfg-RecursiveMode` 选项
3. 重构UE输出的数据生成模块，简化维护复杂度
4. 修复lua输出UTF-8字符转义错误的问题
5. 更新 `protobuf` 到 3.21.8
6. 更新 `org.apache.poi` 到 5.2.3
7. 更新 `org.apache.pdfbox` 到 2.0.27
8. 更新 `org.apache.santuario` 到 3.0.1
9. 更新 `org.json` 到 20220924
10. 更新 `org.apache.logging.log4j` 到 2.19.0
11. 更新 `maven-shade-plugin` 到 3.4.0

## 2.11.0-RC3

1. 更新 `protobuf` 到 3.21.5
2. 更新 `org.apache.logging.log4j` 到 2.18.0
3. 更新 `org.apache.poi` 到 5.2.2
4. 更新 `org.apache.santuario` 到 3.0.0
5. 更新 `org.json` 到 20220320
6. 更新 `commons-cli` 到 1.5.0
7. 更新 `jacoco-maven-plugin` 到 0.8.8
8. 更新 `maven-compiler-plugin` 到 3.10.1
9. 更新 `maven-shade-plugin` 到 3.3.0
10. 更新 `build-helper-maven-plugin` 到 3.3.0
11. 更新 `org.msgpack.msgpack-core` 到 0.9.3
12. 更新 `org.apache.pdfbox` 到 2.0.26
13. 抽离出协议为单独的子模块
14. Java运行时要求升级到11
15. 调整Example内UE输出代码路径。
16. 修订UE-Csv模式输出时，无法转出 plain 模式的repeated message字段的BUG
17. 裁剪数组尾部空元素 `--list-strip-empty-tail` ，对 `Ue-Csv` 模式输出可用了
18. 默认使用 `--disable-excel-formular` 关闭公式实时计算，使用更高效的索引器
19. 增加 `--enable-string-macro` 用于让Macro(文本替换)，对字符串类型生效。或使用 `--disable-string-macro` 让Macro(文本替换)，对字符串类型不生效。

  > 默认不生效，批量转表工具中可以通过全局开启 `--enable-string-macro` ，特定表使用 `--disable-string-macro` 来实现默认开启字符串文本替换，特定表不替换

### 更新迁移指引

由于变更了默认的索引器，导致对Excel一些内置的数据类型处理和先前有一些差异。比如对于日期时间类型、百分率等。
现在会先转出原始的文本，再根据protocol的目标类型做转换。如果需要回退到老的POI索引，可以使用 `--enable-excel-formular` 选项切换到老的索引器。

## 2.11.0-RC2

1. 文本类型输出也增加Hash Code（和二进制输出的计算规则不同）
2. 更新 `org.apache.logging.log4j` 到 2.14.1
3. 更新 `org.msgpack.msgpack-core` 到 0.9.0
4. 更新 `org.json` 到 20210307
5. 更新 `com.google.protobuf.protobuf-java` 到 3.18.0
6. 升级 `org.apache.xmlbeans` 到 5.0.0
7. 更新 `commons-csv` 到 1.9.0
8. 修复内置索引引擎为设置数据源目录的BUG(Thanks to [vividkings](https://github.com/vividkings))
9. 增加 `-r/--descriptor-print` 选项，用于导出所有描述数据
10. 修复 Plain 模式二进制输出enum数组的BUG

## 2.11.0-RC1

1. 修复保留空list处理64位整数不正确的问题 [\#5](https://github.com/xresloader/xresloader/issues/5)
2. `--disable-empty-list` 和 `--enable-empty-list` 改为不推荐使用。新增 `--list-keep-empty` , `--list-strip-all-empty` 和 `--list-strip-empty-tail` 。
3. 新增 `--list-strip-empty-tail` 模式仅移List除尾部的空元素。
4. 修订文本输出的排序规则，仅和Key有关，和Value无关。
5. Plain模式的message数组允许指定下标。
6. 优化公式存在不支持函数时的输出。

## 2.10.3

1. 修复Excel文件压缩率过高时无法打开文件的问题。

## 2.10.2

1. 修复 `Ue-Csv` 和 `Ue-Json` 输出时，结构名字和输出资源文件名不一致时生成的路径错误
2. 生成的UE的加载代码允许自己指定Loader

## 2.10.1

1. 修复在 `Ue-Csv` 和 `Ue-Json` 输出时， Excel存在空的合成Key单元格时会crash的BUG。
2. 升级 `org.apache.poi` 到 5.0.0
3. 升级 `org.apache.xmlbeans` 到 4.0.0

## 2.10.0

1. 增加 `--enable-alias-mapping` 选项，用于在映射Excel列到目标数据结构时，开启别名匹配。
2. protobuf的枚举类型解析支持message内嵌enum。
3. 对大文件处理每5000行打印一次进度日志。
4. \[实验性\] 使用 `--disable-excel-formular` 关闭公式后，将采用流式读取机制索引数据，以降低内存开销。同时会关闭日期格式的探测。
5. 优化公式错误时的处理流程，现在公式错误不会中断执行仅会打印出Warning。
6. 更新 `org.codehaus.mojo.build-helper-maven-plugin` 到 3.2.0
7. 更新 `commons-codec` 到 1.15
8. 更新 `org.msgpack.msgpack-core` 到 0.8.22
9. 更新 `com.google.protobuf.protobuf-java` 到 3.13.0
10. 更新 `org.apache.logging.log4j` 到 2.14.0
11. 更新 `org.json` 到 20201115
12. 更新 `com.google.protobuf` 到 3.14.0

## 2.10.0-rc2

1. 补充部分遗漏的大文件的进度日志
2. 修复大文件日志的异常问题

## 2.10.0-rc1

1. 增加 `--enable-alias-mapping` 选项，用于在映射Excel列到目标数据结构时，开启别名匹配。
2. protobuf的枚举类型解析支持message内嵌enum。
3. 对大文件处理每5000行打印一次进度日志。
4. 更新 `maven-shade-plugin.maven-shade-plugin` 到 3.2.4
5. 更新 `org.codehaus.mojo.build-helper-maven-plugin` 到 3.2.0
6. 更新 `commons-codec` 到 1.15
7. 更新 `org.msgpack.msgpack-core` 到 0.8.21
8. 更新 `com.google.protobuf.protobuf-java` 到 3.13.0
9. \[实验性\] 使用 `--disable-excel-formular` 关闭公式后，将采用流式读取机制索引数据，以降低内存开销。同时会关闭日期格式的探测。

## 2.9.3

1. 增加 `UeCfg-CsvObjectWrapper` 选项，接受两个参数用于指定 `Ue-Csv` 模式输出时，map和array的包裹字符
2. 优化命令行模式的参数解析

## 2.9.2

1. 支持自定义的验证器指向oneof
2. 修复在Excel验证器中配置完整路径时索引不到验证器的BUG

## 2.9.1

1. header里（不包含UE输出）增加 `data_source` 来方便各类工具在读取失败时提示数据来源。
2. 修正自定义插件重名时的反射数据导出问题，如果发现名字冲突，插件的key采用full name
3. 修复大整数可能丢失精度的BUG

## 2.9.0

1. 允许输入的pb文件不用打包插件的proto( `xresloader.proto` , `xresloader_ue.proto` )和protobuf官方的proto文件(如: `google/protobuf/duration.proto`, `google/protobuf/descriptor.proto` 等)
2. 增加 `--ignore-unknown-dependency` 选项用于忽略未知的输入协议的依赖项
3. 优化版本号选项 `--data-version` ，在批处理模式设置位默认版本号，对所有批处理项生效，在普通模式设置版本号，仅对当前转表项生效
4. 重构，重命名 `DataDstMessageDescriptor` -> `DataDstTypeDescriptor` 。
5. 增加对map类型的支持,map的key必须是整数或字符串，value可以是任意类型。在 **UE-Json** 和 **UE-Csv** 输出中，会使用 `TMap<KEY类型, VALUE类型>` 来输出代码。

## 2.8.0

1. 优化一些issue的上报提示
2. 增加plain模式的 `oneof` 支持，输入为 `[类型名或ID或别名][分隔符][具体内容的plain模式数据]` , 比如: `item|1001,123` 。不支持 **UE-Csv** 输出的非嵌套模式（ `UeCfg-RecursiveMode=False` ）
3. 增加 `oneof` 的sample
4. 修复 `enum` 字段类型输出二进制时的值类型错误
5. 优化 `oneof` 字段的配置冲突检测
6. 标记 **UE-Csv** 输出的非嵌套模式（ `UeCfg-RecursiveMode=False` ） 为不推荐的，以后将被移除。
7. \[实验性\] **UE-Json** 和 **UE-Csv** 输出的蓝图代码中，增加指示oneof分支的字段，便于对 `oneof` 输出的分支判断和反射使用
8. 优化一系列错误提示流程
9. 增加一个python工具，可以打印转出的protobuf二进制数据（需要 `pip/python3 -m pip install protobuf [--user]`）

## 2.7.3

1. 修复 **UE-Csv** 格式输出的一处空数据的崩溃BUG
2. 增加BUG上报说明
3. 修复启用多文件合并功能时，**UE-Csv** 格式输出会输出多次Header的BUG
4. sample输出增加递归模式的 **UE-Csv**
5. \[实验性\] **UE-Csv** 格式输出的现在也支持plain模式了（不包含非嵌套模式（ `UeCfg-RecursiveMode=False` ））

## 2.7.2

1. 修复一处枚举类型验证器复用丢失的问题
2. 增加插件 `org.xresloader.field_required` ， 用于向proto3提供，proto2的 **required** 约束
3. 升级protobuf到3.12.1
4. 升级log4j到2.13.3
5. 升级org.json到20200518

## 2.7.1

1. 常规更新
2. 升级protobuf到3.11.4
3. 升级maven-shade-plugin到3.2.3
4. 升级build-helper-maven-plugin到3.1.0
5. 升级commons-codec到1.14
6. 升级log4j到2.13.2
7. 升级msgpack-core到0.8.20
8. 升级poi到4.1.2
9. 升级commons-csv到1.8
10. 修复导出文件选项可能爆栈失败的问题
11. 调整协议描述输出的枚举数据，同时输出name和number
12. 更新图标

## 2.7.0

1. 修复一处协议配置错误没有打印具体原因的问题
2. 添加 `--require-mapping-all` 选项用于检查message中所有字段都必须被配置映射关系，用于检查配置遗漏
3. 增加protobuf插件 - `org.xresloader.msg_require_mapping_all` 可以设置某个message的所有字段必须被全部映射，用于检查配置遗漏
4. 协议里直接配置 `enum` 类型也支持默认增加该类型的验证器 
5. 大量优化内部数据结构，更好地分离映射关系和AST描述
6. 增加实验性功能Plain模式，允许把message所有字段或动态长度的数组配置在一个单元格内（UE-CSV模式暂不支持）
7. 修复输出的UE导入文件的没有自动删除老的导入条目的问题
8. UE-CSV 和 UE-Json 模式输出也按照field定义顺序输出(即: 先Key再Value，然后二级排序按field的定义number)

## 2.6.1

1. 对 `-i/--option-print <文件名>` 和 `-c/--const-print <文件名>` 的字段输出排序，有利于diff
2. 更新 protobuf 到 3.10.0
3. 使用 [org.msgpack.msgpack-core](https://mvnrepository.com/artifact/org.msgpack/msgpack-core) 替换 [org.msgpack.msgpack](https://mvnrepository.com/artifact/org.msgpack/msgpack)，保持和 lua/javascript/xml等一样的输出顺序。
4. msgpack 的输出结构变化（详见: https://github.com/xresloader/xresloader/tree/master/loader-binding/msgpack ）

## 2.6.0

1. 更新log4j到2.12.1
2. 更新org.json到20190722
3. 更新protobuf到3.9.2
4. 更新poi到4.1.1
5. hash算法更替为SHA-256
6. 协议外层包体增加 `data_message_type` 用于方便使用者通过反射机制查找 `data_block` 对应的数据结构
7. 增加 `-i/--option-print <文件名>` 用于输出协议配置中的选项信息
8. 对内部的 `Map<string, int32>` 类型输出排序，有利于 `-c/--const-print <文件名>` 时有序输出枚举值
9. 移除 `-c/--const-print <文件名>` 时导出的 `google.protobuf` 的内置枚举类型值。

## 2.5.0

1. 增加protobuf插件 - org.xresloader.field_alias 可以设置字段别名并用于配置了验证器的excel数据中
2. 增加protobuf插件 - org.xresloader.enum_alias 可以设置枚举项目的别名并用于配置了验证器的excel数据中
3. 增加protobuf插件 - org.xresloader.field_ratio 可以设置字段放大倍数值，可用于需要转出整数类型的百分率/千分率/万分率，但excel中保留小数表达

## 2.4.0

1. 常量导出现在会导出protobuf的message里包含oneof了(使用C++的命名规则 `k大写驼峰名字` )
2. 支持解析protobuf的内嵌message

## 2.3.1

1. 数字类型转字符串，使用 `%g` 格式（去除不必要的小数点和0）
2. 更新 maven-compiler-plugin 到 3.8.1
3. 更新 commons-codec 到 1.13
4. 更新 log4j 到 2.12.0
5. 更新 protobuf 到 3.9.1
6. 更新 org.apache.poi 到 4.1.0
7. 更新 commons-csv 到 1.7
8. 修订powershell的sample生成脚本

## 2.3.0

1. 优化sample输出脚本，先移除过期的 `UnreaImportSettings.json` 文件。
2. 增加 `UeCfg-RecursiveMode=true/false` 的 **SchemeMeta** 用以控制是否启用嵌套模式，默认统一为开启嵌套模式。
3. 增加 `UeCfg-DestinationPath=<PATH>` 的 **SchemeMeta** 用以控制输出的uassert目录。
4. 重新开启 `--disable-empty-list` 和 `--enable-empty-list` 的功能。

## 2.2.0

1. 增加protobuf插件 - org.xresloader.ue.ue_type_is_class (用于UE代码输出时直接输出UE支持的 `TSoftClassPtr<T>` 来替换 `TSoftObjectPtr<T>` 类型)
2. 增加和优化xresloader内部的Message描述抽象
3. 支持UE-JSON的嵌套模式导出
4. 自动创建输出目录
5. 不再自动猜测Name字段，必须指定key_tag或者显式命名Name字段。（用于区分嵌套子类型和转表类型）
6. 优化统一插件命名
7. 增加protobuf插件 - org.xresloader.ue.not_data_table (用于UE代码输出时显式指定不输出转表数据加载代码)

## 2.1.1

1. 增加UE输出的Clear接口，用于把所有的数据清空
2. 增加UE输出的GetRowName接口，用于方便C++接口调用
3. 修复UE接口返回的IsValid参数错误的问题

## 2.1.0

1. 对protobuf的proto 3增加protobuf的扩展插件支持
2. 增加protobuf插件 - org.xresloader.verifier (支持和Excel里一样的验证器语法)
3. 增加protobuf插件 - org.xresloader.msg_description
4. 增加protobuf插件 - org.xresloader.field_description
5. 增加protobuf插件 - org.xresloader.ue.key_tag (用于UE代码输出时输出Key -> Name 的函数)
6. 增加protobuf插件 - org.xresloader.ue.ue_type_name (用于UE代码输出时直接输出UE支持的 `TSoftObjectPtr<T>` 类型)
7. 增加protobuf插件 - org.xresloader.ue.helper (用于UE代码输出时的Utility类的类名后缀)
8. sample里增加一个协议里的verifier示例

## 2.0.1

1. 修复UE代码输出中，多输出了Name字段的问题
2. 修复int32类型转出UE代码时，变成了int64的问题
3. 输出的类名包含包名前缀

## 2.0.0

1. 包名由 com.owent.xresloader 改为 org.xresloader.core
2. 协议包名由 com.owent.xresloader.pb 改为 org.xresloader.pb
3. 初步支持 UnrealEngine 4 的数据表和代码导出
4. 更新protobuf到 3.7.1
5. 更新maven-compiler-plugin到3.8.0
6. 更新maven-shade-plugin到3.2.1
7. 更新commons-codec到1.12
8. 更新log4j到2.11.2
9. 使用org.msgpack.msgpack-core代替org.msgpack.msgpack
10. 更新org.json到20180813
11. 更新poi到4.0.1
12. 修复一些lint，迁移到 POI 4.X.X 的API

## 1.4.3

1. javascript全局导出支持多层namespace
2. 修订sample脚本问题导致的node.js转出选项不正确的问题
3. 优化执行转表时创建对象的数量，使用共享的输出缓存，不再依赖运行时java反射
4. 补充sample的新的示例的README

## 1.4.2

1. 增加一些错误提示信息的行提示和表名提示，用以辅助排查配置错误
2. org.json更新到20180130
3. com.google.protobuf更新到3.5.1
4. Lua常量输出支持module("模块名", package.seeall)的模式

## 1.4.1

1. 支持使用"或"符号|分隔的多个验证器
2. 支持整数范围验证器
3. 更新protobuf到3.5.0
4. 更新log4j到2.10.0
5. 修复部分类型没有使用公式的BUG

## 1.4.0

1. 修改版本号规则为3位，和maven内大多数库的结构一致，主版本号.功能版本号.修订版本号
2. 增加-a --data-version用于允许用户设置数据内数据版本号，方便业务做多版本兼容
3. 更新commons-codec到1.11
4. 更新log4j到2.9.1
5. 更新org.json到20171018

1.## 3.3.2

1. 更新POI到3.17
2. 更新protobuf到3.4.0
3. 更新json到20170506
4. 更新log4j到2.9.0
5. 更新maven-shade-plugin到3.1.0
6. 更新maven-compiler-plugin到3.7.0
7. 统一使用“sheet”作为输出的“表”的称谓

1.## 3.3.1

1. 修复lua和javascript无pretty模式输出的首行注释问题
2. 修复验证器误报error日志的问题

1.## 3.3.0

1. 更新log4j到2.8.2
2. 修复1.3.2.0版本中，使用命令行模式时，同一个表会被转多次的BUG
3. 使用maven-compiler-plugin来指定java版本
4. 发现jvm在Windows平台下，默认编码为UTF-8时的BUG，加一个FAQ
5. 移除一批对poi的deprecated的API的使用，兼容至poi 4.1版本。poi 4.2版本后API又要改名，到时候再换一次
6. 现在对1970年之前的时间，都视为日期无效，只取时分秒

1.## 3.2.0

1. 更新build-helper-maven-plugin到3.0.0
2. 更新maven-shade-plugin到3.0.0
3. 更新commons-cli到1.4
4. 更新log4j到2.8.1
5. 更新protobuf到3.2.0
6. 优化命令行模式的信息输出
7. 修复scheme表读取失败时返回码未增加失败计数的问题

1.## 3.1.0

1. 允许多个package（命名空间）,当缩写类型名称存在于多个命名空间时，必须使用完整类型名

1.## 3.0.2

1. 修复1.3.0.1中仅二进制输出会导出默认required字段的问题
2. 修正移除转表失败是的错误提示信息

1.## 3.0.1

1. 修复1.3.0.0不兼容以前版本对proto v2 的required字段生成默认值的行为
2. 重复的元表增加warning

1.## 3.0.0

1. 支持差异化合表(不再要求合表的Key的那一行必须一致)
2. 增加验证器功能,允许使用验证器校验数据和转换数据
3. json配置和conf元表配置也支持合表
4. 字符串类型不再走元表替换(没有实际意义)
5. 允许通过外部参数设置描述信息(Meta)
6. 大规模优化数据流，减少转表开销
7. sample里生成pb文件时也会调用tools里的脚本生成header的pb和cpp文件（现在c++的header增加了版本号检查，需要不低于当前所用的protobuf版本）

1.## 1.0.0

1. 更新build-helper-maven-plugin到1.12
2. 更新log4j到2.6.2
3. 更新ini4j到20160810
4. 更新支持protobuf 3.0
5. sample和loader-binding自动适应proto v2和proto v3
6. 同步更新sample代码，同时支持proto v2和proto v3

1.## 0.6.1

1. 修复excel里配置成list而协议不是repeated时的错误提示
2. 使用[log4j2](http://logging.apache.org/log4j/2.x/)来输出日志
3. 控制台输出颜色支持

1.## 0.6.0

1. javascript的写入全局可以设置导出的到统一名字空间(lua和其他输出很容易在上层做包装，但是js不行，所以js特别处理一下)
2. 修正数组嵌套数组的结构转换丢失的BUG
3. 补充msgpack的读取说明，补充node.js的读取示例代码
4. 修正一些导出头字段名错误的问题
5. 修正XML输出数组类型数据会多一层的BUG
6. XML输出数组将会带数据索引号
7. 统一版本号管理，从POM文件中自动生成
8. 更新sample数据

1.## 0.5.0

1. 增加javascript的导出支持

1.## 0.4.0

1. 返回码改为失败的数量，无失败则返回0（用以和jre执行崩溃的返回码是1相统一）
2. 更新build-helper-maven-plugin到1.10
3. 更新maven-shade-plugin到2.4.3
4. 更新json到20160212
5. 优化转换错误的一些错误提示

1.## 0.3.0

1. 依赖库更新,全部更新到目前为止的最新稳定版
2. 增加导出enum名称集的功能
3. 修正没有元信息表时，公式选项被忽略的BUG
4. 修正Shceme数据未覆盖时会使用上一次的配置而不是默认值的BUG
5. xml输出允许指定根节点Tag Name

1.## 0.2.1

1. 允许把枚举类型提取到外部
2. 优化protobuf的读取逻辑，对于相同的描述文件不再反复转换
3. 整理部分字符串拼接代码
4. macro数据缓存（几乎一个项目的macro表只有一个，但是要转换的表有几十个，所以对macro可以做特殊的预处理）

1.## 0.2.0

1. 增加通过std输入转换列表的功能，提升转换速度
2. 更换gnu.getopt为commons-cli，优化参数转换代码
