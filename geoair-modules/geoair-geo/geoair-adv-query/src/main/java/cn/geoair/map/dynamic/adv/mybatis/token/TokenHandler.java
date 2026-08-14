package cn.geoair.map.dynamic.adv.mybatis.token;

/**
 * Token 处理器接口，用于处理 SQL 模板中 {@code #{}} 和 {@code ${}} 之间的表达式。
 * <p>
 * 实现此接口可自定义 token 的替换逻辑：
 * <ul>
 *   <li>用于 {@code ${}} 时：直接替换为常量值（SQL 拼接）</li>
 *   <li>用于 {@code #{}} 时：替换为 {@code ?} 占位符，同时收集参数值</li>
 * </ul>
 *
 * @see TokenParser
 * @author zhangjun
 */
public interface TokenHandler {

    /**
     * 处理 openToken 和 closeToken 之间的表达式内容。
     *
     * @param content token 之间的表达式文本（已去除首尾空白）
     * @return 替换后的文本
     */
    String handleToken(String content);
}
