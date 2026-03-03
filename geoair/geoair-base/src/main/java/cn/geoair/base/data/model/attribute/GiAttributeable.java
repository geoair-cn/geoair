package cn.geoair.base.data.model.attribute;

import cn.geoair.base.data.model.GiModelable;

public interface GiAttributeable<T extends GiModelable<?>> {

	default void save() {

	}

}
