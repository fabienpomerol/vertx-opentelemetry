# OpenTelemetry integration for Vert.x 4

[![CircleCI](https://circleci.com/gh/fabienpomerol/vertx-opentelemetry.svg?style=shield)](https://app.circleci.com/pipelines/github/fabienpomerol/vertx-opentelemetry)

Based on the OpenTelemetry Java SDK : https://github.com/open-telemetry/opentelemetry-java and inspired by https://github.com/eclipse-vertx/vertx-tracing

Supported exporter :

| Exporters                   |
| --------------------------- |
| Jaeger                      |
| Logging                     |
| In Memory                   |


## Usage

Add the following dependency to your pom.xml

```xml
<repositories>
  <repository>
    <id>oss.sonatype.org-snapshot</id>
    <url>https://oss.sonatype.org/content/repositories/snapshots</url>
  </repository>
</repositories>

<dependencies>
  ...
  <dependency>
    <groupId>io.github.fabienpomerol</groupId>
    <artifactId>vertx-opentelemetry</artifactId>
    <version>1.0.0-SNAPSHOT</version>
  </dependency>
  ...
</dependencies>
```

```java
Vertx vertx = Vertx.vertx(new VertxOptions()
  .setTracingOptions(
    new OpenTelemetryOptions()
      .addExporter(JaegerExporter.newBuilder()
        .setServiceName("MyService")
        .setIp("127.0.0.1")
        .setPort(14254)
        .setDeadline(1_000)
        .build()
      )
      .addExporter(LoggingExporter
        .newBuilder()
        .build()
      )
      .setEnabled(true)
  )
);
```

### How to use Jaeger as backend

Start a Jaeger instance with the following command

```bash
docker run -d --name jaeger \
  -e COLLECTOR_ZIPKIN_HTTP_PORT=9411 \
  -p 5775:5775/udp \
  -p 6831:6831/udp \
  -p 6832:6832/udp \
  -p 5778:5778 \
  -p 16686:16686 \
  -p 14268:14268 \
  -p 14254:14254 \
  -p 9411:9411 \
  jaegertracing/all-in-one
```

The Jaeger UI will be accessible at http://localhost:16686/

![image](https://user-images.githubusercontent.com/496277/79852973-bb295480-83c7-11ea-96ae-eda43faee5e5.png)

## Todo

- Add zipkin Exporter
