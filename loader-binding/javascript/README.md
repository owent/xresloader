javascript配置读取
======

> 用于读取输出类型为javascript的转换结果

支持多种模式

1. 写入全局
2. node.js require接口
3. AMD接口

写入全局的读取
------
--javascript-global选项可以指定输出的数据导入到哪个全局变量，如果不指定则导出到全局。这里的全局在浏览器中指window，在nodejs中指global。

比如在示例中使用了 --javascript-global sample，那么在代码中就是。

```javascript
// browser
var all_cfg = window['sample']['role_cfg'];

// nodejs
var all_cfg = global['sample']['role_cfg'];

// 建立key-value索引
// ...
```

Node.js模式的读取
------

直接require即可

```javascript
// data set
var all_cfg_block = require('./role_cfg.n');
var all_cfg = all_cfg_block['role_cfg']; // 除了数据外还有header
// 建立key-value索引
// ...
```

AMD模式的读取
------

直接go即可(未测试)。

```javascript
// data set
go('role_cfg.amd', function(amdJS, all_cfg_block){
    var all_cfg = all_cfg_block['role_cfg']; // 除了数据外还有header
    // 建立key-value索引
    // ... 
});

```

AMD模式也可以用于在某些情况下按AMD的规则自定义define函数，然后自定义导入数据的方式。