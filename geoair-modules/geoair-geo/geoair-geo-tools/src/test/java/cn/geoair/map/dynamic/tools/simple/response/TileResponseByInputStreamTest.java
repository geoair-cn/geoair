package cn.geoair.map.dynamic.tools.simple.response;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.junit.Assert;
import org.junit.Test;

/** {@link TileResponseByInputStream} 的流长度与缓存语义测试。 */
public class TileResponseByInputStreamTest {

    @Test
    public void shouldNotTreatAvailableAsContentLengthAndShouldCacheConsumedStream()
            throws IOException {
        byte[] content = new byte[] {1, 2, 3};
        ZeroAvailableInputStream inputStream = new ZeroAvailableInputStream(content);
        TileResponseByInputStream response = TileResponseByInputStream.success(inputStream, null);

        Assert.assertTrue(response.isValid());
        Assert.assertNull(response.getContentLength());
        Assert.assertArrayEquals(content, response.toByteArrays());
        Assert.assertTrue(inputStream.closed);
        Assert.assertNull(response.getInputStream());
        Assert.assertEquals(Long.valueOf(content.length), response.getContentLength());

        InputStream cachedStream = response.toInputStream();
        Assert.assertNotNull(cachedStream);
        Assert.assertArrayEquals(content, readAll(cachedStream));
    }

    @Test
    public void shouldKeepExplicitContentLengthWhenProvided() {
        TileResponseByInputStream response =
                TileResponseByInputStream.success(
                        new ByteArrayInputStream(new byte[] {1, 2, 3}), null, 3L);

        Assert.assertEquals(Long.valueOf(3L), response.getContentLength());
    }

    @Test
    public void shouldNotMarkMissingInputStreamAsExisting() {
        TileResponseByInputStream response = TileResponseByInputStream.success(null, null);

        Assert.assertFalse(response.isExists());
        Assert.assertFalse(response.isValid());
        Assert.assertNull(response.getContentLength());
    }

    private byte[] readAll(InputStream inputStream) throws IOException {
        byte[] content = new byte[3];
        Assert.assertEquals(3, inputStream.read(content));
        Assert.assertEquals(-1, inputStream.read());
        return content;
    }

    private static final class ZeroAvailableInputStream extends ByteArrayInputStream {
        private boolean closed;

        private ZeroAvailableInputStream(byte[] content) {
            super(content);
        }

        @Override
        public int available() {
            return 0;
        }

        @Override
        public void close() throws IOException {
            closed = true;
            super.close();
        }
    }
}
