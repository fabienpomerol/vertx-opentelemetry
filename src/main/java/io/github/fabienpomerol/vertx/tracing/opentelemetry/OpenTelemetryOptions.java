package io.github.fabienpomerol.vertx.tracing.opentelemetry;

import io.opentelemetry.trace.Tracer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.spi.tracing.VertxTracer;
import io.vertx.core.tracing.TracingOptions;
import io.github.fabienpomerol.vertx.tracing.opentelemetry.exporters.BackendExporter;

import java.util.ArrayList;
import java.util.List;

public class OpenTelemetryOptions extends TracingOptions {

  private Tracer tracer;

  private List<BackendExporter> exporters = new ArrayList<>();

  public OpenTelemetryOptions(Tracer tracer) {
    this.tracer = tracer;
  }

  public OpenTelemetryOptions() {
  }

  public OpenTelemetryOptions(JsonObject json) {
    super(json);
  }

  public OpenTelemetryOptions addExporter(BackendExporter exporter) {
    this.exporters.add(exporter);
    return this;
  }

  VertxTracer<?, ?> buildTracer() {
    if (tracer != null) {
      return new OpenTelemetryTracer(false, tracer);
    } else {
      return new OpenTelemetryTracer(true, OpenTelemetryTracer.createDefaultTracer());
    }
  }
}
