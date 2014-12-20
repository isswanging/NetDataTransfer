NetDataTransfer
===============

用Java实现的类似飞鸽传书的工具。
主要是用于学习Java的socket编程，因此即使在局域网也采用了TCP和UDP的方式传输

UDP主要传输文字聊天
TCP主要传输文件或文件夹

目前主要功能完成。

![image](https://github.com/isswanging/NetDataTransfer/blob/master/img-folder/%E7%95%8C%E9%9D%A2.png)

-----------------------------

主要存在一个bug：
传文件夹时采用的策略是先把所有文件的路径用UDP的方式发给接收方，然后再用TCP的方式一个个的把文件传过去。
那么问题来了：
如果路径太长，超过了UDP一次发送的长度，就会有问题了。
