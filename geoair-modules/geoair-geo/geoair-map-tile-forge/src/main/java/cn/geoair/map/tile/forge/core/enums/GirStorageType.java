package cn.geoair.map.tile.forge.core.enums;

import cn.geoair.base.data.GiVisualValuable;

// StorageType.java
public enum GirStorageType implements GiVisualValuable<String> {

    LOCAL_ZIP("local_zip"),
    S3_ZIP("s3_zip"),
    LOCAL_UNZIPPED("local_unzipped"),
    S3_UNZIPPED("s3_unzipped"),

    ;

    private final String value;

    GirStorageType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}


