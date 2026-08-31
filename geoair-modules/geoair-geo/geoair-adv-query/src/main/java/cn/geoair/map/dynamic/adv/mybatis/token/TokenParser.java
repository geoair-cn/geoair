package cn.geoair.map.dynamic.adv.mybatis.token;

/**
 * SQL 模板中的 Token 解析器，负责扫描文本中的 openToken/closeToken 对并调用 {@link TokenHandler} 进行替换。
 *
 * <p>支持的 token 格式：
 *
 * <ul>
 *   <li>{@code #{expression}} — 参数占位符，由调用方替换为 {@code ?} 并收集参数值
 *   <li>{@code ${expression}} — 常量替换，由调用方直接替换为表达式的值
 * </ul>
 *
 * <p>支持反斜杠转义：{@code \#\{} 和 {@code \}} 会被当作普通文本而非 token 边界。
 *
 * @author zhangjun
 */
public class TokenParser {

    private final String openToken;
    private final String closeToken;
    private final TokenHandler tokenHandler;

    /**
     * 创建 Token 解析器。
     *
     * @param openToken 开始标记，如 "#{"
     * @param closeToken 结束标记，如 "}"
     * @param tokenHandler token 内容的处理器
     */
    public TokenParser(String openToken, String closeToken, TokenHandler tokenHandler) {
        this.openToken = openToken;
        this.closeToken = closeToken;
        this.tokenHandler = tokenHandler;
    }

    /**
     * 解析文本，将所有 token 替换为处理器的返回值。
     *
     * <p>扫描逻辑：
     *
     * <ol>
     *   <li>查找下一个 openToken
     *   <li>如果前面有反斜杠 {@code \}，则当作转义字符，跳过
     *   <li>否则开始匹配对应的 closeToken（同样支持反斜杠转义）
     *   <li>找到匹配后调用 {@link TokenHandler#handleToken} 替换内容
     * </ol>
     *
     * @param text 待解析的 SQL 文本
     * @return 解析后的文本
     */
    public String parse(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        int start = text.indexOf(openToken);
        if (start == -1) {
            return text;
        }
        char[] src = text.toCharArray();
        int offset = 0;
        final StringBuilder builder = new StringBuilder();
        StringBuilder expression = null;
        do {
            // 转义的 openToken（\#{）当作普通文本
            if (start > 0 && src[start - 1] == '\\') {
                builder.append(src, offset, start - offset - 1).append(openToken);
                offset = start + openToken.length();
            } else {
                if (expression == null) {
                    expression = new StringBuilder();
                } else {
                    expression.setLength(0);
                }
                builder.append(src, offset, start - offset);
                offset = start + openToken.length();

                // 搜索对应的 closeToken
                int end = text.indexOf(closeToken, offset);
                while (end > -1) {
                    // 转义的 closeToken（\}）当作普通文本
                    if (end > offset && src[end - 1] == '\\') {
                        expression.append(src, offset, end - offset - 1).append(closeToken);
                        offset = end + closeToken.length();
                        end = text.indexOf(closeToken, offset);
                    } else {
                        expression.append(src, offset, end - offset);
                        break;
                    }
                }
                // 未找到匹配的 closeToken，将剩余文本作为普通文本
                if (end == -1) {
                    builder.append(src, start, src.length - start);
                    offset = src.length;
                } else {
                    builder.append(tokenHandler.handleToken(expression.toString().trim()));
                    offset = end + closeToken.length();
                }
            }
            start = text.indexOf(openToken, offset);
        } while (start > -1);
        if (offset < src.length) {
            builder.append(src, offset, src.length - offset);
        }
        return builder.toString();
    }
}
