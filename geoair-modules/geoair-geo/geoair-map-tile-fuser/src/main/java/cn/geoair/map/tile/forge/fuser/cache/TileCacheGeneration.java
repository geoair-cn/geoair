package cn.geoair.map.tile.forge.fuser.cache;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/** 单进程内缓存代次管理。一份指针由一个实例管理，缓存文件如何命名和清理由调用方决定。 */
public final class TileCacheGeneration {
    private final Path pointer;
    private final ReentrantReadWriteLock guard = new ReentrantReadWriteLock(true);
    private volatile long current;

    public TileCacheGeneration(Path pointer) throws IOException {
        this.pointer = pointer.toAbsolutePath().normalize();
        try {
            current =
                    Long.parseLong(
                            new String(Files.readAllBytes(this.pointer), StandardCharsets.UTF_8)
                                    .trim());
            if (current <= 0) throw new IOException("缓存代次必须大于零");
        } catch (NoSuchFileException missing) {
            current = 1;
        } catch (NumberFormatException invalid) {
            throw new IOException("缓存代次文件无效", invalid);
        }
    }

    public long current() {
        return current;
    }

    /** 切换代次后，仍在出图的旧请求只能将结果返回给调用方。 */
    public boolean writeIfCurrent(long expected, Runnable write) {
        Objects.requireNonNull(write, "缓存写入操作不能为空");
        guard.readLock().lock();
        try {
            if (current != expected) return false;
            write.run();
            return true;
        } finally {
            guard.readLock().unlock();
        }
    }

    /** 原子发布新代次，持久化失败时保留原代次。 */
    public long advance() throws IOException {
        guard.writeLock().lock();
        try {
            long next = Math.addExact(current, 1L);
            Path directory = Files.createDirectories(pointer.getParent());
            Path temporary = Files.createTempFile(directory, "cache-generation-", ".tmp");
            try {
                ByteBuffer bytes = StandardCharsets.UTF_8.encode(Long.toString(next));
                try (FileChannel file = FileChannel.open(temporary, StandardOpenOption.WRITE)) {
                    while (bytes.hasRemaining()) file.write(bytes);
                    file.force(true);
                }
                Files.move(
                        temporary,
                        pointer,
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
                current = next;
                return next;
            } finally {
                Files.deleteIfExists(temporary);
            }
        } finally {
            guard.writeLock().unlock();
        }
    }
}
