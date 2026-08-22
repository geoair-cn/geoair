package cn.geoair.map.tile.forge.core.zip.decompression;


import java.io.IOException;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

/**
 * ZIP(DEFLATE算法)解压适配器
 */
public class ZipHandler implements DecompressionHandler {



    @Override
    public byte[] decompress(byte[] compressedData, long expectedSize) throws IOException {
        DecompressionLimits.validateExpectedSize(expectedSize);
        Inflater inflater = new Inflater(true);
        inflater.setInput(compressedData);

        byte[] output = new byte[(int) expectedSize];
        try {
            int inflated = inflater.inflate(output);
            if (inflated != expectedSize) {
                throw new IOException("ZIP解压不完整，预期:" + expectedSize + ", 实际:" + inflated);
            }
            return output;
        } catch (DataFormatException e) {
            throw new IOException("ZIP数据格式错误", e);
        } finally {
            inflater.end();
        }
    }
}
