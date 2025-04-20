FROM openjdk:21-jdk-slim

# 镜像内目录（专门建个目录放 jar 和 config）
WORKDIR /app

# 复制 jar 包到容器里
COPY ./target/vortexa-script-node-app-1.0-SNAPSHOT.jar /app/app.jar



# 暴露端口
EXPOSE 10110 18848

# 启动命令，指定 config 目录
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -cp \"lib/*:app.jar\" -jar app.jar $@"]

CMD ["--help"]
