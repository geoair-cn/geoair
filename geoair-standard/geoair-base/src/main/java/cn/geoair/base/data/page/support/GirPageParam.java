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
    private int pageNum = 1;

    @GaModelField(text = "排序条件")
    private GirSort sort;

    @GaModelField(text = "分页是不是从0开始")
    boolean pageNumStartZero = false;

    @GaModelField(text = "是否查询总条数")
    private boolean countTotal = true;

    public GirPageParam() {
    }

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

    public boolean isPageNumStartZero() {
        return pageNumStartZero;
    }

    @Override
    public GirPageParam putParam(Integer pageSize, Integer pageNum, Long startRow) {
        return putParam(pageSize, pageNum, startRow, pageNumStartZero);
    }

    /**
     * 设置分页参数，并指定页码起始值
     *
     * @param pageSize         分页大小
     * @param pageNum          页码
     * @param startRow         起始行
     * @param pageNumStartZero 页码是否从0开始
     * @return 当前对象
     */
    public GirPageParam putParam(Integer pageSize, Integer pageNum, Long startRow, boolean pageNumStartZero) {
        // 先设置页码起始方式
        this.pageNumStartZero = pageNumStartZero;

        if (pageSize != null) {
            this.pageSize = pageSize.intValue();
        } else {
            this.pageSize = 25;
        }

        if (pageNum != null) {
            this.pageNum = pageNum.intValue();
            // 根据 pageNumStartZero 计算 startRow
            if (pageNumStartZero) {
                // 页码从0开始：startRow = pageNum * pageSize
                this.startRow = (long) this.pageNum * this.pageSize;
            } else {
                // 页码从1开始：startRow = (pageNum - 1) * pageSize
                this.startRow = (long) (this.pageNum - 1) * this.pageSize;
            }
        } else if (startRow != null) {
            this.startRow = startRow.intValue();
            // 根据 pageNumStartZero 计算 pageNum
            if (pageNumStartZero) {
                // 页码从0开始：pageNum = startRow / pageSize
                this.pageNum = (int) (startRow / pageSize);
            } else {
                // 页码从1开始：pageNum = (startRow / pageSize) + 1
                this.pageNum = (int) ((startRow / pageSize) + 1);
            }
        } else {
            // 默认值
            if (pageNumStartZero) {
                this.pageNum = 0;
                this.startRow = 0;
            } else {
                this.pageNum = 1;
                this.startRow = 0;
            }
        }

        return this;
    }

    public GirPageParam putParam(GiPageParam pageParam) {
        return putParam(pageParam.pageSize(), pageParam.pageNum(), pageParam.startRow(), pageParam.isPageNumStartZero())
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
