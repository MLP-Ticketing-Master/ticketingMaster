# 1단계 빌드 — Gradle 래퍼로 jar 생성
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY . .
RUN chmod +x gradlew && ./gradlew clean bootJar -x test --no-daemon

# 2단계 실행 — 경량 JRE 에 jar 만 복사
FROM eclipse-temurin:21-jre
WORKDIR /app
# 타임존 한국 고정 — 안 하면 UTC 라 9시간 어긋남
ENV TZ=Asia/Seoul
COPY --from=build /app/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]