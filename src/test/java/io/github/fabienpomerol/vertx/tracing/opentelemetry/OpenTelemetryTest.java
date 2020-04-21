package io.github.fabienpomerol.vertx.tracing.opentelemetry;

import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.trace.*;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpClient;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.github.fabienpomerol.vertx.tracing.opentelemetry.exporters.InMemoryExporter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.omg.CosNaming.NamingContextExtPackage.StringNameHelper;

import java.util.List;

import static org.junit.Assert.*;

@RunWith(VertxUnitRunner.class)
public class OpenTelemetryTest {

  private Vertx vertx;
  private Tracer tracer;
  private InMemoryExporter exporter;

  @Before
  public void before() {
    tracer = OpenTelemetryTracer.createDefaultTracer();
    exporter = InMemoryExporter.newBuilder().build();
    vertx = Vertx.vertx(new VertxOptions().setTracingOptions(
      new OpenTelemetryOptions(tracer)
        .addExporter(exporter)
        .setEnabled(true)
    ));
  }

  @After
  public void after(TestContext ctx) {
    vertx.close(ctx.asyncAssertSuccess());
  }

  public List<SpanData> waitUntil(int expected) throws Exception {
    long now = System.currentTimeMillis();
    while (exporter.getSpanExporter().getFinishedSpanItems().size() < expected && (System.currentTimeMillis() - now) < 10000 ) {
      Thread.sleep(10);
    }
    assertEquals(expected, exporter.getSpanExporter().getFinishedSpanItems().size());
    return exporter.getSpanExporter().getFinishedSpanItems();
  }

  void assertSingleTrace(List<SpanData> spans) {
    long result = spans.stream().map(span -> span.getTraceId()).distinct().count();
    assertEquals(1, result);
  }

  @Test
  public void testHttpServerRequest(TestContext ctx) throws Exception {
    Async listenLatch = ctx.async();
    vertx.createHttpServer().requestHandler(req -> {
      req.response().end();
    }).listen(8080, ctx.asyncAssertSuccess(v -> listenLatch.complete()));
    listenLatch.awaitSuccess();
    Async responseLatch = ctx.async();
    HttpClient client = vertx.createHttpClient();
    client.get(8080, "localhost", "/", ctx.asyncAssertSuccess(resp ->{
      responseLatch.complete();
    }));

    responseLatch.awaitSuccess();

    List<SpanData> spans = waitUntil(1);
    SpanData spanData = spans.get(0);

    assertEquals("GET", spanData.getName());
    assertEquals("GET", spanData.getAttributes().get("http.method").getStringValue());
    assertEquals("http://localhost:8080/", spanData.getAttributes().get("http.url").getStringValue());
    assertEquals("200", spanData.getAttributes().get("http.status_code").getStringValue());
  }

  @Test
  public void testHttpClientRequest(TestContext ctx) throws Exception {
    Async listenLatch = ctx.async(2);
    HttpClient c = vertx.createHttpClient();
    vertx.createHttpServer().requestHandler(req -> {
      c.get(8081, "localhost", "/", ctx.asyncAssertSuccess(resp -> {
        req.response().end();
      }));
    }).listen(8080, ctx.asyncAssertSuccess(v -> listenLatch.countDown()));
    vertx.createHttpServer().requestHandler(req -> {
      req.response().end();
    }).listen(8081, ctx.asyncAssertSuccess(v -> listenLatch.countDown()));
    listenLatch.awaitSuccess();
    Async responseLatch = ctx.async();
    HttpClient client = vertx.createHttpClient();
    client.get(8080, "localhost", "/", ctx.asyncAssertSuccess(resp ->{
      responseLatch.complete();
    }));
    responseLatch.awaitSuccess();
    List<SpanData> spans = waitUntil(3);
    SpanData spanData = spans.get(0);

    assertSingleTrace(spans);
    assertEquals("GET", spanData.getName());
    assertEquals("GET", spanData.getAttributes().get("http.method").getStringValue());
    assertEquals("http://localhost:8081/", spanData.getAttributes().get("http.url").getStringValue());
    assertEquals("200", spanData.getAttributes().get("http.status_code").getStringValue());
  }

  @Test
  public void testSpanHierarchy(TestContext ctx) throws Exception {
    Async listenLatch = ctx.async(2);
    HttpClient c = vertx.createHttpClient();
    vertx.createHttpServer().requestHandler(req -> {
      c.get(8081, "localhost", "/", ctx.asyncAssertSuccess(resp -> {
        req.response().end();
      }));
    }).listen(8080, ctx.asyncAssertSuccess(v -> listenLatch.countDown()));
    vertx.createHttpServer().requestHandler(req -> {
      req.response().end();
    }).listen(8081, ctx.asyncAssertSuccess(v -> listenLatch.countDown()));
    listenLatch.awaitSuccess();
    Async responseLatch = ctx.async();
    HttpClient client = vertx.createHttpClient();
    client.get(8080, "localhost", "/", ctx.asyncAssertSuccess(resp ->{
      responseLatch.complete();
    }));
    responseLatch.awaitSuccess();
    List<SpanData> spans = waitUntil(3);

    assertSingleTrace(spans);
    assertEquals(spans.get(2).getSpanId(), spans.get(0).getParentSpanId());
    assertTrue(spans.get(0).getHasRemoteParent());
    assertEquals(new SpanId(0), spans.get(1).getParentSpanId());
    assertFalse(spans.get(1).getHasRemoteParent());
    assertEquals(spans.get(1).getSpanId(), spans.get(2).getParentSpanId());
    assertFalse(spans.get(2).getHasRemoteParent());
  }

  @Test
  public void testEventBus(TestContext ctx) throws Exception {
    Async listenLatch = ctx.async(2);
    vertx.createHttpServer().requestHandler(req -> {
      vertx.eventBus().request("the-address", "ping", ctx.asyncAssertSuccess(resp -> {
        req.response().end();
      }));
    }).listen(8080, ctx.asyncAssertSuccess(v -> listenLatch.countDown()));
    vertx.eventBus().consumer("the-address", msg -> {
      msg.reply("pong");
    });
    vertx.createHttpServer().requestHandler(req -> {
      req.response().end();
    }).listen(8081, ctx.asyncAssertSuccess(v -> listenLatch.countDown()));
    listenLatch.awaitSuccess();
    Async responseLatch = ctx.async();
    HttpClient client = vertx.createHttpClient();
    client.get(8080, "localhost", "/", ctx.asyncAssertSuccess(resp ->{
      responseLatch.complete();
    }));
    responseLatch.awaitSuccess();
    List<SpanData> spans = waitUntil(3);
    assertSingleTrace(spans);
    SpanData span = spans.get(0);
    assertEquals("send", span.getName());
    assertEquals("the-address", span.getAttributes().get("peer.service").getStringValue());
  }
}
