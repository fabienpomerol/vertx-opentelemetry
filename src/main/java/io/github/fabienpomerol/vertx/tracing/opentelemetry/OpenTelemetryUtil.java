package io.github.fabienpomerol.vertx.tracing.opentelemetry;

import io.opentelemetry.trace.Span;
import io.vertx.core.Context;
import io.vertx.core.Vertx;

/**
 * Add helpers for associating and disassociating spans with the current {@link Context}
 */
public final class OpenTelemetryUtil {

  static final String ACTIVE_SPAN = "vertx.tracing.opentelemetry.span";

  /**
   * Get the active span from the current {@link Context}
   *
   * @return a {@link Span} or null
   */
  public static Span getSpan() {
    Context c = Vertx.currentContext();
    return c == null ? null : c.getLocal(ACTIVE_SPAN);
  }

  /**
   * Set the span as active on the context.
   *
   * @param span the span to associate with the context.
   */
  public static void setSpan(Span span) {
    if (span != null) {
      Context c = Vertx.currentContext();
      if (c != null) {
        c.putLocal(ACTIVE_SPAN, span);
      }
    }
  }

  /**
   * Remove any active span on the context.
   */
  public static void clearContext() {
    Context c = Vertx.currentContext();
    if (c != null) {
      c.removeLocal(ACTIVE_SPAN);
    }
  }
}
