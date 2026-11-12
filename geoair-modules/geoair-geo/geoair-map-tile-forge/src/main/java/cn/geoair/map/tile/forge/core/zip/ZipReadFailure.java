package cn.geoair.map.tile.forge.core.zip;

/** 不存在是正常查询结果，其余失败必须保留实际阶段，不能伪装成瓦片不存在。 */
public enum ZipReadFailure {
    INVALID_PATH, INVALID_ARCHIVE, ROOT_NOT_FOUND, RESOURCE_NOT_FOUND,
    SOURCE_CHANGED, READ_FAILED, READ_TIMEOUT, EXTRACTION_FAILED, WAIT_TIMEOUT, INTERRUPTED
}
