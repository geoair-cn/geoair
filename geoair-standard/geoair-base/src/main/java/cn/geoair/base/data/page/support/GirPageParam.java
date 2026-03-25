package cn.geoair.base.data.page.support;

import cn.geoair.base.data.model.annotation.GaModel;
import cn.geoair.base.data.model.annotation.GaModelField;
import cn.geoair.base.data.page.GiPageParam;
import cn.geoair.base.gpa.support.GirSort;

@SuppressWarnings("serial")
@GaModel(text = "简单分页参数")
public class GirPageParam implements GiPageParam {

    @GaModelField(text = "总页数")
    private int pageSize = 0; // 分页大小

    @GaModelField(text = "游标开始行")
    private long startRow = 0;

    @GaModelField(text = "当前页码")
    private int pageNum = 0;

    @GaModelField(text = "排序条件")
    private GirSort sort;

    @GaModelField(text = "是否查询总条数")
    private boolean countTotal = true;

    public GirPageParam() {}

    public int getPageSize() {
        return pageSize;
    }

    public long getStartRow() {
        return startRow;
    }

    public int getPageNum() {
        return pageNum;
    }

    @Override
    public int pageSize() {
        return pageSize;
    }

    @Override
    public long startRow() {
        return startRow;
    }

    @Override
    public int pageNum() {
        return pageNum;
    }

    @Override
    public GirPageParam putParam(Integer pagesize, Integer pagenum, Long startrow) {
        if (pagesize != null) {
            this.pageSize = pagesize.intValue();
        } else {
            this.pageSize = 25;
        }
        if (pagenum != null) {
            this.pageNum = pagenum.intValue();
            this.startRow = this.pageNum * this.pageSize;
        } else if (startrow != null) {
            this.startRow = startrow.intValue();
            this.pageNum = (int) ((startRow / pageSize) + 1);
        } else {
            this.pageNum = 1;
            this.startRow = 0;
        }

        return this;
    }

    public GirPageParam putParam(GiPageParam pageParam) {
        return putParam(pageParam.pageSize(), pageParam.pageNum(), pageParam.startRow())
                .putSort(pageParam.sort())
                .putCountTotal(pageParam.countTotal());
    }

    @Override
    public GirSort sort() {
        return sort;
    }

    @Override
    public GirPageParam putSort(GirSort sort) {
        this.sort = sort;
        return this;
    }

    @Override
    public boolean countTotal() {
        return countTotal;
    }

    @Override
    public GirPageParam putCountTotal(boolean countTotal) {
        this.countTotal = countTotal;
        return this;
    }
}
