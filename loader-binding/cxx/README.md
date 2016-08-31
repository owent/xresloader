二进制配置读取
======

> 用于读取输出类型为bin，协议类型为protobuf时的转换结果
> 需要使用[protobuf c++](https://github.com/google/protobuf)库

FAQ
======

出现xresloader符号重定义（multiple definition of `com::owent::xresloader::pb::xresloader_XXX）
------

[pb_header.pb.cc](pb_header.pb.cc) 和 [pb_header_v3.pb.cc](pb_header_v3.pb.cc) 只能保留一个

如果系统采用的是proto v3则保留[pb_header_v3.pb.cc](pb_header_v3.pb.cc)

如果系统采用的是老版本的proto v2则保留[pb_header.pb.cc](pb_header.pb.cc)