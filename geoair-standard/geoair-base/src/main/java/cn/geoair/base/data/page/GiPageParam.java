package cn.geoair.base.data.page;

import cn.geoair.base.gpa.support.GirSort;
import cn.geoair.base.sp.GirSpHelper;
import java.io.Serializable;

/**
 * 包装分页请求参数的接口
 *
 * @author Ray
 */
public interface GiPageParam extends Serializable {

    /**
     * 获取每页显示的记录数量
     *
     * @return 每页记录数
     */
    public int pageSize();

    /**
     * 获取游标开始行号
     *
     * @return 游标开始行号
     */
    public long startRow();

    /**
     * 获取当前页码
     *
     * @return 当前页码
     */
    public int pageNum();

    /**
     * 是否包含count查询
     *
     * @return true:包含count查询,false:不包含count查询
     */
    public boolean countTotal();

    /**
     * 获取排序条件
     *
     * @return 排序条件
     */
    public GirSort sort();

    /**
     * 设置分页参数
     *
     * @param pageSize 每页记录数
     * @param pageNum 页码
     * @param startRow 游标开始行号
     * @return 分页参数对象
     */
    public GiPageParam putParam(Integer pageSize, Integer pageNum, Long startRow);

    /**
     * 设置排序条件
     *
     * @param sort 排序条件
     * @return 分页参数对象
     */
    public GiPageParam putSort(GirSort sort);

    /**
     * 设置是否包含count查询
     *
     * @param countTotal 是否包含count查询
     * @return 分页参数对象
     */
    public GiPageParam putCountTotal(boolean countTotal);

    /**
     * 初始化一个GiPageParam对象。
     *
     * @return GiPageParam实例
     */
    public static GiPageParam of() {
        return GirSpHelper.load(GiPageConfig.class).getPageParamProvider().getPageParam();
    }

    /**
     * 初始化一个GiPageParam对象。
     *
     * @param pageSize 每页记录数
     * @param pageNum 页码
     * @param startRow 游标开始行号
     * @return GiPageParam实例
     */
    public static GiPageParam of(Integer pageSize, Integer pageNum, Long startRow) {
        return of().putParam(pageSize, pageNum, startRow);
    }
}
