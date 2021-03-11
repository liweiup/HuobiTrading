# Docker image for springboot file run
# VERSION 0.0.1
# Author: eangulee
# 基础镜像使用java
FROM openjdk:8-jdk-alpine
# 作者
MAINTAINER eangulee <liwei@qq.com>
# VOLUME 指定了临时文件目录为/tmp。
# 其效果是在主机 /var/lib/docker 目录下创建了一个临时文件，并链接到容器的/tmp
VOLUME /tmp
ARG docker-harvest_data
RUN ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime
RUN echo 'Asia/Shanghai' >/etc/timezone
ENV JAVA_OPTS="\
-server \
-Xss300K \
-Xms100M \
-Xmx200M"
#复制上下文目录下的target/demo-1.0.0.jar 到容器里
COPY target/harvest_trading-0.0.1-SNAPSHOT.jar harvest_trading.jar
#指定容器启动程序及参数   <ENTRYPOINT> "<CMD>"
#ENTRYPOINT exec java $JAVA_OPTS -jar build/libs/harvest_trading.jar
#ENTRYPOINT ["java","-jar","harvest_trading.jar"]
ENTRYPOINT java ${JAVA_OPTS} -Djava.security.egd=file:/dev/./urandom -jar /harvest_trading.jar