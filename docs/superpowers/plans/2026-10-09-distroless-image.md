# Distroless Container Image Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ship `tmf-ms-communication` as a distroless, non-root, read-only container image with a CVE gate in CI, replacing the bash-entrypoint Ubuntu image.

**Architecture:** A two-stage Dockerfile splits the Spring Boot layered WAR in a Temurin JDK builder and copies the layers onto `gcr.io/distroless/java25-debian13:nonroot`, whose stock `java -jar` entrypoint runs the thin application WAR. JVM flags the app needs live in the WAR manifest; operators tune memory through `JDK_JAVA_OPTIONS`. Swarm compose adds `read_only`, `cap_drop: ALL` and a size-capped tmpfs `/tmp`. Travis gains one Trivy step; Dependabot keeps base-image digests fresh.

**Tech Stack:** Java 25 (Temurin), Spring Boot 4.0.8 (`tools` jarmode), Docker BuildKit, Docker Swarm compose 3.8, Travis CI, Trivy 0.75.0, cosign v2.6.0 (run from its container), Dependabot.

**Spec:** `docs/superpowers/specs/2026-10-09-distroless-image-design.md`

## Global Constraints

- Runtime base: `gcr.io/distroless/java25-debian13:nonroot@sha256:28a3986989d7d74cb5cfd6ba369e64d3d634f83ac9fa562dac8db2d20827f90e` (re-resolve and cosign-verify in Task 3; use the fresh digest if it moved).
- Builder base: `eclipse-temurin:25-jdk@sha256:8c0a84ea11c8f6ed52600fc19f1040121f2a162998e9f50a5faebbbad9172dcc` (re-resolve in Task 3).
- No entrypoint scripts, no `RUN apt-get`, no shell in the runtime image, no new runtime components.
- Process runs as UID 65532 (from the image), port 8701.
- Deployment contract: exactly one tuning variable, `JDK_JAVA_OPTIONS`, default `-Xms128m -Xmx512m`. `JAVA_OPTS`, `XMX`, `JHIPSTER_SLEEP`, JMX remote, `/run/secrets` export and `APPLICATION_EXTERNAL_CLASSPATH` are removed, not emulated.
- Compose stays version `3.8`; only keys that `docker stack deploy` honours: `read_only`, `cap_drop`, long-syntax tmpfs volume with `size: 67108864`.
- Trivy gate: `--exit-code 1 --severity CRITICAL,HIGH --ignore-unfixed --scanners vuln`, image `aquasec/trivy:0.75.0`.
- The `docker build` line in `.travis.yml` does not change.
- Out of scope: Jib config in `gradle/docker.gradle`, `jibDockerBuild` mention in README, sibling services, CI migration off Travis.
- Git: commit after every task with the messages given; no `Co-Authored-By` / `Claude-Session` trailers; never push (the user decides when).
- Working branch: `feature/distroless-image` (already exists, holds the spec and this plan).

## Review Focus

1. An operator overrides `JDK_JAVA_OPTIONS` with just `-Xmx1g`: the heap must be exactly 1 GiB and nothing the app needs may be lost with the default. Pinned in Task 3, Step 6.
2. Ukrainian and other non-ASCII text in logs and e-mail templates must stay UTF-8 now that the `locales` package is gone. Pinned in Task 3, Step 6.
3. `TZ=Europe/Kiev` is a legacy alias of `Europe/Kyiv`; Java 25 must still resolve it so log timestamps and scheduled sends stay in Kyiv time. Pinned in Task 3, Step 6.
4. Read-only root FS with a 64 MB tmpfs must absorb the Tomcat work dir and the native libraries Kafka/Netty extract: no `Read-only file system`, no `No space left on device`. Pinned in Task 4, Step 4.
5. An older Swarm engine silently drops `cap_drop`/tmpfs; the compose must parse on the local CLI without any `Ignoring unsupported options` warning. Pinned in Task 4, Step 3.

---

### Task 1: Bump FreeMarker to 2.3.35 (the CI gate would fail on the current version)

A baseline Trivy scan of the new image on 2026-10-09 found exactly one fixable CRITICAL: `org.freemarker:freemarker 2.3.34`, CVE-2026-84939 (path traversal via malformed locale identifier), fixed in 2.3.35. The OS layer and every other jar were clean. Fixing it first means every later build already carries the fix.

**Files:**
- Modify: `gradle.properties:59` (`freemarker_version=2.3.34`)

**Interfaces:**
- Consumes: nothing.
- Produces: runtime classpath with `org.freemarker:freemarker:2.3.35`; Task 2 builds the WAR from it.

- [ ] **Step 1: Confirm the current resolved version (the failing check)**

Run:
```bash
./gradlew -q dependencies --configuration runtimeClasspath | grep -m1 -oE 'org.freemarker:freemarker:[0-9.]+'
```
Expected: `org.freemarker:freemarker:2.3.34`

- [ ] **Step 2: Bump the version property**

In `gradle.properties`, replace line 59:
```properties
freemarker_version=2.3.34
```
with:
```properties
freemarker_version=2.3.35
```

- [ ] **Step 3: Confirm resolution and run the FreeMarker-related tests**

Run:
```bash
./gradlew -q dependencies --configuration runtimeClasspath | grep -m1 -oE 'org.freemarker:freemarker:[0-9.]+'
./gradlew runCategorizedTests --no-daemon \
  --tests '*EmailTemplateControllerIntTest' --tests '*EmailTemplateServiceUnitTest' \
  --tests '*MailServiceUnitTest' --tests '*MessageTemplateConfigurationServiceUnitTest' \
  --tests '*TenantManagerConfigurationUnitTest' --tests '*TwilioMessageTemplateServiceUnitTest' \
  --tests '*TwilioServiceUnitTest' 2>&1 | grep -E 'BUILD|tests completed|FAILED' | tail -3
```
Expected: `org.freemarker:freemarker:2.3.35`, then `BUILD SUCCESSFUL` with no `FAILED` lines. (The full suite runs in Travis on the PR.)

- [ ] **Step 4: Commit**

```bash
git add gradle.properties
git commit -m "Bump FreeMarker to 2.3.35 (CVE-2026-84939)"
```

---

### Task 2: Declare `Add-Opens` in the WAR manifest

The current image passes `--add-opens=java.base/java.lang=ALL-UNNAMED --add-opens=java.base/java.lang.invoke=ALL-UNNAMED` through `JAVA_OPTS` for Groovy (LEP scripts). The JVM honours an `Add-Opens` main-manifest attribute for `java -jar`, and the Spring Boot `tools` extraction keeps custom manifest attributes in the thin application WAR, so the flags move into the artifact.

**Files:**
- Modify: `build.gradle:121-128` (the `bootWar { ... }` block)

**Interfaces:**
- Consumes: Task 1's dependency bump (it is picked up by the build automatically).
- Produces: `build/libs/tmf-ms-communication-<version>.war` whose `META-INF/MANIFEST.MF` contains `Add-Opens: java.base/java.lang java.base/java.lang.invoke`. Task 3 copies this WAR into the image.

- [ ] **Step 1: Build the WAR and confirm the attribute is absent (the failing check)**

Run:
```bash
./gradlew -x test -Pprod clean bootWar -q
unzip -p build/libs/*.war META-INF/MANIFEST.MF | grep -c '^Add-Opens'
```
Expected: the build succeeds (takes a few minutes), `grep -c` prints `0`.

- [ ] **Step 2: Add the attribute to the `bootWar` manifest**

In `build.gradle`, replace lines 121-128:
```groovy
bootWar {
    mainClass = 'com.icthh.xm.tmf.ms.communication.CommunicationApp'
    manifest {
        attributes(
            "Implementation-Version": archiveVersion
        )
    }
}
```
with:
```groovy
bootWar {
    mainClass = 'com.icthh.xm.tmf.ms.communication.CommunicationApp'
    manifest {
        attributes(
            "Implementation-Version": archiveVersion,
            // Groovy (LEP) needs deep reflection into java.lang; the JVM honours this
            // manifest attribute for `java -jar`, so no --add-opens flags are needed at runtime.
            "Add-Opens": "java.base/java.lang java.base/java.lang.invoke"
        )
    }
}
```

- [ ] **Step 3: Rebuild and confirm the attribute is present**

Run:
```bash
./gradlew -x test -Pprod clean bootWar -q
unzip -p build/libs/*.war META-INF/MANIFEST.MF | grep '^Add-Opens'
```
Expected: `Add-Opens: java.base/java.lang java.base/java.lang.invoke`

- [ ] **Step 4: Confirm the thin application WAR produced by the `tools` extraction keeps it**

Run:
```bash
rm -rf build/extracted
java -Djarmode=tools -jar build/libs/*.war extract --layers --destination build/extracted
unzip -p build/extracted/application/*.war META-INF/MANIFEST.MF | grep -E '^(Add-Opens|Main-Class)'
```
Expected:
```
Add-Opens: java.base/java.lang java.base/java.lang.invoke
Main-Class: com.icthh.xm.tmf.ms.communication.CommunicationApp
```

- [ ] **Step 5: Commit**

```bash
git add build.gradle
git commit -m "Declare Add-Opens in WAR manifest instead of JVM flags"
```

---

### Task 3: Distroless multi-stage Dockerfile

**Files:**
- Create: `src/main/docker/Dockerfile` (full replacement of the existing file)
- Create: `.dockerignore` (repository root)
- Delete: `src/main/docker/entrypoint.sh`
- Delete: `src/main/docker/.dockerignore` (never applied: the build context is the repo root)

**Interfaces:**
- Consumes: `build/libs/*.war` from Task 2 (exactly one file; run `./gradlew clean` first if an older WAR is lying around).
- Produces: local image tag `communication:distroless`, user `65532`, entrypoint `/usr/bin/java -jar`, cmd `application.war`, workdir `/app`. Tasks 4-7 run this tag.

- [ ] **Step 1: Verify the base image signature and resolve current digests**

Run:
```bash
docker run --rm ghcr.io/sigstore/cosign/cosign:v2.6.0 verify \
  gcr.io/distroless/java25-debian13:nonroot \
  --certificate-oidc-issuer https://accounts.google.com \
  --certificate-identity keyless@distroless.iam.gserviceaccount.com >/dev/null
echo "cosign exit=$?"
docker buildx imagetools inspect gcr.io/distroless/java25-debian13:nonroot --format '{{.Manifest.Digest}}'
docker buildx imagetools inspect eclipse-temurin:25-jdk --format '{{.Manifest.Digest}}'
```
Expected: `cosign exit=0` (stderr shows "The following checks were performed on each of these signatures"), then two `sha256:...` lines. If they differ from the Global Constraints, use the printed values in Step 2.

- [ ] **Step 2: Write the Dockerfile, the root `.dockerignore`, and remove the old files**

`src/main/docker/Dockerfile` (replace the whole file; substitute the digests from Step 1 if they moved):
```dockerfile
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
```

`.dockerignore` at the repository root (new file):
```
*
!build/libs/*.war
```

Remove the old files:
```bash
git rm -q src/main/docker/entrypoint.sh src/main/docker/.dockerignore
```

- [ ] **Step 3: Build the image and confirm the context is only the WAR**

Run:
```bash
docker build --progress=plain -t communication:distroless -f src/main/docker/Dockerfile . 2>&1 \
  | grep -E "transferring context:.*done|ERROR|naming to" | tail -3
```
Expected: a `transferring context: ~200MB` line (the WAR alone, not the repository), no `ERROR`, ends with `naming to docker.io/library/communication:distroless`.

- [ ] **Step 4: Confirm image identity, no shell, and layer sizes**

Run:
```bash
docker image inspect communication:distroless \
  --format 'user={{.Config.User}} entrypoint={{json .Config.Entrypoint}} cmd={{json .Config.Cmd}} workdir={{.Config.WorkingDir}}'
docker run --rm --entrypoint /bin/sh communication:distroless -c true; echo "shell exit=$?"
docker history communication:distroless --format '{{.Size}}	{{.CreatedBy}}' | head -5
```
Expected:
```
user=65532 entrypoint=["/usr/bin/java","-jar"] cmd=["application.war"] workdir=/app
```
then an error containing `/bin/sh: no such file or directory` with a non-zero `shell exit`, then a history where the `application/` COPY layer is under 1 MB and the `dependencies/` COPY layer is about 200 MB.

- [ ] **Step 5: Smoke-run: the JVM starts, picks up the default options, and gets as far as Consul**

The service cannot start without the XM platform (Consul + xm-ms-config), so the expected outcome is a Consul connection error, which proves the classpath, manifest and launcher are correct. The container exits by itself.

Run:
```bash
docker run --rm -e SPRING_PROFILES_ACTIVE=dev communication:distroless 2>&1 \
  | grep -m2 -E "Picked up JDK_JAVA_OPTIONS|localhost:8500" | cut -c1-120
```
Expected:
```
NOTE: Picked up JDK_JAVA_OPTIONS: -Xms128m -Xmx512m
org.springframework.web.client.ResourceAccessException: I/O error on GET request for "http://localhost:8500/...
```

- [ ] **Step 6: Review-focus checks: heap override, UTF-8, timezone**

Run:
```bash
# 1. operator override of JDK_JAVA_OPTIONS
docker run --rm -e JDK_JAVA_OPTIONS="-Xmx1g" --entrypoint java communication:distroless \
  -XX:+PrintFlagsFinal -version 2>/dev/null | grep -E '^\s*size_t MaxHeapSize'
# 2. encodings without the locales package
docker run --rm --entrypoint java communication:distroless -XshowSettings:properties -version 2>&1 \
  | grep -E '^\s*(file|stdout|sun\.jnu)\.encoding'
# 3. TZ=Europe/Kiev resolves: first log timestamp hour equals Kyiv hour on the host
docker run --rm -e SPRING_PROFILES_ACTIVE=dev communication:distroless 2>&1 \
  | grep -m1 -oE '^[0-9]{4}-[0-9]{2}-[0-9]{2} [0-9]{2}'
TZ=Europe/Kiev date '+%Y-%m-%d %H'
```
Expected:
```
size_t MaxHeapSize = 1073741824 {product} {command line}
file.encoding = UTF-8
stdout.encoding = UTF-8
sun.jnu.encoding = UTF-8
<date> <HH>
<the same date and HH>
```
(whitespace in the first line varies; the value `1073741824` and `{command line}` are what matter).

- [ ] **Step 7: Commit**

```bash
git add .dockerignore src/main/docker/Dockerfile
git commit -m "Build distroless, non-root, layered container image"
```
(`git rm` in Step 2 already staged the two deletions.)

---

### Task 4: Read-only, capability-free Swarm service

**Files:**
- Modify: `deploy/docker-compose.yml` (service `communication-app`)
- Modify: `deploy/env/communication-app.env:4` (`JHIPSTER_SLEEP=0`)

**Interfaces:**
- Consumes: image `communication:distroless` from Task 3 for the local run in Step 4.
- Produces: compose keys `read_only`, `cap_drop`, tmpfs volume; env file documents `JDK_JAVA_OPTIONS`. Task 6 (README) refers to these flags.

- [ ] **Step 1: Confirm the hardening keys are absent (the failing check)**

Run:
```bash
docker stack config -c deploy/docker-compose.yml | grep -cE 'read_only|cap_drop|tmpfs'
```
Expected: `0`

- [ ] **Step 2: Add the keys to the service and clean the env file**

`deploy/docker-compose.yml` becomes:
```yaml
version: '3.8'
services:
    communication-app:
        image: xmonline/tmf-ms-communication:${IMAGE_TMF_MS_COMMUNICATION_TAG:-latest}
        # The image has no shell, runs as UID 65532 and writes only to /tmp.
        read_only: true
        cap_drop:
            - ALL
        volumes:
            - type: tmpfs
              target: /tmp
              tmpfs:
                  size: 67108864   # 64 MB: Tomcat work dir + Kafka/Netty native libs
        networks:
            - xm2
        env_file:
            - ./env/communication-app.env
        deploy:
            mode: replicated
            replicas: 1
            restart_policy:
                condition: on-failure
        logging:
            driver: syslog
            options:
                tag: communication
                syslog-facility: local7

networks:
    xm2:
        driver: overlay
```

`deploy/env/communication-app.env`: delete line 4 (`JHIPSTER_SLEEP=0`) and append at the end:
```
# JVM tuning (replaces the former JAVA_OPTS and XMX); image default is -Xms128m -Xmx512m
# JDK_JAVA_OPTIONS=-Xms128m -Xmx1g
```

- [ ] **Step 3: Confirm the Swarm CLI accepts every key without dropping any**

Run:
```bash
docker stack config -c deploy/docker-compose.yml 2>&1 | grep -E 'Ignoring|read_only|cap_drop|ALL|type: tmpfs|target: /tmp|size: 67108864'
```
Expected (order may differ), and no line containing `Ignoring`:
```
    read_only: true
    cap_drop:
    - ALL
      type: tmpfs
      target: /tmp
        size: 67108864
```

- [ ] **Step 4: Run the image with the same restrictions and confirm no filesystem errors**

Run:
```bash
docker run --rm --read-only --tmpfs /tmp:size=64m --cap-drop ALL \
  --env-file deploy/env/communication-app.env -e SPRING_PROFILES_ACTIVE=dev \
  communication:distroless > /tmp/ro-run.log 2>&1
grep -ciE 'read-only file system|no space left|AccessDeniedException|Permission denied' /tmp/ro-run.log
grep -m1 -oE 'http://consul:8500[^"]*' /tmp/ro-run.log
```
Expected: `0` (no filesystem errors), then a `http://consul:8500/...` URL (the app got as far as Consul, past Tomcat initialisation which writes its work dir under `/tmp`).

- [ ] **Step 5: Commit**

```bash
git add deploy/docker-compose.yml deploy/env/communication-app.env
git commit -m "Run container read-only, without capabilities, with tmpfs /tmp"
```

---

### Task 5: Trivy gate in Travis and Dependabot for base-image digests

**Files:**
- Modify: `.travis.yml:33-34` (insert one script step after the `docker build` line) and `:40-43` (cache directories)
- Create: `.github/dependabot.yml`
- Create only if Step 1 needs it: `.trivyignore`

**Interfaces:**
- Consumes: image `communication:distroless` from Task 3 (built from the WAR that already carries Task 1's FreeMarker fix).
- Produces: CI fails on fixable CRITICAL/HIGH CVEs in OS packages or jars; weekly Dependabot PRs for `src/main/docker/Dockerfile` digests.

- [ ] **Step 1: Run the exact CI scan locally and confirm it is clean**

Run:
```bash
mkdir -p "$HOME/.cache/trivy"
docker run --rm -v /var/run/docker.sock:/var/run/docker.sock -v "$HOME/.cache/trivy:/root/.cache/" \
  aquasec/trivy:0.75.0 image --exit-code 1 --severity CRITICAL,HIGH --ignore-unfixed --scanners vuln \
  communication:distroless; echo "trivy exit=$?"
```
Expected: `trivy exit=0`, with `Total: 0 (HIGH: 0, CRITICAL: 0)` for the OS layer and for `Java (jar)`. (On 2026-10-09 the only finding was FreeMarker, fixed in Task 1.)

If `trivy exit=1` because the vulnerability database has moved on, triage each row of the findings table before continuing:
- The row names a jar whose version is managed in `gradle.properties` (the `# Security overrides` block) or `build.gradle`: bump to the `Fixed Version` shown, run `./gradlew -x test -Pprod clean bootWar -q`, rebuild the image (Task 3 Step 3), and rescan. Commit the bump separately: `git commit -m "Bump <artifact> to <version> (CVE-...)"`.
- The bump is not possible in this PR (breaks compilation, needs a Spring Boot upgrade): create `.trivyignore` at the repo root with one line per CVE, `CVE-XXXX-NNNNN  # <artifact>: <one-line reason and ticket>`, and add `-v $PWD/.trivyignore:/.trivyignore:ro` to the `docker run` in Step 2 so CI uses it. Mention each ignored CVE in the PR description.

- [ ] **Step 2: Add the scan step and the cache directory to `.travis.yml`**

After line 33 (the `docker build ...` item) and before `- for TAG in $TAGS;`, insert:
```yaml
  - docker run --rm -v /var/run/docker.sock:/var/run/docker.sock -v $HOME/.cache/trivy:/root/.cache/ aquasec/trivy:0.75.0 image --exit-code 1 --severity CRITICAL,HIGH --ignore-unfixed --scanners vuln app-docker-img
```
In the `cache.directories` list (lines 41-43) add after `- "~/.m2"`:
```yaml
  - "~/.cache/trivy"
```

- [ ] **Step 3: Validate the YAML and the step position**

Run:
```bash
ruby -ryaml -e 'y = YAML.load_file(".travis.yml"); s = y["script"]; puts s.length; puts s.index { |l| l.start_with?("docker run --rm -v /var/run/docker.sock") } - s.index { |l| l.start_with?("docker build") }; puts y["cache"]["directories"].inspect'
```
Expected:
```
11
1
["~/.gradle", "~/.m2", "~/.cache/trivy"]
```
(11 script items, the Trivy step immediately after `docker build`, three cache dirs.)

- [ ] **Step 4: Add Dependabot for Docker digests**

`.github/dependabot.yml` (new file):
```yaml
version: 2
updates:
  - package-ecosystem: "docker"
    directory: "/src/main/docker"
    schedule:
      interval: "weekly"
```
Validate:
```bash
ruby -ryaml -e 'y = YAML.load_file(".github/dependabot.yml"); puts y["updates"][0]["package-ecosystem"], y["updates"][0]["directory"]'
```
Expected:
```
docker
/src/main/docker
```

- [ ] **Step 5: Commit**

```bash
git add .travis.yml .github/dependabot.yml
git commit -m "Scan image with Trivy in CI and keep base image digests updated"
```
(add `.trivyignore` to the `git add` if Step 1 created it.)

---

### Task 6: README section "Docker image"

**Files:**
- Modify: `README.md` (insert a new section immediately before line 53, `## Testing`; nothing else changes)

**Interfaces:**
- Consumes: Dockerfile from Task 3, compose flags from Task 4, cosign/digest commands from Task 3 Step 1.
- Produces: operator documentation.

- [ ] **Step 1: Insert the section**

Insert the following before the `## Testing` heading (keep one blank line before and after):
````markdown
## Docker image

The image is built from `src/main/docker/Dockerfile` on top of
`gcr.io/distroless/java25-debian13:nonroot`: no shell, no package manager,
runs as UID 65532, and writes only to `/tmp` at runtime.

Build locally (`clean` first, so `build/libs` holds a single WAR):

```bash
./gradlew -Pprod clean bootWar
docker build -t communication -f src/main/docker/Dockerfile .
```

Run locally with the same hardening as the Swarm stack (`deploy/docker-compose.yml`):

```bash
docker run --rm --read-only --tmpfs /tmp:size=64m --cap-drop ALL \
  --env-file deploy/env/communication-app.env -p 8701:8701 communication
```

JVM tuning goes through `JDK_JAVA_OPTIONS` (image default `-Xms128m -Xmx512m`);
it replaces the former `JAVA_OPTS` and `XMX`. JMX remote is no longer enabled.

Troubleshooting: there is no shell in the image, use `docker logs`. If you
must look inside a container, build a one-off image with the base tag changed
to `debug-nonroot` (adds busybox) and never publish it.

Bumping the base image: verify the signature, then put the new digest into
the Dockerfile. Dependabot also opens weekly PRs for digest updates.

```bash
docker run --rm ghcr.io/sigstore/cosign/cosign:v2.6.0 verify \
  gcr.io/distroless/java25-debian13:nonroot \
  --certificate-oidc-issuer https://accounts.google.com \
  --certificate-identity keyless@distroless.iam.gserviceaccount.com
docker buildx imagetools inspect gcr.io/distroless/java25-debian13:nonroot --format '{{.Manifest.Digest}}'
```

Outside Swarm add `no-new-privileges` (Docker) or `allowPrivilegeEscalation: false`
(Kubernetes); Swarm ignores that option, and the image has no setuid binaries.
Swarm needs Docker 20.10+ for `cap_drop`.
````

- [ ] **Step 2: Confirm placement and that the documented run command works**

Run:
```bash
grep -n -E '^## (Docker image|Testing)' README.md
docker run --rm --read-only --tmpfs /tmp:size=64m --cap-drop ALL \
  --env-file deploy/env/communication-app.env -p 8701:8701 -e SPRING_PROFILES_ACTIVE=dev \
  communication:distroless 2>&1 | grep -m1 -oE 'http://consul:8500[^"]*'
```
Expected: `## Docker image` printed on a line number smaller than `## Testing`; then a `http://consul:8500/...` URL (same stopping point as Task 4 Step 4).

- [ ] **Step 3: Commit**

```bash
git add README.md
git commit -m "Document the distroless image, hardening flags and base image bump"
```

---

### Task 7: End-to-end verification on a real environment and handoff

The application needs Consul and xm-ms-config to start, so the final proof runs against the dev platform. Pick whichever access you have; both are acceptable.

**Files:** none.

**Interfaces:**
- Consumes: image `communication:distroless` (local) or the image Travis pushes after the PR merges; compose from Task 4.
- Produces: evidence for the PR description.

- [ ] **Step 1: Local image against dev Consul (if the dev Consul is reachable from your machine)**

Run (replace `<dev-consul-host>`):
```bash
docker run --rm --read-only --tmpfs /tmp:size=64m --cap-drop ALL -p 8701:8701 \
  --env-file deploy/env/communication-app.env -e SPRING_CLOUD_CONSUL_HOST=<dev-consul-host> \
  communication:distroless > /tmp/e2e.log 2>&1 &
sleep 90
curl -s -o /dev/null -w 'health HTTP %{http_code}\n' http://localhost:8701/management/health
grep -ciE 'read-only file system|no space left|AccessDeniedException' /tmp/e2e.log
grep -m1 "Application 'communication' is running" /tmp/e2e.log | cut -c1-80
```
Expected: `health HTTP 200`, `0`, and the "is running" line. Then send one real message through the API (e-mail or SMS, whichever the dev tenant has configured) and confirm it is delivered: this exercises Groovy/LEP (the `Add-Opens` path) and Kafka native libraries in `/tmp`.

- [ ] **Step 2: Or: deploy the stack on the dev Swarm after the image is published**

After the PR is merged and Travis has pushed the image:
```bash
IMAGE_TMF_MS_COMMUNICATION_TAG=<version-build> docker stack deploy -c deploy/docker-compose.yml xm
docker service logs --since 5m xm_communication-app 2>&1 | grep -ciE 'read-only file system|no space left|AccessDeniedException'
docker service ps xm_communication-app --no-trunc | head -3
```
Expected: `0`, the task in `Running` state, Consul shows the service healthy, and one real message is delivered as in Step 1.

- [ ] **Step 3: Hand the branch over**

Use the `superpowers:finishing-a-development-branch` skill. Do not push: the user decides when the branch is pushed and the PR opened. The PR description should list the deployment contract changes table from the spec, the FreeMarker bump from Task 1, and any `.trivyignore` entries from Task 5.
