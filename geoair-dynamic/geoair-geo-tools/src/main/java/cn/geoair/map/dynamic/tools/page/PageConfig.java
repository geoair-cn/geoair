package cn.geoair.map.dynamic.tools.page;

import lombok.Data;

/**
 * @author ：张逢吉
 * @date ：Created in 2025/12/19 13:45 @description： 分页配置
 */
@Data
public class PageConfig {

	/**
	 * 符合条件的总记录数
	 */
	private Long totalCount;

	/**
	 * 最大页码
	 */
	private Long maxPageNo;

	/**
	 * 每页记录数
	 */
	private Long pageSize = 0L;

	/**
	 * 第一页是否从0开始
	 */
	private boolean pageNumStartByZero;

	/**
	 * 是否保存最终的结果 默认不保存，因为在流里面进行消费了。
	 */
	private boolean saveResultListIs = false;

	/**
	 * 是否并行消费每一条记录
	 */
	private boolean parallelConsumeRecordIs = true;

	/**
	 * 是否并行执行分页,当 parallelConsumeRecordIs 设置为串行的时候，这里就强制为串行执行，无论设置什么都失效
	 */
	private boolean parallelExecPageIs = true;

}
