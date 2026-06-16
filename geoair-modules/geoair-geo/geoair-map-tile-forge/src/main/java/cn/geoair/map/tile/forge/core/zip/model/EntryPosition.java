package cn.geoair.map.tile.forge.core.zip.model;

import lombok.Data;

/**
 * @author ：张俊
 * @date ：Created in 2026/4/27 15:52
 * @description： TODO
 */
@Data
public class EntryPosition {
    public long offset;
    public int totalLength;

    public EntryPosition(long offset, int totalLength) {
        this.offset = offset;
        this.totalLength = totalLength;
    }
}
