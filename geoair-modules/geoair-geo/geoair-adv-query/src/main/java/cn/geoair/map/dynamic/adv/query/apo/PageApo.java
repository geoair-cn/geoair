package cn.geoair.map.dynamic.adv.query.apo;

import cn.geoair.base.data.page.GiPageParam;
import cn.geoair.base.data.page.GiPager;
import cn.hutool.core.collection.ListUtil;
import java.io.Serializable;
import java.util.List;
import lombok.Getter;

/**
 * @author ：张逢吉
 * @date ：Created in 2025/10/10 10:52 @description： 分页对象的返回结果
 */
@Getter
public class PageApo<T> implements Serializable, GiPager<T> {

    /** 每页条数 */
    private int pageSize = 0; // 状态码

    /** 最后一页的页号（总页数） */
    private int lastPageNum = 0;

    /** 游标开始行 */
    private long startRow = 0;

    /** 当前页码 */
    private int pageNum = 0;

    /** 总条数 */
    private long total = 0;

    /**
     * 1. 若pageNumStartZero = true（页码从0开始）：OFFSET = pageNum × pageSize 2. 若pageNumStartZero =
     * false（页码从1开始）：OFFSET = (pageNum - 1) × pageSize
     */
    private boolean pageNumStartZero;

    /** 数据 */
    private Iterable<T> records;

    /** 字段列表 */
    private DataFieldsApo dataFieldsApo;

    public PageApo<T> setDataFieldsApo(DataFieldsApo dataFieldsApo) {
        this.dataFieldsApo = dataFieldsApo;
        return this;
    }

    public PageApo<T> setPageSize(int pageSize) {
        this.pageSize = pageSize;
        return this;
    }

    public PageApo<T> setLastPageNum(int lastPageNum) {
        this.lastPageNum = lastPageNum;
        return this;
    }

    public PageApo<T> setStartRow(long startRow) {
        this.startRow = startRow;
        return this;
    }

    public PageApo<T> setPageNum(int pageNum) {
        this.pageNum = pageNum;
        return this;
    }

    public PageApo<T> setTotal(long total) {
        this.total = total;
        return this;
    }

    public PageApo<T> setPageNumStartZero(boolean pageNumStartZero) {
        this.pageNumStartZero = pageNumStartZero;
        return this;
    }

    public PageApo<T> setRecords(Iterable<T> records) {
        this.records = records;
        return this;
    }

    /**
     * 少一个t字母是为了防止给json反序列化把它当做get器处理了
     *
     * @return
     */
    public List<T> geRecordsList() {
        return ListUtil.toList(records);
    }

    @Override
    public long total() {
        return total;
    }

    @Override
    public GiPageParam pageParam() {
        return GiPageParam.of().putParam(pageNum, pageSize, startRow, pageNumStartZero);
    }

    @Override
    public GiPager<T> put(Iterable<T> list, long total, GiPageParam pageParam) {
        this.total = total;
        this.records = list;
        int pagedNum = pageParam.pageNum();
        int pagedSize = pageParam.pageSize();
        long startedRow = pageParam.startRow();
        this.pageNumStartZero = pageParam.isPageNumStartZero();
        this.pageNum = pagedNum;
        this.pageSize = pagedSize;
        this.startRow = startedRow;
        return this;
    }

    @Override
    public GiPager<T> put(
            Iterable<T> list, long total, GiPageParam pageParam, boolean pageNumStartZero) {
        put(list, total, pageParam);
        this.pageNumStartZero = pageNumStartZero;
        return this;
    }
}
