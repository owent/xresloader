MessagePack配置读取
======

> 用于读取输出类型为msgpack的转换结果

由于[MessagePack](http://msgpack.org/) 支持的语言很多，并且接口不统一，所以不提供读取库。所有打包的数据类似下面的形式：

```
{
    xres_ver: "版本号字符串",
	data_ver: "版本号字符串",
	count: 配置记录个数,
	hash_code: "hash算法:hash值",
}
配置协议名: [
    {配置内容},
	{配置内容},
	{配置内容},
]
```


Sample - Python2读取示例：
------

```bash
# 安装pip,已安装可跳过
wget -c --no-check-certificate "https://bootstrap.pypa.io/get-pip.py"
python get-pip.py

# 安装msgpack模块,已安装可跳过(以下2选1)
pip install msgpack-python
python -m pip install msgpack-python

```

```python
#!/usr/bin/python
# -*- coding: utf-8 -*-

import msgpack

unpacker = msgpack.Unpacker(open('role_cfg.msgpack.bin'))

cfg_mgr = {}

for unpacked in unpacker:
    # dump raw data
    print(unpacked)
    # make index in cfg_mgr
    for cfg_name in unpacked:
        cfg_set = unpacked[cfg_name]
        # header must be skiped
        if list != type(cfg_set):
            break
        cfg_index_set = {}
        for cfg_item in cfg_set:
            # we assume id is the key
            cfg_index_set[cfg_item['id']] = cfg_item
        cfg_mgr[cfg_name] = cfg_index_set

# dump cfg_mgr
print(cfg_mgr)

# get cfg item from role_cfg with key = 10001
print(cfg_mgr['role_cfg'][10001])
```


Sample - Node.js读取示例：
------

```bash
# 安装msgpack模块,已安装可跳过
# 当然也可以用另一个模块 npm install msgpack5 --save，但是下面的例子使用的是msgpack-lite
npm install --save msgpack-lite
```

```javascript
var fs = require("fs");
var msgpack = require("msgpack-lite");

var readStream = fs.createReadStream("role_cfg.msgpack.bin");
var decodeStream = msgpack.createDecodeStream();

// show multiple objects decoded from stream
var cfg_mgr = {};

readStream.pipe(decodeStream).on("data", function(obj){
    for(var cfg_name in obj) {
        var cfg_set = obj[cfg_name];
        // header must be skiped
        if('object' != typeof(cfg_set)) {
            break;
        }
        var cfg_index = {};
        for (var key in cfg_set) {
            var cfg_item = cfg_set[key];
            // we assume id is the key
            cfg_index[cfg_item.id || 0] = cfg_item
        }
        
        cfg_mgr[cfg_name] = cfg_index;
    }
}).on("end", function() { // must run after read all data 
    // dump cfg_mgr ( )
    console.log(cfg_mgr)
    
    // get cfg item from role_cfg with key = 10001
    console.log(cfg_mgr['role_cfg'][10001])
});
```
