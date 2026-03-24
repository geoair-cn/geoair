package cn.geoair.map.dynamic.adv.query.apo;

import java.io.Serializable;
import java.util.List;

import cn.hutool.core.collection.ListUtil;

/**
 * @author ：张逢吉
 * @date ：Created in 2025/10/10 10:52 @description： 分页对象的返回结果
 */
public class PageApo<T> implements Serializable {

	/**
	 * 每页条数
	 */
	private int pageSize = 0;// 状态码

	/**
	 * 最后一页的页号（总页数）
	 */
	private int lastPageNum = 0;

	/**
	 * 游标开始行
	 */
	private long startRow = 0;

	/**
	 * 当前页码
	 */
	private int pageNum = 0;

	/**
	 * 总条数
	 */
	private long total = 0;

	/**
	 * 1. 若pageNumStartZero = true（页码从0开始）：OFFSET = pageNum × pageSize 2.
	 * 若pageNumStartZero = false（页码从1开始）：OFFSET = (pageNum - 1) × pageSize
	 */
	private boolean pageNumStartZero;

	/**
	 * 数据
	 */
	private Iterable<T> records;

	/**
	 * 字段列表
	 */
	private DataFieldsApo dataFieldsApo;

	public DataFieldsApo getDataFieldsApo() {
		return dataFieldsApo;
	}

	public PageApo<T> setDataFieldsApo(DataFieldsApo dataFieldsApo) {
		this.dataFieldsApo = dataFieldsApo;
		return this;
	}

	public int getPageSize() {
		return pageSize;
	}

	public PageApo<T> setPageSize(int pageSize) {
		this.pageSize = pageSize;
		return this;
	}

	public int getLastPageNum() {
		return lastPageNum;
	}

	public PageApo<T> setLastPageNum(int lastPageNum) {
		this.lastPageNum = lastPageNum;
		return this;
	}

	public long getStartRow() {
		return startRow;
	}

	public PageApo<T> setStartRow(long startRow) {
		this.startRow = startRow;
		return this;
	}

	public int getPageNum() {
		return pageNum;
	}

	public PageApo<T> setPageNum(int pageNum) {
		this.pageNum = pageNum;
		return this;
	}

	public long getTotal() {
		return total;
	}

	public PageApo<T> setTotal(long total) {
		this.total = total;
		return this;
	}

	public Iterable<T> getRecords() {
		return records;
	}

	public PageApo<T> setPageNumStartZero(boolean pageNumStartZero) {
		this.pageNumStartZero = pageNumStartZero;
		return this;
	}

	public boolean isPageNumStartZero() {
		return pageNumStartZero;
	}

	public PageApo<T> setRecords(Iterable<T> records) {
		this.records = records;
		return this;
	}

	/**
	 * 少一个t字母是为了防止给json反序列化把它当做get器处理了
	 * @return
	 */
	public List<T> geRecordsList() {
		return ListUtil.toList(records);
	}

}
