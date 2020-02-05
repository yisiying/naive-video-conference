## PC客户端

### 一、主要功能

- 主播功能

  - 进行个人直播
  - 开启连线功能
  - 批准连线请求
  - 断开连线
  - 设置连线时画面布局（大小窗口，对等窗口等）
  - 是否开启人脸检测
  
- 观众功能
  
   - 查看房间列表
   - 进入直播房间（观看直播）
   - 申请连线（需登陆）
   - 留言（需登陆）
   
### 二、模块说明

#### 1. 前端页面

- **登录页**
  - 登陆
  - 注册

- **主页**
    - 登陆控制
      - 显示登陆状态
      - 点击弹出登陆窗口
         
    - 开始直播
      - 登陆状态下进入直播间（主播视角）
      - 未登录状态下弹出登录窗口
       
    - 观看直播
      - 进入房间列表页
       
- **房间列表页**
    - 查看房间列表
      - 点击进入直播间（观众视角）
    
- **直播间（主播视角）**
    - 显示直播画面（自己 或 自己 + 连线观众）
      - 非连线状态显示摄像头获取的画面
      - 连线状态显示摄像头画面 + 连线观众画面
      
    - 显示及设置直播间信息
      - 显示直播间信息
        - roomId
        - roomName
        - roomDes
        - userId
      - 修改房间信息
      
    - 设置直播内容
      - 设置直播内容
        - 连线功能是否开启
        - 人脸检测功能是否开启
        - 画面格式设置
          - 大小窗口
          - 对等窗口
        
    - 连线申请控制区
        - 显示申请连线的观众列表
        - 可对每条申请进行接受/拒绝操作
        
    - 留言板
      - 显示观众留言
      
    - 直播开关控制
      - 开启直播
      - 关闭直播
      
- **直播间（观众视角）**
    - 显示直播画面（主播 或 自己 + 主播）
      - 非连线状态下显示主播画面
      - 连线状态下显示摄像头画面 + 主播画面
      
    - 显示直播间信息
       - roomId
       - roomName
       - roomDes
       - userId    
         
    - 申请连线（登陆状态 + 直播间允许连线状态）
      - 点击可向主播发出连线申请
      
    - 留言板
      - 显示留言板
      - 发表评论（登陆）
    
  
#### 2. 交互控制

- **与roomManager**
  - Http
    - 注册
    - 登陆
    - 获取房间列表
    - 查询房间
    
  - Web Socket
  - 主播
    - 发送
      - 发起直播申请
      - 设置房间信息
      - 设置直播内容
      - 连线申请审批
      - 断开连线请求
      
    - 接收
      - 申请直播反馈
      - 设置房间信息反馈
      - 设置直播内容反馈
      - 连线申请
      - 连线申请审批反馈
      
  - 观众
    - 发送
      - 申请连线
      - 断开连线请求
      - 留言
      
    - 接收
      - 申请连线反馈
  
  
- **与rtpServer**
  
  - 申请推流
  - 拉流
  - 断开推流
   
 