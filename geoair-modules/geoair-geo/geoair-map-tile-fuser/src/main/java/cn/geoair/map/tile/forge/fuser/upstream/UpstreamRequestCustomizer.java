package cn.geoair.map.tile.forge.fuser.upstream;

/** 每次尝试调用，可注入动态 Token/签名。并发安全及凭证刷新由实现者维护。 */
@FunctionalInterface
public interface UpstreamRequestCustomizer {
    void customize(UpstreamTileRequest request);
}
