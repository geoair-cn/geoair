package cn.geoair.gtc.base.data.page.support;

import cn.geoair.gtc.base.data.model.annotation.GaModel;
import cn.geoair.gtc.base.data.model.annotation.GaModelField;
import cn.geoair.gtc.base.data.page.GiPageParam;
import cn.geoair.gtc.base.gpa.support.GtcSort;

@SuppressWarnings("serial")
@GaModel(text="简单分页参数")
public class GtcPageParam implements GiPageParam {


	@GaModelField(text="总页数")
	private int pageSize = 0;//分页大小

	@GaModelField(text="游标开始行")
	private long startRow = 0;

	@GaModelField(text="当前页码")
	private int pageNum = 0;


	@GaModelField(text="排序条件")
	private GtcSort sort;

	@GaModelField(text="是否查询总条数")
	private boolean countTotal = true;

	public GtcPageParam() {

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

	@Override
	public GtcPageParam putParam(Integer pagesize, Integer pagenum, Long startrow) {
		if(pagesize != null) {
        	this.pageSize = pagesize.intValue();
        }else {
        	this.pageSize = 25;
        }
        if(pagenum != null) {
        	this.pageNum = pagenum.intValue();
        	this.startRow = this.pageNum * this.pageSize;
        }else if(startrow != null) {
        	this.startRow = startrow.intValue();
        	this.pageNum = (int) ((startRow / pageSize) + 1);
        }else {
        	this.pageNum = 1;
        	this.startRow = 0;
        }

        return this;
	}

	public GtcPageParam putParam(GiPageParam pageParam) {
		return putParam(pageParam.pageSize(),pageParam.pageNum(),pageParam.startRow()).putSort(pageParam.sort()).putCountTotal(pageParam.countTotal());
	}

	@Override
	public GtcSort sort() {
		return sort;
	}

	@Override
	public GtcPageParam putSort(GtcSort sort) {
		this.sort = sort;
		return this;
	}

	@Override
	public boolean countTotal() {
		return countTotal;
	}

	@Override
	public GtcPageParam putCountTotal(boolean countTotal) {
		this.countTotal = countTotal;
		return this;
	}

}
