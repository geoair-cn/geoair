package cn.geoair.base.data.page;

import cn.geoair.base.convert.GiConverter;
import cn.geoair.base.data.GiValuable;
import cn.geoair.base.sp.GirSpHelper;
import cn.geoair.base.util.GutilClass;
import cn.geoair.base.util.GutilGenericType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 分页结果集接口
 *
 * @param <T> 分页数据的泛型类型
 * @author Ray
 */
public interface GiPager<T> extends GiValuable<Iterable<T>> {

    /**
     * 获取分页数据总条数
     *
     * @return 返回分页查询的总记录数
     */
    long total();

    /**
     * 获取分页查询参数
     *
     * @return 返回分页查询条件参数对象
     */
    GiPageParam pageParam();

    /**
     * 判断页码是否从0开始
     * <p>
     * 当返回true时，表示页码从0开始计数，起始页为第0页；
     * 当返回false时，表示页码从1开始计数，起始页为第1页。
     * </p>
     *
     * @return true表示页码从0开始，false表示页码从1开始
     */
    boolean isPageNumStartZero();

    /**
     * 设置分页数据
     *
     * @param list      分页数据列表
     * @param total     总记录数
     * @param pageParam 分页参数
     * @return 返回设置后的GiPager对象
     */
    GiPager<T> put(Iterable<T> list, long total, GiPageParam pageParam);

    GiPager<T> put(Iterable<T> list, long total, GiPageParam pageParam, boolean pageNumStartZero);

    /**
     * 获取泛型类型的实际类
     *
     * @return 返回GiPager泛型参数T的实际类型
     */
    @SuppressWarnings("unchecked")
    default Class<T> returnClass() {
        return (Class<T>)
                GutilGenericType.resolveTypeArguments(GutilClass.getUserClass(this), GiPager.class)[
                        0];
    }

    /**
     * 转换成另外一个Pager对象，对结果集进行遍历转换
     *
     * @param <V>       目标类型泛型
     * @param <K>       目标Pager类型泛型
     * @param pager     目标Pager对象
     * @param converter 数据转换器
     * @return 转换后的Pager对象
     */
    default <V, K extends GiPager<V>> K convert(K pager, GiConverter<T, V> converter) {
        Objects.requireNonNull(converter, "转换器不能为空");
        List<V> list = new ArrayList<>();
        this.value()
                .forEach(
                        t -> {
                            V v = converter.convert(t);
                            list.add(v);
                        });
        pager.put(list, this.total(), this.pageParam());
        return pager;
    }

    /**
     * 根据返回值类型获取一个GiPager包装类
     *
     * @param <T> 泛型类型
     * @param clz Pager数据类型
     * @return 对应类型的GiPager实例
     */
    static <T> GiPager<T> ofClass(Class<T> clz) {
        return GirSpHelper.load(GiPageConfig.class).getPagerProvider().getPager(clz);
    }
}
