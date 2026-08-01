import cn.geoair.map.dynamic.file.geojson.GeoJsonGeoFileReader;
import cn.geoair.map.dynamic.file.geojson.GeoJsonLinkInfo;

/**
 * @author ：张俊
 * @date ：Created in 2026/8/1 11:10
 * @description： TODO
 */
public class ReaderTest {
    public static void main(String[] args) {
        GeoJsonLinkInfo geoJsonLinkInfo =
                new GeoJsonLinkInfo()
                        .setGeoJsonFilePath("E:\\gis测试数据\\测试数据\\geojson\\poi.geojson")
                        .setCharset("UTF-8");
        GeoJsonGeoFileReader geoJsonReader = new GeoJsonGeoFileReader();
        geoJsonReader.setLinkInfo(geoJsonLinkInfo);
        long featureCount = geoJsonReader.getFeatureCount();
        System.out.println(featureCount);
    }
}
