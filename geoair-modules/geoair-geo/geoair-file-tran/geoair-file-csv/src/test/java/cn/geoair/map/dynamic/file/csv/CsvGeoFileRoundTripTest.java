package cn.geoair.map.dynamic.file.csv;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cn.geoair.map.dynamic.file.core.enums.TranStatus;
import cn.geoair.map.dynamic.file.core.tran.GeoFileTran;
import cn.geoair.map.dynamic.file.core.tran.GeoFileTranImpl;
import cn.geoair.map.dynamic.file.core.tran.model.TranContext;
import cn.geoair.map.dynamic.file.core.tran.model.TranResult;
import cn.geoair.map.dynamic.file.core.write.config.WriteConfig;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class CsvGeoFileRoundTripTest {

    @Test
    void lonLatCsvCanRoundTrip() throws Exception {
        Path input = Files.createTempFile("geoair-csv-in", ".csv");
        Path output = Files.createTempFile("geoair-csv-out", ".csv");
        Files.write(
                input,
                ("name,lon,lat\nA,120.1,30.2\nB,121.3,31.4\n").getBytes(StandardCharsets.UTF_8));

        CsvLinkInfo readInfo =
                new CsvLinkInfo()
                        .setCsvFilePath(input.toString())
                        .setGeometryMode(CsvGeometryMode.LON_LAT)
                        .setLongitudeColumnName("lon")
                        .setLatitudeColumnName("lat");
        CsvGeoFileReader reader = new CsvGeoFileReader();
        reader.setLinkInfo(readInfo);

        CsvLinkInfo writeInfo =
                new CsvLinkInfo()
                        .setCsvFilePath(output.toString())
                        .setGeometryMode(CsvGeometryMode.WKT)
                        .setWktColumnName("wkt");
        CsvGeoFileWriter writer = new CsvGeoFileWriter();
        writer.setLinkInfo(writeInfo);
        writer.setWriteConfig(new WriteConfig().setOutPutSrid(4326));

        GeoFileTran tran = new GeoFileTranImpl();
        TranResult result =
                tran.transform(reader, writer, new TranContext().setSkipErrorRecord(false));

        assertEquals(TranStatus.SUCCESS, result.getStatus());
        assertTrue(Files.size(output) > 0);
        assertTrue(
                new String(Files.readAllBytes(output), StandardCharsets.UTF_8).contains("POINT"));
    }

    @Test
    void wktCsvCanRoundTrip() throws Exception {
        Path input = Files.createTempFile("geoair-csv-wkt-in", ".csv");
        Path output = Files.createTempFile("geoair-csv-wkt-out", ".csv");
        Files.write(
                input,
                ("name,wkt\nA,\"POINT (120.1 30.2)\"\nB,\"LINESTRING (120 30,121 31)\"\n")
                        .getBytes(StandardCharsets.UTF_8));

        CsvLinkInfo readInfo =
                new CsvLinkInfo()
                        .setCsvFilePath(input.toString())
                        .setGeometryMode(CsvGeometryMode.WKT)
                        .setWktColumnName("wkt");
        CsvGeoFileReader reader = new CsvGeoFileReader();
        reader.setLinkInfo(readInfo);

        CsvLinkInfo writeInfo =
                new CsvLinkInfo()
                        .setCsvFilePath(output.toString())
                        .setGeometryMode(CsvGeometryMode.WKT)
                        .setWktColumnName("wkt");
        CsvGeoFileWriter writer = new CsvGeoFileWriter();
        writer.setLinkInfo(writeInfo);
        writer.setWriteConfig(new WriteConfig().setOutPutSrid(4326));

        GeoFileTran tran = new GeoFileTranImpl();
        TranResult result =
                tran.transform(reader, writer, new TranContext().setSkipErrorRecord(false));

        assertEquals(TranStatus.SUCCESS, result.getStatus());
        String outputText = new String(Files.readAllBytes(output), StandardCharsets.UTF_8);
        assertTrue(outputText.contains("POINT (120.1 30.2)"));
        assertTrue(outputText.contains("LINESTRING (120 30, 121 31)"));
    }
}
