package cn.geoair.base.data.page.support;

import cn.geoair.base.data.model.annotation.GaModel;
import cn.geoair.base.data.model.annotation.GaModelField;
import cn.geoair.base.data.page.GiPageParam;
import cn.geoair.base.data.page.GiPager;

@SuppressWarnings("serial")
@GaModel(text = "分页")
public class GirPager<T> implements GiPager<T> {

    @GaModelField(text = "总页数")
    private int pageSize = 0;

    @GaModelField(text = "游标开始行")
    private long startRow = 0;

    @GaModelField(text = "当前页码")
    private int pageNum = 1;

    @GaModelField(text = "分页参数")
    private GiPageParam pageParam;

    @GaModelField(text = "总条数")
    private long total = 0;

    @GaModelField(text = "业务数据")
    private Iterable<T> list;

    @GaModelField(text = "页码是否从0开始")
    private boolean pageNumStartZero = false;

    private Class<T> typeClass;

    public GirPager() {
    }

    public GirPager(Class<T> cls) {
        typeClass = cls;
    }

    /**
     * 带页码起始方式的构造方法
     */
    public GirPager(Class<T> cls, boolean pageNumStartZero) {
        typeClass = cls;
        this.pageNumStartZero = pageNumStartZero;
    }

    @Override
    public Class<T> returnClass() {
        return typeClass;
    }

    public int getPageSize() {
        return pageSize;
    }

    @Override
    public long total() {
        return total;
    }

    @Override
    public Iterable<T> value() {
        return list;
    }

    @Override
    public GirPager<T> put(Iterable<T> list, long total, GiPageParam pageParam) {
        this.list = list;
        this.total = total;
        this.pageParam = pageParam;
        // 从 pageParam 获取分页信息
        this.pageNum = pageParam.pageNum();
        this.startRow = pageParam.startRow();
        this.pageSize = pageParam.pageSize();
        this.pageNumStartZero = pageParam.isPageNumStartZero();
        return this;
    }

    /**
     * 重载方法：支持直接传入分页参数和页码起始方式
     */
    @Override
    public GirPager<T> put(Iterable<T> list, long total, GiPageParam pageParam, boolean pageNumStartZero) {
        put(list, total, pageParam);
        this.pageNumStartZero = pageNumStartZero;
        return this;
    }

    /**
     * 计算总页数
     */
    public int getTotalPages() {
        if (pageSize <= 0) {
            return 0;
        }
        return (int) Math.ceil((double) total / pageSize);
    }

    /**
     * 判断是否有下一页
     */
    public boolean hasNext() {
        if (pageSize <= 0) {
            return false;
        }
        long currentEnd = startRow + pageSize;
        return currentEnd < total;
    }

    /**
     * 判断是否有上一页
     */
    public boolean hasPrevious() {
        if (pageNumStartZero) {
            return pageNum > 0;
        } else {
            return pageNum > 1;
        }
    }

    /**
     * 获取下一页页码
     */
    public int getNextPageNum() {
        if (pageNumStartZero) {
            return pageNum + 1;
        } else {
            return pageNum + 1;
        }
    }

    /**
     * 获取上一页页码
     */
    public int getPreviousPageNum() {
        if (pageNumStartZero) {
            return Math.max(0, pageNum - 1);
        } else {
            return Math.max(1, pageNum - 1);
        }
    }

    public GiPageParam getPageParam() {
        return pageParam;
    }

    public long getTotal() {
        return total;
    }

    public long getStartRow() {
        return startRow;
    }

    public int getPageNum() {
        return pageNum;
    }

    public Iterable<T> getList() {
        return list;
    }

    @Override
    public GiPageParam pageParam() {
        return pageParam;
    }

    @Override
    public boolean isPageNumStartZero() {
        return pageNumStartZero;
    }

    public void setPageNumStartZero(boolean pageNumStartZero) {
        this.pageNumStartZero = pageNumStartZero;
    }
}
