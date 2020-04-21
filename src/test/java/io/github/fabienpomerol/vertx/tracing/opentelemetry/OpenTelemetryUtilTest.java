package io.github.fabienpomerol.vertx.tracing.opentelemetry;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import io.opentelemetry.trace.DefaultTracer;
import io.opentelemetry.trace.Tracer;
import io.opentelemetry.trace.Span;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class OpenTelemetryUtilTest {

  private Vertx vertx;
  private Tracer tracer;

  @Before
  public void before() {
    tracer = DefaultTracer.getInstance();
    vertx = Vertx.vertx(new VertxOptions().setTracingOptions(new OpenTelemetryOptions(tracer).setEnabled(true)));
  }

  @After
  public void after(TestContext ctx) {
    vertx.close(ctx.asyncAssertSuccess());
  }

  @Test
  public void getSpan_should_retrieve_a_span_from_the_currentContext(TestContext ctx) {
    Span span = tracer.spanBuilder("test").startSpan();
    vertx.runOnContext(ignored -> {
      assertNull(OpenTelemetryUtil.getSpan());
      Context context = Vertx.currentContext();
      context.putLocal(OpenTelemetryUtil.ACTIVE_SPAN, span);

      assertSame(span, OpenTelemetryUtil.getSpan());
    });
  }

  @Test
  public void getSpan_should_return_null_when_there_is_no_current_context(TestContext ctx) {
    Span span = tracer.spanBuilder("test").startSpan();
    OpenTelemetryUtil.setSpan(span);
    assertNull(OpenTelemetryUtil.getSpan());
  }

  @Test
  public void setSpan_should_put_the_span_on_the_current_context() {
    Span span = tracer.spanBuilder("test").startSpan();
    vertx.runOnContext(ignored -> {
      assertNull(OpenTelemetryUtil.getSpan());
      OpenTelemetryUtil.setSpan(span);

      Context context = Vertx.currentContext();
      assertSame(span, context.getLocal(OpenTelemetryUtil.ACTIVE_SPAN));
    });
  }

  @Test
  public void clearContext_should_remove_any_span_from_the_context() {
    Span span = tracer.spanBuilder("test").startSpan();
    vertx.runOnContext(ignored -> {
      assertNull(OpenTelemetryUtil.getSpan());
      OpenTelemetryUtil.setSpan(span);

      OpenTelemetryUtil.clearContext();
      assertNull(OpenTelemetryUtil.getSpan());
    });
  }
}
