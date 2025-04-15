# 基础镜像（用个体积小、稳定的 JDK 21）
FROM eclipse-temurin:21-jre

# 镜像内目录（专门建个目录放 jar 和 config）
WORKDIR /app

# 复制 jar 包到容器里
COPY vortexa-platform.jar /app/

# 复制 config 配置目录到容器里
COPY config /app/config/

# 暴露端口
EXPOSE 10110 18848

# 启动命令，指定 config 目录
ENTRYPOINT ["java", "-jar", "vortexa-platform.jar", "--spring.config.location=./config/application.yaml"]
