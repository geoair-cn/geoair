package cn.geoair.base.gpa.section;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * 日期分表Dao, dao继承该类型实现简单按日期分表功能
 *
 * @author Ray
 * @param <M> 模型
 * @param <PK> 主键类型
 */
public abstract class DateSectionDao<M extends DateSectionModel<PK>, PK extends Serializable>
		extends SectionDao<M, PK, Date> {

	/**
	 * 获取时间字段 (子类实现)
	 * @return 时间字段名称
	 */
	protected abstract String getColumnDate();

	/**
	 * 获取分表形式 (子类实现)
	 * @return 分表类型枚举值
	 */
	protected abstract DateSectionType getDateSectionType();

	/**
	 * 在缓存中查询时间范围内所涉及的表
	 * @param bDate 开始时间（包含）
	 * @param eDate 结束时间（包含）
	 * @return 指定时间范围内的所有表名集合
	 */
	public Set<String> getVisibleTableNamesByDate(Date bDate, Date eDate) {
		Set<String> set = new HashSet<String>();
		if (bDate.after(eDate)) {
			return set;
		}
		Date beginDate = bDate;
		String tName;
		Calendar c = Calendar.getInstance();
		DateSectionType dst = getDateSectionType();
		while (beginDate.before(eDate)) {
			tName = getTableNamePrefix() + getTableNameSuffix(beginDate);
			if (hasCacheTableName(tName)) {
				set.add(tName);
			}
			c.setTime(beginDate);
			if (dst == DateSectionType.年) {
				c.add(Calendar.YEAR, 1);
			}
			else if (dst == DateSectionType.月) {
				c.add(Calendar.MONTH, 1);
			}
			else if (dst == DateSectionType.周) {
				c.add(Calendar.WEEK_OF_YEAR, 1);
			}
			else if (dst == DateSectionType.日) {
				c.add(Calendar.DATE, 1);
			}
			else if (dst == DateSectionType.时) {
				c.add(Calendar.HOUR, 1);
			}
			else if (dst == DateSectionType.分) {
				c.add(Calendar.MINUTE, 1);
			}
			beginDate = c.getTime();
		}
		tName = getTableNamePrefix() + getTableNameSuffix(eDate);
		if (hasCacheTableName(tName)) {
			set.add(tName);
		}
		return set;
	}

	/**
	 * 根据分表时间因子获得表后缀
	 * @param factor 分表时间因子
	 * @return 表名后缀
	 */
	@Override
	public String getTableNameSuffix(Date factor) {
		return dateFormat.get().format(factor);
	}

	/**
	 * 根据分表类型获取时间格式化DateFormat
	 * @param dst 分表类型
	 * @return 对应的时间格式化器
	 */
	protected DateFormat getDateFormatBySectionType(DateSectionType dst) {
		if (dst == DateSectionType.年) {
			return new SimpleDateFormat("yyyy");
		}
		else if (dst == DateSectionType.月) {
			return new SimpleDateFormat("yyyy_MM");
		}
		else if (dst == DateSectionType.周) {
			return new SimpleDateFormat("YYYY_ww");
		}
		else if (dst == DateSectionType.日) {
			return new SimpleDateFormat("yyyy_MM_dd");
		}
		else if (dst == DateSectionType.时) {
			return new SimpleDateFormat("yyyy_MM_dd_HH");
		}
		else if (dst == DateSectionType.分) {
			return new SimpleDateFormat("yyyy_MM_dd_HH_mm");
		}
		return new SimpleDateFormat("yyyy");
	}

	private final ThreadLocal<DateFormat> dateFormat = new ThreadLocal<DateFormat>() {
		@Override
		protected DateFormat initialValue() {
			return getDateFormatBySectionType(getDateSectionType());
		}
	};

	/**
	 * 日期分表类型枚举
	 */
	public static enum DateSectionType {

		年, 月, 周, 日, 时, 分

	}

	/**
	 * 批量存储数据到对应的分表中
	 * @param list 待存储的数据列表
	 * @return 存储的数据总条数
	 */
	public int saveSection(List<M> list) {
		if (list == null || list.isEmpty()) {
			return 0;
		}
		Map<String, List<M>> section = new HashMap<>();
		List<M> item = null;
		String tn;
		for (M m : list) {
			tn = getTableNameByFactor(m.factor() != null ? m.factor() : new Date(), false);
			if (section.containsKey(tn)) {
				item = section.get(tn);
			}
			else {
				item = new ArrayList<>();
				section.put(tn, item);
			}
			item.add(m);
		}
		for (Entry<String, List<M>> entry : section.entrySet()) {
			saveBatch(entry.getValue(), getExistTableName(entry.getKey()));
		}
		return list.size();
	}

	/**
	 * 批量存储数据到对应的分表中（逐个处理）
	 * @param list 待存储的数据列表
	 * @return 存储的数据总条数
	 */
	public int saveSectionOneByOne(List<M> list) {
		if (list == null || list.isEmpty()) {
			return 0;
		}
		Map<String, List<M>> section = new HashMap<>();
		List<M> item = null;
		String tn;
		for (M m : list) {
			tn = getTableNameByFactor(m.factor() != null ? m.factor() : new Date(), false);
			if (section.containsKey(tn)) {
				item = section.get(tn);
			}
			else {
				item = new ArrayList<>();
				section.put(tn, item);
			}
			item.add(m);
		}
		for (Entry<String, List<M>> entry : section.entrySet()) {
			saveBatch(entry.getValue(), getExistTableName(entry.getKey()));
		}
		return list.size();
	}

	/**
	 * 批量存储或更新数据到对应的分表中
	 * @param list 待存储或更新的数据列表
	 * @return 成功存储或更新的数据总条数
	 */
	public int saveOrUpdateSection(List<M> list) {
		if (list == null || list.isEmpty()) {
			return 0;
		}
		Map<String, List<M>> section = new HashMap<>();
		List<M> item = null;
		String tn;
		for (M m : list) {
			tn = getTableNameByFactor(m.factor() != null ? m.factor() : new Date(), false);
			if (section.containsKey(tn)) {
				item = section.get(tn);
			}
			else {
				item = new ArrayList<>();
				section.put(tn, item);
			}
			item.add(m);
		}
		int len = 0;
		for (Entry<String, List<M>> entry : section.entrySet()) {
			len = saveOrUpdateBatch(entry.getValue(), entry.getKey()) + len;
		}
		return len;
	}

}
