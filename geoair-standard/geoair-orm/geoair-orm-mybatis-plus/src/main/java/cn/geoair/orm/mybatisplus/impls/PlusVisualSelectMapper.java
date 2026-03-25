package cn.geoair.orm.mybatisplus.impls;

import cn.geoair.base.data.model.support.GirVisualModelKid;
import cn.geoair.base.gpa.dao.GiVisualSelectDao;
import cn.geoair.base.gpa.entity.GiEntityVisuable;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.io.Serializable;
import java.util.List;

public interface PlusVisualSelectMapper<T extends GiEntityVisuable<PK>, PK extends Serializable>
        extends GiVisualSelectDao<T, PK>, BaseMapper<T> {

    @SuppressWarnings("rawtypes")
    default List<GirVisualModelKid> selectVisualModel(String displayQuery, String[] containKeys) {

        return null;
    }
}
