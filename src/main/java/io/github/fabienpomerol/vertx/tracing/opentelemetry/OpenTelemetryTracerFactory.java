package io.github.fabienpomerol.vertx.tracing.opentelemetry;

import io.vertx.core.json.JsonObject;
import io.vertx.core.spi.VertxTracerFactory;
import io.vertx.core.spi.tracing.VertxTracer;
import io.vertx.core.tracing.TracingOptions;

public class OpenTelemetryTracerFactory implements VertxTracerFactory {

  @Override
  public VertxTracer tracer(TracingOptions options) {
    OpenTelemetryOptions openTelemetryOptions;
    if (options instanceof OpenTelemetryOptions) {
      openTelemetryOptions = (OpenTelemetryOptions) options;
    } else {
      openTelemetryOptions = new OpenTelemetryOptions();
    }
    return openTelemetryOptions.buildTracer();
  }

  @Override
  public OpenTelemetryOptions newOptions() {
    return new OpenTelemetryOptions();
  }

  @Override
  public OpenTelemetryOptions newOptions(JsonObject jsonObject) {
    return new OpenTelemetryOptions(jsonObject);
  }
}
