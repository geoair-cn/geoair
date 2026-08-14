package cn.geoair.comp.db.service.core.test;

import static org.junit.Assert.*;

import cn.geoair.comp.db.service.core.basic.util.IPUtil;
import org.junit.Test;

/**
 * {@link IPUtil} 的单元测试。
 */
public class IPUtilTest {

    @Test
    public void testGetIpAddress() {
        String ip = IPUtil.getIpAddress();
        assertNotNull(ip);
        // 本机至少有一个 IPv4 地址，或返回空字符串
        assertTrue(ip.isEmpty() || ip.matches("\\d+\\.\\d+\\.\\d+\\.\\d+"));
    }
}
