xresloader示例文件说明:
======

文件名               |  描述                                        |  参考命令 
--------------------|----------------------------------------------|----------
*.proto             |  protobuf格式的协议描述文件                     | 需要根据实际使用手动编辑
kind.pb             |  protobuf工具的打包proto文件的输出结果           | protoc -o kind.pb *.proto 或 gen_protocol.py
*.xlsx              |  保存配置数据和生成规则描述的Excel文件            | 需要根据实际使用手动编辑
kind_const.lua      |  导出的lua格式的常量(protobuf里的enum)          | java -client -jar xresloader.jar -t lua -p protobuf -f kind.pb --pretty 2 -c kind_const.lua --lua-global
role_cfg.bin        |  打包Excel数据之后的协议（protobuf）二进制文件    | java -client -jar xresloader.jar -t bin -p protobuf -f kind.pb -s 资源转换示例.xlsx -m scheme_kind
role_cfg.lua        |  打包Excel数据之后的Lua格式输出                 | java -client -jar xresloader.jar -t lua -p protobuf -f kind.pb --pretty 4 -s 资源转换示例.xlsx -m scheme_kind -n "/(?i)\.bin$/\.lua/"
role_cfg.json       |  打包Excel数据之后的Json格式输出                | java -client -jar xresloader.jar -t json -p protobuf -f kind.pb -n "/(?i)\.bin$/\.json/" -s 资源转换示例.xlsx -m scheme_kind
role_cfg.xml        |  打包Excel数据之后的XML格式输出                 | java -client -jar xresloader.jar -t xml -p protobuf -f kind.pb -s 资源转换示例.xlsx -m scheme_kind -n '/(?i)\.bin$/\.xml/'
role_cfg.msgpack.bin|  打包Excel数据之后的Msgpack格式输出             | java -client -jar xresloader.jar -t msgpack -p protobuf -f kind.pb -s 资源转换示例.xlsx -m scheme_kind -n "/(?i)\.bin$/\.msgpack.bin/"
role_cfg.js         |  打包Excel数据之后的Javascript格式输出          | java -client -jar xresloader.jar -t js -p protobuf -f kind.pb --pretty 2 -s 资源转换示例.xlsx -m scheme_kind -n "/(?i)\.bin$/\.js/" --javascript-global sample
role_cfg.n.js       |  打包Excel数据之后的Node.js模块输出             | java -client -jar xresloader.jar -t js -p protobuf -f kind.pb --pretty 2 -s 资源转换示例.xlsx -m scheme_kind -n "/(?i)\.bin$/\.n\.js/" --javascript-export nodejs
role_cfg.amd.js     |  打包Excel数据之后的AMD.js模块输出              | java -client -jar xresloader.jar -t js -p protobuf -f kind.pb --pretty 2 -s 资源转换示例.xlsx -m scheme_kind -n "/(?i)\.bin$/\.amd\.js/" --javascript-export amd



1. 数据读取依赖../header/pb_header.proto里的数据描述
2. protobuf的数据生成规则是最外层一个xresloader_datablocks结构，里面是header和打包后的数据集，每一个data_block条目是一个数据项。header内的校验码是xresloader_header.hash_code为空字符串时的校验码
3. 解码时需要依赖protobuf和pb_header
4. gen_protocol.py用于生成sample所需的相关源码文件(包含**kind.pb**和**c++读取数据用得代码**)
5. [cxx/read_kind_sample.cpp](cxx/read_kind_sample.cpp) 是C++读取协议二进制示例代码(依赖 [../loader-binding/cxx](../loader-binding/cxx) 里的内容)
6. [lua/sample_lua.lua](lua/sample_lua.lua) 是用Lua读取Lua输出的示例(依赖 [../loader-binding/lua](../loader-binding/lua) 里提及的内容)
7. [pbc/sample_pbc.lua](pbc/sample_pbc.lua) 是用Lua读取协议二进制示例代码(依赖 [../loader-binding/pbc](../loader-binding/pbc) 里提及的内容)
8. [../loader-binding/javascript](../loader-binding/javascript) 内有读取javascript输出的相关说明
9. [../loader-binding/msgpack](../loader-binding/msgpack) 内有读取msgpack输出的相关说明和node.js和python的示例
