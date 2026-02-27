package cn.geoair.map.dynamic.file.core.enums;

/**
 * 转换状态枚举
 */
public enum TranStatus {
    INIT, // 初始化
    RUNNING, // 运行中
    SUCCESS, // 成功完成
    FAILED, // 失败
    ABORTED, // 被终止
    TIMEOUT // 超时
}
