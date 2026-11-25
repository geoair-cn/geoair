package cn.geoair.map.dynamic.adv.utils;

import cn.hutool.core.lang.Pair;
import cn.hutool.core.util.StrUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 简单 SQL 解析工具 —— 仅处理 {@code SELECT ... FROM ... WHERE ...} 结构。
 *
 * <p>从 SELECT 子句中提取字段名（有 AS 别名用别名，无别名取最后一截）， 从 FROM 子句中提取 schema 和表名。
 *
 * @author zhangjun
 */
public class AdvSqlParser {

    private static final Pattern SELECT_PATTERN =
            Pattern.compile("^\\s*select\\s+", Pattern.CASE_INSENSITIVE);
    private static final Pattern BLOCK_COMMENT_PATTERN =
            Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);
    private static final Pattern LINE_COMMENT_PATTERN =
            Pattern.compile("--.*?(\\r?\\n|$)", Pattern.DOTALL);

    /** SQL 标识符字符类（字母、数字、下划线、中文），必须声明在引用它的 Pattern 之前 */
    private static final String ID_CHARS = "a-zA-Z0-9_\\u4e00-\\u9fa5";

    /** 匹配 AS 别名（as "xxx" / as xxx / "xxx" / xxx），末尾或逗号前，支持中文 */
    private static final Pattern ALIAS_PATTERN =
            Pattern.compile(
                    "(?:\\s+as\\s+|\\s+)(\"[^\"]+\"|'[^']+'|`[^`]+`|[a-zA-Z0-9_\\u4e00-\\u9fa5]+)"
                            + "\\s*$",
                    Pattern.CASE_INSENSITIVE);

    /** SQL 标识符字符类（字母、数字、下划线、中文），必须声明在引用它的 Pattern 之前 */

    /** 复杂 SQL 特征：JOIN / 子查询 / CTE / UNION / 多表逗号连接 */
    private static final Pattern COMPLEX_SQL_PATTERN =
            Pattern.compile(
                    "\\s(join|inner\\s+join|left\\s+join|right\\s+join|full\\s+join|cross\\s+join"
                            + "|union|intersect|except|with|from\\s*\\()",
                    Pattern.CASE_INSENSITIVE);

    /** 检测 FROM 子句中表名后面是否还有逗号（多表连接：FROM t1, t2） */
    private static final Pattern MULTI_TABLE_COMMA_PATTERN =
            Pattern.compile(
                    "from\\s+[a-zA-Z0-9_\"'`.]+\\s*,\\s*[a-zA-Z0-9_\"'`.]+",
                    Pattern.CASE_INSENSITIVE);

    /**
     * 解析 SELECT 语句，提取表名和字段列表。
     *
     * <p>仅处理最简单的单表查询。遇到多表 JOIN、子查询、CTE、UNION 等复杂 SQL 直接返回空结果。
     *
     * @param sql SELECT 语句
     * @return 解析结果（包含 schema、tableName、fields），复杂 SQL 返回空
     */
    public static SqlParseResult parse(String sql) {
        SqlParseResult result = new SqlParseResult();
        if (StrUtil.isEmpty(sql)) return result;

        String processed = preprocess(sql);
        if (!SELECT_PATTERN.matcher(processed).find()) return result;

        // 复杂 SQL 不解析：JOIN / 子查询 / CTE / UNION / 多表逗号连接
        if (COMPLEX_SQL_PATTERN.matcher(processed).find()
                || MULTI_TABLE_COMMA_PATTERN.matcher(processed).find()) return result;

        // 提取表名
        Pair<String, String> schemaAndTable = extractTableName(processed);
        result.setSchema(schemaAndTable.getKey());
        result.setTableName(schemaAndTable.getValue());

        // 提取字段
        result.setFields(extractFields(processed));

        return result;
    }

    /** 按括号深度分割字段，然后对每段取别名或原始名 */
    static List<String> extractFields(String processedSql) {
        // 截取 SELECT 和 FROM 之间的内容
        Pattern pat =
                Pattern.compile(
                        "select\\s+(.*?)\\s+from\\s", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher m = pat.matcher(processedSql);
        if (!m.find()) return Collections.emptyList();

        String fieldsPart = m.group(1).trim();

        // 按括号深度分割
        List<String> segments = splitByTopLevelComma(fieldsPart);
        List<String> fields = new ArrayList<>();
        for (String seg : segments) {
            seg = seg.trim();
            if (seg.isEmpty() || equalsIgnoreCase(seg, "DISTINCT")) continue;
            fields.add(extractFieldName(seg));
        }
        return fields;
    }

    /** 按顶层逗号（不在括号内）分割 */
    static List<String> splitByTopLevelComma(String s) {
        List<String> result = new ArrayList<>();
        int depth = 0;
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '(') {
                depth++;
                buf.append(c);
            } else if (c == ')') {
                depth--;
                buf.append(c);
            } else if (c == ',' && depth == 0) {
                result.add(buf.toString());
                buf.setLength(0);
            } else {
                buf.append(c);
            }
        }
        if (buf.length() > 0) result.add(buf.toString());
        return result;
    }

    /** 从单段字段表达式中提取最终名：有 AS 别名取别名，否则取最后一截 */
    static String extractFieldName(String segment) {
        // 1. 尝试匹配别名
        Matcher aliasM = ALIAS_PATTERN.matcher(segment);
        if (aliasM.find()) {
            String alias = aliasM.group(1);
            if (alias != null) {
                return unquote(alias);
            }
        }
        // 2. 无别名 → 取点号最后一段
        String[] parts = segment.split("\\.");
        String last = parts[parts.length - 1].trim();

        // 3. 如果是函数调用，保留函数名
        int parenIdx = last.indexOf('(');
        if (parenIdx > 0) {
            return last.substring(0, parenIdx);
        }
        return unquote(last);
    }

    /** 去掉首尾引号 */
    private static String unquote(String s) {
        if (s.length() >= 2) {
            char first = s.charAt(0), last = s.charAt(s.length() - 1);
            if ((first == '"' && last == '"')
                    || (first == '\'' && last == '\'')
                    || (first == '`' && last == '`')) {
                return s.substring(1, s.length() - 1);
            }
        }
        return s;
    }

    /** 提取表名和可选 schema。正则分组：group1=整个schema. , group2=schema名, group3=表名 */
    private static final Pattern TABLE_NAME_PATTERN =
            Pattern.compile(
                    "from\\s+"
                            + "(" // group 1: 整个 schema+dot（可选）
                            + "(" // group 2: schema 名
                            + "\"[^\"]+\"|'[^']+'|`[^`]+`|["
                            + ID_CHARS
                            + "]+"
                            + ")"
                            + "\\."
                            + ")?"
                            + "(" // group 3: 表名
                            + "\"[^\"]+\"|'[^']+'|`[^`]+`|["
                            + ID_CHARS
                            + "]+"
                            + ")",
                    Pattern.CASE_INSENSITIVE);

    static Pair<String, String> extractTableName(String processedSql) {
        Matcher m = TABLE_NAME_PATTERN.matcher(processedSql);
        if (m.find()) {
            String schema = m.group(2);
            String table = m.group(3);
            if (schema != null) schema = unquote(schema);
            if (table != null) table = unquote(table);
            return Pair.of(schema, table);
        }
        return Pair.of(null, null);
    }

    /** 预处理：去注释，合并空白 */
    private static String preprocess(String sql) {
        String s = BLOCK_COMMENT_PATTERN.matcher(sql).replaceAll(" ");
        s = LINE_COMMENT_PATTERN.matcher(s).replaceAll(" ");
        s = s.replaceAll("\\s+", " ");
        return s.trim();
    }

    private static boolean equalsIgnoreCase(String a, String b) {
        return a != null && a.equalsIgnoreCase(b);
    }

    // ==================== 结果封装 ====================

    public static class SqlParseResult {
        private String tableName;
        private String schema;
        private List<String> fields = new ArrayList<>();

        public String getTableName() {
            return tableName;
        }

        public void setTableName(String tableName) {
            this.tableName = tableName;
        }

        public String getSchema() {
            return schema;
        }

        public void setSchema(String schema) {
            this.schema = schema;
        }

        public List<String> getFields() {
            return fields;
        }

        public void setFields(List<String> fields) {
            this.fields = fields;
        }

        @Override
        public String toString() {
            return "schema: " + schema + ", table: " + tableName + ", fields: " + fields;
        }
    }

    // ==================== 测试 ====================

    public static void main(String[] args) {
        String[] cases = {
            // 基础
            "select * from t",
            "SELECT id, name FROM user WHERE status = 1 ORDER BY create_time DESC",
            "select user_id as uid, user_name name FROM t_user LIMIT 10",

            // 函数内含逗号
            "select ST_Transform(geom, 4326) as g, id from t",
            "SELECT CONCAT(a, b, c) AS full_name, age FROM person",
            "select COALESCE(x, 0) as x, COALESCE(y, 0) as y from coords",

            // 带 schema
            "select * from public.geo_poi_list11 where id = '1' limit 1",
            "SELECT a.id, b.name AS username FROM schema.table b WHERE b.age > 18",

            // 带引号
            "select * from \"geo_poi_list11\" where id = '1'",
            "select `用户姓名`, age from `public`.`学生表_测试` as t limit 10",
            "select a.id, b.name AS \"user name\" from \"schema\".\"table\" b",

            // 别名变体
            "select user_id as uid, user_name as uname FROM t_user",
            "select id 编号, name 姓名 from t", // 无 as 别名
            "select id as '用户ID', name from t",

            // 复杂 SQL — 不解析，返回空
            "SELECT a.id, b.name FROM t1 a, t2 b WHERE a.id = b.id",
            "SELECT * FROM t1 JOIN t2 ON t1.id = t2.id",
            "SELECT * FROM (SELECT id FROM t) AS sub",

            // 带注释
            "SELECT /* comment */ id, name FROM user -- line comment\n WHERE status=1",

            // DISTINCT
            "SELECT DISTINCT id, name FROM t",
            "SELECT DISTINCT id, COUNT(*) AS cnt FROM t GROUP BY id",

            // 表达式
            "select a + b as sum, c * 2 as double_c from calc",
        };

        int pass = 0, fail = 0;
        for (String sql : cases) {
            try {
                SqlParseResult r = parse(sql);
                boolean isComplex = r.tableName == null;
                String flag = isComplex ? "(skip)" : "OK     ";
                System.out.printf(
                        "  %s  fields=%-35s  table=%-15s  schema=%s    %s %n",
                        flag, r.fields, r.tableName != null ? r.tableName : "-", r.schema, sql);
                pass++;
            } catch (Exception e) {
                System.out.printf("  FAIL: %s%n", e.getMessage());
                fail++;
            }
        }
        System.out.printf("%n%d pass, %d fail%n", pass, fail);
    }
}
