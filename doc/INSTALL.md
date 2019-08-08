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
+ *或到下面列举的仓库列表中查找*

更换maven仓库
------
由于国内访问官方maven仓库的速度比较慢，所以可以尝试使用oschina提供的maven仓库镜像

添加mirror节点到settings.xml里的mirrors即可。
比如:
```xml
        <mirror>
            <id>tencent-cloud</id>
            <mirrorOf>central</mirrorOf>
            <name>Aliyun Mirror.</name>
            <url>http://mirrors.cloud.tencent.com/nexus/repository/maven-public/</url>
        </mirror>

        <mirror>
            <id>aliyun</id>
            <mirrorOf>central</mirrorOf>
            <name>Aliyun Mirror.</name>
            <url>http://maven.aliyun.com/nexus/content/groups/public/</url>
        </mirror>

        <mirror>
            <id>repo2</id>
            <mirrorOf>central</mirrorOf>
            <name>Human Readable Name for this Mirror.</name>
            <url>http://repo2.maven.org/maven2/</url>
        </mirror>

        <mirror>
            <id>ui</id>
            <mirrorOf>central</mirrorOf>
            <name>Human Readable Name for this Mirror.</name>
            <url>http://uk.maven.org/maven2/</url>
        </mirror>

        <mirror>
            <id>jboss-public-repository-group</id>
            <mirrorOf>central</mirrorOf>
            <name>JBoss Public Repository Group</name>
            <url>http://repository.jboss.org/nexus/content/groups/public</url>
        </mirror>

        <mirror>
            <id>repo1</id>
            <mirrorOf>central</mirrorOf>
            <name>Human Readable Name for this Mirror.</name>
            <url>http://repo1.maven.org/maven2/</url>
        </mirror>
```

如果HOME/.m2下没有settings.xml文件，可以去http://maven.apache.org/download.cgi下载个发布包，然后复制一个出来

设置完maven配置之后，可以用如下命令编译打包

```bash
# 编译
mvn -s [settings.xml路径] compile
# 打包
mvn -s [settings.xml路径] package
```

### 其他仓库地址

#### 公有仓库地址：

1. **http://mirrors.cloud.tencent.com/nexus/repository/maven-public/**
2. **http://maven.aliyun.com/nexus/#view-repositories**
3. **[http://search.maven.org/](http://search.maven.org/#browse)**
4. **http://mvnrepository.com/**
5. http://repository.jboss.com/maven2/
6. http://repository.sonatype.org/content/groups/public/
7. http://mirrors.ibiblio.org/pub/mirrors/maven2/org/acegisecurity/

#### 私有仓库地址：
1. http://repository.codehaus.org/
2. http://snapshots.repository.codehaus.org/

其他maven功能
------
参见： https://maven.apache.org/


