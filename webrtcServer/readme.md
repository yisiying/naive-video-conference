### WebRtc-System

#### 工程描述
> 提供webrtc通信的信令服务，对接浏览器和kurento媒体服务器<br>
> 获得媒体流，将其封装为mpeg-ts格式推送到RTP-Server

#### 功能模块
1. 对接前端
	* 方式：webSocket通信，提供可跨域调用api `ws://IP:PORT/webrtcServer/userJoin?liveId=88&liveCode=***`
	* 描述：协调建立webrtc通信
	* 消息格式
	
		|字段|数据类型|
		|:----:|:----:|
		|liveId|待定|
		|eventId|String|
		|其他|待定|
		
		```
		object EventId {
		    val INIT_USER = "INIT_USER"  //用户信息
		    val CONNECT_MSG = "CONNECT_MSG"  //连接状态
		    val Live_Handler = "Live_Handler" //连接类型
		    val PING = "PING"
		    val PONG = "PONG"
		    val Anchor_SDP_OFFER = "Anchor_SDP_OFFER"  //直播webrtc建立请求
		    val Audience_SDP_OFFER = "Audience_SDP_OFFER" //连线webrtc建立请求
		    val PROCESS_SDP_ANSWER = "PROCESS_SDP_ANSWER"  // webrtc建立答复
		    val ADD_ICE_CANDIDATE = "ADD_ICE_CANDIDATE"  //设置公网地址
		    val ADD_ASK = "ADD_ASK"  //请求连线
		    val CHOSE_ANS = "CHOSE_ANS"  //选择连线者
		    val STOP = "STOP"  //停止、断连
		}
		```	
	

2. 对接kurento服务器
	* 方式：SDK调用（底层webSocket通信）
	* 描述：协调建立webrtc通信并推出rtp流
	* 备注：本部分是否依赖kurento服务器待定，功能实现暂时需要
	
3. 封装RTP流
	* 方式：待定
	* 描述：封装为mpeg-ts格式
	
4. 对接RTP-Server
	* 方式：http请求
	* 描述：用户鉴权，获得RTP-ssrc，将封装后的流推送到RTP-Server