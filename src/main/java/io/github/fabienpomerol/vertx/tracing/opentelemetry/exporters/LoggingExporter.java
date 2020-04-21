package io.github.fabienpomerol.vertx.tracing.opentelemetry.exporters;

import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.export.SimpleSpansProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;

public class LoggingExporter implements BackendExporter {

  private LoggingExporter() {

    SpanExporter exporter = new io.opentelemetry.exporters.logging.LoggingSpanExporter();

    OpenTelemetrySdk.getTracerProvider()
      .addSpanProcessor(SimpleSpansProcessor.newBuilder(exporter).build());
  }

  public static LoggingExporter.Builder newBuilder() {
    return new LoggingExporter.Builder();
  }

  public static class Builder {

    /**
     * Constructs a new instance of the exporter based on the builder's values.
     *
     * @return a new exporter's instance
     */
    public LoggingExporter build() {
      return new LoggingExporter();
    }
  }
}


