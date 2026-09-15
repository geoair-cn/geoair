package cn.geoair.map.tile.forge.fuser.utils;

import cn.geoair.map.tile.forge.core.bygwc.io.Resource;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import org.junit.Assert;
import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;

/** 使用 {@link HttpTileRequestUtils} 对三个常见网络瓦片源执行单瓦片测试。 */
public class HttpTileRequestUtilsProviderTest {

    private static final String PROXY_HOST = "192.168.0.196";
    private static final int PROXY_PORT = 8887;
    private static final int TIMEOUT_MILLIS = 30_000;
    private static final Path OUTPUT_DIR = Paths.get("target", "tile-provider-test");
    private static final Map<String, String> BROWSER_HEADERS = createBrowserHeaders();

    private static final String OSM_URL = "https://tile.openstreetmap.org/0/0/0.png";
    private static final String ARCGIS_URL =
            "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/0/0/0";
    /** 仅用于验证 HTTP 工具兼容性；正式接入应使用带 API Key 和 session 的 Google Map Tiles API。 */
    private static final String GOOGLE_URL = "https://mt1.google.com/vt/lyrs=s&x=0&y=0&z=0";

    @Test
    public void testOpenStreetMapTile() throws IOException {
        requestAndSaveTile("osm", OSM_URL);
    }

    @Test
    public void testArcGisTile() throws IOException {
        requestAndSaveTile("arcgis", ARCGIS_URL);
    }

    @Test
    public void testGoogleTile() throws IOException {
        requestAndSaveTile("google", GOOGLE_URL);
    }

    /** 可直接运行，一次按顺序测试 OSM、ArcGIS、Google 各一张瓦片。 */
    public static void main(String[] args) {
        if (args.length > 0 && "capture-osm".equalsIgnoreCase(args[0])) {
            try {
                captureOpenStreetMapBrowserResponse();
            } catch (IOException e) {
                throw new IllegalStateException("Failed to capture raw OSM response", e);
            } finally {
                HttpTileRequestUtils.closeHttpClient();
            }
            return;
        }

        HttpTileRequestUtilsProviderTest test = new HttpTileRequestUtilsProviderTest();
        int failureCount = 0;
        try {
            failureCount += runProviderTest("OSM", test::testOpenStreetMapTile);
            failureCount += runProviderTest("ArcGIS", test::testArcGisTile);
            failureCount += runProviderTest("Google", test::testGoogleTile);
        } finally {
            HttpTileRequestUtils.closeHttpClient();
        }
        if (failureCount > 0) {
            throw new AssertionError(failureCount + " tile provider test(s) failed");
        }
        System.out.println("All tile provider tests passed.");
    }

    /** 保存 OSM 的原始响应图片，不经过业务方法对 x-blocked 的拦截，便于检查实际响应内容。 */
    private static void captureOpenStreetMapBrowserResponse() throws IOException {
        Proxy proxy = HttpTileRequestUtils.createHttpProxy(PROXY_HOST, PROXY_PORT);
        HttpRequest request = HttpTileRequestUtils.createGetRequest(
                OSM_URL, proxy, TIMEOUT_MILLIS, HttpTileRequestUtils.buildHeaders(BROWSER_HEADERS));
        try (HttpResponse response = request.execute()) {
            System.out.println("Raw OSM HTTP status: " + response.getStatus());
            System.out.println("Raw OSM response headers:");
            for (Map.Entry<String, java.util.List<String>> entry : response.headers().entrySet()) {
                System.out.println("  " + entry.getKey() + ": " + entry.getValue());
            }

            byte[] responseBytes = response.bodyBytes();
            Files.createDirectories(OUTPUT_DIR);
            Path outputPath = OUTPUT_DIR.resolve("osm-browser-raw.png").toAbsolutePath().normalize();
            Files.write(outputPath, responseBytes);
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(responseBytes));
            System.out.println("Raw OSM response bytes: " + responseBytes.length);
            System.out.println("Raw OSM image size: "
                    + (image == null ? "not an image" : image.getWidth() + "x" + image.getHeight()));
            System.out.println("Raw OSM image saved to: " + outputPath);
        }
    }

    private static int runProviderTest(String provider, TileTest test) {
        try {
            test.run();
            return 0;
        } catch (Throwable e) {
            System.err.println(provider + " failed: " + e.getMessage());
            e.printStackTrace(System.err);
            return 1;
        }
    }

    private void requestAndSaveTile(String provider, String url) throws IOException {
        Proxy proxy = HttpTileRequestUtils.createHttpProxy(PROXY_HOST, PROXY_PORT);
        Files.createDirectories(OUTPUT_DIR);
        Path outputPath = OUTPUT_DIR.resolve(provider + ".png").toAbsolutePath().normalize();
        Files.deleteIfExists(outputPath);

        System.out.println("Requesting " + provider + ": " + url);
        System.out.println("Proxy: http://" + PROXY_HOST + ":" + PROXY_PORT);
        System.out.println("User-Agent: " + HttpTileRequestUtils.DEFAULT_USER_AGENT);

        Resource resource = HttpTileRequestUtils.requestTile(
                url, proxy, TIMEOUT_MILLIS, null, provider + " default-user-agent test");
        Assert.assertNotNull(provider + " tile request failed", resource);

        byte[] tileBytes = resource.getByteData();
        Assert.assertNotNull(provider + " tile bytes are null", tileBytes);
        Assert.assertTrue(provider + " tile is empty", tileBytes.length > 0);

        BufferedImage image = ImageIO.read(new ByteArrayInputStream(tileBytes));
        Assert.assertNotNull(provider + " response is not a readable image", image);
        Assert.assertEquals(provider + " tile width", 256, image.getWidth());
        Assert.assertEquals(provider + " tile height", 256, image.getHeight());

        Files.write(outputPath, tileBytes);
        System.out.println(provider + " passed: " + image.getWidth() + "x" + image.getHeight()
                + ", " + tileBytes.length + " bytes, saved to " + outputPath);
    }

    private static Map<String, String> createBrowserHeaders() {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                        + "(KHTML, like Gecko) Chrome/155.0.0.0 Safari/537.36");
        headers.put("Sec-Ch-Ua",
                "\"Not_A Brand\";v=\"8\", \"Chromium\";v=\"155\", \"Google Chrome\";v=\"155\"");
        headers.put("Sec-Ch-Ua-Mobile", "?0");
        headers.put("Sec-Ch-Ua-Platform", "\"Windows\"");
        headers.put("Sec-Fetch-Dest", "image");
        headers.put("Sec-Fetch-Mode", "no-cors");
        headers.put("Sec-Fetch-Site", "cross-site");
        return headers;
    }

    private interface TileTest {
        void run() throws IOException;
    }
}
