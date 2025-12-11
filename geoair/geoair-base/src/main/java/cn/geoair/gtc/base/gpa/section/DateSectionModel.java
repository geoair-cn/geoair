package cn.geoair.gtc.base.gpa.section;

import java.io.Serializable;
import java.util.Date;

/**
 * 按时间分表模型
 * <p>
 * 该接口定义了基于时间因子的分表策略，用于实现按时间维度进行数据分片存储
 * </p>
 *
 * @author Ray
 * @since 2022-04-25
 */
public interface DateSectionModel<PK extends Serializable> extends SectionModel<PK, Date> {
	
	/**
	 * 获取分表时间因子
	 * <p>
	 * 返回当前时间作为分表依据，默认实现返回系统当前时间
	 * </p>
	 *
	 * @return Date 分表时间因子
	 */
	@Override
	default Date factor() {
		return new Date();
	}
}
