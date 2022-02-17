# HuobiTrading

**实现了一个火币的自动交易的程序。火币网socket实时订阅数据等。交流使用**

增加了talib的一些指标
包括跨期套利，rsi 买卖 还有2个tradingview移植过来的策略.

**缺少火币的sdk请自行火币官网下载打成jar包放到maven依赖中**
![image](https://user-images.githubusercontent.com/20676490/154517224-70223561-fafa-4704-9f26-496700de252e.png)

**自行修改配置**
HuobiTrading/harvest_data/src/main/resources/application-mylocal.yml

huobi:
  api_key: ####
  secret_key: ####
  
**自行docker打包**
具体看dockerfile

**运行效果**
![image](https://user-images.githubusercontent.com/20676490/154516260-fbffec6a-ed78-4020-8811-273a38ee7531.png)

**云服务器配置内存不建议低于1g 最好香港的**

需要分开打包。
harvest_data 负责订阅实时数据
harvest_trading 负责交易

![image](https://user-images.githubusercontent.com/20676490/154517835-58628cb0-c206-4535-bc6a-c0789e8dfd8e.png)

**！！！**
