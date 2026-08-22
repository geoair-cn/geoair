package cn.geoair.comp.jdbc.url.impl;

import cn.geoair.comp.jdbc.url.beans.JdbcEndpoint;
import cn.geoair.comp.jdbc.url.beans.JdbcUrl;
import cn.geoair.comp.jdbc.url.beans.JdbcUrlProperty;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.Data;

/**
 * 方言实现共享的 URL 片段、端点及参数区解析工具。
 *
 * <p>该类仅服务于本模块内部。各方言只负责自身语法，公共字符串处理集中在这里，
 * 避免 PostgreSQL、SQL Server 等实现再次产生分裂的参数规则。</p>
 *
 * @author 张逢吉
 */
final class JdbcUrlDialectSupport {

    private JdbcUrlDialectSupport() {
    }

    static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    static ParsedTail splitTail(String url, JdbcUrl.PropertyStyle preferredStyle) {
        int queryIndex = url.indexOf('?');
        int semicolonIndex = url.indexOf(';');
        int splitIndex = -1;
        JdbcUrl.PropertyStyle style = JdbcUrl.PropertyStyle.NONE;
        if (queryIndex >= 0 && (semicolonIndex < 0 || queryIndex < semicolonIndex)) {
            splitIndex = queryIndex;
            style = JdbcUrl.PropertyStyle.QUERY;
        } else if (semicolonIndex >= 0) {
            splitIndex = semicolonIndex;
            style = JdbcUrl.PropertyStyle.SEMICOLON;
        }
        if (splitIndex < 0) {
            return new ParsedTail(url, Collections.<JdbcUrlProperty>emptyList(), preferredStyle);
        }
        String parameterText = url.substring(splitIndex + 1);
        String separator = style == JdbcUrl.PropertyStyle.QUERY ? "&" : ";";
        List<JdbcUrlProperty> properties = new ArrayList<JdbcUrlProperty>();
        for (String item : parameterText.split(java.util.regex.Pattern.quote(separator), -1)) {
            if (item.length() == 0) {
                continue;
            }
            int equals = item.indexOf('=');
            properties.add(equals < 0 ? new JdbcUrlProperty(item, null, false)
                    : new JdbcUrlProperty(item.substring(0, equals), item.substring(equals + 1), true));
        }
        return new ParsedTail(url.substring(0, splitIndex), properties, style);
    }

    static List<JdbcEndpoint> parseEndpoints(String authority) {
        List<JdbcEndpoint> result = new ArrayList<JdbcEndpoint>();
        for (String item : authority.split(",")) {
            String host = item;
            Integer port = null;
            int colon = item.lastIndexOf(':');
            // IPv6 使用 [] 包裹；右中括号后的冒号才是端口分隔符。
            if (colon > 0 && colon < item.length() - 1 && item.indexOf(']') < colon) {
                host = item.substring(0, colon);
                port = parsePort(item.substring(colon + 1));
            }
            result.add(new JdbcEndpoint(host, port));
        }
        return result;
    }

    static Integer parsePort(String value) {
        try {
            return value.length() == 0 ? null : Integer.valueOf(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    static String endpoint(String host, Integer port) {
        if (isBlank(host)) {
            throw new IllegalArgumentException("网络 JDBC URL 的 host 不能为空");
        }
        return port == null ? host : host + ":" + port;
    }

    @Data
    static final class ParsedTail {
        /** 不包含参数区的 JDBC URL 主体。 */
        final String coreUrl;
        /** 按原始顺序解析出的参数列表。 */
        final List<JdbcUrlProperty> properties;
        /** 此参数区使用的分隔风格。 */
        final JdbcUrl.PropertyStyle style;

        ParsedTail(String coreUrl, List<JdbcUrlProperty> properties, JdbcUrl.PropertyStyle style) {
            this.coreUrl = coreUrl;
            this.properties = properties;
            this.style = style;
        }
    }
}
