package cn.geoair.gtc.base.data.page.support;

import cn.geoair.gtc.base.data.model.annotation.GaModel;
import cn.geoair.gtc.base.data.model.annotation.GaModelField;
import cn.geoair.gtc.base.data.page.GiPageParam;
import cn.geoair.gtc.base.data.page.GiPager;

@SuppressWarnings("serial")
@GaModel(text="分页")
public class GtcPager<T> implements GiPager<T> {

	//@GaModelField(text="版本号")
	//protected String version = PRODUCT_VERSION;//版本号

	@GaModelField(text="总页数")
	private int pageSize = 0;//状态码

	@GaModelField(text="游标开始行")
	private long startRow = 0;

	@GaModelField(text="当前页码")
	private int pageNum = 0;

	@GaModelField(text="分页参数")
	private GiPageParam pageParam;

	@GaModelField(text="总条数")
	private long total = 0;//消息类型

	@GaModelField(text="业务数据")
	private Iterable<T> list;// 数据


	//
	public GtcPager() {
	}

	private Class<T> typeClass;

	public GtcPager(Class<T> cls) {
		typeClass = cls;
	}

	@Override
	public Class<T> returnClass(){
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
	public GtcPager<T> put(Iterable<T> list, long total, GiPageParam pageParam) {
		this.list = list;
		this.total = total;
		this.pageParam = pageParam;
		this.pageNum = pageParam.pageNum();
		this.startRow = pageParam.startRow();
		this.pageSize = pageParam.pageSize();

		return this;
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
}
