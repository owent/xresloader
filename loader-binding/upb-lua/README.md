upb lua 配置读取
======

> 用于读取输出类型为bin，协议类型为protobuf时的转换结果
> 需要使用 protobuf的 [upb][1] 作protobuf的Lua代码生成解析器和运行时

由于 [upb][1] 的使用比较繁琐，涉及编译运行时、编译protoc插件、生成代码、加载运行时和Lua binding等等。
详情可以去 [xres-code-generator][2] 子项目（读表代码生成工具），执行 [sample](https://github.com/xresloader/xres-code-generator/tree/main/sample) 目录中的 `sample_gen.sh` 文件后查看生成的代码和示例。

[1]: https://github.com/protocolbuffers/upb
[2]: https://github.com/xresloader/xres-code-generator
