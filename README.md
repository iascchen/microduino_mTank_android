# microduino_mTank_android

Microduino mTank APP, Android version

## Features

基于 Microduino BLE 蓝牙实现。

[x]. 控制小车的前进、后退和转弯。

[x]. 控制车载云台的俯仰、旋转。

[x]. 控制小车发炮等其他功能。

基于 MicroWrt Wifi 实现：

[x]. 连接架设在云台上的 MicroWrt 和 Wifi Camera，并在屏幕上显示。

[x]. 拍照并存放到手机照片集中。

VR 功能

[ ]. 利用 Cardboard 显示双摄像头返回的视频流。

[ ]. 增加手持摇杆支持。

其它功能：

[x]. 支持“美国手（左手油门）”和“日本手（右手油门）”的切换。
请参考：[http://www.crazepony.com/wiki/japan-american-rc.html](http://www.crazepony.com/wiki/japan-american-rc.html)

## TODO



## Related Hardware

* Microduino Core
* Microduino BLE
* Microduino 

* MicroWrt
* MicroWrt-Hub

* Microduino USB Camera

## Control Protocol

BLE 小车控制参考 8 通道 WMC 协议。协议标准如下：

### 协议数据格式：

    [head][code][data][checksum]
    
    * head是固定的，是数据头：[占两个字节]，十六进制为0xAA,0xBB；
    * code是指令代码：[占一个字节]，十六进制为0xC8，十进制为200
    * data是遥控通道数据：[占十六个字节]，每个通道的数值范围为1000~2000
    
        一共八个通道数据：小车转向, 小车油门, 云台旋转, 云台俯仰, aux1, aux2, aux3, aux4
        每个数据占2个字节，低位在前，比如：0xdc 0x05数据；数据是低位在前，所以0x05 0xdc是数据，值是1500
        
    * checksum为校验位：[占一个字节]
    
        校验程序：
        
            byte getChecksum(byte length,byte cmd,byte mydata[]) { 
                //三个参数分别为： 数据长度（16）  ，  指令代码（200），  实际数据数组
                byte checksum=0;
                checksum ^= (length&0xFF);
                checksum ^= (cmd&0xFF);
                for(int i=0;i<length;i++)
                   checksum ^= (mydata[i]&0xFF);
                return checksum;
            }
            
        调用：
            
            byte check_sum=getChecksum(16,200,data);
    
    示例代码：[一共二十二个字节]
    
        发送：1500,1500,2000,1007,1500,1500,1500,1500八个通道数据
        0xAA,0xBB,0xC8,0xDC,0x05,0xDC,0x05,0xD0,0x07,0xEF,0x03,0xDC,0x05,0xDC,0x05,0xDC,0x05,0xDC,0x05,0xE3

## Microduino Program



## How to Play

1. 将 Microduino Program upload 到 Microduino Core

2. 连接 MicroWrt，连接上的 USB 摄像头：

    确认设备已经被正确识别：
        
        ls /dev/video0
        
    启动 mjpg_streamer 输出视频流：
        
        mjpg_streamer -i "input_uvc.so -d /dev/video0 -r 640x480 -f 20" -o "output_http.so -p 8080 -w /root/mjpg/www/"

    连接名为 `MicroWrt****` 的 Wifi SSID，然后验证视频流：
    
        http://192.168.1.1:8080/?action=stream

3. 打开 mCar App，进行设置：

    点按屏幕中间的 M 图标，打开设置界面，你可以在此：
    
        * 选择采用“美国手（左手油门）”和“日本手（右手油门）”。
        * 扫描 BLE 设备，请选择名为 “Microduino” 的蓝牙设备。
        * 设置 Web Camera 的 IP 地址和端口，例如：`192.168.1.1:8080`。系统在访问时会自动增加 `/?action=stream` 的访问入口
    
    设置完成后，请移动到设置屏幕最下端，点按 `Save` 按钮，保存。
    
        * 如果 BLE 连接成功，屏幕下端显示 “Ready” 后，即可对小车进行控制。
        * 如果 Web Camera 连接正常，背景屏幕会显示摄像头输入。否则会显示一张静态背景图片。
    
4. 控制界面

        * 油门摇控杆
        * 云台摇控杆
        * 如果 Web Camera 连接成功，会在屏幕右上角显示拍照按钮
        * 下方功能按钮分别是：发炮

## Used 3rd Lib

* BugStick [https://github.com/justasm/Bugstick](https://github.com/justasm/Bugstick) , 
I modified it to support RECT stick.

* simplemjpegview [https://bitbucket.org/neuralassembly/simplemjpegview](https://bitbucket.org/neuralassembly/simplemjpegview), 
I modified it to support display fit screen width and capture view to image.

                        Intel License Agreement
                For Open Source Computer Vision Library
    
    Copyright (C) 2000, Intel Corporation, all rights reserved.
    Third party copyrights are property of their respective owners.
    Redistribution and use in source and binary forms, with or without modification,
    are permitted provided that the following conditions are met:
    
        Redistribution's of source code must retain the above copyright notice,
        this list of conditions and the following disclaimer.
    
        Redistribution's in binary form must reproduce the above copyright notice,
        this list of conditions and the following disclaimer in the documentation
        and/or other materials provided with the distribution.
    
        The name of Intel Corporation may not be used to endorse or promote products
        derived from this software without specific prior written permission.
    
    This software is provided by the copyright holders and contributors "as is" and
    any express or implied warranties, including, but not limited to, the implied
    warranties of merchantability and fitness for a particular purpose are disclaimed.
    
    In no event shall the Intel Corporation or contributors be liable for any direct,
    indirect, incidental, special, exemplary, or consequential damages
    (including, but not limited to, procurement of substitute goods or services;
    loss of use, data, or profits; or business interruption) however caused
    and on any theory of liability, whether in contract, strict liability,
    or tort (including negligence or otherwise) arising in any way out of
    the use of this software, even if advised of the possibility of such damage.
    
## Please enjoy It. 