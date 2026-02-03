package cn.geoair.gtc.orm.mybatisplus.impls;

import java.io.Serializable;
import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import cn.geoair.gtc.base.data.model.support.GirVisualModelKid;
import cn.geoair.gtc.base.gpa.dao.GiVisualSelectDao;
import cn.geoair.gtc.base.gpa.entity.GiEntityVisuable;


public interface PlusVisualSelectMapper<T extends GiEntityVisuable<PK>,PK extends Serializable> extends GiVisualSelectDao<T,PK>,BaseMapper<T> {



	@SuppressWarnings("rawtypes")
	default List<GirVisualModelKid> selectVisualModel(String displayQuery, String[] containKeys){

		return null;
	}


}
