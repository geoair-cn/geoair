package cn.geoair.base.gpa.id;

import java.io.Serializable;

public interface GiEntityIdGenerator<PK extends Serializable> {

    /**
     * 自定义ID生成
     *
     * @return
     */
    public PK generatorId();
}
