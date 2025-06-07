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

免责声明：
本程序作者初衷是为了完成数据库课设和学习javafx
使用该工具必须遵守国家有关的政策和法律，如刑法、国家安全法、保密法、计算机信息系统安全保护条例等，保护国家利益，保护国家安全，
对于违法使用该工具而引起的一切责任，由用户负全部责任。
一旦您使用了本程序，将视为您已清楚了解上列全部声明并且完全同意。
本程序仅供合法的渗透测试以及爱好者参考学习。










