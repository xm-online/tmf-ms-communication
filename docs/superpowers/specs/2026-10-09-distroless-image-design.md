# Distroless container image for tmf-ms-communication

Date: 2026-10-09
Status: approved design, pending implementation plan

## Goals

1. Ship the service as a distroless image: no shell, no package manager, no
   tooling beyond the JRE.
2. Minimal image and minimal dependency surface.
3. Run the process as non-root, with no Linux capabilities, on a read-only
   root filesystem.
4. Add security checks to the build pipeline and pin the base images.

The solution must stay simple: no custom entrypoint scripts, no new runtime
components, and a deployment contract that fits in one environment variable.

## Non-goals

- Sibling services (`tmf-ms-activation`, `tmf-ms-resource`,
  `tmf-ms-qualification`, `xm-ms-config`) use the same Dockerfile pattern and
  can adopt this design later, in their own PRs.
- The Jib configuration in `gradle/docker.gradle` and the `jibDockerBuild`
  mention in the README are stale, unused by CI, and left untouched.
- Migrating CI away from Travis.
- A jlink-trimmed JRE (`distroless/java-base`). It would shave roughly half
  of the runtime size but requires maintaining a module list that Groovy/LEP
  dynamic class loading makes brittle. Rejected in favour of simplicity.

## Current state

- `src/main/docker/Dockerfile`: `eclipse-temurin:25-jre` (Ubuntu), runs as
  root, installs `locales-all zip unzip curl` with apt, starts through a bash
  `entrypoint.sh`.
- `entrypoint.sh` exports `/run/secrets/*` as environment variables, can
  repack the WAR with jars from `APPLICATION_EXTERNAL_CLASSPATH`, sleeps
  `JHIPSTER_SLEEP` seconds and runs `java $JAVA_OPTS -Xmx$XMX -jar app.war`.
- Default `JAVA_OPTS` enable JMX remote on port 19999 with
  `authenticate=false`, `ssl=false`, `local.only=false`.
- CI (`.travis.yml`) builds the WAR with Gradle on the host, then
  `docker build -f src/main/docker/Dockerfile .` and pushes to Docker Hub
  `xmonline/tmf-ms-communication`.
- Deployment is Docker Swarm (`deploy/docker-compose.yml`, overlay network,
  env file).

Confirmed with the team: `JAVA_OPTS`/`XMX` are used by operations; JMX is not
needed at all; `/run/secrets` export and `APPLICATION_EXTERNAL_CLASSPATH` are
not used.

## Design

### 1. Image and build

`src/main/docker/Dockerfile` is replaced entirely:

```dockerfile
FROM eclipse-temurin:25-jdk@sha256:8c0a84ea11c8f6ed52600fc19f1040121f2a162998e9f50a5faebbbad9172dcc AS builder
WORKDIR /builder
COPY build/libs/*.war application.war
RUN java -Djarmode=tools -jar application.war extract --layers --destination extracted

FROM gcr.io/distroless/java25-debian13:nonroot@sha256:28a3986989d7d74cb5cfd6ba369e64d3d634f83ac9fa562dac8db2d20827f90e
WORKDIR /app
COPY --from=builder /builder/extracted/dependencies/ ./
COPY --from=builder /builder/extracted/spring-boot-loader/ ./
COPY --from=builder /builder/extracted/snapshot-dependencies/ ./
COPY --from=builder /builder/extracted/application/ ./
ENV TZ=Europe/Kiev JDK_JAVA_OPTIONS="-Xms128m -Xmx512m"
EXPOSE 8701
CMD ["application.war"]
```

- The builder stage splits the Spring Boot layered WAR with the `tools`
  jarmode. Verified on the real artifact: `dependencies/lib` is 193 MB and
  changes rarely; `application/application.war` is 458 KB and carries
  `Main-Class` plus a `Class-Path` manifest, so no Spring Boot launcher is
  involved at runtime.
- `ENTRYPOINT` is inherited from the base image (`/usr/bin/java -jar`).
  `WORKDIR /app` makes the relative `CMD` resolve.
- `JDK_JAVA_OPTIONS` is read natively by the `java` launcher and replaces
  both `JAVA_OPTS` and `XMX`. The default keeps parity with the current
  `XMX=512m`. Operations override the whole variable in the env file.
- JVM flags that belong to the application (`--add-opens` for Groovy)
  move into the WAR manifest. In `build.gradle`, `bootWar.manifest` gets
  `"Add-Opens": "java.base/java.lang java.base/java.lang.invoke"`. The `tools`
  extraction preserves custom manifest attributes, and the JVM honours
  `Add-Opens` for `java -jar`; both verified empirically, including inside
  the distroless image.
- Removed: `entrypoint.sh`, apt packages, `locales` (the base image ships
  `LANG=C.UTF-8`), `JHIPSTER_SLEEP` (Consul config retry is already set to
  100 attempts), `SPRING_OUTPUT_ANSI_ENABLED` (ANSI codes in syslog are
  noise), `TERM`, and the JMX remote flags.
- `src/main/docker/.dockerignore` is deleted: it never applied because the
  build context is the repository root. A root `.dockerignore` with two lines
  (`*` and `!build/libs/*.war`) keeps `.git`, `build/classes` and the rest
  out of the context.
- The `docker build` command in `.travis.yml` does not change.

Base image digests resolved on 2026-10-09 (re-resolve and verify when
implementing):

- `gcr.io/distroless/java25-debian13:nonroot@sha256:28a3986989d7d74cb5cfd6ba369e64d3d634f83ac9fa562dac8db2d20827f90e`
- `eclipse-temurin:25-jdk@sha256:8c0a84ea11c8f6ed52600fc19f1040121f2a162998e9f50a5faebbbad9172dcc`

### 2. Runtime hardening (`deploy/docker-compose.yml`)

Four keys are added to `communication-app`; nothing else changes:

```yaml
read_only: true
cap_drop:
    - ALL
volumes:
    - type: tmpfs
      target: /tmp
      tmpfs:
          size: 67108864   # 64 MB: Tomcat work dir + Kafka/Netty native libs
```

- `/tmp` is the only writable path. `docker diff` on a writable run of the
  new image showed exactly two writes: `/tmp/hsperfdata_nonroot` and
  `/tmp/tomcat.8701.*`.
- `/tmp` cannot be `noexec`: zstd-jni, lz4 and Netty extract native
  libraries there and map them executable.
- The tmpfs size cap protects the node from `/tmp` filling memory.
- `user` is not set in compose: UID 65532 is baked into the image and is the
  single source of truth.
- `security_opt: no-new-privileges` is deliberately omitted: `docker stack
  deploy` ignores it with a warning on every deploy, and the image contains
  zero setuid/setgid files, so there is no escalation path for it to block.
  The README mentions the equivalent for non-Swarm runtimes.
- All three keys are supported by Swarm with compose file version 3.8.

`deploy/env/communication-app.env`: the dead `JHIPSTER_SLEEP=0` line is
removed and a commented `# JDK_JAVA_OPTIONS=-Xms128m -Xmx1g` example is added
so operators see where memory tuning lives now.

### 3. CI checks, pinning, documentation

**Trivy scan** in `.travis.yml`, one step between `docker build` and the
tag/push loop:

```yaml
- docker run --rm -v /var/run/docker.sock:/var/run/docker.sock
    -v $HOME/.cache/trivy:/root/.cache/ aquasec/trivy:0.75.0
    image --exit-code 1 --severity CRITICAL,HIGH --ignore-unfixed app-docker-img
```

It scans both the Debian packages of the distroless layer and every jar under
`/app/lib`. The build fails on CRITICAL/HIGH findings that have a fix;
unfixable ones are reported but do not block. `~/.cache/trivy` is added to
Travis `cache.directories`. A `.trivyignore` file is the escape hatch for
accepted risks and is created only when first needed.

**Pinning.** Both `FROM` lines use `tag@sha256:digest`. The distroless digest
is taken after a keyless `cosign verify` (issuer
`https://accounts.google.com`, identity
`keyless@distroless.iam.gserviceaccount.com`); cosign runs from its container
image, so nothing is installed locally. `.github/dependabot.yml` gets a
`docker` ecosystem entry for `/src/main/docker`, weekly, so the digests keep
moving with the tags. Hadolint is not added: a 12-line Dockerfile does not
justify it.

**README.** One new section, "Docker image", about 20 lines: build locally,
run locally with the same hardening flags as Swarm, memory tuning via
`JDK_JAVA_OPTIONS`, troubleshooting without a shell (`docker logs`, temporary
switch to the `debug-nonroot` tag), bumping the base image (cosign verify,
new digest). Nothing else in the README changes.

## Deployment contract changes

| Before | After |
|---|---|
| `JAVA_OPTS`, `XMX` | `JDK_JAVA_OPTIONS` (default `-Xms128m -Xmx512m`) |
| JMX remote on 19999 | removed |
| `/run/secrets/*` exported as env | removed (unused) |
| `APPLICATION_EXTERNAL_CLASSPATH` | removed (unused) |
| `JHIPSTER_SLEEP` | removed (Consul retry covers startup ordering) |
| root user, writable FS | UID 65532, `read_only`, tmpfs `/tmp`, no capabilities |

## Verification

1. Locally: `./gradlew -Pprod bootWar`, `docker build -f src/main/docker/Dockerfile .`,
   `docker image inspect` shows user 65532 and the expected size; running the
   image with `--entrypoint /bin/sh` fails because there is no shell.
2. Start the container with `--read-only --tmpfs /tmp --cap-drop ALL` against
   a dev environment that has Consul and xm-ms-config, or deploy the updated
   compose to the dev/stage Swarm: health is UP, a test email/SMS goes
   through, logs contain no `Read-only file system` errors. The application
   cannot start fully offline because `xm-config` is mandatory and resolves
   the config service through Consul, so this step needs a real environment.
3. A PR build in Travis is green: image build plus Trivy scan.

## Evidence from the validation spike (2026-10-09, throwaway, deleted)

| Metric | Current image | New image |
|---|---|---|
| Uncompressed size (arm64) | 861 MB | 437 MB |
| Debian packages | 133 | 24 |
| setuid/setgid files | 11 | 0 |
| Shells / package managers | bash, sh, dash, apt, dpkg, curl, zip, unzip | none |
| Executables outside the JRE | many | none |
| Layer rewritten per release | 193 MB | 458 KB |

Filesystem writes observed on a writable run: `/tmp/hsperfdata_nonroot`,
`/tmp/tomcat.8701.*`. Read-only run with tmpfs `/tmp` and no capabilities
reached the same startup point with no filesystem errors.
