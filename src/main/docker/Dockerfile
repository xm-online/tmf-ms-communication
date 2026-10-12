# Mirror prefix for air-gapped builds, e.g. --build-arg REGISTRY=mirror.example.com/ pulls mirror.example.com/gcr.io/...
ARG REGISTRY=

# Build stage: split the Spring Boot WAR into layers (dependencies change rarely, application code often)
FROM eclipse-temurin:25-jre AS builder
WORKDIR /build
COPY build/libs/*.war application.war
RUN java -Djarmode=tools -jar application.war extract --layers --destination extracted

# Runtime stage: distroless (no shell, no package manager), non-root UID 65532, files owned by root
FROM ${REGISTRY}gcr.io/distroless/java25-debian13:nonroot
# COPY before WORKDIR: COPY creates /app as root, WORKDIR would create it as the nonroot user (writable)
COPY --from=builder /build/extracted/dependencies/ /app/
COPY --from=builder /build/extracted/spring-boot-loader/ /app/
COPY --from=builder /build/extracted/snapshot-dependencies/ /app/
COPY --from=builder /build/extracted/application/ /app/
WORKDIR /app
ENV TZ=Europe/Kiev JDK_JAVA_OPTIONS="-Xms128m -Xmx512m"
EXPOSE 8701
# user.country keeps the default Locale en_US as before (LANG=C.UTF-8 alone yields "en")
ENTRYPOINT ["java", "-Duser.country=US", "-jar", "application.war"]
