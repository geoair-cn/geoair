package cn.geoair.map.tile.forge.fuser.utils;

import static org.junit.Assert.*;

import cn.geoair.map.tile.forge.fuser.entity.PxyLayerInfo;
import cn.geoair.map.tile.forge.fuser.execution.*;
import cn.geoair.map.tile.forge.fuser.upstream.UpstreamRequestOptions;
import cn.geoair.web.mime.GirImageMime;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import org.junit.*;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import javax.imageio.ImageIO;

/** 仅使用本地 HTTP 服务，覆盖鉴权隔离、重试、重定向及在途取消。 */
public class LayerHttpTileClientTest {
    private HttpServer server;
    private ExecutorService workers;
    private byte[] png;
    private String url;

    @Before
    public void setup() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB), "png", out);
        png = out.toByteArray();
        workers = Executors.newCachedThreadPool();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.setExecutor(workers);
        server.start();
        url = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @After
    public void cleanup() {
        server.stop(0);
        workers.shutdownNow();
    }

    private PxyLayerInfo layer(String name) {
        return new PxyLayerInfo()
                .setLayerName(name)
                .setUpstreamRequest(new UpstreamRequestOptions());
    }

    private void reply(HttpExchange exchange, int status) throws java.io.IOException {
        try {
            exchange.sendResponseHeaders(status, png.length);
            exchange.getResponseBody().write(png);
        } finally {
            exchange.close();
        }
    }

    @Test
    public void fixedHeadersAndQueryAreLayerScopedAndOverrideCaseInsensitively() throws Exception {
        AtomicInteger correct = new AtomicInteger();
        server.createContext(
                "/tile",
                e -> {
                    String name = e.getRequestURI().getQuery();
                    if (name.equals("token=A")
                            && "A".equals(e.getRequestHeaders().getFirst("Authorization"))
                            && e.getRequestHeaders().get("User-Agent").size() == 1
                            && "custom".equals(e.getRequestHeaders().getFirst("User-Agent"))
                            && "https://maps.example"
                                    .equals(e.getRequestHeaders().getFirst("Origin"))
                            && "https://maps.example/view"
                                    .equals(e.getRequestHeaders().getFirst("Referer")))
                        correct.incrementAndGet();
                    if (name.equals("token=B")
                            && "B".equals(e.getRequestHeaders().getFirst("Authorization"))
                            && e.getRequestHeaders().getFirst("Referer") == null)
                        correct.incrementAndGet();
                    reply(e, 200);
                });
        PxyLayerInfo a = layer("a"), b = layer("b");
        a.getUpstreamRequest().setReferer("https://maps.example/view");
        a.getUpstreamRequest().setOrigin("https://maps.example");
        a.getUpstreamRequest().getHeaders().put("user-agent", "custom");
        a.getUpstreamRequest().getHeaders().put("Authorization", "A");
        b.getUpstreamRequest().getHeaders().put("Authorization", "B");
        a.getUpstreamRequest().getQueryParameters().put("token", "A");
        b.getUpstreamRequest().getQueryParameters().put("token", "B");
        Future<?> one =
                workers.submit(
                        () ->
                                LayerHttpTileClient.fetch(
                                        a,
                                        url + "/tile?token=old",
                                        1,
                                        2,
                                        3,
                                        GirImageMime.png,
                                        5000));
        Future<?> two =
                workers.submit(
                        () ->
                                LayerHttpTileClient.fetch(
                                        b, url + "/tile", 1, 2, 3, GirImageMime.png, 5000));
        assertNotNull(one.get(6, TimeUnit.SECONDS));
        assertNotNull(two.get(6, TimeUnit.SECONDS));
        assertEquals(2, correct.get());
    }

    @Test
    public void dynamicStrategyRefreshesOnUnauthorizedWithCoordinates() {
        AtomicInteger received = new AtomicInteger();
        server.createContext(
                "/auth",
                e -> {
                    received.incrementAndGet();
                    reply(e, "fresh".equals(e.getRequestHeaders().getFirst("X-Key")) ? 200 : 401);
                });
        PxyLayerInfo layer = layer("dynamic");
        AtomicInteger attempts = new AtomicInteger();
        layer.setUpstreamRequestCustomizer(
                request -> {
                    assertEquals("dynamic", request.getLayerName());
                    assertEquals(7, request.getZ());
                    assertEquals(8, request.getX());
                    assertEquals(9, request.getY());
                    request.getHeaders()
                            .put("X-Key", request.getPreviousStatus() == 401 ? "fresh" : "expired");
                    attempts.incrementAndGet();
                });
        assertNotNull(
                LayerHttpTileClient.fetch(layer, url + "/auth", 7, 8, 9, GirImageMime.png, 5000));
        assertEquals(2, received.get());
        assertEquals(2, attempts.get());
    }

    @Test
    public void doesNotCarryResponseCookiesIntoAnotherLayer() {
        AtomicReference<String> cookie = new AtomicReference<>();
        server.createContext(
                "/set",
                e -> {
                    e.getResponseHeaders().add("Set-Cookie", "secret=one; Path=/");
                    reply(e, 200);
                });
        server.createContext(
                "/check",
                e -> {
                    cookie.set(e.getRequestHeaders().getFirst("Cookie"));
                    reply(e, 200);
                });
        LayerHttpTileClient.fetch(layer("a"), url + "/set", 0, 0, 0, GirImageMime.png, 3000);
        LayerHttpTileClient.fetch(layer("b"), url + "/check", 0, 0, 0, GirImageMime.png, 3000);
        assertNull(cookie.get());
    }

    @Test
    public void rejectsHeaderInjectionAndCrossOriginRedirect() {
        AtomicInteger targetCalls = new AtomicInteger();
        server.createContext(
                "/redirect",
                e -> {
                    e.getResponseHeaders()
                            .set(
                                    "Location",
                                    "http://localhost:"
                                            + server.getAddress().getPort()
                                            + "/target");
                    e.sendResponseHeaders(302, -1);
                    e.close();
                });
        server.createContext(
                "/target",
                e -> {
                    targetCalls.incrementAndGet();
                    reply(e, 200);
                });
        PxyLayerInfo layer = layer("secret");
        layer.getUpstreamRequest().getHeaders().put("X-Key", "secret");
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        LayerHttpTileClient.fetch(
                                layer, url + "/redirect", 0, 0, 0, GirImageMime.png, 3000));
        layer.getUpstreamRequest().getHeaders().put("X-Key", "x\r\nBad: y");
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        LayerHttpTileClient.fetch(
                                layer, url + "/target", 0, 0, 0, GirImageMime.png, 3000));
        assertEquals(0, targetCalls.get());
    }

    @Test
    public void sameOriginRedirectAndMissingVersusFailure() {
        server.createContext(
                "/redirect",
                e -> {
                    e.getResponseHeaders().set("Location", "/target");
                    e.sendResponseHeaders(302, -1);
                    e.close();
                });
        server.createContext("/target", e -> reply(e, 200));
        server.createContext("/missing", e -> reply(e, 404));
        server.createContext("/forbidden", e -> reply(e, 403));
        PxyLayerInfo layer = layer("x");
        assertNotNull(
                LayerHttpTileClient.fetch(
                        layer, url + "/redirect", 0, 0, 0, GirImageMime.png, 3000));
        assertNull(
                LayerHttpTileClient.fetch(
                        layer, url + "/missing", 0, 0, 0, GirImageMime.png, 3000));
        assertEquals(
                TileOperationStatus.FAILED,
                assertThrows(
                                TileOperationException.class,
                                () ->
                                        LayerHttpTileClient.fetch(
                                                layer,
                                                url + "/forbidden",
                                                0,
                                                0,
                                                0,
                                                GirImageMime.png,
                                                3000))
                        .getStatus());
    }

    @Test
    public void totalDeadlineStopsTricklingResponse() {
        server.createContext(
                "/slow",
                e -> {
                    e.sendResponseHeaders(200, 0);
                    try {
                        for (int i = 0; i < 100; i++) {
                            e.getResponseBody().write(1);
                            e.getResponseBody().flush();
                            Thread.sleep(30);
                        }
                    } catch (Exception ignored) {
                    } finally {
                        e.close();
                    }
                });
        long start = System.nanoTime();
        TileOperationException failure =
                assertThrows(
                        TileOperationException.class,
                        () ->
                                LayerHttpTileClient.fetch(
                                        layer("x"), url + "/slow", 0, 0, 0, GirImageMime.png, 250));
        assertEquals(TileOperationStatus.TIMED_OUT, failure.getStatus());
        assertTrue(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start) < 2000);
    }

    @Test
    public void cancellationAbortsInflightAndRestoresContext() throws Exception {
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        server.createContext(
                "/block",
                e -> {
                    entered.countDown();
                    try {
                        release.await();
                        reply(e, 200);
                    } catch (Exception ignored) {
                        e.close();
                    }
                });
        TileCancellationToken cancellation = new TileCancellationToken();
        Future<TileOperationStatus> task =
                workers.submit(
                        () -> {
                            try (TileOperationScope ignored =
                                    new TileOperationContext(cancellation, 10000).bind()) {
                                LayerHttpTileClient.fetch(
                                        layer("x"),
                                        url + "/block",
                                        0,
                                        0,
                                        0,
                                        GirImageMime.png,
                                        10000);
                                return TileOperationStatus.DOWNLOADED;
                            } catch (TileOperationException e) {
                                assertNull(TileOperationContext.current());
                                return e.getStatus();
                            }
                        });
        try {
            assertTrue(entered.await(3, TimeUnit.SECONDS));
            cancellation.cancel();
            assertEquals(TileOperationStatus.CANCELLED, task.get(2, TimeUnit.SECONDS));
        } finally {
            release.countDown();
        }
    }
}
