# 基于WebRtc的1v1视频通话
# 引子

> 疫情时代，地球的人类靠着远程工作艰难地维持着生活，作为生存在地球上为数不多的秃头超人们艰难地用技术支持着人们的远程办公生存状态，而机器的限制无法突破，迟早有一天，服务器将会被贪婪的人类耗尽

# 简介

WebRTC，您可以在基于开放标准的应用程序中添加实时通信功能。它支持在同级之间发送视频，语音和通用数据，从而使开发人员能够构建功能强大的语音和视频通信解决方案。该技术可在所有现代浏览器以及所有主要平台的本机客户端上使用。 WebRTC背后的技术被实现为一个开放的Web标准，并在所有主要浏览器中均以常规JavaScript API的形式提供。对于本机客户端（例如Android和iOS应用程序），可以使用提供相同功能的库。 WebRTC项目是开源的，并得到Apple，Google，Microsoft和Mozilla等的支持

WebRtc官网：https://webrtc.org/
本项目地址：https://github.com/ZYF99/RealTimeMessageApp

### 原理：
WebRtc 是P2P的技术，在真实场景中，为了实现建立对端通道，我们仍需要一个第三方为我们提供服务，这里称为信令服务器（Signalling Server）
信令服务器接受客户端的信号并将信号中转至对端客户端，对端客户端与我们建立通道，视频/音频流直接走通道进行传输，不通过信令服务器
> 往往信令服务器只是开启简单的Socket通信，你可以使用WebSocket、SocketIo、甚至Mq实现，因为它的职责只有一个：中转消息

#### 基本的图示：
![simple_arch.png](https://upload-images.jianshu.io/upload_images/17794320-feb0a465b41c6213.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

### Demo截图
![Screenshot_20210318-092518.jpg](https://upload-images.jianshu.io/upload_images/17794320-d67fa3c8f6287826.jpg?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

需要服务端代码请联系我
![qq_pic_merged_1616061987181.jpg](https://upload-images.jianshu.io/upload_images/17794320-b90aea2b2e9a9093.jpg?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)


## 请作者喝杯咖啡吧~
![qq_pic_merged_1608260264392.jpg](https://upload-images.jianshu.io/upload_images/17794320-823a9710246c4272.jpg?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

## 个人博客
[ZIKI(安卓学弟)](https://zyf99.github.io/Blog/)
	
## License

	Copyright 2020, ZEKI
	
	   Licensed under the Apache License, Version 2.0 (the "License");
	   you may not use this file except in compliance with the License.
	   You may obtain a copy of the License at
	
	       http://www.apache.org/licenses/LICENSE-2.0
	
	   Unless required by applicable law or agreed to in writing, software
	   distributed under the License is distributed on an "AS IS" BASIS,
	   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
	   See the License for the specific language governing permissions and
	   limitations under the License.
