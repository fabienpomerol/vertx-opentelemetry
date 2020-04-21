package io.github.fabienpomerol.vertx.tracing.opentelemetry;

import io.opentelemetry.OpenTelemetry;
import io.opentelemetry.common.AttributeValue;
import io.opentelemetry.context.ContextUtils;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.HttpTextFormat;
import io.opentelemetry.trace.Span;
import io.opentelemetry.trace.Status;
import io.opentelemetry.trace.Tracer;
import io.vertx.core.Context;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.spi.tracing.TagExtractor;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.StreamSupport;

/**
 * - https://github.com/open-telemetry/opentelemetry-specification/blob/master/specification/trace/api.md
 * - https://github.com/open-telemetry/opentelemetry-specification/blob/master/specification/trace/sdk.md
 */
public class OpenTelemetryTracer implements io.vertx.core.spi.tracing.VertxTracer<Span, Span> {

  /**
   * Instantiate an OpenTelemtry tracer
   */
  static Tracer createDefaultTracer() {
    return OpenTelemetry.getTracerProvider().get("io.vertx.tracing.opentelemetry.OpenTelemetryTracer");
  }

  private final boolean closeTracer;
  private final Tracer tracer;

  /**
   * Instantiate a OpenTelemetry tracer using the specified {@code tracer}.
   *
   * @param closeTracer close the tracer when necessary
   * @param tracer      the tracer instance
   */
  public OpenTelemetryTracer(boolean closeTracer, Tracer tracer) {
    this.closeTracer = closeTracer;
    this.tracer = tracer;
  }

  @Override
  public <R> Span receiveRequest(Context context, R request, String operation, Iterable<Map.Entry<String, String>> headers, TagExtractor<R> tagExtractor) {

    // when we receive a request we try to resolve the context based on headers in a HttpTextFormat
    // This format is used to share context into carriers that travel in-band across process boundaries
    HttpTextFormat.Getter<Iterable<Map.Entry<String, String>>> getter = new HttpTextFormat.Getter<Iterable<Map.Entry<String, String>>>() {
      @Nullable
      @Override
      public String get(Iterable<Map.Entry<String, String>> headers, String s) {
        return StreamSupport.stream(headers.spliterator(), false)
          .filter(entry -> entry.getKey().equals(s))
          .findFirst()
          .map(Map.Entry::getValue)
          .orElse(null);
      }
    };

    io.grpc.Context extractedContext = OpenTelemetry.getPropagators()
      .getHttpTextFormat()
      .extract(io.grpc.Context.current(), headers, getter);

    Span serverSpan;

    try (Scope scope = ContextUtils.withScopedContext(extractedContext)) {
      serverSpan = tracer.spanBuilder(operation)
        .setSpanKind(Span.Kind.SERVER)
        .startSpan();
      serverSpan.setAttribute("component", "vertx");

      addAttributes(serverSpan, request, tagExtractor);
      context.putLocal(OpenTelemetryUtil.ACTIVE_SPAN, serverSpan);
    }

    return serverSpan;
  }

  @Override
  public <R> void sendResponse(
    Context context, R response, Span span, Throwable failure, TagExtractor<R> tagExtractor) {
    if (span != null) {
      context.removeLocal(OpenTelemetryUtil.ACTIVE_SPAN);

      if (failure != null) {
        reportError(span, failure.getClass().getName(), failure.getMessage());
      }

      reportResponseError(response, span);
      addAttributes(span, response, tagExtractor);
      span.end();
    }
  }

  @Override
  public <R> Span sendRequest(Context context, R request, String operation, BiConsumer<String, String> headers, TagExtractor<R> tagExtractor) {
    Span activeSpan = context.getLocal(OpenTelemetryUtil.ACTIVE_SPAN);

    if (activeSpan != null) {
      Span span = tracer
        .spanBuilder(operation)
        .setParent(activeSpan)
        .setSpanKind(Span.Kind.CLIENT)
        .startSpan();

      span.setAttribute("component", "vertx");
      addAttributes(span, request, tagExtractor);

      if (headers != null) {
        // We inject the current context in headers to cross process boundaries
        try (Scope scope = tracer.withSpan(span)) {
          OpenTelemetry.getPropagators().getHttpTextFormat().inject(io.grpc.Context.current(), headers, BiConsumer::accept);
        }
      }

      return span;
    }
    return null;
  }

  @Override
  public <R> void receiveResponse(Context context, R response, Span span, Throwable failure,
                                  TagExtractor<R> tagExtractor) {
    if (span != null) {
      if (failure != null) {
        reportError(span, failure.getClass().getName(), failure.getMessage());
      }

      reportResponseError(response, span);

      addAttributes(span, response, tagExtractor);
      span.end();
    }
  }

  /**
   * Add span attributes based on the extracted tags
   * @param span
   * @param obj
   * @param tagExtractor
   * @param <T>
   */
  private <T> void addAttributes(Span span, T obj, TagExtractor<T> tagExtractor) {
    int len = tagExtractor.len(obj);
    for (int idx = 0; idx < len; idx++) {
      span.setAttribute(tagExtractor.name(obj, idx), tagExtractor.value(obj, idx));
    }
  }

  /**
   * In an HTTP Context check for HTTP Error in the response to add an error event to the Span
   * @param response The response
   * @param span The span
   * @param <R> HTTP Server / Client or EventBus response
   */
  private <R> void reportResponseError(R response, Span span) {
    if (response instanceof HttpServerResponse) {
      HttpServerResponse resp = (HttpServerResponse) response;
      if (resp.getStatusCode() == 500) {
        reportError(span, "Functional", resp.getStatusMessage());
      }
    } else if (response instanceof HttpClientResponse) {
      HttpClientResponse resp = (HttpClientResponse) response;
      if (resp.statusCode() == 500) {
        reportError(span, "Functional", resp.statusMessage());
      }
    }
  }

  /**
   * Add an error event to the given span and mark the flag the span as error
   * @param span the Span
   * @param errorKind The error kind
   * @param message The error message
   */
  private void reportError(Span span, String errorKind, String message) {
    Map<String, AttributeValue> errorEvent = new HashMap<>();
    errorEvent.put("event", AttributeValue.stringAttributeValue("error"));
    errorEvent.put("error.kind", AttributeValue.stringAttributeValue(errorKind));
    // TODO replace by error.object regarding the current spec ?
    // see https://github.com/open-telemetry/opentelemetry-specification/issues/67
    errorEvent.put("message", AttributeValue.stringAttributeValue(message));

    span.addEvent("error", errorEvent);
    span.setAttribute("error", "true");
    span.setStatus(Status.INTERNAL);
  }

  @Override
  public void close() {
    if (closeTracer && tracer != null) {
      tracer.getCurrentSpan().end();
    }
  }
}
