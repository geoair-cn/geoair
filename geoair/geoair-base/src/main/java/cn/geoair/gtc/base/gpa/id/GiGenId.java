package cn.geoair.gtc.base.gpa.id;

public interface GiGenId<T> {

	T genId(Object entity, String table, String column);

}
