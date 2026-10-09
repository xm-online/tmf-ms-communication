# Stage 1: split the Spring Boot layered WAR so dependencies and application code
# land in separate image layers (a release then re-pushes ~0.5 MB, not ~200 MB).
FROM eclipse-temurin:25-jdk@sha256:8c0a84ea11c8f6ed52600fc19f1040121f2a162998e9f50a5faebbbad9172dcc AS builder
WORKDIR /builder
COPY build/libs/*.war application.war
RUN java -Djarmode=tools -jar application.war extract --layers --destination extracted

# Stage 2: distroless runtime. No shell, no package manager, runs as UID 65532,
# ENTRYPOINT is the base image's `java -jar`. Only /tmp is written at runtime.
FROM gcr.io/distroless/java25-debian13:nonroot@sha256:28a3986989d7d74cb5cfd6ba369e64d3d634f83ac9fa562dac8db2d20827f90e
WORKDIR /app
COPY --from=builder /builder/extracted/dependencies/ ./
COPY --from=builder /builder/extracted/spring-boot-loader/ ./
COPY --from=builder /builder/extracted/snapshot-dependencies/ ./
COPY --from=builder /builder/extracted/application/ ./
# JDK_JAVA_OPTIONS is read by the java launcher itself; operators override it to tune the JVM.
ENV TZ=Europe/Kiev JDK_JAVA_OPTIONS="-Xms128m -Xmx512m"
EXPOSE 8701
CMD ["application.war"]
