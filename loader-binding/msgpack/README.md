MessagePack配置读取
======

> 用于读取输出类型为msgpack的转换结果

由于[MessagePack](http://msgpack.org/) 支持的语言很多，并且接口不统一，所以不提供读取库。所有打包的数据类似下面的形式：

```
{
    xrex_ver: "版本号字符串",
	data_ver: "版本号字符串",
	xrex_ver: 配置记录个数,
	hash_code: "hash算法:hash值",
}
配置协议名: [
    {配置内容},
	{配置内容},
	{配置内容},
]
```


如sample的python版本数据读取打印示例：
```python
#!/usr/bin/python
# -*- coding: utf-8 -*-

import msgpack

unpacker = msgpack.Unpacker(open('role_cfg.msgpack.bin'))

for unpacked in unpacker:
    print(unpacked)

```
