package cn.geoair.map.dynamic.tools.page;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author ：张逢吉
 * @date ：Created in 2025/12/19 13:45 @description： 分页配置
 */
@Data
@Accessors(chain = true)
public class PageConfig {

    /** 符合条件的总记录数 */
    @Deprecated private Long totalCount;

    /** 允许的最大页码 通过允许的最大页码反着设置每页大小，防止数据量大的时候 ，频繁访问数据库 */
    private Long maxPageNo;

    /** 每页记录数 通过每页大小 计算 总页数，进行每页遍历。数据量大的时候可能有很多页 */
    private Long pageSize = 0L;

    /** 第一页是否从0开始 */
    private boolean pageNumStartByZero = false;

    /** 是否保存最终的结果 默认不保存，因为在流里面进行消费了。 */
    private boolean saveResultListIs = false;

    /** 是否并行消费每一条记录 */
    private boolean parallelConsumeRecordIs = true;

    /** 是否并行执行分页,当 parallelConsumeRecordIs 设置为串行的时候，这里就强制为串行执行，无论设置什么都失效 */
    private boolean parallelExecPageIs = true;
}
