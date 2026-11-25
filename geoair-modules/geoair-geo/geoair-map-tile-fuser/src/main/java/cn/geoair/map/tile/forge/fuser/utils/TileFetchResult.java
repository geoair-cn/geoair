package cn.geoair.map.tile.forge.fuser.utils;

import cn.geoair.map.tile.forge.core.bygwc.io.Resource;

/** HTTP 尝试结果，仅在传输实现内部使用，不暴露凭证。 */
final class TileFetchResult {
    final Resource resource;
    final int statusCode;
    final int bodySize;
    final String location;

    private TileFetchResult(Resource resource, int statusCode, int bodySize, String location) {
        this.resource = resource;
        this.statusCode = statusCode;
        this.bodySize = bodySize;
        this.location = location;
    }

    static TileFetchResult success(Resource resource, int size) {
        return new TileFetchResult(resource, 200, size, null);
    }

    static TileFetchResult failure(int status) {
        return new TileFetchResult(null, status, 0, null);
    }

    static TileFetchResult redirect(int status, String location) {
        return new TileFetchResult(null, status, 0, location);
    }
}
