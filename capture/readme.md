## 媒体流采集器


### 使用方式 - 非阻塞式消息交互

#### 一、主要功能
- 实现摄像头和桌面视频采集
- 实现音频采集
- 实现对采集到的流的推送

#### 二、主要模块
#### core
- CaptureManager : 与用户进行消息交互
- DesktopCapture : 采集桌面图像
- ImageCapture : 采集摄像头图像
- MontageActor : 融合摄像头和桌面图像
- SoundCapture : 采集声音

#### protocol
- 所有交互信息的定义

#### sdk
- MediaCapture : 采集数据sdk

#### 三、使用方法
- 创建类MediaCapture
- 调用MediaCapture的set方法设置采集参数
- CaptureManager初始化完成后给captureActor发送CaptureStartSuccess(manager: ActorRef[CaptureManager.Command])消息
- 给captureManager发送AskFrame消息获取采集到摄像头图像的Frame
- 给captureManager发送AskDesktopFrame消息获取采集到桌面图像的Frame
- 给captureManager发送AskSamples消息获取采集到声音的信息
- 给captureManager发送StartEncodeFile消息将采集到的流写入文件；发送StopEncodeFile停止将流写入文件
- 给captureManager发送 StartEncodeStream 消息将采集到的流写入指定的OutputStream；发送StopEncodeStream停止将流写入OutputStream
- 给captureManager发送RecordToBiliBili 消息将采集的流推到指定的url;给captureManager发送StopRecordToBiliBili 停止将流推到指定的url
- 调用MediaCapture的showDesktop方法采集桌面图像
- 调用MediaCapture的showCamera方法采集摄像头图像
- 调用MediaCapture的showBoth方法同时采集摄像头和桌面图像
- 调用MediaCapture的stop方法停止流的采集。
