package cn.geoair.map.dynamic.adv.orm;

import cn.geoair.base.gpa.dao.GiUpdateDao;
import cn.geoair.base.gpa.entity.GiEntityAlterable;
import cn.geoair.map.dynamic.adv.spring.GirSpringAdvExecutor;
import java.io.Serializable;
import java.util.List;

public interface AdvUpdateOrmOpt<T extends GiEntityAlterable<PK>, PK extends Serializable>
        extends GiUpdateDao<T, PK> {

    /**
     * 根据主键更新记录(更新所有字段)
     *
     * @param t
     * @return
     */
    @Override
    default int gtcUpdateByPK(T t) {

        return GirSpringAdvExecutor.getInstance().bUpdateByPK(t);
    }

    /**
     * 根据主键更新记录(更新不为Null的字段)
     *
     * @param t
     * @return
     */
    @Override
    default int gtcUpdateByPKSelective(T t) {
        return GirSpringAdvExecutor.getInstance().bUpdateByPKSelective(t);
    }

    /**
     * 根据主键批量更新
     *
     * @param records
     * @return
     */
    @Override
    default int gtcUpdateByPK(List<T> records) {
        GirSpringAdvExecutor.getInstance().bUpdateBatchByPK(records, u -> u.setBatchSize(200));
        return records.size();
    }

    /**
     * 根据主键批量更新(更新不为Null的字段)
     *
     * @param records
     * @return
     */
    @Override
    default int gtcUpdateByPKSelective(List<T> records) {
        GirSpringAdvExecutor.getInstance()
                .bUpdateBatchByPKSelective(records, u -> u.setBatchSize(200));
        return records.size();
    }
}
