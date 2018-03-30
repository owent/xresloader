生成脚本
======

+ Unix/Linux/Mingw/Msys脚本见 [gen_sample_output.sh](gen_sample_output.sh)
+ Windows脚本见（中文文件名在Windows下可能编码原因会提示文件找不到） [gen_sample_output.ps1](gen_sample_output.ps1)


关于proto v2和proto v3
======
sample的脚本会分别生成proto v2和proto v3的配置，实际上由于转表系统没有使用v3的新数据结构，所以任意一种转出来的配置都喝一同时被v2和v3的读取工具读取。


xresloader示例文件说明:
======

1. 数据读取依赖[../header/pb_header.proto](../header/pb_header.proto)或[../header/pb_header_v3.proto](../header/pb_header.proto)里的数据描述
2. protobuf的数据生成规则是最外层一个xresloader_datablocks结构，里面是header和打包后的数据集，每一个data_block条目是一个数据项。header内的校验码是xresloader_header.hash_code为空字符串时的校验码
3. 解码时需要依赖protobuf和pb_header
4. [proto_v2](sample/proto_v2)目录下是使用protobuf v2的协议文件，[gen_protocol.py](gen_protocol.py)用于生成protobuf v2的pb文件和C++所需代码
5. [proto_v3](sample/proto_v3)目录下是使用protobuf v3的协议文件，[gen_protocol_v3.py](gen_protocol.py)用于生成protobuf v3的pb文件和所需代码
6. [cxx/read_kind_sample.cpp](cxx/read_kind_sample.cpp) 是C++读取协议二进制示例代码(依赖 [../loader-binding/cxx](../loader-binding/cxx) 里的内容)
7. [lua/sample_lua.lua](lua/sample_lua.lua) 是用Lua读取Lua输出的示例(依赖 [../loader-binding/lua](../loader-binding/lua) 里提及的内容)
8. [pbc/sample_pbc.lua](pbc/sample_pbc.lua) 是用Lua读取协议二进制示例代码(依赖 [../loader-binding/pbc](../loader-binding/pbc) 里提及的内容)
9. [../loader-binding/javascript](../loader-binding/javascript) 内有读取javascript输出的相关说明
10. [../loader-binding/msgpack](../loader-binding/msgpack) 内有读取msgpack输出的相关说明和node.js和python的示例


以下生成的sample文件假设协议是 **$proto_dir** （***proto_v2***或***proto_v3***）,等效执行命令表

文件名               |  描述                                        |  参考命令 
--------------------|----------------------------------------------|----------
*.proto             |  protobuf格式的协议描述文件                     | 需要根据实际使用手动编辑
proto_v2/kind.pb    |  protobuf工具的打包proto文件的输出结果           | protoc -o proto_v2/kind.pb proto_v2/*.proto -I proto_v2 或 gen_protocol.py
proto_v3/kind.pb    |  protobuf工具的打包proto文件的输出结果           | protoc -o proto_v3/kind.pb proto_v3/*.proto -I proto_v3 或 gen_protocol.py
*.xlsx              |  保存配置数据和生成规则描述的Excel文件            | 需要根据实际使用手动编辑
kind_const.lua      |  导出的lua格式的常量(protobuf里的enum)          | java -client -jar xresloader.jar -t lua -p protobuf -o $proto_dir -f $proto_dir/kind.pb --pretty 2 -c kind_const.lua--lua-global
role_cfg.bin        |  打包Excel数据之后的协议（protobuf）二进制文件，指定数据版本号为 1.0.0.0 | java -client -jar xresloader.jar -t bin -p protobuf -o $proto_dir -f $proto_dir/kind.pb -s *.xlsx -m scheme_kind -a 1.0.0.0
role_cfg.lua        |  打包Excel数据之后的Lua格式输出，指定数据版本号为 1.0.0.0 | java -client -jar xresloader.jar -t lua -p protobuf -o $proto_dir -f $proto_dir/kind.pb --pretty 4 -s *.xlsx -m scheme_kind -n '/(?i)\.bin$/\.lua/' --data-version 1.0.0.0
role_cfg.json       |  打包Excel数据之后的Json格式输出                | java -client -jar xresloader.jar -t json -p protobuf -o $proto_dir -f $proto_dir/kind.pb -n '/(?i)\.bin$/\.json/' -s *.xlsx -m scheme_kind
role_cfg.xml        |  打包Excel数据之后的XML格式输出                 | java -client -jar xresloader.jar -t xml -p protobuf -o $proto_dir -f $proto_dir/kind.pb -s *.xlsx -m scheme_kind -n '/(?i)\.bin$/\.xml/'
role_cfg.msgpack.bin|  打包Excel数据之后的Msgpack格式输出             | java -client -jar xresloader.jar -t msgpack -p protobuf -o $proto_dir -f $proto_dir/kind.pb -s *.xlsx -m scheme_kind -n '/(?i)\.bin$/\.msgpack.bin/'
role_cfg.js         |  打包Excel数据之后的Javascript格式输出          | java -client -jar xresloader.jar -t js -p protobuf -o $proto_dir -f $proto_dir/kind.pb --pretty 2 -s *.xlsx -m scheme_kind -n '/(?i)\.bin$/\.js/' --javascript-global sample.xresloader 
role_cfg.n.js       |  打包Excel数据之后的Node.js模块输出             | java -client -jar xresloader.jar -t js -p protobuf -o $proto_dir -f $proto_dir/kind.pb --pretty 2 -m "DataSource=$(ls *.xlsx)|kind|3,1" -m "MacroSource=$(ls *.xlsx)|macro|2,1" -m "ProtoName=role_cfg" -m "OutputFile=role_cfg.bin" -m "KeyRow=2" -m "KeyCase=lower" -m "KeyWordSplit=_" -m 'KeyWordRegex=[A-Z_\$ \t]|[_\$ \t]|[a-zA-Z_\$]' -n '/(?i)\.bin$/\.n\.js/' --javascript-export nodejs
role_cfg.amd.js     |  打包Excel数据之后的AMD.js模块输出              | java -client -jar xresloader.jar -t js -p protobuf -o $proto_dir -f $proto_dir/kind.pb --pretty 2 -s *.xlsx -m scheme_kind -n '/(?i)\.bin$/\.amd\.js/' --javascript-export amd
arr_in_arr_cfg.lua  |  嵌套数组示例Lua输出                           | java -client -jar xresloader.jar -t lua -p protobuf -o $proto_dir -f $proto_dir/kind.pb --pretty 2 -m "DataSource=$(ls *.xlsx)|arr_in_arr|3,1" -m "MacroSource=$(ls *.xlsx)|macro|2,1" -m ProtoName=arr_in_arr_cfg -m OutputFile=arr_in_arr_cfg.lua -m KeyRow=2 -o proto_v3
arr_in_arr_cfg.bin  |  嵌套数组示例协议二进制输出                     | java -client -jar xresloader.jar -t bin -p protobuf -o $proto_dir -f $proto_dir/kind.pb -m "DataSource=$(ls *.xlsx)|arr_in_arr|3,1" -m "MacroSource=$(ls *.xlsx)|macro|2,1" -m ProtoName=arr_in_arr_cfg -m OutputFile=arr_in_arr_cfg.bin -m KeyRow=2 -o proto_v3
role_upgrade_cfg.lua |  Lua输出upgrade表                           | java -client -jar xresloader.jar -t lua -p protobuf -o '$proto_dir'     -f '$proto_dir/kind.pb' -s '$XLSX_FILE' -m scheme_upgrade -n "/(?i)\.bin$/\.lua/"
role_upgrade_cfg.json | Json输出upgrade表                          | java -client -jar xresloader.jar -t json -p protobuf -o '$proto_dir'    -f '$proto_dir/kind.pb' -s '$XLSX_FILE' -m scheme_upgrade -n "/(?i)\.bin$/\.json/"
kind_const_module.lua |  使用module来导出lua枚举类型                 | java -client -jar xresloader.jar -t lua -p protobuf -o $proto_dir -f $proto_dir/kind.pb --pretty 2 --lua-module ProtoEnums.Kind -c kind_const_module.lua 
role_cfg_module.lua |  使用module来导出lua输出，指定数据版本号为 1.0.0.0 | java -client -jar xresloader.jar -t lua -p protobuf -o $proto_dir -f $proto_dir/kind.pb --lua-module ProtoData.Kind -s $XLSX_FILE -m scheme_kind -n "/(?i)\.bin\$/_module\.lua/" --data-version 1.0.0.0 