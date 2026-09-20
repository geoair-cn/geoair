package cn.geoair.map.tile.forge.core.zip;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.geoair.map.tile.forge.core.s3.S3ClientGetter;
import cn.hutool.extra.spring.SpringUtil;
import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
 

import java.io.ByteArrayOutputStream;
import java.io.IOException;

 
public class S3CompressionHandler extends AbstractZipCompressionHandler {
    public static GiLogger log = GirLoggerFactory.getLogger();

    /**
     * 单次读取的最多重试次数，不含首次尝试
     */
    private static final int MAX_READ_RETRIES = 3;

    /**
     * 重试的初始退避毫秒数，之后按倍数递增
     */
    private static final long RETRY_BASE_BACKOFF_MILLIS = 300L;

    /**
     * 被限流的状态码，值得退避重试
     */
    private static final int HTTP_STATUS_THROTTLED = 429;

    static S3ClientGetter s3ClientGetter;

    public S3CompressionHandler() {
        if (s3ClientGetter == null) {
            s3ClientGetter = SpringUtil.getBean(S3ClientGetter.class);
        }
    }

    /**
     * 读取指定范围内的数据
     * <p>
     * S3 的 Range 请求在两类情况下会拿不到完整数据：一是网络抖动导致请求直接失败，
     * 二是服务端提前断流但没有抛异常，返回的字节数少于请求的范围。
     * 这里做了三重处理：可恢复的错误退避重试、短读从断点继续、最后按期望长度校验。
     *
     * @param source 对象键名
     * @param start  起始字节，含
     * @param end    结束字节，含
     * @return 范围内的数据，长度必定等于 end - start + 1
     * @throws IOException 重试耗尽或遇到不可恢复的错误
     */
    @Override
    protected byte[] readRange(String source, long start, long end) throws IOException {
        if (start > end) {
            throw new IllegalArgumentException("无效的范围：start=" + start + ", end=" + end);
        }

        long expectedLength = end - start + 1;
        ByteArrayOutputStream collector = new ByteArrayOutputStream();
        long currentOffset = start;
        int retry = 0;

        while (currentOffset <= end) {
            try {
                // 进度按整段请求的长度算，断点续读时已读到的部分算作已完成，进度不会回退
                byte[] part = readRangeOnce(source, currentOffset, end, expectedLength, collector.size());
                if (part.length == 0) {
                    // 服务端把连接关了但没报错，这里按失败处理，交给下面的重试分支
                    throw new IOException("S3返回了空数据");
                }
                collector.write(part, 0, part.length);
                // 从断点续读，已经读到的部分不会重复拉取
                currentOffset += part.length;
                retry = 0;
            } catch (IOException e) {
                retry++;
                if (retry > MAX_READ_RETRIES || !isRetryable(e)) {
                    log.error("读取S3数据失败，重试耗尽：source={}，range=[{},{}]", source, currentOffset, end, e);
                    throw e;
                }
                long backoffMillis = RETRY_BASE_BACKOFF_MILLIS << (retry - 1);
                log.warn("读取S3数据出错，等待{}ms后发起第{}次重试：source={}，断点offset={}",
                        backoffMillis, retry, source, currentOffset, e);
                sleepQuietly(backoffMillis);
            }
        }

        byte[] data = collector.toByteArray();
        if (data.length != expectedLength) {
            throw new IOException("读取S3数据长度不符：source=" + source
                    + "，range=[" + start + "," + end + "]，期望=" + expectedLength + "，实际=" + data.length);
        }
        return data;
    }

    /**
     * 发起一次 Range 请求，返回的字节数可能少于请求的范围
     *
     * @param progressTotal 整段请求的总字节数，用于进度上报
     * @param progressBase  本次尝试之前已经读到的字节数，用于进度上报
     */
    private byte[] readRangeOnce(String source, long start, long end, long progressTotal, long progressBase) throws IOException {
        GetObjectRequest request = new GetObjectRequest(s3ClientGetter.getDefaultBucket(), source)
                .withRange(start, end);

        try (S3Object s3Object = s3ClientGetter.getClient().getObject(request);
             S3ObjectInputStream in = s3Object.getObjectContent();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            long readInAttempt = 0;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
                readInAttempt += bytesRead;
                reportReadProgress(progressTotal, progressBase + readInAttempt);
            }
            return out.toByteArray();
        }
    }

    /**
     * 判断异常是否值得重试
     * <p>
     * AWS SDK 自带的重试策略基本只覆盖请求阶段，一旦已经开始读响应体，
     * 超时和连接重置大多会直接冒泡到调用方，所以在应用层再兜一层。
     */
    private static boolean isRetryable(Throwable t) {
        if (t instanceof AmazonS3Exception) {
            int statusCode = ((AmazonS3Exception) t).getStatusCode();
            return statusCode >= 500 || statusCode == HTTP_STATUS_THROTTLED;
        }
        if (t instanceof SdkClientException) {
            // SDK 把 SocketTimeoutException、连接重置这类异常包在这一层
            Throwable cause = t.getCause();
            return cause == null || cause instanceof IOException;
        }
        // 读响应体阶段的超时、连接重置，多数以 IOException 的形态冒上来，同样值得重试。
        // 404、403 这类业务错误是 AmazonS3Exception，走到这里之前就已经抛出去了。
        return t instanceof IOException;
    }

    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public long getFileSize(String source) {
        try {
            return s3ClientGetter.getClient().getObjectMetadata(s3ClientGetter.getDefaultBucket(), source).getContentLength();
        } catch (Exception e) {
            log.error("获取S3文件大小失败，source:{}", source, e);
            throw new RuntimeException("获取S3文件大小失败", e);
        }
    }


}
