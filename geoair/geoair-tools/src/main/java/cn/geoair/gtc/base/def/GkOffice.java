package cn.geoair.gtc.base.def;

/**
 * 获取类型为 T 的操作者
 *
 * @author Ray
 * @param <T>
 */
@FunctionalInterface
public interface GkOffice<T extends GkOperater> {

	T getOperater();

}
