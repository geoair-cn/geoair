package cn.geoair.map.tile.forge.core.zip;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.geoair.map.tile.forge.core.s3.S3ClientGetter;
import cn.hutool.extra.spring.SpringUtil;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class S3CompressionHandler extends AbstractZipCompressionHandler {
    public static GiLogger log = GirLoggerFactory.getLogger();

    static S3ClientGetter s3ClientGetter;

    public S3CompressionHandler() {
        if (s3ClientGetter == null) {
            s3ClientGetter = SpringUtil.getBean(S3ClientGetter.class);
        }
    }

    @Override
    public List<byte[]> readFileByChunks(
            String source, long startOffset, long totalSize, int chunkSize) throws IOException {
        if (chunkSize <= 0 || totalSize <= 0 || startOffset < 0) {
            throw new IllegalArgumentException(
                    "无效的参数：chunkSize="
                            + chunkSize
                            + ", totalSize="
                            + totalSize
                            + ", startOffset="
                            + startOffset);
        }

        List<byte[]> chunks = new ArrayList<>();
        long remaining = totalSize;
        long currentOffset = startOffset;

        while (remaining > 0) {
            int readSize = (int) Math.min(chunkSize, remaining);
            byte[] chunk = readRange(source, currentOffset, currentOffset + readSize - 1);
            chunks.add(chunk);

            currentOffset += readSize;
            remaining -= readSize;
        }
        return chunks;
    }

    @Override
    protected byte[] readRange(String source, long start, long end) throws IOException {
        if (start > end) {
            throw new IllegalArgumentException("无效的范围：start=" + start + ", end=" + end);
        }

        GetObjectRequest request =
                new GetObjectRequest(s3ClientGetter.getDefaultBucket(), source)
                        .withRange(start, end);

        try (S3Object s3Object = s3ClientGetter.getClient().getObject(request);
                S3ObjectInputStream in = s3Object.getObjectContent();
                ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
            return out.toByteArray();
        }
    }

    @Override
    public long getFileSize(String source) {
        try {
            return s3ClientGetter
                    .getClient()
                    .getObjectMetadata(s3ClientGetter.getDefaultBucket(), source)
                    .getContentLength();
        } catch (Exception e) {
            log.error("获取S3文件大小失败，source:{}", source, e);
            throw new RuntimeException("获取S3文件大小失败", e);
        }
    }
}
