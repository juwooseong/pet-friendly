# ---- 1단계: Gradle 빌드 (프로젝트 wrapper 사용) ----
# 주의: Spring Boot 플러그인 3.2.2의 bootJar는 Gradle 9.x와 호환되지 않는다(getDirMode() 제거).
# wrapper를 Gradle 9로 올리면 이 빌드가 실패하므로, 올릴 때는 Spring Boot도 함께 올린다.
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app

# Gradle wrapper와 빌드 스크립트를 먼저 복사해 의존성 다운로드를 캐시한다.
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
# Windows에서 체크아웃하면 CRLF가 섞일 수 있어 정리한다.
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew && ./gradlew dependencies --no-daemon > /dev/null

COPY src ./src
RUN ./gradlew bootJar -x test --no-daemon

# ---- 2단계: 실행 이미지 ----
FROM eclipse-temurin:17-jre
WORKDIR /app
# 리뷰 사진 볼륨(/app/uploads)은 처음 만들 때 이 폴더의 소유자를 물려받아 petspot 사용자가 쓸 수 있다
RUN useradd --system --uid 1001 petspot     && mkdir -p /app/uploads && chown petspot /app/uploads
COPY --from=build /app/build/libs/*.jar app.jar
USER petspot

EXPOSE 8080
ENV SPRING_PROFILES_ACTIVE=prod \
    TZ=Asia/Seoul
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
