# ---------- Build stage ----------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Gradle wrapper & build scripts 먼저 복사(캐시 최적화)
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./

# 소스 복사
COPY src src

# 빌드(테스트는 필요 시 끄거나 켜세요)
RUN chmod +x gradlew && ./gradlew clean bootJar -x test

# ---------- Run stage ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

# 빌드 산출물 복사
COPY --from=build /app/build/libs/*.jar app.jar

# 기본 포트
EXPOSE 8080

# 프로필은 환경변수로 바꿀 수 있게
ENV SPRING_PROFILES_ACTIVE=prod

ENTRYPOINT ["java", "-jar", "app.jar"]
