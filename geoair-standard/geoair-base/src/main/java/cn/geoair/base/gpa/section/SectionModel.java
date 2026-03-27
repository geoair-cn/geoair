package cn.geoair.base.gpa.section;

import cn.geoair.base.data.model.GiModelable;
import java.io.Serializable;

/**
 * 分表模型接口
 *
 * <p>定义分表模型的基本结构，继承自GiModelable接口，添加了分表因子方法
 *
 * @author Ray
 * @date 2022-04-25
 * @param <PK> 主键类型，必须实现Serializable接口
 * @param <F> 分表因子类型
 */
public interface SectionModel<PK extends Serializable, F> extends GiModelable<PK> {

    /**
     * 获取分表因子
     *
     * @return 分表因子值
     */
    F factor();
}
