package cn.geoair.map.tile.forge.core.servlet;

import cn.geoair.map.dynamic.tools.simple.response.TileResponse;
import cn.geoair.map.dynamic.tools.simple.response.TileResponseByByte;
import cn.geoair.map.dynamic.tools.simple.response.TileResponseProvider;
import cn.geoair.map.tile.forge.core.TileRequest;
import org.junit.Assert;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

/**
 * {@link MvtTilesServlet} 的 style.json 地址替换测试。
 *
 * @author 张逢吉
 */
public class MvtTilesServletTest {

    @Test
    public void shouldKeepRelativeBaseUrlForLegacySingleArgumentCall() {
        String style = renderStyle("/mvtTilesService/id/file/layer/style.json?version=1", null);

        Assert.assertEquals("{\"url\":\"/mvtTilesService/id/file/layer/{z}/{x}/{y}.pbf\"}", style);
    }

    @Test
    public void shouldKeepRelativeBaseUrlWhenRequestHostIsNotProvided() {
        String style = renderStyle(
                "https://tile.example.com:8443/mvtTilesService/id/file/layer/style.json?version=1", null);

        Assert.assertEquals("{\"url\":\"/mvtTilesService/id/file/layer/{z}/{x}/{y}.pbf\"}", style);
    }

    @Test
    public void shouldPreferExplicitHostAndNeverDuplicateAbsoluteUrlOrigin() {
        String style = renderStyle(
                "http://internal.example/mvtTilesService/id/file/layer/style.json?version=1",
                "https://maps.example.com/");

        Assert.assertEquals("{\"url\":\"https://maps.example.com/mvtTilesService/id/file/layer/{z}/{x}/{y}.pbf\"}", style);
    }

    @Test
    public void shouldKeepExistingProviderImplementationCompatible() {
        TileResponse expected = TileResponse.notFound();
        TileResponseProvider provider = new TileResponseProvider() {
            @Override
            public TileResponse getTileResponse(String requestUri) {
                return expected;
            }
        };

        Assert.assertSame(expected, provider.getTileResponse("/tiles/1/2/3", "https://maps.example.com"));
    }

    private String renderStyle(String requestUri, String requestHost) {
        MvtTilesServlet servlet = new MvtTilesServlet(null);
        TileRequest tileRequest = new TileRequest();
        tileRequest.setBytes("{\"url\":\"{BASE_URL}/{z}/{x}/{y}.pbf\"}".getBytes(StandardCharsets.UTF_8));
        tileRequest.setSize(tileRequest.getBytes().length);
        tileRequest.setExists(true);
        TileParseResult parseResult = TileParseResult.of()
                .setRequestURI(requestUri)
                .setRequestHost(requestHost);

        TileResponse response = servlet.createTileResponse(tileRequest, parseResult, requestUri);
        return new String(((TileResponseByByte) response).getBytes(), StandardCharsets.UTF_8);
    }
}
