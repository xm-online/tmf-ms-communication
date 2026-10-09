[![Build Status](https://travis-ci.org/xm-online/tmf-ms-communication.svg?branch=master)](https://travis-ci.org/xm-online/tmf-ms-communication) [![Quality Gate](https://sonarcloud.io/api/project_badges/measure?&metric=sqale_index&branch=master&project=xm-online:tmf-ms-communication)](https://sonarcloud.io/dashboard/index/xm-online:tmf-ms-communication) [![Quality Gate](https://sonarcloud.io/api/project_badges/measure?&metric=ncloc&branch=master&project=xm-online:tmf-ms-communication)](https://sonarcloud.io/dashboard/index/xm-online:tmf-ms-communication) [![Quality Gate](https://sonarcloud.io/api/project_badges/measure?&metric=coverage&branch=master&project=xm-online:tmf-ms-communication)](https://sonarcloud.io/dashboard/index/xm-online:tmf-ms-communication)

# communication

This application was generated using JHipster 6.5.1, you can find documentation and help at [https://www.jhipster.tech/documentation-archive/v6.5.1](https://www.jhipster.tech/documentation-archive/v6.5.1).

This is a "microservice" application intended to be part of a microservice architecture, please refer to the [Doing microservices with JHipster][] page of the documentation for more information.

This application is configured for Service Discovery and Configuration with Consul. On launch, it will refuse to start if it is not able to connect to Consul at [http://localhost:8500](http://localhost:8500). For more information, read our documentation on [Service Discovery and Configuration with Consul][].

## Development

To start your application in the dev profile, simply run:

    ./gradlew

For further instructions on how to develop with JHipster, have a look at [Using JHipster in development][].

### Doing API-First development using openapi-generator

[OpenAPI-Generator]() is configured for this application. You can generate API code from the `src/main/resources/swagger/api.yml` definition file by running:

```bash
./gradlew openApiGenerate
```

Then implements the generated delegate classes with `@Service` classes.

To edit the `api.yml` definition file, you can use a tool such as [Swagger-Editor](). Start a local instance of the swagger-editor using docker by running: `docker-compose -f src/main/docker/swagger-editor.yml up -d`. The editor will then be reachable at [http://localhost:7742](http://localhost:7742).

Refer to [Doing API-First development][] for more details.

## Building for production

### Packaging as jar

To build the final jar and optimize the communication application for production, run:

    ./gradlew -Pprod clean bootJar

To ensure everything worked, run:

    java -jar build/libs/*.jar

Refer to [Using JHipster in production][] for more details.

### Packaging as war

To package your application as a war in order to deploy it to an application server, run:

    ./gradlew -Pprod -Pwar clean bootWar

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

## Testing

To launch your application's tests, run:

    ./gradlew test integrationTest jacocoTestReport

For more information, refer to the [Running tests page][].

### Code quality

Sonar is used to analyse code quality. You can start a local Sonar server (accessible on http://localhost:9001) with:

```
docker-compose -f src/main/docker/sonar.yml up -d
```

You can run a Sonar analysis with using the [sonar-scanner](https://docs.sonarqube.org/display/SCAN/Analyzing+with+SonarQube+Scanner) or by using the gradle plugin.

Then, run a Sonar analysis:

```
./gradlew -Pprod clean check jacocoTestReport sonarqube
```

For more information, refer to the [Code quality page][].

## Using Docker to simplify development (optional)

You can use Docker to improve your JHipster development experience. A number of docker-compose configuration are available in the [src/main/docker](src/main/docker) folder to launch required third party services.

You can also fully dockerize your application and all the services that it depends on.
To achieve this, first build a docker image of your app by running:

    ./gradlew bootJar -Pprod jibDockerBuild

Then run:

    docker-compose -f src/main/docker/app.yml up -d

For more information refer to [Using Docker and Docker-Compose][], this page also contains information on the docker-compose sub-generator (`jhipster docker-compose`), which is able to generate docker configurations for one or several JHipster applications.

## Continuous Integration (optional)

To configure CI for your project, run the ci-cd sub-generator (`jhipster ci-cd`), this will let you generate configuration files for a number of Continuous Integration systems. Consult the [Setting up Continuous Integration][] page for more information.

[jhipster homepage and latest documentation]: https://www.jhipster.tech
[jhipster 6.5.1 archive]: https://www.jhipster.tech/documentation-archive/v6.5.1
[doing microservices with jhipster]: https://www.jhipster.tech/documentation-archive/v6.5.1/microservices-architecture/
[using jhipster in development]: https://www.jhipster.tech/documentation-archive/v6.5.1/development/
[service discovery and configuration with consul]: https://www.jhipster.tech/documentation-archive/v6.5.1/microservices-architecture/#consul
[using docker and docker-compose]: https://www.jhipster.tech/documentation-archive/v6.5.1/docker-compose
[using jhipster in production]: https://www.jhipster.tech/documentation-archive/v6.5.1/production/
[running tests page]: https://www.jhipster.tech/documentation-archive/v6.5.1/running-tests/
[code quality page]: https://www.jhipster.tech/documentation-archive/v6.5.1/code-quality/
[setting up continuous integration]: https://www.jhipster.tech/documentation-archive/v6.5.1/setting-up-ci/
[openapi-generator]: https://openapi-generator.tech
[swagger-editor]: https://editor.swagger.io
[doing api-first development]: https://www.jhipster.tech/documentation-archive/v6.5.1/doing-api-first-development/
