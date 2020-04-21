package io.github.fabienpomerol.vertx.tracing.opentelemetry.exporters;

import io.opentelemetry.exporters.inmemory.InMemorySpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.export.SimpleSpansProcessor;

public class InMemoryExporter implements BackendExporter {

  InMemorySpanExporter exporter;

  private InMemoryExporter() {

    exporter = InMemorySpanExporter.create();

    OpenTelemetrySdk.getTracerProvider()
      .addSpanProcessor(SimpleSpansProcessor.newBuilder(exporter).build());
  }

  public static InMemoryExporter.Builder newBuilder() {
    return new InMemoryExporter.Builder();
  }

  public static class Builder {
    /**
     * Constructs a new instance of the exporter based on the builder's values.
     *
     * @return a new exporter's instance
     */
    public InMemoryExporter build() {
      return new InMemoryExporter();
    }
  }

  public InMemorySpanExporter getSpanExporter() {
    return this.exporter;
  }
}
