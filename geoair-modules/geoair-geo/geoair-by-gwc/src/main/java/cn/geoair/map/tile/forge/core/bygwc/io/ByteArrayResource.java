package cn.geoair.map.tile.forge.core.bygwc.io;

import cn.hutool.core.lang.Assert;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.util.Arrays;

public class ByteArrayResource
        implements cn.geoair.map.tile.forge.core.bygwc.io.Resource, Serializable {

    private byte[] data;

    private int offset;

    private int length;

    private long lastModified = System.currentTimeMillis();

    /** Create a new empty ByteArrayResource */
    public ByteArrayResource() {
        this((byte[]) null);
    }

    /**
     * Create a new ByteArrayResource from the given byte array.
     *
     * @param data The array of bytes. It will be retained by the Resource for storage.
     */
    public ByteArrayResource(byte[] data) {
        this.data = data;
        this.offset = 0;
        this.length = data == null ? 0 : data.length;
    }

    public ByteArrayResource(String string) {
        //  用于 解决中文乱码问题 ，
        //  utf-8 编码在 string.getBytes()
        //  这个方法在 windows 环境默认使用的是 GBK ，所以要在代码里面强制指定为 UTF-8
        //  详情请参见 Charset.defaultCharset().name();
        byte[] bytes = string.getBytes(Charset.forName("UTF-8"));
        this.data = bytes;
        this.offset = 0;
        this.length = data == null ? 0 : data.length;
    }

    /**
     * Create a new ByteArrayResource from the given byte array.
     *
     * @param data the array of bytes. It will be retained by the Resource for storage.
     * @param offset the beginning of the portion of the array to use as content
     * @param length the length of the portion of the array to use as content
     */
    public ByteArrayResource(byte[] data, int offset, int length) {
        this.data = data;
        if (data == null) {
            this.data = null;
            this.offset = 0;
            this.length = 0;
        } else {
            this.data = data;
            Assert.isTrue(offset < data.length, "Offset should be less than data length");
            Assert.isTrue(
                    offset + length <= data.length, "Offset + length should be less than length");
            this.offset = offset;
            this.length = length;
        }
    }

    /** Create a new empty ByteArrayResource with a particular initial capacity. */
    public ByteArrayResource(final int initialCapacity) {
        this(new byte[initialCapacity], 0, 0);
    }

    /**
     * @see Resource#getLastModified()
     */
    public long getLastModified() {
        return lastModified;
    }

    /**
     * @see Resource#getSize()
     */
    public long getSize() {
        return length;
    }

    /**
     * @see Resource#transferTo(WritableByteChannel)
     */
    public long transferTo(WritableByteChannel channel) throws IOException {
        if (length > 0) {
            ByteBuffer buffer = ByteBuffer.wrap(data, offset, length);
            long written = 0;
            while ((written += channel.write(buffer)) < length) {
                ;
            }
        }
        return length;
    }

    /**
     * @see Resource#transferFrom(ReadableByteChannel)
     */
    public long transferFrom(ReadableByteChannel channel) throws IOException {
        if (channel instanceof FileChannel) {
            FileChannel fc = (FileChannel) channel;
            offset = 0;
            length = (int) fc.size();
            if (data == null || data.length < length) {
                data = new byte[length];
            }
            ByteBuffer buffer = ByteBuffer.wrap(data);
            int read = 0;
            while ((read += channel.read(buffer)) < length) {
                ;
            }
        } else {
            offset = 0;
            length = 0;
            if (data == null) {
                data = new byte[4096];
            }
            ByteBuffer buffer = ByteBuffer.wrap(data);
            int numRead = 0;
            while ((numRead = channel.read(buffer)) > -1) {
                length += numRead;
                if (buffer.position() == buffer.capacity()) {
                    int position = buffer.position();
                    expand();
                    buffer = ByteBuffer.wrap(data);
                    buffer.position(position);
                }
            }
        }
        return length;
    }

    /**
     * @see Resource#getInputStream()
     */
    public SeekableInputStream getInputStream() throws IOException {
        if (data == null) {
            throw new IOException("no data");
        }
        return new SeekableInputStream(this);
    }

    /** Get the contents of the resource. */
    public byte[] getContents() {
        if (data == null || length == 0) {
            return null;
        }
        if (offset == 0 && data.length == length) {
            return data;
        }
        byte[] buff = new byte[length];
        System.arraycopy(data, offset, buff, 0, length);
        return buff;
    }

    /** Discard the contents. */
    public void truncate() {
        offset = 0;
        length = 0;
    }

    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }

    /**
     * @see Resource#getOutputStream()
     */
    public OutputStream getOutputStream() throws IOException {
        return new SeekableOutputStream(this);
    }

    @Override
    public byte[] getByteData() throws IOException {
        return Arrays.copyOf(data, length);
    }

    private void expand() {
        if (data == null) {
            data = new byte[4096];
            length = 0;
            offset = 0;
        } else {
            byte[] newdata = new byte[(int) (data.length * 1.5)];
            System.arraycopy(data, 0, newdata, 0, data.length);
            data = newdata;
        }
    }

    public static final class SeekableInputStream extends ByteArrayInputStream {

        private final long lower;

        public SeekableInputStream(ByteArrayResource res) {
            super(res.data == null ? new byte[0] : res.data, res.offset, res.length);
            lower = res.offset;
        }

        public void seek(long pos) throws IOException {
            if (pos < 0 || pos > super.count) {
                throw new IOException(
                        "Can't seek to pos " + pos + ". buffer is 0.." + (super.count - 1));
            }
            super.pos = (int) (lower + pos);
        }

        public int length() {
            return super.count;
        }
    }

    public static final class SeekableOutputStream extends OutputStream {

        private ByteArrayResource res;

        private int remaining;

        public SeekableOutputStream(ByteArrayResource res) {
            this.res = res;
            remaining = res.data == null ? 0 : res.data.length;
        }

        @Override
        public void write(int b) throws IOException {
            if (remaining == 0) {
                res.expand();
                remaining = res.data.length - res.length;
            }
            res.data[res.length++] = (byte) b;
            --remaining;
        }

        @Override
        public void write(byte[] buff, int off, int len) throws IOException {
            if (remaining < len) {
                while (remaining < len) {
                    this.res.expand();
                    remaining = res.data.length - res.length;
                }
            }
            System.arraycopy(buff, off, res.data, res.length, len);
            res.length += len;
            remaining -= len;
        }
    }
}
