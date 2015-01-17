xresloader示例说明:
======
1. *.proto 文件都是protobuf格式的协议描述文件
2. *.xlsx 文件是保存配置数据和生成规则描述的Excel文件
3. role_cfg.bin 文件是打包Excel数据之后的二进制文件(参考命令: java -jar xresloader.jar -t bin -p protobuf -f kind.pb -s 资源转换示例.xlsx -m scheme_kind)
4. role_cfg.lua 文件是打包Excel数据之后的生成的lua文件(参考命令: java -jar xresloader.jar -t lua -p protobuf -f kind.pb -s 资源转换示例.xlsx -m scheme_kind && mv role_cfg.bin role_cfg.lua)
5. 数据读取依赖../header/pb_header.proto里的数据描述
6. protobuf的数据生成规则是最外层一个xresloader_datablocks结构，里面是header和打包后的数据集，每一个data_block条目是一个数据项。header内的校验码是xresloader_header.hash_code为空字符串时的校验码
7. 解码时需要依赖protobuf和pb_header
8. gen_protocol.py用于生成sample所需的相关源码文件
9. cxx/read_kind_sample.cpp 是C++读取示例代码