package cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto;

import org.junit.Assert;
import org.junit.Test;

/**
 * 第三方约定的 {@code #jdbc:} 协议兼容性测试。
 */
public class ProtocolUrlTest {

    @Test
    public void shouldKeepAgreedPostgreSqlProtocolAndBuildJdbcUrl() {
        String protocol = "#jdbc:postgresql://postgres#secret/10.0.0.1:5432/mydb/public/tile_cache";

        ProtocolUrl parsed = new ProtocolUrl(protocol);

        Assert.assertEquals(protocol, parsed.toString());
        Assert.assertEquals("postgres", parsed.getUsername());
        Assert.assertEquals("public", parsed.getSchema());
        Assert.assertEquals("tile_cache", parsed.getTableName());
        Assert.assertEquals("public.tile_cache", parsed.getTableForSql());
        Assert.assertEquals("jdbc:postgresql://10.0.0.1:5432/mydb?currentSchema=public",
                parsed.toJdbcUrl());
    }

    @Test
    public void shouldKeepEmptySchemaSlotForTableOnlyProtocol() {
        String protocol = "#jdbc:mysql://root#secret/10.0.0.1:3306/gisdb//tile_cache";

        ProtocolUrl parsed = new ProtocolUrl(protocol);

        Assert.assertNull(parsed.getSchema());
        Assert.assertEquals("tile_cache", parsed.getTableName());
        Assert.assertEquals("tile_cache", parsed.getTableForSql());
        Assert.assertEquals(protocol, parsed.toString());
        Assert.assertEquals("jdbc:mysql://10.0.0.1:3306/gisdb", parsed.toJdbcUrl());
    }

    @Test
    public void shouldNormalizePostgisToPostgreSqlJdbcProtocol() {
        ProtocolUrl parsed = new ProtocolUrl(
                "#jdbc:postgis://postgres#secret/10.0.0.1:5432/mydb/public/tile_cache");

        Assert.assertEquals("jdbc:postgresql://10.0.0.1:5432/mydb?currentSchema=public",
                parsed.toJdbcUrl());
    }

    @Test
    public void shouldSupportBracketedIpv6Host() {
        ProtocolUrl parsed = new ProtocolUrl(
                "#jdbc:postgresql://postgres#secret/[2001:db8::1]:5432/mydb/public/tile_cache");

        Assert.assertEquals("[2001:db8::1]", parsed.getHost());
        Assert.assertEquals("jdbc:postgresql://[2001:db8::1]:5432/mydb?currentSchema=public",
                parsed.toJdbcUrl());
    }
}
