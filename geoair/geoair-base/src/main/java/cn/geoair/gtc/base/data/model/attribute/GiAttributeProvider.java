package cn.geoair.gtc.base.data.model.attribute;

@SuppressWarnings("rawtypes")
public interface GiAttributeProvider<T extends GiAttributeable> {

	public void saveAttribute(T t);

	public T readAttribute();

}
