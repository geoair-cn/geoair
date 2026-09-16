package cn.geoair.map.tile.forge.fuser.enums;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 瓦片服务转换操作。
 */
public enum TileServiceOperation {

    /** Google 网格转 4326 网格。 */
    GOOGLE_TO_4326("google-to-4326"),

    /** Grid4490 网格转 3857 网格。 */
    GRID4490_TO_3857("grid4490-to-3857"),

    /** 源网格与请求网格一致时原生直出。 */
    SAME_GRID("same-grid");

    private static final Map<String, TileServiceOperation> CODE_MAP;

    static {
        Map<String, TileServiceOperation> operations = new HashMap<>();
        for (TileServiceOperation operation : values()) {
            operations.put(operation.code, operation);
        }
        CODE_MAP = Collections.unmodifiableMap(operations);
    }

    private final String code;

    TileServiceOperation(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    /** 未识别的操作返回 {@code null}。 */
    public static TileServiceOperation fromCode(String code) {
        if (code == null) {
            return null;
        }
        return CODE_MAP.get(code.trim().toLowerCase());
    }

    /** 未识别的操作抛出包含非法值的明确异常。 */
    public static TileServiceOperation requireFromCode(String code) {
        TileServiceOperation operation = fromCode(code);
        if (operation == null) {
            throw new IllegalArgumentException("Unsupported tile-fuser operation: " + code);
        }
        return operation;
    }
}
