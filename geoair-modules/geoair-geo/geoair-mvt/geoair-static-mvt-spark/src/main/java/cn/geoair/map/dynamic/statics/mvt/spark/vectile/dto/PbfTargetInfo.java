package cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto;

import cn.geoair.map.dynamic.mvt.tools.model.PPbfType;
import java.io.Serializable;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author ：张逢吉
 * @date ：Created in 2022/1/21 17:47 @description： TODO
 */
@Data
@Accessors(chain = true)
public class PbfTargetInfo implements Serializable {

    // 是否仅仅生成一个pbf，用于节省内存
    private boolean isOnly = false;

    // 生成的pbf类型
    private PPbfType pPbfType = PPbfType.rootPbf;

    // 是否保存要素列表
    private boolean saveFeatureList = false;

    /**
     * 创建一个新的默认 PbfTargetInfo 实例（线程安全：每次返回独立对象）。
     */
    public static PbfTargetInfo newInstance() {
        return new PbfTargetInfo()
                .setSaveFeatureList(false)
                .setPPbfType(PPbfType.rootPbf)
                .setOnly(false);
    }

    /**
     * @deprecated 使用 {@link #newInstance()} 替代，避免多线程共享可变状态。
     */
    @Deprecated
    public static PbfTargetInfo getInstance() {
        return newInstance();
    }
}
