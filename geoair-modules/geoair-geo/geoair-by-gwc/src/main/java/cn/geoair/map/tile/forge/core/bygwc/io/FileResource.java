/**
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU Lesser General Public License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * <p>You should have received a copy of the GNU Lesser General Public License along with this
 * program. If not, see <http://www.gnu.org/licenses/>.
 *
 * <p>Copyright 2019
 */
package cn.geoair.map.tile.forge.core.bygwc.io;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

public class FileResource implements cn.geoair.map.tile.forge.core.bygwc.io.Resource {

    private final File file;

    public FileResource(File file) {
        this.file = file;
    }

    /**
     * @see Resource#getLastModified()
     */
    public long getLastModified() {
        return file.lastModified();
    }

    /**
     * @see Resource#getSize()
     */
    public long getSize() {
        // avoid a (relatively expensive) call to File.exists(), file.length() returns 0 if the file
        // doesn't exist anyway
        long size = file.length();
        return size == 0 ? -1 : size;
    }

    public long transferTo(WritableByteChannel target) throws IOException {
        // FileLock lock = in.lock();

        try (FileInputStream fis = new FileInputStream(file);
                FileChannel in = fis.getChannel(); ) {
            final long size = in.size();
            long written = 0;
            while ((written += in.transferTo(written, size, target)) < size) {
                ;
            }
            return size;
        }
    }

    public long transferFrom(ReadableByteChannel channel) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(file);
                FileChannel out = fos.getChannel();
                FileLock lock = out.lock(); ) {
            final int buffsize = 4096;
            long position = 0;
            long read;
            while ((read = out.transferFrom(channel, position, buffsize)) > 0) {
                position += read;
            }
            return position;
        }
    }

    public InputStream getInputStream() throws IOException {
        return new FileInputStream(file);
    }

    public OutputStream getOutputStream() throws IOException {
        return new FileOutputStream(file);
    }

    @Override
    public byte[] getByteData() throws IOException {
        // 检查文件是否存在
        if (!file.exists()) {
            throw new FileNotFoundException("文件不存在: " + file.getAbsolutePath());
        }

        // 检查文件大小
        long fileSize = file.length();
        if (fileSize > Integer.MAX_VALUE) {
            throw new IOException("文件过大，无法转换为字节数组: " + fileSize + " bytes");
        }

        byte[] data = new byte[(int) fileSize];
        try (FileInputStream fis = new FileInputStream(file);
                BufferedInputStream bis = new BufferedInputStream(fis)) {

            int offset = 0;
            int remaining = data.length;
            while (remaining > 0) {
                int read = bis.read(data, offset, remaining);
                if (read == -1) {
                    break;
                }
                offset += read;
                remaining -= read;
            }

            // 验证读取的字节数
            if (offset != data.length) {
                throw new IOException("读取文件不完整，期望: " + data.length + ", 实际: " + offset);
            }

            return data;
        }
    }

    public File getFile() {
        return file;
    }
}
