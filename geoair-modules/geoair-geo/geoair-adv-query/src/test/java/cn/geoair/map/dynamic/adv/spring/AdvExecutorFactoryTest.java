package cn.geoair.map.dynamic.adv.spring;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/** 注册式数据库方言工厂的无数据库契约测试。 */
public class AdvExecutorFactoryTest {

    @Test
    public void shouldResolveBuiltInAndCompatibleDatabaseProducts() {
        assertEquals("postgresql", AdvExecutorFactory.resolveDialectIdByProductName("PostgreSQL"));
        assertEquals("kingbase", AdvExecutorFactory.resolveDialectIdByProductName("KingbaseES"));
        assertEquals("opengauss", AdvExecutorFactory.resolveDialectIdByProductName("openGauss"));
        assertEquals(null, AdvExecutorFactory.resolveDialectIdByProductName("GaussDB"));
        assertEquals("mariadb", AdvExecutorFactory.resolveDialectIdByProductName("MariaDB"));
        assertEquals("sqlserver", AdvExecutorFactory.resolveDialectIdByProductName("Microsoft SQL Server"));
        assertEquals("oracle", AdvExecutorFactory.resolveDialectIdByProductName("Oracle"));
        assertEquals("dm", AdvExecutorFactory.resolveDialectIdByProductName("DM DBMS"));
    }

    @Test
    public void shouldExposeRegisteredProviderByStableDialectId() {
        AdvDialectProvider provider = AdvExecutorFactory.getProvider("KINGBASE");

        assertNotNull(provider);
        assertEquals("kingbase", provider.getDialectId());
    }
}
