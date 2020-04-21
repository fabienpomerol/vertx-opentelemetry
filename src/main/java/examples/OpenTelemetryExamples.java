package examples;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.github.fabienpomerol.vertx.tracing.opentelemetry.OpenTelemetryOptions;
import io.github.fabienpomerol.vertx.tracing.opentelemetry.exporters.JaegerExporter;
import io.github.fabienpomerol.vertx.tracing.opentelemetry.exporters.LoggingExporter;

public class OpenTelemetryExamples {

  public void ex1() {
    Vertx vertx = Vertx.vertx(new VertxOptions()
      .setTracingOptions(
        new OpenTelemetryOptions()
          .addExporter(JaegerExporter.newBuilder()
            .setServiceName("MyService")
            .setDeadline(1_000)
            .build()
          )
          .setEnabled(true)
      )
    );
  }

  public void ex2() {
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
  }
}
