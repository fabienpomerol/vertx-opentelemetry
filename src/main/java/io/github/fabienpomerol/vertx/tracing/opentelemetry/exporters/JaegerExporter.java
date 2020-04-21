package io.github.fabienpomerol.vertx.tracing.opentelemetry.exporters;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.opentelemetry.exporters.jaeger.JaegerGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.Sampler;
import io.opentelemetry.sdk.trace.Samplers;
import io.opentelemetry.sdk.trace.TracerSdkProvider;
import io.opentelemetry.sdk.trace.config.TraceConfig;
import io.opentelemetry.sdk.trace.export.SimpleSpansProcessor;

import javax.annotation.concurrent.ThreadSafe;

@ThreadSafe
public final class JaegerExporter implements BackendExporter {

  private static final String IP_DEFAULT = "0.0.0.0";
  private static final int PORT_DEFAULT = 14250;

  private JaegerExporter(String serviceName, String ip, int port, long deadline, Sampler sampler) {
    ManagedChannel channel = ManagedChannelBuilder
      .forAddress(ip, port)
      .usePlaintext()
      .build();

    JaegerGrpcSpanExporter exporter = JaegerGrpcSpanExporter.newBuilder()
      .setServiceName(serviceName)
      .setChannel(channel)
      .setDeadlineMs(deadline)
      .build();

    TracerSdkProvider tracerProvider = OpenTelemetrySdk.getTracerProvider();

    TraceConfig traceConfig = TraceConfig.getDefault().toBuilder().setSampler(
      sampler
    ).build();

    tracerProvider.addSpanProcessor(SimpleSpansProcessor.newBuilder(exporter).build());

    tracerProvider.updateActiveTraceConfig(traceConfig);
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  public static class Builder {
    private String serviceName;
    private String ip = IP_DEFAULT;
    private int port = PORT_DEFAULT;
    private long deadline = 1_000; // ms
    private Sampler sampler = Samplers.alwaysOn();

    public Builder setServiceName(String serviceName) {
      this.serviceName = serviceName;
      return this;
    }

    public Builder setIp(String ip) {
      this.ip = ip;
      return this;
    }

    public Builder setPort(int port) {
      this.port = port;
      return this;
    }

    public Builder setDeadline(long deadline) {
      this.deadline = deadline;
      return this;
    }

    public Builder setSampler(Sampler sampler) {
      this.sampler = sampler;
      return this;
    }

    /**
     * Constructs a new instance of the exporter based on the builder's values.
     *
     * @return a new exporter's instance
     */
    public JaegerExporter build() {
      return new JaegerExporter(serviceName, ip, port, deadline, sampler);
    }

  }



}
