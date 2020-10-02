# Spring-Mirai-Client

用于收发QQ消息，并通过 websocket + protobuf 上报给 server 进行处理。

本项目对应前端：https://github.com/protobufbot/spring-mirai-client-ui

server端可以使用任意语言编写，通信协议：https://github.com/lz1998/onebot_idl

Java/Kotlin用户推荐使用 [spring-boot-starter](https://github.com/protobufbot/pbbot-spring-boot-starter)

支持发送的消息：文字、表情、图片、闪照、atQQ、atAll

## 使用说明

下载release：https://github.com/ProtobufBot/Spring-Mirai-Client/releases

解压后运行
```bash
java -jar spring-mirai-client-版本.jar
```

浏览器打开 http://localhost:9000/

创建机器人并处理下方验证码（图形验证码或设备锁）


![截图](https://github.com/lz1998/Spring-Mirai-Client/blob/master/screenshot/client.jpg?raw=true)