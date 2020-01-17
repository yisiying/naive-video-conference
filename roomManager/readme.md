## 2.0版本

### 直播流程

    1. 对接processor转为对接distributor
    2. 直播地址以及录像地址由distributor进行管理
    3. 每更新拉流liveId需要通知distributor

### 连线流程

    1. 同意连线时，将主播、观众以及反推的liveId都发送给processor，同时roomManager管理这三个liveId,断开连线即通知processor关闭房间，取消连线者和反推的liveId维护
    2. 同意连线或者断开连线通知distributor更新拉流的liveId
    3. 同意连线或者断开连线通过ws通知pcClient
    

    
# RoomManager后台管理系统

##一、主要功能

- 实现用户注册和登录流程
- 用户信息管理
- 处理用户的操作
- 管理直播流程
- 管理连线流程
- 录像管理功能
- 管理员功能
- 录像观看统计功能

##二、关键概念

###用户：

用户分类按是否注册，分为注册用户和临时用户。注册用户的权限比临时用户的权限更多，注册用户可以进行直播，观看直播，发送弹幕和请求连线，临时用户只能观看直播。
按照直播流程分为观众和主播，主播进行直播。主播可以将自己的画面推到每个观众端，观众则可以观看主播画面，通过弹幕和连线与主播进行互动。
    
###房间：

每个主播直播时都需要开一个房间，即一个RoomActor。
房间管理主播的整个直播过程，处理直播过程中的webSocket消息，管理直播连线操作
    
###连线：

观众发送连线请求，主播同意后，直播画面切换成包括主播和连线观众的共同画面，观众就能和主播通过视频进行互动
    
###直播信息：
    
包括主播的账号信息，观众信息，直播房间信息，LiveId，LiveCode，LiveAddress等信息
    
###封禁和解封：

用户id被封禁后，不能再进行发弹幕，送礼物，连线等操作。当被解封后，才能恢复这些功能的权限

##三、主要模块

- core
    - RegisterManager
        - 用户注册管理
    - RegisterActor
        - 用户注册认证操作
    - EmailActor
        - 向用户发送认证邮件
    - RoomManager
        - 管理直播房间
        - 获取房间信息
        - 更改用户信息
        - 获取录像信息
    - RoomActor
        - 开始和关闭直播
        - 获取和更新房间信息
        - 处理连线过程
        - 接收、处理并分发webSocket消息
    - UserManager
        - 管理临时用户
        - 封禁和解封用户
        - 建立webSocket流
    - UserActor
        - 切换主播和观众状态
        - 处理并向客户端分发webSocket消息
- http
    - AdminService
        - 管理员登录
         - 删除录像
        - 封禁和解封用户
         - 获取用户列表
        - 禁播和解除
    - FileService
        - 上传图片，包括主播头像和直播间封面
        - 下载客户端打包文件
    - HttpService
        - 接口汇总
    - RecordCommentService
        - 增加录像评论
        - 获取录像评论列表
    - RecordService
        - 获取录像列表
        - 搜索录像并返回录像URL
        - 更新录像观看信息
        - 获取主播录像列表
        - 删除录像
    - RtmpService
        - 获取和更新token
        - 获取直播信息
    - RtpService
        - 获取直播信息
    - StatisticService
        - 获取登录统计数据
        - 获取录像统计信息
        - 更新录像观看时长
    - UserService
        - 用户注册
         - 用户点击认证邮件
        - 用户登录
        - 建立webSocket
        - 获取房间列表
        - 获取直播房间的房间信息RoomInfo
        - 更改昵称
        - 获取主播自己的房间信息RoomInfo
- Models
    - 向数据库获取或写入信息操作
- Protocol
    - 所有消息类型的定义
- utils
    - DistributorClient
        - 查询录像接口
        - 删除录像接口
        - 开始和停止推流接口
        - 核对流信息接口
    - ProcessorClient
        - 建立连线连接接口
        - 更新房间信息接口
        - 关闭房间接口
    - RtpClient
        - 获取直播信息接口

##四、关键流程

###注册流程：
用户在前端输入用户名密码和邮箱后，将用户信息发送到UserService。
确认用户信息无误后，生成认证码，用EmailActor发送到用户邮箱，同时生成一个对应的RegisterActor。
用户点击认证链接，RegisterActor核对认证码是否正确，并完成注册认证流程。

###直播流程：
用户选择直播，建立webSocket连接，并向UserActor发送开始直播的消息。UserActor向RoomManager发送用户开始直播的信息，生成一个新的RoomActor。
RoomActor从RtpClient获取直播信息，并向Distributor发送开始推流请求。
主播停止推流，会给Distributor发送停止推流的请求，当主播再次直播时，流程和之前一样。
当主播选择关闭房间，RoomActor向Processor发送关闭房间请求，向Distributor发送停止推流请求，一段延时后从Distributor获取直播录像，关闭RoomActor。

###连线流程：
连线流程：
观众向主播发送连线请求的webSocket消息，RoomActor收到消息，从RtpClient获取观众的直播信息和混流后的直播信息，向Distributor发送开始推混合之后的流的请求，并向Processor发送建立连接的请求。
如果主播或者观众一端断开连线，RoomActor向Processor发送关闭房间请求，更新主播的直播信息，切回主播端的画面。

##五、系统中易错的地方

    1. 用户注册时多次点击认证邮件的处理
    2. 主播开始直播时roomActor向RtpClient和Distributor获取信息时失败后的处理
    3. 在roomActor在初始化的时候收到获取房间信息的消息处理
