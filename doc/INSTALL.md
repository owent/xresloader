安装说明
==========

编译和打包
======

+ 本项目使用[apache maven](https://maven.apache.org/)管理包依赖和打包构建流程。
+ JDK 需要1.8或以上版本

```bash
# 编译
mvn compile
# 打包
mvn package
```

以上命令会自动下载依赖文件、包和插件。

编译完成后，输出的结果默认会放在 ***target*** 目录下。

更新依赖包
------

需要更新依赖包版本只要修改[pom.xml](pom.xml)并修改版本号即可。

依赖包和插件的组名、包名和版本可以在以下仓库内找到:

+ 中心maven仓库: [http://search.maven.org/](http://search.maven.org/#browse)
+ oschina镜像仓库: http://maven.oschina.net
+ *或到下面列举的仓库列表中查找*

更换maven仓库
------
由于国内访问官方maven仓库的速度比较慢，所以可以尝试使用oschina提供的maven仓库镜像

+ 具体设置方法请参照 http://maven.oschina.net/help.html
+ 简易安装方法是直接下载 http://maven.oschina.net/static/xml/settings.xml 并修改里面的 **localRepository** 选项，配置成你的环境中的本地缓存地址

设置完maven配置之后，可以用如下命令编译打包

```bash
# 编译
mvn -s [settings.xml路径] compile
# 打包
mvn -s [settings.xml路径] package
```

### 其他仓库地址
#### 公有仓库地址：
1. **[http://search.maven.org/](http://search.maven.org/#browse)**
2. **http://mvnrepository.com/**
3. **http://maven.oschina.net**
5. http://mirrors.ibiblio.org/maven2/
6. http://repository.jboss.com/maven2/
7. http://repository.sonatype.org/content/groups/public/
8. http://mirrors.ibiblio.org/pub/mirrors/maven2/org/acegisecurity/

#### 私有仓库地址：
1. http://repository.codehaus.org/
2. http://snapshots.repository.codehaus.org/
3. http://people.apache.org/repo/m2-snapshot-repository
4. http://people.apache.org/repo/m2-incubating-repository/

其他maven功能
------
参见： https://maven.apache.org/
