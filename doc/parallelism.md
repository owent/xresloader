# 并发转表

## 使用

`--parallelism N|auto` 控制 stdin 批次的并发任务数，默认 `auto`：

```bash
java -jar xresloader.jar --stdin --parallelism auto < tasks.txt
```

每个非空输入行是一项任务，同一任务中的多个 scheme 按顺序执行，不拆分表内行。
线程数由启动命令确定，stdin 行内的该选项不会调整线程池。

`auto` 在启动时按可用 CPU 和 JVM 最大堆选择线程数：

```text
max(1, min(8, ceil(availableProcessors / 2), floor(maxHeap / 256 MiB)))
```

例如 16 个逻辑 CPU、2 GiB 堆选择 8，512 MiB 堆选择 2。显式正整数不受自动上限限制。
该规则是内存预算估算；大表并发时需结合实际内存占用调整线程数。
依赖前一项输出或共享脚本外部状态的任务应使用 `--parallelism 1`。

## 状态与缓存

- 参数、scheme、验证器、宏展开、脚本引擎和可变字段绑定使用任务 TLS，成功或失败后均清理。
- 启动命令的数据版本作为批次默认值，行内版本仅覆盖当前任务；mapping 文件和 seed 按输入顺序继承。
- 输入字节、Excel 流式索引和只读协议描述使用全局读写锁缓存：命中持读锁，构建完成后持写锁发布。
- 输入字节、pb 解析和协议索引按 key 单独协调构建；不同 key 可并发加载，同 key 只发布一次成功结果，失败可重试。
- 输入字节缓存键包含规范路径、大小、修改时间和文件身份；协议索引键还包含输入顺序和依赖解析选项。
  失败或缺失依赖的协议链接结果不缓存。
- 协议缓存命中时，`PbInfoSet` 直接引用共享只读索引，增量加载时才复制；首次构建保留原遍历顺序。
  验证器、字段绑定、快速查找缓存和失败状态保留在任务 TLS。
- Excel 缓存以原子访问序号记录淘汰顺序，插入和淘汰持写锁；淘汰不影响活动读者。
  流式表加载按规范文件路径使用独立读写锁，不同文件可并发构建；构建期间不持有全局索引锁，缓存命中仍可并发读取。
  Workbook 和 FormulaEvaluator 属于任务，关闭后释放，只共享其原始输入字节。

## 写出与失败

转换并发执行，文件写出按输入顺序进行，保留重复路径的覆盖顺序、UE 代码首次生成和导入配置合并语义。
mapping 按目标文件归并并排序输出。在途任务数不超过线程数，避免结果无限积压。
单项失败后继续处理后续任务；退出前等待全部已接收任务，汇总失败数，退出码上限为 255。

## 验证

依次执行 `mvn clean verify`、Gradle `test shadowJar` 和 `mvn verify`，检查全量及交替构建。
两者的类文件目录独立，但 `clean` 共用 `target`，不能同时构建。
按 [sample 输出回归验证](../sample/README.md#输出回归验证) 比较默认、1、4、8、auto 模式的完整文件集合和 SHA-256。
保持输入、平台和隔离目录一致，不忽略数据字段、换行或排序。

缓存发布、TLS 隔离和写出顺序的 jcstress 检查：

```bash
python tools/run_concurrency_stress.py --jar target/xresloader-2.24.0-beta.jar --work-dir build/concurrency-stress-run
```

jcstress 是针对指定并发行为的压力测试，不能证明整个程序不存在数据竞争。
