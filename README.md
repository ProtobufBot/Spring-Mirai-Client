# Spring-Mirai-Client
可通过HTTP请求创建Mirai QQ机器人，处理登陆验证码、URL等信息

本项目(client端)使用 Kotlin 编写（因为 Mirai 是 kotlin 的）

server 端：可使用任意语言编写

通信协议：https://github.com/lz1998/onebot/tree/master/v11/specs/idl

## TODO
- 整合 onebot 协议，与远程服务器通过 http/websocket/rpc 方式进行通信
- 群/私聊 消息事件 -> protobuf/json
- 接受API调用，执行并返回结果

