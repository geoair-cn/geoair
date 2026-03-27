package cn.geoair.orm.springjpa.impls;

import cn.geoair.base.data.model.support.GirVisualModelKid;
import cn.geoair.base.data.page.GiPageParam;
import cn.geoair.base.data.page.GiPager;
import cn.geoair.base.gpa.dao.GiVisualSelectDao;
import cn.geoair.base.gpa.entity.GiEntityVisuable;
import java.io.Serializable;
import java.util.List;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;

public interface VisualSelectRepository<T extends GiEntityVisuable<PK>, PK extends Serializable>
        extends CrudRepository<T, PK>, JpaSpecificationExecutor<T>, GiVisualSelectDao<T, PK> {

    @SuppressWarnings("rawtypes")
    default List<GirVisualModelKid> searchVisualModel(String displayQuery, String[] containKeys) {
        return null;
    }

    @SuppressWarnings("rawtypes")
    default GiPager<GirVisualModelKid> searchVisualPage(
            String displayQuery, String[] containKeys, GiPageParam pageParam) {
        return null;
    }
}
