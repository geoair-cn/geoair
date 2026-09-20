package cn.geoair.map.tile.forge.core.zip.decompression;


import java.io.IOException;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

/**
 * ZIP(DEFLATE算法)解压适配器
 */
public class ZipHandler implements DecompressionHandler {

    /**
     * byte[] 能申请到的最大长度。留 8 个字节的余量，跟 JDK 里 ArrayList 等地方的口径保持一致。
     */
    private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;

    @Override
    public byte[] decompress(byte[] compressedData, long expectedSize) throws IOException {
        DecompressionLimits.validateExpectedSize(expectedSize);
        // 解压结果要一次性塞进 byte[]，长度超过 int 上限时 (int) 强转会变成负数，
        // 抛出来的是 NegativeArraySizeException，光看堆栈根本看不出是大小超限，这里提前拦住。
        if (expectedSize > MAX_ARRAY_SIZE) {
            throw new IOException("解压结果超过单个字节数组的上限，无法一次性解压，预期大小: " + expectedSize);
        }
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
