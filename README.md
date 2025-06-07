# dbMonitor

一个数据库课设，学了点javaFX，借用了几位大师傅造好的轮子，整合写了款数据库利用工具。



dbMonitor是一款在渗透测试中获得数据库高权限（dba）账户后的深入利用的工具。目前仅开发mysql和mssql的功能。

功能如下：

1、sql监视

2、sql执行

3、xp_cmdshell提权

4、UDF提权

5、CLR绕过杀软一键上线CS（具有时效性，如果不免杀了可以issue下）



使用方法如下：

java8

```plain
java -jar dbMonitor.jar
```

java8+

```plain
java --module-path "D:/JDK/javafx-sdk-23.0.1/lib"(修改为你的javafxsdk的lib) --add-modules javafx.controls,javafx.fxml -jar dbMonitor.jar
```










