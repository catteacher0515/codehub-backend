# Build stage
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .

# 配置阿里云 Maven 镜像加速依赖下载
RUN mkdir -p /root/.m2 && \
    echo '<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0" \
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" \
    xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 https://maven.apache.org/xsd/settings-1.0.0.xsd"> \
    <mirrors> \
        <mirror> \
            <id>aliyunmaven</id> \
            <mirrorOf>*</mirrorOf> \
            <name>阿里云公共仓库</name> \
            <url>https://maven.aliyun.com/repository/public</url> \
        </mirror> \
    </mirrors> \
    </settings>' > /root/.m2/settings.xml

# 预下载依赖，利用 Docker 缓存
RUN mvn dependency:go-offline

COPY src ./src
RUN mvn clean package -DskipTests

# Run stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8125
ENTRYPOINT ["java", "-jar", "app.jar"]
