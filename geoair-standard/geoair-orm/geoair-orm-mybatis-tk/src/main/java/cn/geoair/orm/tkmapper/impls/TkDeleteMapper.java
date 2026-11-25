package cn.geoair.orm.tkmapper.impls;

import cn.geoair.base.Gir;
import cn.geoair.base.exception.GirException;
import cn.geoair.base.gpa.dao.GiDeleteDao;
import cn.geoair.base.gpa.entity.GiEntityRemovable;
import cn.geoair.base.util.GutilObject;
import cn.geoair.orm.mybatis.impls.MyBatisMapper;

import tk.mybatis.mapper.common.base.BaseDeleteMapper;
import tk.mybatis.mapper.common.example.DeleteByExampleMapper;
import tk.mybatis.mapper.entity.EntityColumn;
import tk.mybatis.mapper.entity.Example;
import tk.mybatis.mapper.mapperhelper.EntityHelper;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public interface TkDeleteMapper<T extends GiEntityRemovable<PK>, PK extends Serializable>
        extends MyBatisMapper<T, PK>,
                GiDeleteDao<T, PK>,
                BaseDeleteMapper<T>,
                DeleteByExampleMapper<T> /* ,DeleteByIdsMapper<T> */ {

    /**
     * 根据实体属性作为条件进行删除，查询条件使用等号
     *
     * @param t
     * @return
     */
    @Override
    default int gtcDeleteBy(T t) {
        return this.delete(t);
    }

    /**
     * 根据主键删除记录
     *
     * @param key
     * @return
     */
    @Override
    default void gtcDeleteByPK(PK pk) {
        if (GutilObject.isEmpty(pk)) {
            Gir.log.error("删除的时候主键为空 ,不进行操作");
        } else {
            this.deleteByPrimaryKey(pk);
        }
    }

    /**
     * 根据主键批量删除
     *
     * @param key
     * @return
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    default void gtcDeleteByPK(List<PK> pks) {
        if (GutilObject.isEmpty(pks)) {
            Gir.log.error("删除的时候主键列表为空 ,可能会导致全表删除，不进行操作");
            return;
        }
        Set<EntityColumn> cs = EntityHelper.getPKColumns(this.getModelClass());
        if (cs.size() == 1) {
            if (this instanceof tk.mybatis.mapper.additional.idlist.DeleteByIdListMapper) {
                /** 根据主键字符串进行删除，类中只有存在一个带有@Id注解的字段 */
                ((tk.mybatis.mapper.additional.idlist.DeleteByIdListMapper) this)
                        .deleteByIdList(pks);
            } else {
                throw new GirException(
                        "根据主键批量删除如果只有一个带有@Id注解的字段，{} 需要实现tk.mybatis.mapper.additional.idlist.DeleteByIdListMapper接口",
                        this.getClass().getName());
            }
        } else {
            Set<PK> ids = new HashSet<>(pks);
            for (PK pk : ids) {
                this.deleteByPrimaryKey(pk);
            }
        }
    }

    /**
     * 删除所有数据
     *
     * @param key
     * @return
     */
    @Override
    default void gtcDeleteAll() {
        Example ex = new Example(this.getModelClass());
        // ex.and(ex.createCriteria().andCondition("1=1"));
        this.deleteByExample(ex);
    }
}
