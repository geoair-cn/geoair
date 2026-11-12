package cn.geoair.map.tile.forge.core.zip;

import cn.geoair.map.tile.forge.core.caches.S3CacheProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import java.io.ByteArrayInputStream;
import java.lang.reflect.Proxy;
import java.nio.file.*;
import java.util.concurrent.atomic.*;
import static org.junit.Assert.*;

/** 使用真实 TransferManager 和模拟 S3 客户端验证文件传输，不连接外部存储。 */
public class S3FileCacheProviderTest {
    @Rule public TemporaryFolder folder=new TemporaryFolder();

    @Test public void transferManagerUploadsAFileAndRestoresThroughAnInputStream() throws Exception {
        AtomicReference<byte[]> stored=new AtomicReference<>();
        AtomicInteger puts=new AtomicInteger();
        AmazonS3 client=(AmazonS3)Proxy.newProxyInstance(AmazonS3.class.getClassLoader(),new Class<?>[]{AmazonS3.class},(proxy,method,args)->{
            if("doesBucketExistV2".equals(method.getName())) return true;
            if("getRegionName".equals(method.getName())) return "us-east-1";
            if("putObject".equals(method.getName())) {
                PutObjectRequest request=(PutObjectRequest)args[0];
                assertEquals("prefix/index",request.getKey());
                assertNotNull(request.getFile());
                stored.set(Files.readAllBytes(request.getFile().toPath()));
                puts.incrementAndGet();
                PutObjectResult result=new PutObjectResult();result.setETag("test");return result;
            }
            if("getObject".equals(method.getName())) {
                S3Object object=new S3Object();ObjectMetadata metadata=new ObjectMetadata();
                metadata.setContentLength(stored.get().length);object.setObjectMetadata(metadata);
                object.setObjectContent(new ByteArrayInputStream(stored.get()));return object;
            }
            if("toString".equals(method.getName())) return "TestS3";
            throw new UnsupportedOperationException(method.getName());
        });
        S3CacheProvider provider=new S3CacheProvider(client,"bucket","prefix","");
        Path source=folder.getRoot().toPath().resolve("index.sqlite");
        Files.write(source,new byte[]{1,2,3,4});
        provider.putFile("index",source,-1);
        assertEquals(1,puts.get());
        Path target=folder.getRoot().toPath().resolve("restored.sqlite");
        assertTrue(provider.copyTo("index",target));
        assertArrayEquals(new byte[]{1,2,3,4},Files.readAllBytes(target));
    }
}
