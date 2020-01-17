### RTMP-System

#### 工程描述
> 接收obs推流，并将其封装为mpeg-ts格式推送到RTP-Server

#### 功能模块
1. 封装RTP流
	* 方式：待定
	* 描述：封装为mpeg-ts格式
	
2. 对接RTP-Server
	* 方式：http请求
	* 描述：用户鉴权，获得RTP-ssrc，将封装后的流推送到RTP-Server