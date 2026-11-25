package cn.geoair.comp.dynamic.ds.readwrite.enums;

/**
 * @author ：zhangjun
 * @date ：Created in 2026/5/28 17:07
 * @description： SQL操作类型枚举
 */
public enum SQLType {
    READ, // 读操作（SELECT）
    WRITE, // 写操作（INSERT、UPDATE、DELETE等）
    UNKNOWN // 未知类型
}
