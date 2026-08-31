package cn.geoair.base.util;

import cn.geoair.base.text.GuStrFormatter;

import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * 字符串工具类
 *
 * @author
 */
public class GutilStr {

    public static final int INDEX_NOT_FOUND = -1;

    /** 字符常量：空格符 {@code ' '} */
    public static final char C_SPACE = GutilChar.SPACE;

    /** 字符常量：制表符 {@code '\t'} */
    public static final char C_TAB = GutilChar.TAB;

    /** 字符常量：点 {@code '.'} */
    public static final char C_DOT = GutilChar.DOT;

    /** 字符常量：斜杠 {@code '/'} */
    public static final char C_SLASH = GutilChar.SLASH;

    /** 字符常量：反斜杠 {@code '\\'} */
    public static final char C_BACKSLASH = GutilChar.BACKSLASH;

    /** 字符常量：回车符 {@code '\r'} */
    public static final char C_CR = GutilChar.CR;

    /** 字符常量：换行符 {@code '\n'} */
    public static final char C_LF = GutilChar.LF;

    /** 字符常量：下划线 {@code '_'} */
    public static final char C_UNDERLINE = GutilChar.UNDERLINE;

    /** 字符常量：逗号 {@code ','} */
    public static final char C_COMMA = GutilChar.COMMA;

    /** 字符常量：花括号（左） <code>'{'</code> */
    public static final char C_DELIM_START = GutilChar.DELIM_START;

    /** 字符常量：花括号（右） <code>'}'</code> */
    public static final char C_DELIM_END = GutilChar.DELIM_END;

    /** 字符常量：中括号（左） {@code '['} */
    public static final char C_BRACKET_START = GutilChar.BRACKET_START;

    /** 字符常量：中括号（右） {@code ']'} */
    public static final char C_BRACKET_END = GutilChar.BRACKET_END;

    /** 字符常量：冒号 {@code ':'} */
    public static final char C_COLON = GutilChar.COLON;

    /** 字符常量：艾特 <code>'@'</code> */
    public static final char C_AT = GutilChar.AT;

    /** 字符串常量：空格符 {@code " "} */
    public static final String SPACE = " ";

    /** 字符串常量：制表符 {@code "\t"} */
    public static final String TAB = "	";

    /** 字符串常量：点 {@code "."} */
    public static final String DOT = ".";

    /**
     * 字符串常量：双点 {@code ".."} <br>
     * 用途：作为指向上级文件夹的路径，如：{@code "../path"}
     */
    public static final String DOUBLE_DOT = "..";

    /** 字符串常量：斜杠 {@code "/"} */
    public static final String SLASH = "/";

    /** 字符串常量：反斜杠 {@code "\\"} */
    public static final String BACKSLASH = "\\";

    /** 字符串常量：空字符串 {@code ""} */
    public static final String EMPTY = "";

    /**
     * 字符串常量：{@code "null"} <br>
     * 注意：{@code "null" != null}
     */
    public static final String NULL = "null";

    /**
     * 字符串常量：回车符 {@code "\r"} <br>
     * 解释：该字符常用于表示 Linux 系统和 MacOS 系统下的文本换行
     */
    public static final String CR = "\r";

    /** 字符串常量：换行符 {@code "\n"} */
    public static final String LF = "\n";

    /**
     * 字符串常量：Windows 换行 {@code "\r\n"} <br>
     * 解释：该字符串常用于表示 Windows 系统下的文本换行
     */
    public static final String CRLF = "\r\n";

    /** 字符串常量：下划线 {@code "_"} */
    public static final String UNDERLINE = "_";

    /** 字符串常量：减号（连接符） {@code "-"} */
    public static final String DASHED = "-";

    /** 字符串常量：逗号 {@code ","} */
    public static final String COMMA = ",";

    /** 字符串常量：花括号（左） <code>"{"</code> */
    public static final String DELIM_START = "{";

    /** 字符串常量：花括号（右） <code>"}"</code> */
    public static final String DELIM_END = "}";

    /** 字符串常量：中括号（左） {@code "["} */
    public static final String BRACKET_START = "[";

    /** 字符串常量：中括号（右） {@code "]"} */
    public static final String BRACKET_END = "]";

    /** 字符串常量：冒号 {@code ":"} */
    public static final String COLON = ":";

    /** 字符串常量：艾特 <code>"@"</code> */
    public static final String AT = "@";

    /** 字符串常量：HTML 空格转义 {@code "&nbsp;" -> " "} */
    public static final String HTML_NBSP = "&nbsp;";

    /** 字符串常量：HTML And 符转义 {@code "&amp;" -> "&"} */
    public static final String HTML_AMP = "&amp;";

    /** 字符串常量：HTML 双引号转义 {@code "&quot;" -> "\""} */
    public static final String HTML_QUOTE = "&quot;";

    /** 字符串常量：HTML 单引号转义 {@code "&apos" -> "'"} */
    public static final String HTML_APOS = "&apos;";

    /** 字符串常量：HTML 小于号转义 {@code "&lt;" -> "<"} */
    public static final String HTML_LT = "&lt;";

    /** 字符串常量：HTML 大于号转义 {@code "&gt;" -> ">"} */
    public static final String HTML_GT = "&gt;";

    /** 字符串常量：空 JSON <code>"{}"</code> */
    public static final String EMPTY_JSON = "{}";

    // ------------------------------------------------------------------------ 空白

    /**
     * 字符串是否为空白，空白的定义如下：
     *
     * <ol>
     *   <li>{@code null}
     *   <li>空字符串：{@code ""}
     *   <li>空格、全角空格、制表符、换行符，等不可见字符
     * </ol>
     *
     * <p>例：
     *
     * <ul>
     *   <li>{@code gtcStrUtil.isBlank(null) // true}
     *   <li>{@code gtcStrUtil.isBlank("") // true}
     *   <li>{@code gtcStrUtil.isBlank(" \t\n") // true}
     *   <li>{@code gtcStrUtil.isBlank("abc") // false}
     * </ul>
     *
     * <p>注意：该方法与 {@link #isEmpty(CharSequence)} 的区别是： 该方法会校验空白字符，且性能相对于 {@link
     * #isEmpty(CharSequence)} 略慢。 <br>
     *
     * <p>建议：
     *
     * <ul>
     *   <li>该方法建议仅对于客户端（或第三方接口）传入的参数使用该方法。
     *   <li>需要同时校验多个字符串时，建议采用 {@link #hasBlank(CharSequence...)} 或 {@link
     *       #isAllBlank(CharSequence...)}
     * </ul>
     *
     * @param str 被检测的字符串
     * @return 若为空白，则返回 true
     * @see #isEmpty(CharSequence)
     */
    public static boolean isBlank(CharSequence str) {
        int length;

        if ((str == null) || ((length = str.length()) == 0)) {
            return true;
        }

        for (int i = 0; i < length; i++) {
            // 只要有一个非空字符即为非空字符串
            if (false == GutilChar.isBlankChar(str.charAt(i))) {
                return false;
            }
        }

        return true;
    }

    /**
     * 如果对象是字符串是否为空白，空白的定义如下：
     *
     * <ol>
     *   <li>{@code null}
     *   <li>空字符串：{@code ""}
     *   <li>空格、全角空格、制表符、换行符，等不可见字符
     * </ol>
     *
     * <p>例：
     *
     * <ul>
     *   <li>{@code gtcStrUtil.isBlankIfStr(null) // true}
     *   <li>{@code gtcStrUtil.isBlankIfStr("") // true}
     *   <li>{@code gtcStrUtil.isBlankIfStr(" \t\n") // true}
     *   <li>{@code gtcStrUtil.isBlankIfStr("abc") // false}
     * </ul>
     *
     * <p>注意：该方法与 {@link #isEmptyIfStr(Object)} 的区别是： 该方法会校验空白字符，且性能相对于 {@link
     * #isEmptyIfStr(Object)} 略慢。
     *
     * @param obj 对象
     * @return 如果为字符串是否为空串
     * @see GutilStr#isBlank(CharSequence)
     * @since 3.3.0
     */
    public static boolean isBlankIfStr(Object obj) {
        if (null == obj) {
            return true;
        } else if (obj instanceof CharSequence) {
            return isBlank((CharSequence) obj);
        }
        return false;
    }

    /**
     * 字符串是否为非空白，非空白的定义如下：
     *
     * <ol>
     *   <li>不为 {@code null}
     *   <li>不为空字符串：{@code ""}
     *   <li>不为空格、全角空格、制表符、换行符，等不可见字符
     * </ol>
     *
     * <p>例：
     *
     * <ul>
     *   <li>{@code gtcStrUtil.isNotBlank(null) // false}
     *   <li>{@code gtcStrUtil.isNotBlank("") // false}
     *   <li>{@code gtcStrUtil.isNotBlank(" \t\n") // false}
     *   <li>{@code gtcStrUtil.isNotBlank("abc") // true}
     * </ul>
     *
     * <p>注意：该方法与 {@link #isNotEmpty(CharSequence)} 的区别是： 该方法会校验空白字符，且性能相对于 {@link
     * #isNotEmpty(CharSequence)} 略慢。
     *
     * <p>建议：仅对于客户端（或第三方接口）传入的参数使用该方法。
     *
     * @param str 被检测的字符串
     * @return 是否为非空
     * @see GutilStr#isBlank(CharSequence)
     */
    public static boolean isNotBlank(CharSequence str) {
        return false == isBlank(str);
    }

    /**
     * 指定字符串数组中，是否包含空字符串。
     *
     * <p>如果指定的字符串数组的长度为 0，或者其中的任意一个元素是空字符串，则返回 true。 <br>
     *
     * <p>例：
     *
     * <ul>
     *   <li>{@code gtcStrUtil.hasBlank() // true}
     *   <li>{@code gtcStrUtil.hasBlank("", null, " ") // true}
     *   <li>{@code gtcStrUtil.hasBlank("123", " ") // true}
     *   <li>{@code gtcStrUtil.hasBlank("123", "abc") // false}
     * </ul>
     *
     * <p>注意：该方法与 {@link #isAllBlank(CharSequence...)} 的区别在于：
     *
     * <ul>
     *   <li>hasBlank(CharSequence...) 等价于 {@code isBlank(...) || isBlank(...) || ...}
     *   <li>{@link #isAllBlank(CharSequence...)} 等价于 {@code isBlank(...) && isBlank(...) && ...}
     * </ul>
     *
     * @param strs 字符串列表
     * @return 是否包含空字符串
     */
    public static boolean hasBlank(CharSequence... strs) {
        if (GutilArray.isEmpty(strs)) {
            return true;
        }

        for (CharSequence str : strs) {
            if (isBlank(str)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 指定字符串数组中的元素，是否全部为空字符串。
     *
     * <p>如果指定的字符串数组的长度为 0，或者所有元素都是空字符串，则返回 true。 <br>
     *
     * <p>例：
     *
     * <ul>
     *   <li>{@code gtcStrUtil.isAllBlank() // true}
     *   <li>{@code gtcStrUtil.isAllBlank("", null, " ") // true}
     *   <li>{@code gtcStrUtil.isAllBlank("123", " ") // false}
     *   <li>{@code gtcStrUtil.isAllBlank("123", "abc") // false}
     * </ul>
     *
     * <p>注意：该方法与 {@link #hasBlank(CharSequence...)} 的区别在于：
     *
     * <ul>
     *   <li>{@link #hasBlank(CharSequence...)} 等价于 {@code isBlank(...) || isBlank(...) || ...}
     *   <li>isAllBlank(CharSequence...) 等价于 {@code isBlank(...) && isBlank(...) && ...}
     * </ul>
     *
     * @param strs 字符串列表
     * @return 所有字符串是否为空白
     */
    public static boolean isAllBlank(CharSequence... strs) {
        if (GutilArray.isEmpty(strs)) {
            return true;
        }

        for (CharSequence str : strs) {
            if (isNotBlank(str)) {
                return false;
            }
        }
        return true;
    }

    // ------------------------------------------------------------------------ 空

    /**
     * 字符串是否为空，空的定义如下：
     *
     * <ol>
     *   <li>{@code null}
     *   <li>空字符串：{@code ""}
     * </ol>
     *
     * <p>例：
     *
     * <ul>
     *   <li>{@code gtcStrUtil.isEmpty(null) // true}
     *   <li>{@code gtcStrUtil.isEmpty("") // true}
     *   <li>{@code gtcStrUtil.isEmpty(" \t\n") // false}
     *   <li>{@code gtcStrUtil.isEmpty("abc") // false}
     * </ul>
     *
     * <p>注意：该方法与 {@link #isBlank(CharSequence)} 的区别是：该方法不校验空白字符。
     *
     * <p>建议：
     *
     * <ul>
     *   <li>该方法建议用于工具类或任何可以预期的方法参数的校验中。
     *   <li>需要同时校验多个字符串时，建议采用 {@link #hasEmpty(CharSequence...)} 或 {@link
     *       #isAllEmpty(CharSequence...)}
     * </ul>
     *
     * @param str 被检测的字符串
     * @return 是否为空
     * @see #isBlank(CharSequence)
     */
    public static boolean isEmpty(CharSequence str) {
        return str == null || str.length() == 0;
    }

    /**
     * 如果对象是字符串是否为空串，空的定义如下： <br>
     *
     * <ol>
     *   <li>{@code null}
     *   <li>空字符串：{@code ""}
     * </ol>
     *
     * <p>例：
     *
     * <ul>
     *   <li>{@code gtcStrUtil.isEmptyIfStr(null) // true}
     *   <li>{@code gtcStrUtil.isEmptyIfStr("") // true}
     *   <li>{@code gtcStrUtil.isEmptyIfStr(" \t\n") // false}
     *   <li>{@code gtcStrUtil.isEmptyIfStr("abc") // false}
     * </ul>
     *
     * <p>注意：该方法与 {@link #isBlankIfStr(Object)} 的区别是：该方法不校验空白字符。
     *
     * @param obj 对象
     * @return 如果为字符串是否为空串
     * @since 3.3.0
     */
    public static boolean isEmptyIfStr(Object obj) {
        if (null == obj) {
            return true;
        } else if (obj instanceof CharSequence) {
            return 0 == ((CharSequence) obj).length();
        }
        return false;
    }

    /**
     * 字符串是否为非空白，非空白的定义如下：
     *
     * <ol>
     *   <li>不为 {@code null}
     *   <li>不为空字符串：{@code ""}
     * </ol>
     *
     * <p>例：
     *
     * <ul>
     *   <li>{@code gtcStrUtil.isNotEmpty(null) // false}
     *   <li>{@code gtcStrUtil.isNotEmpty("") // false}
     *   <li>{@code gtcStrUtil.isNotEmpty(" \t\n") // true}
     *   <li>{@code gtcStrUtil.isNotEmpty("abc") // true}
     * </ul>
     *
     * <p>注意：该方法与 {@link #isNotBlank(CharSequence)} 的区别是：该方法不校验空白字符。
     *
     * <p>建议：该方法建议用于工具类或任何可以预期的方法参数的校验中。
     *
     * @param str 被检测的字符串
     * @return 是否为非空
     * @see GutilStr#isEmpty(CharSequence)
     */
    public static boolean isNotEmpty(CharSequence str) {
        return false == isEmpty(str);
    }

    /**
     * 当给定字符串为null时，转换为Empty
     *
     * @param str 被检查的字符串
     * @return 原字符串或者空串
     * @see #ToEmpty(CharSequence)
     * @since 4.6.3
     */
    public static String emptyIfNull(CharSequence str) {
        return nullToEmpty(str);
    }

    /**
     * 当给定字符串为null时，转换为Empty
     *
     * @param str 被转换的字符串
     * @return 转换后的字符串
     */
    public static String nullToEmpty(CharSequence str) {
        return nullToDefault(str, EMPTY);
    }

    /**
     * 如果字符串是 <code>null</code>，则返回指定默认字符串，否则返回字符串本身。
     *
     * <pre>
     * nullToDefault(null, &quot;default&quot;)  = &quot;default&quot;
     * nullToDefault(&quot;&quot;, &quot;default&quot;)    = &quot;&quot;
     * nullToDefault(&quot;  &quot;, &quot;default&quot;)  = &quot;  &quot;
     * nullToDefault(&quot;bat&quot;, &quot;default&quot;) = &quot;bat&quot;
     * </pre>
     *
     * @param str 要转换的字符串
     * @param defaultStr 默认字符串
     * @return 字符串本身或指定的默认字符串
     */
    public static String nullToDefault(CharSequence str, String defaultStr) {
        return (str == null) ? defaultStr : str.toString();
    }

    /**
     * 如果字符串是<code>null</code>或者&quot;&quot;，则返回指定默认字符串，否则返回字符串本身。
     *
     * <pre>
     * emptyToDefault(null, &quot;default&quot;)  = &quot;default&quot;
     * emptyToDefault(&quot;&quot;, &quot;default&quot;)    = &quot;default&quot;
     * emptyToDefault(&quot;  &quot;, &quot;default&quot;)  = &quot;  &quot;
     * emptyToDefault(&quot;bat&quot;, &quot;default&quot;) = &quot;bat&quot;
     * </pre>
     *
     * @param str 要转换的字符串
     * @param defaultStr 默认字符串
     * @return 字符串本身或指定的默认字符串
     * @since 4.1.0
     */
    public static String emptyToDefault(CharSequence str, String defaultStr) {
        return isEmpty(str) ? defaultStr : str.toString();
    }

    /**
     * 如果字符串是<code>null</code>或者&quot;&quot;或者空白，则返回指定默认字符串，否则返回字符串本身。
     *
     * <pre>
     * emptyToDefault(null, &quot;default&quot;)  = &quot;default&quot;
     * emptyToDefault(&quot;&quot;, &quot;default&quot;)    = &quot;default&quot;
     * emptyToDefault(&quot;  &quot;, &quot;default&quot;)  = &quot;default&quot;
     * emptyToDefault(&quot;bat&quot;, &quot;default&quot;) = &quot;bat&quot;
     * </pre>
     *
     * @param str 要转换的字符串
     * @param defaultStr 默认字符串
     * @return 字符串本身或指定的默认字符串
     * @since 4.1.0
     */
    public static String blankToDefault(CharSequence str, String defaultStr) {
        return isBlank(str) ? defaultStr : str.toString();
    }

    /**
     * 当给定字符串为空字符串时，转换为<code>null</code>
     *
     * @param str 被转换的字符串
     * @return 转换后的字符串
     */
    public static String emptyToNull(CharSequence str) {
        return isEmpty(str) ? null : str.toString();
    }

    /**
     * 是否包含空字符串。
     *
     * <p>如果指定的字符串数组的长度为 0，或者其中的任意一个元素是空字符串，则返回 true。 <br>
     *
     * <p>例：
     *
     * <ul>
     *   <li>{@code gtcStrUtil.hasEmpty() // true}
     *   <li>{@code gtcStrUtil.hasEmpty("", null) // true}
     *   <li>{@code gtcStrUtil.hasEmpty("123", "") // true}
     *   <li>{@code gtcStrUtil.hasEmpty("123", "abc") // false}
     *   <li>{@code gtcStrUtil.hasEmpty(" ", "\t", "\n") // false}
     * </ul>
     *
     * <p>注意：该方法与 {@link #isAllEmpty(CharSequence...)} 的区别在于：
     *
     * <ul>
     *   <li>hasEmpty(CharSequence...) 等价于 {@code isEmpty(...) || isEmpty(...) || ...}
     *   <li>{@link #isAllEmpty(CharSequence...)} 等价于 {@code isEmpty(...) && isEmpty(...) && ...}
     * </ul>
     *
     * @param strs 字符串列表
     * @return 是否包含空字符串
     */
    public static boolean hasEmpty(CharSequence... strs) {
        if (GutilArray.isEmpty(strs)) {
            return true;
        }

        for (CharSequence str : strs) {
            if (isEmpty(str)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 指定字符串数组中的元素，是否全部为空字符串。
     *
     * <p>如果指定的字符串数组的长度为 0，或者所有元素都是空字符串，则返回 true。 <br>
     *
     * <p>例：
     *
     * <ul>
     *   <li>{@code gtcStrUtil.isAllEmpty() // true}
     *   <li>{@code gtcStrUtil.isAllEmpty("", null) // true}
     *   <li>{@code gtcStrUtil.isAllEmpty("123", "") // false}
     *   <li>{@code gtcStrUtil.isAllEmpty("123", "abc") // false}
     *   <li>{@code gtcStrUtil.isAllEmpty(" ", "\t", "\n") // false}
     * </ul>
     *
     * <p>注意：该方法与 {@link #hasEmpty(CharSequence...)} 的区别在于：
     *
     * <ul>
     *   <li>{@link #hasEmpty(CharSequence...)} 等价于 {@code isEmpty(...) || isEmpty(...) || ...}
     *   <li>isAllEmpty(CharSequence...) 等价于 {@code isEmpty(...) && isEmpty(...) && ...}
     * </ul>
     *
     * @param strs 字符串列表
     * @return 所有字符串是否为空白
     */
    public static boolean isAllEmpty(CharSequence... strs) {
        if (GutilArray.isEmpty(strs)) {
            return true;
        }

        for (CharSequence str : strs) {
            if (isNotEmpty(str)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 指定字符串数组中的元素，是否都不为空字符串。
     *
     * <p>如果指定的字符串数组的长度不为 0，或者所有元素都不是空字符串，则返回 true。 <br>
     *
     * <p>例：
     *
     * <ul>
     *   <li>{@code gtcStrUtil.isAllNotEmpty() // false}
     *   <li>{@code gtcStrUtil.isAllNotEmpty("", null) // false}
     *   <li>{@code gtcStrUtil.isAllNotEmpty("123", "") // false}
     *   <li>{@code gtcStrUtil.isAllNotEmpty("123", "abc") // true}
     *   <li>{@code gtcStrUtil.isAllNotEmpty(" ", "\t", "\n") // true}
     * </ul>
     *
     * <p>注意：该方法与 {@link #isAllEmpty(CharSequence...)} 的区别在于：
     *
     * <ul>
     *   <li>{@link #isAllEmpty(CharSequence...)} 等价于 {@code isEmpty(...) && isEmpty(...) && ...}
     *   <li>isAllNotEmpty(CharSequence...) 等价于 {@code !isEmpty(...) && !isEmpty(...) && ...}
     * </ul>
     *
     * @param args 字符串数组
     * @return 所有字符串是否都不为为空白
     * @since 5.3.6
     */
    public static boolean isAllNotEmpty(CharSequence... args) {
        return false == hasEmpty(args);
    }

    /**
     * 是否存都不为{@code null}或空对象或空白符的对象，通过{@link GutilStr#hasBlank(CharSequence...)} 判断元素
     *
     * @param args 被检查的对象,一个或者多个
     * @return 是否都不为空
     * @since 5.3.6
     */
    public static boolean isAllNotBlank(CharSequence... args) {
        return false == hasBlank(args);
    }

    /**
     * 检查字符串是否为null、“null”、“undefined”
     *
     * @param str 被检查的字符串
     * @return 是否为null、“null”、“undefined”
     * @since 4.0.10
     */
    public static boolean isNullOrUndefined(CharSequence str) {
        if (null == str) {
            return true;
        }
        return isNullOrUndefinedStr(str);
    }

    /**
     * 检查字符串是否为null、“”、“null”、“undefined”
     *
     * @param str 被检查的字符串
     * @return 是否为null、“”、“null”、“undefined”
     * @since 4.0.10
     */
    public static boolean isEmptyOrUndefined(CharSequence str) {
        if (isEmpty(str)) {
            return true;
        }
        return isNullOrUndefinedStr(str);
    }

    /**
     * 检查字符串是否为null、空白串、“null”、“undefined”
     *
     * @param str 被检查的字符串
     * @return 是否为null、空白串、“null”、“undefined”
     * @since 4.0.10
     */
    public static boolean isBlankOrUndefined(CharSequence str) {
        if (isBlank(str)) {
            return true;
        }
        return isNullOrUndefinedStr(str);
    }

    /**
     * 是否为“null”、“undefined”，不做空指针检查
     *
     * @param str 字符串
     * @return 是否为“null”、“undefined”
     */
    private static boolean isNullOrUndefinedStr(CharSequence str) {
        String strString = str.toString().trim();
        return NULL.equals(strString) || "undefined".equals(strString);
    }

    // ------------------------------------------------------------------------ 去除首尾空白

    /**
     * 除去字符串头尾部的空白，如果字符串是<code>null</code>，依然返回<code>null</code>。
     *
     * <p>注意，和<code>String.trim</code>不同，此方法使用<code>NumberUtil.isBlankChar</code> 来判定空白，
     * 因而可以除去英文字符集之外的其它空白，如中文空格。
     *
     * <pre>
     * trim(null)          = null
     * trim(&quot;&quot;)            = &quot;&quot;
     * trim(&quot;     &quot;)       = &quot;&quot;
     * trim(&quot;abc&quot;)         = &quot;abc&quot;
     * trim(&quot;    abc    &quot;) = &quot;abc&quot;
     * </pre>
     *
     * @param str 要处理的字符串
     * @return 除去头尾空白的字符串，如果原字串为<code>null</code>，则返回<code>null</code>
     */
    public static String trim(CharSequence str) {
        return (null == str) ? null : trim(str, 0);
    }

    /**
     * 给定字符串数组全部做去首尾空格
     *
     * @param strs 字符串数组
     * @return 去除首尾空白后的新字符串数组，原数组不会被修改；若入参为{@code null}，返回{@code null}
     */
    public static String[] trim(String[] strs) {
        if (null == strs) {
            return null;
        }
        final String[] result = new String[strs.length];
        String str;
        for (int i = 0; i < strs.length; i++) {
            str = strs[i];
            if (null != str) {
                result[i] = trim(str);
            }
        }
        return result;
    }

    /**
     * 除去字符串头尾部的空白，如果字符串是{@code null}，返回<code>""</code>。
     *
     * <pre>
     *  gtcStrUtil.trimToEmpty(null)          = ""
     *  gtcStrUtil.trimToEmpty("")            = ""
     *  gtcStrUtil.trimToEmpty("     ")       = ""
     *  gtcStrUtil.trimToEmpty("abc")         = "abc"
     *  gtcStrUtil.trimToEmpty("    abc    ") = "abc"
     * </pre>
     *
     * @param str 字符串
     * @return 去除两边空白符后的字符串, 如果为null返回""
     * @since 3.1.1
     */
    public static String trimToEmpty(CharSequence str) {
        return str == null ? EMPTY : trim(str);
    }

    /**
     * 除去字符串头尾部的空白，如果字符串是{@code null}或者""，返回{@code null}。
     *
     * <pre>
     *  gtcStrUtil.trimToNull(null)          = null
     *  gtcStrUtil.trimToNull("")            = null
     *  gtcStrUtil.trimToNull("     ")       = null
     *  gtcStrUtil.trimToNull("abc")         = "abc"
     *  gtcStrUtil.trimToEmpty("    abc    ") = "abc"
     * </pre>
     *
     * @param str 字符串
     * @return 去除两边空白符后的字符串, 如果为空返回null
     * @since 3.2.1
     */
    public static String trimToNull(CharSequence str) {
        final String trimStr = trim(str);
        return EMPTY.equals(trimStr) ? null : trimStr;
    }

    /**
     * 除去字符串头部的空白，如果字符串是<code>null</code>，则返回<code>null</code>。
     *
     * <p>注意，和<code>String.trim</code>不同，此方法使用<code>CharUtil.isBlankChar</code> 来判定空白，
     * 因而可以除去英文字符集之外的其它空白，如中文空格。
     *
     * <pre>
     * trimStart(null)         = null
     * trimStart(&quot;&quot;)           = &quot;&quot;
     * trimStart(&quot;abc&quot;)        = &quot;abc&quot;
     * trimStart(&quot;  abc&quot;)      = &quot;abc&quot;
     * trimStart(&quot;abc  &quot;)      = &quot;abc  &quot;
     * trimStart(&quot; abc &quot;)      = &quot;abc &quot;
     * </pre>
     *
     * @param str 要处理的字符串
     * @return 除去空白的字符串，如果原字串为<code>null</code>或结果字符串为<code>""</code>，则返回 <code>null</code>
     */
    public static String trimStart(CharSequence str) {
        return trim(str, -1);
    }

    /**
     * 除去字符串尾部的空白，如果字符串是<code>null</code>，则返回<code>null</code>。
     *
     * <p>注意，和<code>String.trim</code>不同，此方法使用<code>CharUtil.isBlankChar</code> 来判定空白，
     * 因而可以除去英文字符集之外的其它空白，如中文空格。
     *
     * <pre>
     * trimEnd(null)       = null
     * trimEnd(&quot;&quot;)         = &quot;&quot;
     * trimEnd(&quot;abc&quot;)      = &quot;abc&quot;
     * trimEnd(&quot;  abc&quot;)    = &quot;  abc&quot;
     * trimEnd(&quot;abc  &quot;)    = &quot;abc&quot;
     * trimEnd(&quot; abc &quot;)    = &quot; abc&quot;
     * </pre>
     *
     * @param str 要处理的字符串
     * @return 除去空白的字符串，如果原字串为<code>null</code>或结果字符串为<code>""</code>，则返回 <code>null</code>
     */
    public static String trimEnd(CharSequence str) {
        return trim(str, 1);
    }

    /**
     * 除去字符串头尾部的空白符，如果字符串是<code>null</code>，依然返回<code>null</code>。
     *
     * @param str 要处理的字符串
     * @param mode <code>-1</code>表示trimStart，<code>0</code>表示trim全部， <code>1</code>表示trimEnd
     * @return 除去指定字符后的的字符串，如果原字串为<code>null</code>，则返回<code>null</code>
     */
    public static String trim(CharSequence str, int mode) {
        if (str == null) {
            return null;
        }

        int length = str.length();
        int start = 0;
        int end = length;

        // 扫描字符串头部
        if (mode <= 0) {
            while ((start < end) && (GutilChar.isBlankChar(str.charAt(start)))) {
                start++;
            }
        }

        // 扫描字符串尾部
        if (mode >= 0) {
            while ((start < end) && (GutilChar.isBlankChar(str.charAt(end - 1)))) {
                end--;
            }
        }

        if ((start > 0) || (end < length)) {
            return str.toString().substring(start, end);
        }

        return str.toString();
    }

    /**
     * 字符串是否以给定字符开始
     *
     * @param str 字符串
     * @param c 字符
     * @return 是否开始
     */
    public static boolean startWith(CharSequence str, char c) {
        if (isEmpty(str)) {
            return false;
        }
        return c == str.charAt(0);
    }

    /**
     * 是否以指定字符串开头<br>
     * 如果给定的字符串和开头字符串都为null则返回true，否则任意一个值为null返回false
     *
     * @param str 被监测字符串
     * @param prefix 开头字符串
     * @param ignoreCase 是否忽略大小写
     * @return 是否以指定字符串开头
     * @since 5.4.3
     */
    public static boolean startWith(CharSequence str, CharSequence prefix, boolean ignoreCase) {
        return startWith(str, prefix, ignoreCase, false);
    }

    /**
     * 是否以指定字符串开头<br>
     * 如果给定的字符串和开头字符串都为null则返回true，否则任意一个值为null返回false
     *
     * @param str 被监测字符串
     * @param prefix 开头字符串
     * @param ignoreCase 是否忽略大小写
     * @param ignoreEquals 是否忽略字符串相等的情况
     * @return 是否以指定字符串开头
     * @since 5.4.3
     */
    public static boolean startWith(
            CharSequence str, CharSequence prefix, boolean ignoreCase, boolean ignoreEquals) {
        if (null == str || null == prefix) {
            if (false == ignoreEquals) {
                return false;
            }
            return null == str && null == prefix;
        }

        boolean isStartWith;
        if (ignoreCase) {
            // 使用regionMatches忽略大小写比较，避免toLowerCase产生的额外对象开销与Unicode大小写映射问题
            isStartWith =
                    str.toString().regionMatches(true, 0, prefix.toString(), 0, prefix.length());
        } else {
            isStartWith = str.toString().startsWith(prefix.toString());
        }

        if (isStartWith) {
            return (false == ignoreEquals) || (false == equals(str, prefix, ignoreCase));
        }
        return false;
    }

    /**
     * 是否以指定字符串开头
     *
     * @param str 被监测字符串
     * @param prefix 开头字符串
     * @return 是否以指定字符串开头
     */
    public static boolean startWith(CharSequence str, CharSequence prefix) {
        return startWith(str, prefix, false);
    }

    /**
     * 是否以指定字符串开头，忽略相等字符串的情况
     *
     * @param str 被监测字符串
     * @param prefix 开头字符串
     * @return 是否以指定字符串开头并且两个字符串不相等
     */
    public static boolean startWithIgnoreEquals(CharSequence str, CharSequence prefix) {
        return startWith(str, prefix, false, true);
    }

    /**
     * 是否以指定字符串开头，忽略大小写
     *
     * @param str 被监测字符串
     * @param prefix 开头字符串
     * @return 是否以指定字符串开头
     */
    public static boolean startWithIgnoreCase(CharSequence str, CharSequence prefix) {
        return startWith(str, prefix, true);
    }

    /**
     * 给定字符串是否以任何一个字符串开始<br>
     * 给定字符串和数组为空都返回false
     *
     * @param str 给定字符串
     * @param prefixes 需要检测的开始字符串
     * @return 给定字符串是否以任何一个字符串开始
     * @since 3.0.6
     */
    public static boolean startWithAny(CharSequence str, CharSequence... prefixes) {
        if (isEmpty(str) || GutilArray.isEmpty(prefixes)) {
            return false;
        }

        for (CharSequence suffix : prefixes) {
            if (startWith(str, suffix, false)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 字符串是否以给定字符结尾
     *
     * @param str 字符串
     * @param c 字符
     * @return 是否结尾
     */
    public static boolean endWith(CharSequence str, char c) {
        if (isEmpty(str)) {
            return false;
        }
        return c == str.charAt(str.length() - 1);
    }

    /**
     * 是否以指定字符串结尾<br>
     * 如果给定的字符串和开头字符串都为null则返回true，否则任意一个值为null返回false
     *
     * @param str 被监测字符串
     * @param suffix 结尾字符串
     * @param isIgnoreCase 是否忽略大小写
     * @return 是否以指定字符串结尾
     */
    public static boolean endWith(CharSequence str, CharSequence suffix, boolean isIgnoreCase) {
        if (null == str || null == suffix) {
            return null == str && null == suffix;
        }

        if (isIgnoreCase) {
            // 使用regionMatches忽略大小写比较，避免toLowerCase产生的额外对象开销与Unicode大小写映射问题
            return str.toString()
                    .regionMatches(
                            true,
                            str.length() - suffix.length(),
                            suffix.toString(),
                            0,
                            suffix.length());
        } else {
            return str.toString().endsWith(suffix.toString());
        }
    }

    /**
     * 是否以指定字符串结尾
     *
     * @param str 被监测字符串
     * @param suffix 结尾字符串
     * @return 是否以指定字符串结尾
     */
    public static boolean endWith(CharSequence str, CharSequence suffix) {
        return endWith(str, suffix, false);
    }

    /**
     * 是否以指定字符串结尾，忽略大小写
     *
     * @param str 被监测字符串
     * @param suffix 结尾字符串
     * @return 是否以指定字符串结尾
     */
    public static boolean endWithIgnoreCase(CharSequence str, CharSequence suffix) {
        return endWith(str, suffix, true);
    }

    /**
     * 给定字符串是否以任何一个字符串结尾<br>
     * 给定字符串和数组为空都返回false
     *
     * @param str 给定字符串
     * @param suffixes 需要检测的结尾字符串
     * @return 给定字符串是否以任何一个字符串结尾
     * @since 3.0.6
     */
    public static boolean endWithAny(CharSequence str, CharSequence... suffixes) {
        if (isEmpty(str) || GutilArray.isEmpty(suffixes)) {
            return false;
        }

        for (CharSequence suffix : suffixes) {
            if (endWith(str, suffix, false)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 指定字符是否在字符串中出现过
     *
     * @param str 字符串
     * @param searchChar 被查找的字符
     * @return 是否包含
     * @since 3.1.2
     */
    public static boolean contains(CharSequence str, char searchChar) {
        return indexOf(str, searchChar) > -1;
    }

    /**
     * 指定字符串是否在字符串中出现过
     *
     * @param str 字符串
     * @param searchStr 被查找的字符串
     * @return 是否包含
     * @since 5.1.1
     */
    public static boolean contains(CharSequence str, CharSequence searchStr) {
        if (null == str || null == searchStr) {
            return false;
        }
        return str.toString().contains(searchStr);
    }

    /**
     * 查找指定字符串是否包含指定字符串列表中的任意一个字符串
     *
     * @param str 指定字符串
     * @param testStrs 需要检查的字符串数组
     * @return 是否包含任意一个字符串
     * @since 3.2.0
     */
    public static boolean containsAny(CharSequence str, CharSequence... testStrs) {
        return null != getContainsStr(str, testStrs);
    }

    /**
     * 给定字符串是否包含空白符（空白符包括空格、制表符、全角空格和不间断空格）<br>
     * 如果给定字符串为null或者""，则返回false
     *
     * @param str 字符串
     * @return 是否包含空白符
     * @since 4.0.8
     */
    public static boolean containsBlank(CharSequence str) {
        if (null == str) {
            return false;
        }
        final int length = str.length();
        if (0 == length) {
            return false;
        }

        for (int i = 0; i < length; i += 1) {
            if (GutilChar.isBlankChar(str.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    /**
     * 查找指定字符串是否包含指定字符串列表中的任意一个字符串，如果包含返回找到的第一个字符串
     *
     * @param str 指定字符串
     * @param testStrs 需要检查的字符串数组
     * @return 被包含的第一个字符串
     * @since 3.2.0
     */
    public static String getContainsStr(CharSequence str, CharSequence... testStrs) {
        if (isEmpty(str) || GutilArray.isEmpty(testStrs)) {
            return null;
        }
        for (CharSequence checkStr : testStrs) {
            // 跳过数组中的null元素，避免空指针异常
            if (null == checkStr) {
                continue;
            }
            if (str.toString().contains(checkStr)) {
                return checkStr.toString();
            }
        }
        return null;
    }

    /**
     * 是否包含特定字符，忽略大小写，如果给定两个参数都为<code>null</code>，返回true；<br>
     * 如果被检测字符串为<code>null</code>而测试字符串不为<code>null</code>，或测试字符串为<code>null</code>，返回false
     *
     * @param str 被检测字符串
     * @param testStr 被测试是否包含的字符串
     * @return 是否包含
     */
    public static boolean containsIgnoreCase(CharSequence str, CharSequence testStr) {
        if (null == str) {
            return null == testStr;
        }
        if (null == testStr) {
            return false;
        }
        final String str2 = str.toString();
        final String testStr2 = testStr.toString();
        if (testStr2.isEmpty()) {
            return true;
        }
        // 使用regionMatches忽略大小写比较，避免toLowerCase产生的额外对象开销与Unicode大小写映射问题
        final int limit = str2.length() - testStr2.length();
        for (int i = 0; i <= limit; i++) {
            if (str2.regionMatches(true, i, testStr2, 0, testStr2.length())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 查找指定字符串是否包含指定字符串列表中的任意一个字符串<br>
     * 忽略大小写
     *
     * @param str 指定字符串
     * @param testStrs 需要检查的字符串数组
     * @return 是否包含任意一个字符串
     * @since 3.2.0
     */
    public static boolean containsAnyIgnoreCase(CharSequence str, CharSequence... testStrs) {
        return null != getContainsStrIgnoreCase(str, testStrs);
    }

    /**
     * 查找指定字符串是否包含指定字符串列表中的任意一个字符串，如果包含返回找到的第一个字符串<br>
     * 忽略大小写
     *
     * @param str 指定字符串
     * @param testStrs 需要检查的字符串数组
     * @return 被包含的第一个字符串
     * @since 3.2.0
     */
    public static String getContainsStrIgnoreCase(CharSequence str, CharSequence... testStrs) {
        if (isEmpty(str) || GutilArray.isEmpty(testStrs)) {
            return null;
        }
        for (CharSequence testStr : testStrs) {
            // 跳过数组中的null元素，避免空指针异常
            if (null == testStr) {
                continue;
            }
            if (containsIgnoreCase(str, testStr)) {
                return testStr.toString();
            }
        }
        return null;
    }

    /**
     * 获得set或get或is方法对应的标准属性名<br>
     * 例如：setName 返回 name
     *
     * <pre>
     * getName =》name
     * setName =》name
     * isName  =》name
     * </pre>
     *
     * @param getOrSetMethodName Get或Set方法名
     * @return 如果是set或get方法名，返回field， 否则null
     */
    public static String getGeneralField(CharSequence getOrSetMethodName) {
        if (null == getOrSetMethodName) {
            return null;
        }
        final String getOrSetMethodNameStr = getOrSetMethodName.toString();
        if (getOrSetMethodNameStr.startsWith("get") || getOrSetMethodNameStr.startsWith("set")) {
            return removePreAndLowerFirst(getOrSetMethodName, 3);
        } else if (getOrSetMethodNameStr.startsWith("is")) {
            return removePreAndLowerFirst(getOrSetMethodName, 2);
        }
        return null;
    }

    /**
     * 生成set方法名<br>
     * 例如：name 返回 setName
     *
     * @param fieldName 属性名
     * @return setXxx
     */
    public static String genSetter(CharSequence fieldName) {
        return upperFirstAndAddPre(fieldName, "set");
    }

    /**
     * 生成get方法名
     *
     * @param fieldName 属性名
     * @return getXxx
     */
    public static String genGetter(CharSequence fieldName) {
        return upperFirstAndAddPre(fieldName, "get");
    }

    /**
     * 移除字符串中所有给定字符串<br>
     * 例：removeAll("aa-bb-cc-dd", "-") =》 aabbccdd
     *
     * @param str 字符串
     * @param strToRemove 被移除的字符串
     * @return 移除后的字符串
     */
    public static String removeAll(CharSequence str, CharSequence strToRemove) {
        // strToRemove如果为空， 也不用继续后面的逻辑
        if (isEmpty(str) || isEmpty(strToRemove)) {
            return str(str);
        }
        return str.toString().replace(strToRemove, EMPTY);
    }

    /**
     * 去除字符串中指定的多个字符，如有多个则全部去除
     *
     * @param str 字符串
     * @param chars 字符列表
     * @return 去除后的字符
     * @since 4.2.2
     */
    public static String removeAll(CharSequence str, char... chars) {
        if (null == str || GutilArray.isEmpty(chars)) {
            return str(str);
        }
        final int len = str.length();
        if (0 == len) {
            return str(str);
        }
        final StringBuilder builder = builder(len);
        char c;
        for (int i = 0; i < len; i++) {
            c = str.charAt(i);
            if (false == GutilArray.contains(chars, c)) {
                builder.append(c);
            }
        }
        return builder.toString();
    }

    /**
     * 移除字符串中所有给定字符串，当某个字符串出现多次，则全部移除<br>
     * 例：removeAny("aa-bb-cc-dd", "a", "b") =》 --cc-dd
     *
     * @param str 字符串
     * @param strsToRemove 被移除的字符串
     * @return 移除后的字符串
     * @since 5.3.8
     */
    public static String removeAny(CharSequence str, CharSequence... strsToRemove) {
        String result = str(str);
        if (isNotEmpty(str)) {
            for (CharSequence strToRemove : strsToRemove) {
                result = removeAll(result, strToRemove);
            }
        }
        return result;
    }

    /**
     * 去掉首部指定长度的字符串并将剩余字符串首字母小写<br>
     * 例如：str=setName, preLength=3 =》 return name
     *
     * @param str 被处理的字符串
     * @param preLength 去掉的长度
     * @return 处理后的字符串，若{@code str}为null、{@code preLength}为负数或剩余部分为空（即 {@code str.length() <=
     *     preLength}），不符合规范返回null
     */
    public static String removePreAndLowerFirst(CharSequence str, int preLength) {
        if (str == null || preLength < 0) {
            return null;
        }
        if (str.length() > preLength) {
            char first = Character.toLowerCase(str.charAt(preLength));
            if (str.length() > preLength + 1) {
                return first + str.toString().substring(preLength + 1);
            }
            return String.valueOf(first);
        } else {
            return null;
        }
    }

    /**
     * 去掉首部指定长度的字符串并将剩余字符串首字母小写<br>
     * 例如：str=setName, prefix=set =》 return name
     *
     * @param str 被处理的字符串
     * @param prefix 前缀
     * @return 处理后的字符串，不符合规范返回null
     */
    public static String removePreAndLowerFirst(CharSequence str, CharSequence prefix) {
        return lowerFirst(removePrefix(str, prefix));
    }

    /**
     * 原字符串首字母大写并在其首部添加指定字符串 例如：str=name, preString=get =》 return getName
     *
     * @param str 被处理的字符串
     * @param preString 添加的首部
     * @return 处理后的字符串
     */
    public static String upperFirstAndAddPre(CharSequence str, String preString) {
        if (str == null || preString == null) {
            return null;
        }
        return preString + upperFirst(str);
    }

    /**
     * 大写首字母<br>
     * 例如：str = name, return Name
     *
     * @param str 字符串
     * @return 字符串
     */
    public static String upperFirst(CharSequence str) {
        if (null == str) {
            return null;
        }
        if (str.length() > 0) {
            char firstChar = str.charAt(0);
            if (Character.isLowerCase(firstChar)) {
                return Character.toUpperCase(firstChar) + subSuf(str, 1);
            }
        }
        return str.toString();
    }

    /**
     * 小写首字母<br>
     * 例如：str = Name, return name
     *
     * @param str 字符串
     * @return 字符串
     */
    public static String lowerFirst(CharSequence str) {
        if (null == str) {
            return null;
        }
        if (str.length() > 0) {
            char firstChar = str.charAt(0);
            if (Character.isUpperCase(firstChar)) {
                return Character.toLowerCase(firstChar) + subSuf(str, 1);
            }
        }
        return str.toString();
    }

    /**
     * 去掉指定前缀
     *
     * @param str 字符串
     * @param prefix 前缀
     * @return 切掉后的字符串，若前缀不是 preffix， 返回原字符串
     */
    public static String removePrefix(CharSequence str, CharSequence prefix) {
        if (isEmpty(str) || isEmpty(prefix)) {
            return str(str);
        }

        final String str2 = str.toString();
        if (str2.startsWith(prefix.toString())) {
            return subSuf(str2, prefix.length()); // 截取后半段
        }
        return str2;
    }

    /**
     * 忽略大小写去掉指定前缀
     *
     * @param str 字符串
     * @param prefix 前缀
     * @return 切掉后的字符串，若前缀不是 prefix， 返回原字符串
     */
    public static String removePrefixIgnoreCase(CharSequence str, CharSequence prefix) {
        if (isEmpty(str) || isEmpty(prefix)) {
            return str(str);
        }

        final String str2 = str.toString();
        if (str2.toLowerCase().startsWith(prefix.toString().toLowerCase())) {
            return subSuf(str2, prefix.length()); // 截取后半段
        }
        return str2;
    }

    /**
     * 去掉指定后缀
     *
     * @param str 字符串
     * @param suffix 后缀
     * @return 切掉后的字符串，若后缀不是 suffix， 返回原字符串
     */
    public static String removeSuffix(CharSequence str, CharSequence suffix) {
        if (isEmpty(str) || isEmpty(suffix)) {
            return str(str);
        }

        final String str2 = str.toString();
        if (str2.endsWith(suffix.toString())) {
            return subPre(str2, str2.length() - suffix.length()); // 截取前半段
        }
        return str2;
    }

    /**
     * 去掉指定后缀，并小写首字母
     *
     * @param str 字符串
     * @param suffix 后缀
     * @return 切掉后的字符串，若后缀不是 suffix， 返回原字符串
     */
    public static String removeSufAndLowerFirst(CharSequence str, CharSequence suffix) {
        return lowerFirst(removeSuffix(str, suffix));
    }

    /**
     * 忽略大小写去掉指定后缀
     *
     * @param str 字符串
     * @param suffix 后缀
     * @return 切掉后的字符串，若后缀不是 suffix， 返回原字符串
     */
    public static String removeSuffixIgnoreCase(CharSequence str, CharSequence suffix) {
        if (isEmpty(str) || isEmpty(suffix)) {
            return str(str);
        }

        final String str2 = str.toString();
        if (str2.toLowerCase().endsWith(suffix.toString().toLowerCase())) {
            return subPre(str2, str2.length() - suffix.length());
        }
        return str2;
    }

    /**
     * 去除两边的指定字符串
     *
     * @param str 被处理的字符串
     * @param prefixOrSuffix 前缀或后缀
     * @return 处理后的字符串
     * @since 3.1.2
     */
    public static String strip(CharSequence str, CharSequence prefixOrSuffix) {
        if (equals(str, prefixOrSuffix)) {
            // 对于去除相同字符的情况单独处理
            return EMPTY;
        }
        return strip(str, prefixOrSuffix, prefixOrSuffix);
    }

    /**
     * 去除两边的指定字符串
     *
     * @param str 被处理的字符串
     * @param prefix 前缀
     * @param suffix 后缀
     * @return 处理后的字符串
     * @since 3.1.2
     */
    public static String strip(CharSequence str, CharSequence prefix, CharSequence suffix) {
        if (isEmpty(str)) {
            return str(str);
        }

        int from = 0;
        int to = str.length();

        String str2 = str.toString();
        if (startWith(str2, prefix)) {
            from = prefix.length();
        }
        if (endWith(str2, suffix)) {
            to -= suffix.length();
        }

        return str2.substring(Math.min(from, to), Math.max(from, to));
    }

    /**
     * 去除两边的指定字符串，忽略大小写
     *
     * @param str 被处理的字符串
     * @param prefixOrSuffix 前缀或后缀
     * @return 处理后的字符串
     * @since 3.1.2
     */
    public static String stripIgnoreCase(CharSequence str, CharSequence prefixOrSuffix) {
        return stripIgnoreCase(str, prefixOrSuffix, prefixOrSuffix);
    }

    /**
     * 去除两边的指定字符串，忽略大小写
     *
     * @param str 被处理的字符串
     * @param prefix 前缀
     * @param suffix 后缀
     * @return 处理后的字符串
     * @since 3.1.2
     */
    public static String stripIgnoreCase(
            CharSequence str, CharSequence prefix, CharSequence suffix) {
        if (isEmpty(str)) {
            return str(str);
        }
        int from = 0;
        int to = str.length();

        String str2 = str.toString();
        if (startWithIgnoreCase(str2, prefix)) {
            from = prefix.length();
        }
        if (endWithIgnoreCase(str2, suffix)) {
            to -= suffix.length();
        }
        return str2.substring(from, to);
    }

    /**
     * 如果给定字符串不是以prefix开头的，在开头补充 prefix
     *
     * @param str 字符串
     * @param prefix 前缀
     * @return 补充后的字符串
     */
    public static String addPrefixIfNot(CharSequence str, CharSequence prefix) {
        if (isEmpty(str) || isEmpty(prefix)) {
            return str(str);
        }

        final String str2 = str.toString();
        final String prefix2 = prefix.toString();
        if (false == str2.startsWith(prefix2)) {
            return prefix2.concat(str2);
        }
        return str2;
    }

    /**
     * 如果给定字符串不是以suffix结尾的，在尾部补充 suffix
     *
     * @param str 字符串
     * @param suffix 后缀
     * @return 补充后的字符串
     */
    public static String addSuffixIfNot(CharSequence str, CharSequence suffix) {
        if (isEmpty(str) || isEmpty(suffix)) {
            return str(str);
        }

        final String str2 = str.toString();
        final String suffix2 = suffix.toString();
        if (false == str2.endsWith(suffix2)) {
            return str2.concat(suffix2);
        }
        return str2;
    }

    // ------------------------------------------------------------------------------
    // 切割

    /**
     * 改进JDK subString<br>
     * index从0开始计算，最后一个字符为-1<br>
     * 如果from和to位置一样，返回 "" <br>
     * 如果from或to为负数，则按照length从后向前数位置，如果绝对值大于字符串长度，则from归到0，to归到length<br>
     * 如果经过修正的index中from大于to，则互换from和to，示例如下： <br>
     * abcdefgh 2 3 =》 c <br>
     * abcdefgh 2 -3 =》 cde <br>
     *
     * @param str 字符串
     * @param fromIndexInclude 开始的index（包括）
     * @param toIndexExclude 结束的index（不包括）
     * @return 字串
     */
    public static String sub(CharSequence str, int fromIndexInclude, int toIndexExclude) {
        if (isEmpty(str)) {
            return str(str);
        }
        int len = str.length();

        if (fromIndexInclude < 0) {
            fromIndexInclude = len + fromIndexInclude;
            if (fromIndexInclude < 0) {
                fromIndexInclude = 0;
            }
        } else if (fromIndexInclude > len) {
            fromIndexInclude = len;
        }

        if (toIndexExclude < 0) {
            toIndexExclude = len + toIndexExclude;
            if (toIndexExclude < 0) {
                toIndexExclude = len;
            }
        } else if (toIndexExclude > len) {
            toIndexExclude = len;
        }

        if (toIndexExclude < fromIndexInclude) {
            int tmp = fromIndexInclude;
            fromIndexInclude = toIndexExclude;
            toIndexExclude = tmp;
        }

        if (fromIndexInclude == toIndexExclude) {
            return EMPTY;
        }

        return str.toString().substring(fromIndexInclude, toIndexExclude);
    }

    /**
     * 通过码点（CodePoint）截取字符串，可以正确截断 Emoji 等增补字符<br>
     * 起始位置小于 0 时钳制为 0，结束位置超过字符串码点总数时钳制为总数，不做越界抛错
     *
     * @param str 字符串
     * @param fromIndex 开始的码点位置（包括）
     * @param toIndex 结束的码点位置（不包括）
     * @return 截取后的字符串
     */
    public static String subByCodePoint(CharSequence str, int fromIndex, int toIndex) {
        if (isEmpty(str)) {
            return str(str);
        }

        final int codePointCount = str.toString().codePointCount(0, str.length());
        if (fromIndex < 0) {
            fromIndex = 0;
        }
        if (toIndex > codePointCount) {
            toIndex = codePointCount;
        }
        if (fromIndex > toIndex) {
            fromIndex = toIndex;
        }

        if (fromIndex == toIndex) {
            return EMPTY;
        }

        final StringBuilder sb = new StringBuilder();
        final int subLen = toIndex - fromIndex;
        str.toString()
                .codePoints()
                .skip(fromIndex)
                .limit(subLen)
                .forEach(v -> sb.append(Character.toChars(v)));
        return sb.toString();
    }

    /**
     * 切割指定位置之前部分的字符串
     *
     * @param string 字符串
     * @param toIndexExclude 切割到的位置（不包括）
     * @return 切割后的剩余的前半部分字符串
     */
    public static String subPre(CharSequence string, int toIndexExclude) {
        return sub(string, 0, toIndexExclude);
    }

    /**
     * 切割指定位置之后部分的字符串
     *
     * @param string 字符串
     * @param fromIndex 切割开始的位置（包括）
     * @return 切割后后剩余的后半部分字符串；若为{@code null}返回{@code null}，若为空串返回{@code ""}， 与{@link
     *     #subPre(CharSequence, int)}保持一致
     */
    public static String subSuf(CharSequence string, int fromIndex) {
        if (null == string) {
            return null;
        }
        return sub(string, fromIndex, string.length());
    }

    /**
     * 切割指定长度的后部分的字符串
     *
     * <pre>
     *  gtcStrUtil.subSufByLength("abcde", 3)      =    "cde"
     *  gtcStrUtil.subSufByLength("abcde", 0)      =    ""
     *  gtcStrUtil.subSufByLength("abcde", -5)     =    ""
     *  gtcStrUtil.subSufByLength("abcde", -1)     =    ""
     *  gtcStrUtil.subSufByLength("abcde", 5)       =    "abcde"
     *  gtcStrUtil.subSufByLength("abcde", 10)     =    "abcde"
     *  gtcStrUtil.subSufByLength(null, 3)               =    null
     * </pre>
     *
     * @param string 字符串
     * @param length 切割长度
     * @return 切割后后剩余的后半部分字符串
     * @since 4.0.1
     */
    public static String subSufByLength(CharSequence string, int length) {
        if (isEmpty(string)) {
            return null;
        }
        if (length <= 0) {
            return EMPTY;
        }
        return sub(string, -length, string.length());
    }

    /**
     * 截取字符串,从指定位置开始,截取指定长度的字符串<br>
     * author weibaohui
     *
     * @param input 原始字符串
     * @param fromIndex 开始的index,包括
     * @param length 要截取的长度
     * @return 截取后的字符串
     */
    public static String subWithLength(String input, int fromIndex, int length) {
        return sub(input, fromIndex, fromIndex + length);
    }

    /**
     * 截取分隔字符串之前的字符串，不包括分隔字符串<br>
     * 如果给定的字符串为空串（null或""）或者分隔字符串为null，返回原字符串<br>
     * 如果分隔字符串为空串""，则返回空串，如果分隔字符串未找到，返回原字符串，举例如下：
     *
     * <pre>
     *  gtcStrUtil.subBefore(null, *, false)      = null
     *  gtcStrUtil.subBefore("", *, false)        = ""
     *  gtcStrUtil.subBefore("abc", "a", false)   = ""
     *  gtcStrUtil.subBefore("abcba", "b", false) = "a"
     *  gtcStrUtil.subBefore("abc", "c", false)   = "ab"
     *  gtcStrUtil.subBefore("abc", "d", false)   = "abc"
     *  gtcStrUtil.subBefore("abc", "", false)    = ""
     *  gtcStrUtil.subBefore("abc", null, false)  = "abc"
     * </pre>
     *
     * @param string 被查找的字符串
     * @param separator 分隔字符串（不包括）
     * @param isLastSeparator 是否查找最后一个分隔字符串（多次出现分隔字符串时选取最后一个），true为选取最后一个
     * @return 切割后的字符串
     * @since 3.1.1
     */
    public static String subBefore(
            CharSequence string, CharSequence separator, boolean isLastSeparator) {
        if (isEmpty(string) || separator == null) {
            return null == string ? null : string.toString();
        }

        final String str = string.toString();
        final String sep = separator.toString();
        if (sep.isEmpty()) {
            return EMPTY;
        }
        final int pos = isLastSeparator ? str.lastIndexOf(sep) : str.indexOf(sep);
        if (INDEX_NOT_FOUND == pos) {
            return str;
        }
        if (0 == pos) {
            return EMPTY;
        }
        return str.substring(0, pos);
    }

    /**
     * 截取分隔字符串之前的字符串，不包括分隔字符串<br>
     * 如果给定的字符串为空串（null或""）或者分隔字符串为null，返回原字符串<br>
     * 如果分隔字符串未找到，返回原字符串，举例如下：
     *
     * <pre>
     *  gtcStrUtil.subBefore(null, *, false)      = null
     *  gtcStrUtil.subBefore("", *, false)        = ""
     *  gtcStrUtil.subBefore("abc", 'a', false)   = ""
     *  gtcStrUtil.subBefore("abcba", 'b', false) = "a"
     *  gtcStrUtil.subBefore("abc", 'c', false)   = "ab"
     *  gtcStrUtil.subBefore("abc", 'd', false)   = "abc"
     * </pre>
     *
     * @param string 被查找的字符串
     * @param separator 分隔字符串（不包括）
     * @param isLastSeparator 是否查找最后一个分隔字符串（多次出现分隔字符串时选取最后一个），true为选取最后一个
     * @return 切割后的字符串
     * @since 4.1.15
     */
    public static String subBefore(CharSequence string, char separator, boolean isLastSeparator) {
        if (isEmpty(string)) {
            return null == string ? null : EMPTY;
        }

        final String str = string.toString();
        final int pos = isLastSeparator ? str.lastIndexOf(separator) : str.indexOf(separator);
        if (INDEX_NOT_FOUND == pos) {
            return str;
        }
        if (0 == pos) {
            return EMPTY;
        }
        return str.substring(0, pos);
    }

    /**
     * 截取分隔字符串之后的字符串，不包括分隔字符串<br>
     * 如果给定的字符串为空串（null或""），返回原字符串<br>
     * 如果分隔字符串为null，返回原字符串；如果分隔字符串为空串""，则返回原字符串，如果分隔字符串未找到，返回空串，举例如下：
     *
     * <pre>
     *  gtcStrUtil.subAfter(null, *, false)      = null
     *  gtcStrUtil.subAfter("", *, false)        = ""
     *  gtcStrUtil.subAfter("abc", null, false)  = "abc"
     *  gtcStrUtil.subAfter("abc", "a", false)   = "bc"
     *  gtcStrUtil.subAfter("abcba", "b", false) = "cba"
     *  gtcStrUtil.subAfter("abc", "c", false)   = ""
     *  gtcStrUtil.subAfter("abc", "d", false)   = ""
     *  gtcStrUtil.subAfter("abc", "", false)    = "abc"
     * </pre>
     *
     * @param string 被查找的字符串
     * @param separator 分隔字符串（不包括）
     * @param isLastSeparator 是否查找最后一个分隔字符串（多次出现分隔字符串时选取最后一个），true为选取最后一个
     * @return 切割后的字符串
     * @since 3.1.1
     */
    public static String subAfter(
            CharSequence string, CharSequence separator, boolean isLastSeparator) {
        if (isEmpty(string) || separator == null) {
            return null == string ? null : string.toString();
        }
        final String str = string.toString();
        final String sep = separator.toString();
        final int pos = isLastSeparator ? str.lastIndexOf(sep) : str.indexOf(sep);
        if (INDEX_NOT_FOUND == pos || (string.length() - 1) == pos) {
            return EMPTY;
        }
        return str.substring(pos + separator.length());
    }

    /**
     * 截取分隔字符串之后的字符串，不包括分隔字符串<br>
     * 如果给定的字符串为空串（null或""），返回原字符串<br>
     * 如果分隔字符串为空串（null或""），则返回空串，如果分隔字符串未找到，返回空串，举例如下：
     *
     * <pre>
     *  gtcStrUtil.subAfter(null, *, false)      = null
     *  gtcStrUtil.subAfter("", *, false)        = ""
     *  gtcStrUtil.subAfter("abc", 'a', false)   = "bc"
     *  gtcStrUtil.subAfter("abcba", 'b', false) = "cba"
     *  gtcStrUtil.subAfter("abc", 'c', false)   = ""
     *  gtcStrUtil.subAfter("abc", 'd', false)   = ""
     * </pre>
     *
     * @param string 被查找的字符串
     * @param separator 分隔字符串（不包括）
     * @param isLastSeparator 是否查找最后一个分隔字符串（多次出现分隔字符串时选取最后一个），true为选取最后一个
     * @return 切割后的字符串
     * @since 4.1.15
     */
    public static String subAfter(CharSequence string, char separator, boolean isLastSeparator) {
        if (isEmpty(string)) {
            return null == string ? null : EMPTY;
        }
        final String str = string.toString();
        final int pos = isLastSeparator ? str.lastIndexOf(separator) : str.indexOf(separator);
        if (INDEX_NOT_FOUND == pos) {
            return EMPTY;
        }
        return str.substring(pos + 1);
    }

    /**
     * 截取指定字符串中间部分，不包括标识字符串<br>
     *
     * <p>栗子：
     *
     * <pre>
     *  gtcStrUtil.subBetween("wx[b]yz", "[", "]") = "b"
     *  gtcStrUtil.subBetween(null, *, *)          = null
     *  gtcStrUtil.subBetween(*, null, *)          = null
     *  gtcStrUtil.subBetween(*, *, null)          = null
     *  gtcStrUtil.subBetween("", "", "")          = ""
     *  gtcStrUtil.subBetween("", "", "]")         = null
     *  gtcStrUtil.subBetween("", "[", "]")        = null
     *  gtcStrUtil.subBetween("yabcz", "", "")     = ""
     *  gtcStrUtil.subBetween("yabcz", "y", "z")   = "abc"
     *  gtcStrUtil.subBetween("yabczyabcz", "y", "z")   = "abc"
     * </pre>
     *
     * @param str 被切割的字符串
     * @param before 截取开始的字符串标识
     * @param after 截取到的字符串标识
     * @return 截取后的字符串
     * @since 3.1.1
     */
    public static String subBetween(CharSequence str, CharSequence before, CharSequence after) {
        if (str == null || before == null || after == null) {
            return null;
        }

        final String str2 = str.toString();
        final String before2 = before.toString();
        final String after2 = after.toString();

        final int start = str2.indexOf(before2);
        if (start != INDEX_NOT_FOUND) {
            final int end = str2.indexOf(after2, start + before2.length());
            if (end != INDEX_NOT_FOUND) {
                return str2.substring(start + before2.length(), end);
            }
        }
        return null;
    }

    /**
     * 截取指定字符串中间部分，不包括标识字符串<br>
     *
     * <p>栗子：
     *
     * <pre>
     *  gtcStrUtil.subBetween(null, *)            = null
     *  gtcStrUtil.subBetween("", "")             = ""
     *  gtcStrUtil.subBetween("", "tag")          = null
     *  gtcStrUtil.subBetween("tagabctag", null)  = null
     *  gtcStrUtil.subBetween("tagabctag", "")    = ""
     *  gtcStrUtil.subBetween("tagabctag", "tag") = "abc"
     * </pre>
     *
     * @param str 被切割的字符串
     * @param beforeAndAfter 截取开始和结束的字符串标识
     * @return 截取后的字符串
     * @since 3.1.1
     */
    public static String subBetween(CharSequence str, CharSequence beforeAndAfter) {
        return subBetween(str, beforeAndAfter, beforeAndAfter);
    }

    /**
     * 给定字符串是否被字符包围
     *
     * @param str 字符串
     * @param prefix 前缀
     * @param suffix 后缀
     * @return 是否包围，空串不包围
     */
    public static boolean isSurround(CharSequence str, CharSequence prefix, CharSequence suffix) {
        if (GutilStr.isBlank(str) || null == prefix || null == suffix) {
            return false;
        }
        if (str.length() < (prefix.length() + suffix.length())) {
            return false;
        }

        final String str2 = str.toString();
        return str2.startsWith(prefix.toString()) && str2.endsWith(suffix.toString());
    }

    /**
     * 给定字符串是否被字符包围
     *
     * @param str 字符串
     * @param prefix 前缀
     * @param suffix 后缀
     * @return 是否包围，空串不包围
     */
    public static boolean isSurround(CharSequence str, char prefix, char suffix) {
        if (GutilStr.isBlank(str)) {
            return false;
        }
        if (str.length() < 2) {
            return false;
        }

        return str.charAt(0) == prefix && str.charAt(str.length() - 1) == suffix;
    }

    /**
     * 重复某个字符
     *
     * @param c 被重复的字符
     * @param count 重复的数目，如果小于等于0则返回""
     * @return 重复字符字符串
     */
    public static String repeat(char c, int count) {
        if (count <= 0) {
            return EMPTY;
        }

        char[] result = new char[count];
        for (int i = 0; i < count; i++) {
            result[i] = c;
        }
        return new String(result);
    }

    /**
     * 重复某个字符串
     *
     * @param str 被重复的字符
     * @param count 重复的数目
     * @return 重复字符字符串
     */
    public static String repeat(CharSequence str, int count) {
        if (null == str) {
            return null;
        }
        if (count <= 0 || str.length() == 0) {
            return EMPTY;
        }
        if (count == 1) {
            return str.toString();
        }

        // 检查
        final int len = str.length();
        final long longSize = (long) len * (long) count;
        final int size = (int) longSize;
        if (size != longSize) {
            throw new ArrayIndexOutOfBoundsException(
                    "Required String length is too large: " + longSize);
        }

        final char[] array = new char[size];
        str.toString().getChars(0, len, array, 0);
        int n;
        for (n = len; n < size - n; n <<= 1) { // n <<= 1相当于n *2
            System.arraycopy(array, 0, array, n, n);
        }
        System.arraycopy(array, 0, array, n, size - n);
        return new String(array);
    }

    /**
     * 重复某个字符串到指定长度
     *
     * @param str 被重复的字符
     * @param padLen 指定长度
     * @return 重复字符字符串
     * @since 4.3.2
     */
    public static String repeatByLength(CharSequence str, int padLen) {
        if (null == str) {
            return null;
        }
        if (padLen <= 0) {
            return GutilStr.EMPTY;
        }
        final int strLen = str.length();
        if (0 == strLen) {
            // 空串无法重复，直接返回空串，避免取模除零
            return GutilStr.EMPTY;
        }
        if (strLen == padLen) {
            return str.toString();
        } else if (strLen > padLen) {
            return subPre(str, padLen);
        }

        // 重复，直到达到指定长度
        final char[] padding = new char[padLen];
        for (int i = 0; i < padLen; i++) {
            padding[i] = str.charAt(i % strLen);
        }
        return new String(padding);
    }

    /**
     * 重复某个字符串并通过分界符连接
     *
     * <pre>
     *  gtcStrUtil.repeatAndJoin("?", 5, ",")   = "?,?,?,?,?"
     *  gtcStrUtil.repeatAndJoin("?", 0, ",")   = ""
     *  gtcStrUtil.repeatAndJoin("?", 5, null) = "?????"
     * </pre>
     *
     * @param str 被重复的字符串
     * @param count 数量
     * @param conjunction 分界符
     * @return 连接后的字符串
     * @since 4.0.1
     */
    public static String repeatAndJoin(CharSequence str, int count, CharSequence conjunction) {
        if (count <= 0) {
            return EMPTY;
        }
        StringBuilder builder = new StringBuilder();
        boolean isFirst = true;
        while (count-- > 0) {
            if (isFirst) {
                isFirst = false;
            } else if (isNotEmpty(conjunction)) {
                builder.append(conjunction);
            }
            builder.append(str);
        }
        return builder.toString();
    }

    /**
     * 比较两个字符串（大小写敏感）。
     *
     * <pre>
     * equals(null, null)   = true
     * equals(null, &quot;abc&quot;)  = false
     * equals(&quot;abc&quot;, null)  = false
     * equals(&quot;abc&quot;, &quot;abc&quot;) = true
     * equals(&quot;abc&quot;, &quot;ABC&quot;) = false
     * </pre>
     *
     * @param str1 要比较的字符串1
     * @param str2 要比较的字符串2
     * @return 如果两个字符串相同，或者都是<code>null</code>，则返回<code>true</code>
     */
    public static boolean equals(CharSequence str1, CharSequence str2) {
        return equals(str1, str2, false);
    }

    /**
     * 比较两个字符串（大小写不敏感）。
     *
     * <pre>
     * equalsIgnoreCase(null, null)   = true
     * equalsIgnoreCase(null, &quot;abc&quot;)  = false
     * equalsIgnoreCase(&quot;abc&quot;, null)  = false
     * equalsIgnoreCase(&quot;abc&quot;, &quot;abc&quot;) = true
     * equalsIgnoreCase(&quot;abc&quot;, &quot;ABC&quot;) = true
     * </pre>
     *
     * @param str1 要比较的字符串1
     * @param str2 要比较的字符串2
     * @return 如果两个字符串相同，或者都是<code>null</code>，则返回<code>true</code>
     */
    public static boolean equalsIgnoreCase(CharSequence str1, CharSequence str2) {
        return equals(str1, str2, true);
    }

    /**
     * 比较两个字符串是否相等。
     *
     * @param str1 要比较的字符串1
     * @param str2 要比较的字符串2
     * @param ignoreCase 是否忽略大小写
     * @return 如果两个字符串相同，或者都是<code>null</code>，则返回<code>true</code>
     * @since 3.2.0
     */
    public static boolean equals(CharSequence str1, CharSequence str2, boolean ignoreCase) {
        if (null == str1) {
            // 只有两个都为null才判断相等
            return str2 == null;
        }
        if (null == str2) {
            // 字符串2空，字符串1非空，直接false
            return false;
        }

        if (ignoreCase) {
            return str1.toString().equalsIgnoreCase(str2.toString());
        } else {
            return str1.toString().contentEquals(str2);
        }
    }

    /**
     * 给定字符串是否与提供的中任一字符串相同（忽略大小写），相同则返回{@code true}，没有相同的返回{@code false}<br>
     * 如果参与比对的字符串列表为空，返回{@code false}
     *
     * @param str1 给定需要检查的字符串
     * @param strs 需要参与比对的字符串列表
     * @return 是否相同
     * @since 4.3.2
     */
    public static boolean equalsAnyIgnoreCase(CharSequence str1, CharSequence... strs) {
        return equalsAny(str1, true, strs);
    }

    /**
     * 给定字符串是否与提供的中任一字符串相同，相同则返回{@code true}，没有相同的返回{@code false}<br>
     * 如果参与比对的字符串列表为空，返回{@code false}
     *
     * @param str1 给定需要检查的字符串
     * @param strs 需要参与比对的字符串列表
     * @return 是否相同
     * @since 4.3.2
     */
    public static boolean equalsAny(CharSequence str1, CharSequence... strs) {
        return equalsAny(str1, false, strs);
    }

    /**
     * 给定字符串是否与提供的中任一字符串相同，相同则返回{@code true}，没有相同的返回{@code false}<br>
     * 如果参与比对的字符串列表为空，返回{@code false}
     *
     * @param str1 给定需要检查的字符串
     * @param ignoreCase 是否忽略大小写
     * @param strs 需要参与比对的字符串列表
     * @return 是否相同
     * @since 4.3.2
     */
    public static boolean equalsAny(CharSequence str1, boolean ignoreCase, CharSequence... strs) {
        if (GutilArray.isEmpty(strs)) {
            return false;
        }

        for (CharSequence str : strs) {
            if (equals(str1, str, ignoreCase)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 格式化文本, {} 表示占位符<br>
     * 此方法只是简单将占位符 {} 按照顺序替换为参数<br>
     * 如果想输出 {} 使用 \\转义 { 即可，如果想输出 {} 之前的 \ 使用双转义符 \\\\ 即可<br>
     * 例：<br>
     * 通常使用：format("this is {} for {}", "a", "b") =》 this is a for b<br>
     * 转义{}： format("this is \\{} for {}", "a", "b") =》 this is \{} for a<br>
     * 转义\： format("this is \\\\{} for {}", "a", "b") =》 this is \a for b<br>
     *
     * @param template 文本模板，被替换的部分用 {} 表示，如果模板为null，返回"null"
     * @param params 参数值
     * @return 格式化后的文本，如果模板为null，返回"null"
     */
    public static String format(CharSequence template, Object... params) {
        if (null == template) {
            return NULL;
        }
        if (GutilArray.isEmpty(params) || isBlank(template)) {
            return template.toString();
        }
        return GuStrFormatter.format(template.toString(), params);
    }

    /**
     * 有序的格式化文本，使用{number}做为占位符<br>
     * 通常使用：format("this is {0} for {1}", "a", "b") =》 this is a for b<br>
     *
     * @param pattern 文本格式
     * @param arguments 参数
     * @return 格式化后的文本
     */
    public static String indexedFormat(CharSequence pattern, Object... arguments) {
        return MessageFormat.format(pattern.toString(), arguments);
    }

    /**
     * 格式化文本，使用 {varName} 占位<br>
     * map = {a: "aValue", b: "bValue"} format("{a} and {b}", map) ---=》 aValue and bValue
     *
     * @param template 文本模板，被替换的部分用 {key} 表示
     * @param map 参数值对
     * @return 格式化后的文本
     */
    public static String format(CharSequence template, Map<?, ?> map) {
        return format(template, map, true);
    }

    /**
     * 格式化文本，使用 {varName} 占位<br>
     * map = {a: "aValue", b: "bValue"} format("{a} and {b}", map) ---=》 aValue and bValue
     *
     * @param template 文本模板，被替换的部分用 {key} 表示
     * @param map 参数值对
     * @param ignoreNull 是否忽略 {@code null} 值，忽略则 {@code null} 值对应的变量不被替换，否则替换为""
     * @return 格式化后的文本
     * @since 5.4.3
     */
    public static String format(CharSequence template, Map<?, ?> map, boolean ignoreNull) {
        if (null == template) {
            return null;
        }
        if (null == map || map.isEmpty()) {
            return template.toString();
        }

        String template2 = template.toString();
        String value;
        for (Entry<?, ?> entry : map.entrySet()) {
            value = utf8Str(entry.getValue());
            if (null == value && ignoreNull) {
                continue;
            }
            if (null == value) {
                // 不忽略null值时，按javadoc约定将null值替换为空串
                value = EMPTY;
            }
            template2 = replace(template2, "{" + entry.getKey() + "}", value);
        }
        return template2;
    }

    /** 字符集名称与字符集对象的缓存，避免重复调用 {@link Charset#forName(String)} */
    private static final Map<String, Charset> CHARSET_CACHE = new ConcurrentHashMap<>();

    /**
     * 编码字符串<br>
     * 使用系统默认编码
     *
     * @param str 字符串
     * @return 编码后的字节码
     */
    public static byte[] bytes(CharSequence str) {
        return bytes(str, Charset.defaultCharset());
    }

    /**
     * 编码字符串
     *
     * @param str 字符串
     * @param charset 字符集，如果此字段为空，则解码的结果取决于平台
     * @return 编码后的字节码
     */
    public static byte[] bytes(CharSequence str, String charset) {
        return bytes(str, charsetOf(charset));
    }

    /**
     * 根据字符集名称获取字符集对象，结果带缓存<br>
     * 字符集名称为空时返回系统默认字符集
     *
     * @param charset 字符集名称
     * @return 字符集对象
     */
    private static Charset charsetOf(String charset) {
        if (isBlank(charset)) {
            return Charset.defaultCharset();
        }
        Charset cs = CHARSET_CACHE.get(charset);
        if (null == cs) {
            cs = Charset.forName(charset);
            CHARSET_CACHE.put(charset, cs);
        }
        return cs;
    }

    /**
     * 编码字符串
     *
     * @param str 字符串
     * @param charset 字符集，如果此字段为空，则解码的结果取决于平台
     * @return 编码后的字节码
     */
    public static byte[] bytes(CharSequence str, Charset charset) {
        if (str == null) {
            return null;
        }

        if (null == charset) {
            return str.toString().getBytes();
        }
        return str.toString().getBytes(charset);
    }

    /**
     * 将对象转为字符串
     *
     * <pre>
     * 1、Byte数组和ByteBuffer会被转换为对应字符串的数组
     * 2、对象数组会调用Arrays.toString方法
     * </pre>
     *
     * @param obj 对象
     * @param charsetName 字符集，如果为null或空白，则回退使用系统默认字符集
     * @return 字符串
     */
    public static String str(Object obj, String charsetName) {
        return str(
                obj,
                isBlank(charsetName) ? Charset.defaultCharset() : Charset.forName(charsetName));
    }

    /**
     * 将对象转为字符串
     *
     * <pre>
     * 	 1、Byte数组和ByteBuffer会被转换为对应字符串的数组
     * 	 2、对象数组会调用Arrays.toString方法
     * </pre>
     *
     * @param obj 对象
     * @param charset 字符集
     * @return 字符串
     */
    public static String str(Object obj, Charset charset) {
        if (null == obj) {
            return null;
        }

        if (obj instanceof String) {
            return (String) obj;
        } else if (obj instanceof byte[]) {
            return str((byte[]) obj, charset);
        } else if (obj instanceof Byte[]) {
            return str((Byte[]) obj, charset);
        } else if (obj instanceof ByteBuffer) {
            return str((ByteBuffer) obj, charset);
        } else if (GutilArray.isArray(obj)) {
            return GutilArray.toString(obj);
        }

        return obj.toString();
    }

    /**
     * 将byte数组转为字符串
     *
     * @param bytes byte数组
     * @param charset 字符集
     * @return 字符串
     */
    public static String str(byte[] bytes, String charset) {
        return str(bytes, isBlank(charset) ? Charset.defaultCharset() : Charset.forName(charset));
    }

    /**
     * 解码字节码
     *
     * @param data 字符串
     * @param charset 字符集，如果此字段为空，则解码的结果取决于平台
     * @return 解码后的字符串
     */
    public static String str(byte[] data, Charset charset) {
        if (data == null) {
            return null;
        }

        if (null == charset) {
            return new String(data);
        }
        return new String(data, charset);
    }

    /**
     * 将Byte数组转为字符串<br>
     * 注意：数组中的{@code null}元素按字节值{@code 0}处理（原实现编码为0xFF会破坏解码结果）
     *
     * @param bytes byte数组
     * @param charset 字符集
     * @return 字符串
     */
    public static String str(Byte[] bytes, String charset) {
        return str(bytes, isBlank(charset) ? Charset.defaultCharset() : Charset.forName(charset));
    }

    /**
     * 解码字节码<br>
     * 注意：数组中的{@code null}元素按字节值{@code 0}处理
     *
     * @param data 字符串
     * @param charset 字符集，如果此字段为空，则解码的结果取决于平台
     * @return 解码后的字符串
     */
    public static String str(Byte[] data, Charset charset) {
        if (data == null) {
            return null;
        }

        byte[] bytes = new byte[data.length];
        Byte dataByte;
        for (int i = 0; i < data.length; i++) {
            dataByte = data[i];
            bytes[i] = (null == dataByte) ? 0 : dataByte;
        }

        return str(bytes, charset);
    }

    /**
     * 将编码的byteBuffer数据转换为字符串
     *
     * @param data 数据
     * @param charset 字符集，如果为null或空白使用当前系统字符集
     * @return 字符串
     */
    public static String str(ByteBuffer data, String charset) {
        if (data == null) {
            return null;
        }

        return str(data, isBlank(charset) ? Charset.defaultCharset() : Charset.forName(charset));
    }

    /**
     * 将编码的byteBuffer数据转换为字符串<br>
     * 注意：该方法会消费（读取）传入的{@link ByteBuffer}，调用后其position将移动到末尾
     *
     * @param data 数据，如果为{@code null}，返回{@code null}
     * @param charset 字符集，如果为空使用当前系统字符集
     * @return 字符串
     */
    public static String str(ByteBuffer data, Charset charset) {
        if (null == data) {
            return null;
        }
        if (null == charset) {
            charset = Charset.defaultCharset();
        }
        return charset.decode(data).toString();
    }

    /**
     * {@link CharSequence} 转为字符串，null安全
     *
     * @param cs {@link CharSequence}
     * @return 字符串
     */
    public static String str(CharSequence cs) {
        return null == cs ? null : cs.toString();
    }

    /**
     * 调用对象的toString方法，null会返回字符串 {@code "null"}<br>
     * 注意：本方法与 {@link #str(CharSequence)} 及 {@link #str(Object, Charset)} 的差异在于： 本方法对 {@code null}
     * 入参返回字符串 {@code "null"}，而 str 系列方法对 {@code null} 返回 {@code null}
     *
     * @param obj 对象
     * @return 字符串，对象为 {@code null} 时返回 {@code "null"}
     * @since 4.1.3
     */
    public static String toString(Object obj) {
        return null == obj ? NULL : obj.toString();
    }

    /**
     * 字符串转换为byteBuffer
     *
     * @param str 字符串
     * @param charset 编码
     * @return byteBuffer
     */
    public static ByteBuffer byteBuffer(CharSequence str, String charset) {
        return ByteBuffer.wrap(bytes(str, charset));
    }

    /**
     * 以 conjunction 为分隔符将多个对象转换为字符串
     *
     * @param conjunction 分隔符
     * @param objs 数组
     * @return 连接后的字符串
     * @see GutilArray#join(Object, CharSequence)
     */
    public static String join(CharSequence conjunction, Object... objs) {
        return GutilArray.join(objs, conjunction);
    }

    /**
     * 以 conjunction 为分隔符将多个对象转换为字符串
     *
     * @param list 列表
     * @param conjunction 分隔符
     * @return 连接后的字符串
     * @see GutilArray#join(Object, CharSequence)
     */
    public static String join(List<?> list, CharSequence conjunction) {
        return GutilArray.join(list.toArray(), conjunction);
    }

    /**
     * 将下划线方式命名的字符串转换为驼峰式。如果转换前的下划线大写方式命名的字符串为空，则返回空字符串。<br>
     * 例如：hello_world=》helloWorld
     *
     * @param name 转换前的下划线大写方式命名的字符串
     * @return 转换后的驼峰式命名的字符串
     */
    public static String toCamelCase(CharSequence name) {
        if (null == name) {
            return null;
        }

        String name2 = name.toString();
        if (name2.contains(UNDERLINE)) {
            final StringBuilder sb = new StringBuilder(name2.length());
            boolean upperCase = false;
            for (int i = 0; i < name2.length(); i++) {
                char c = name2.charAt(i);

                if (c == GutilChar.UNDERLINE) {
                    upperCase = true;
                } else if (upperCase) {
                    sb.append(Character.toUpperCase(c));
                    upperCase = false;
                } else {
                    sb.append(Character.toLowerCase(c));
                }
            }
            return sb.toString();
        } else {
            return name2;
        }
    }

    /**
     * 包装指定字符串<br>
     * 当前缀和后缀一致时使用此方法
     *
     * @param str 被包装的字符串
     * @param prefixAndSuffix 前缀和后缀
     * @return 包装后的字符串
     * @since 3.1.0
     */
    public static String wrap(CharSequence str, CharSequence prefixAndSuffix) {
        return wrap(str, prefixAndSuffix, prefixAndSuffix);
    }

    /**
     * 包装指定字符串
     *
     * @param str 被包装的字符串
     * @param prefix 前缀
     * @param suffix 后缀
     * @return 包装后的字符串
     */
    public static String wrap(CharSequence str, CharSequence prefix, CharSequence suffix) {
        return nullToEmpty(prefix).concat(nullToEmpty(str)).concat(nullToEmpty(suffix));
    }

    /**
     * 使用单个字符包装多个字符串
     *
     * @param prefixAndSuffix 前缀和后缀
     * @param strs 多个字符串
     * @return 包装的字符串数组
     * @since 5.4.1
     */
    public static String[] wrapAllWithPair(CharSequence prefixAndSuffix, CharSequence... strs) {
        return wrapAll(prefixAndSuffix, prefixAndSuffix, strs);
    }

    /**
     * 包装多个字符串
     *
     * @param prefix 前缀
     * @param suffix 后缀
     * @param strs 多个字符串
     * @return 包装的字符串数组
     * @since 4.0.7
     */
    public static String[] wrapAll(CharSequence prefix, CharSequence suffix, CharSequence... strs) {
        final String[] results = new String[strs.length];
        for (int i = 0; i < strs.length; i++) {
            results[i] = wrap(strs[i], prefix, suffix);
        }
        return results;
    }

    /**
     * 包装指定字符串，如果前缀或后缀已经包含对应的字符串，则不再包装
     *
     * @param str 被包装的字符串
     * @param prefix 前缀
     * @param suffix 后缀
     * @return 包装后的字符串
     */
    public static String wrapIfMissing(CharSequence str, CharSequence prefix, CharSequence suffix) {
        int len = 0;
        if (isNotEmpty(str)) {
            len += str.length();
        }
        if (isNotEmpty(prefix)) {
            len += prefix.length();
        }
        if (isNotEmpty(suffix)) {
            len += suffix.length();
        }
        StringBuilder sb = new StringBuilder(len);
        if (isNotEmpty(prefix) && false == startWith(str, prefix)) {
            sb.append(prefix);
        }
        if (isNotEmpty(str)) {
            sb.append(str);
        }
        if (isNotEmpty(suffix) && false == endWith(str, suffix)) {
            sb.append(suffix);
        }
        return sb.toString();
    }

    /**
     * 使用成对的字符包装多个字符串，如果已经包装，则不再包装
     *
     * @param prefixAndSuffix 前缀和后缀
     * @param strs 多个字符串
     * @return 包装的字符串数组
     * @since 5.4.1
     */
    public static String[] wrapAllWithPairIfMissing(
            CharSequence prefixAndSuffix, CharSequence... strs) {
        return wrapAllIfMissing(prefixAndSuffix, prefixAndSuffix, strs);
    }

    /**
     * 包装多个字符串，如果已经包装，则不再包装
     *
     * @param prefix 前缀
     * @param suffix 后缀
     * @param strs 多个字符串
     * @return 包装的字符串数组
     * @since 4.0.7
     */
    public static String[] wrapAllIfMissing(
            CharSequence prefix, CharSequence suffix, CharSequence... strs) {
        final String[] results = new String[strs.length];
        for (int i = 0; i < strs.length; i++) {
            results[i] = wrapIfMissing(strs[i], prefix, suffix);
        }
        return results;
    }

    /**
     * 去掉字符包装，如果未被包装则返回原字符串
     *
     * @param str 字符串
     * @param prefix 前置字符串
     * @param suffix 后置字符串
     * @return 去掉包装字符的字符串
     * @since 4.0.1
     */
    public static String unWrap(CharSequence str, String prefix, String suffix) {
        if (isWrap(str, prefix, suffix)) {
            final int end = str.length() - suffix.length();
            if (end < prefix.length()) {
                // 前缀与后缀重叠，字符串整体均属于包装字符，返回空串
                return EMPTY;
            }
            return sub(str, prefix.length(), end);
        }
        return str.toString();
    }

    /**
     * 去掉字符包装，如果未被包装则返回原字符串
     *
     * @param str 字符串
     * @param prefix 前置字符
     * @param suffix 后置字符
     * @return 去掉包装字符的字符串
     * @since 4.0.1
     */
    public static String unWrap(CharSequence str, char prefix, char suffix) {
        if (isEmpty(str)) {
            return str(str);
        }
        if (str.charAt(0) == prefix && str.charAt(str.length() - 1) == suffix) {
            return sub(str, 1, str.length() - 1);
        }
        return str.toString();
    }

    /**
     * 去掉字符包装，如果未被包装则返回原字符串
     *
     * @param str 字符串
     * @param prefixAndSuffix 前置和后置字符
     * @return 去掉包装字符的字符串
     * @since 4.0.1
     */
    public static String unWrap(CharSequence str, char prefixAndSuffix) {
        return unWrap(str, prefixAndSuffix, prefixAndSuffix);
    }

    /**
     * 指定字符串是否被包装
     *
     * @param str 字符串
     * @param prefix 前缀
     * @param suffix 后缀
     * @return 是否被包装
     */
    public static boolean isWrap(CharSequence str, String prefix, String suffix) {
        if (GutilArray.hasNull(str, prefix, suffix)) {
            return false;
        }
        final String str2 = str.toString();
        return str2.startsWith(prefix) && str2.endsWith(suffix);
    }

    /**
     * 指定字符串是否被同一字符包装（前后都有这些字符串）
     *
     * @param str 字符串
     * @param wrapper 包装字符串
     * @return 是否被包装
     */
    public static boolean isWrap(CharSequence str, String wrapper) {
        return isWrap(str, wrapper, wrapper);
    }

    /**
     * 指定字符串是否被同一字符包装（前后都有这些字符串）
     *
     * @param str 字符串
     * @param wrapper 包装字符
     * @return 是否被包装
     */
    public static boolean isWrap(CharSequence str, char wrapper) {
        return isWrap(str, wrapper, wrapper);
    }

    /**
     * 指定字符串是否被包装
     *
     * @param str 字符串
     * @param prefixChar 前缀
     * @param suffixChar 后缀
     * @return 是否被包装
     */
    public static boolean isWrap(CharSequence str, char prefixChar, char suffixChar) {
        if (null == str || 0 == str.length()) {
            return false;
        }

        return str.charAt(0) == prefixChar && str.charAt(str.length() - 1) == suffixChar;
    }

    /**
     * 补充字符串以满足最小长度
     *
     * <pre>
     *  gtcStrUtil.padPre(null, *, *);//null
     *  gtcStrUtil.padPre("1", 3, "ABC");//"AB1"
     *  gtcStrUtil.padPre("123", 2, "ABC");//"12"
     * </pre>
     *
     * @param str 字符串
     * @param minLength 最小长度
     * @param padStr 补充的字符
     * @return 补充后的字符串
     */
    public static String padPre(CharSequence str, int minLength, CharSequence padStr) {
        if (null == str) {
            return null;
        }
        final int strLen = str.length();
        if (strLen == minLength) {
            return str.toString();
        } else if (strLen > minLength) {
            return subPre(str, minLength);
        }

        return repeatByLength(padStr, minLength - strLen).concat(str.toString());
    }

    /**
     * 补充字符串以满足最小长度
     *
     * <pre>
     *  gtcStrUtil.padPre(null, *, *);//null
     *  gtcStrUtil.padPre("1", 3, '0');//"001"
     *  gtcStrUtil.padPre("123", 2, '0');//"12"
     * </pre>
     *
     * @param str 字符串
     * @param minLength 最小长度
     * @param padChar 补充的字符
     * @return 补充后的字符串
     */
    public static String padPre(CharSequence str, int minLength, char padChar) {
        if (null == str) {
            return null;
        }
        final int strLen = str.length();
        if (strLen == minLength) {
            return str.toString();
        } else if (strLen > minLength) {
            return subPre(str, minLength);
        }

        return repeat(padChar, minLength - strLen).concat(str.toString());
    }

    /**
     * 补充字符串以满足最小长度
     *
     * <pre>
     *  gtcStrUtil.padAfter(null, *, *);//null
     *  gtcStrUtil.padAfter("1", 3, '0');//"100"
     *  gtcStrUtil.padAfter("123", 2, '0');//"23"
     * </pre>
     *
     * @param str 字符串，如果为<code>null</code>，直接返回null
     * @param minLength 最小长度
     * @param padChar 补充的字符
     * @return 补充后的字符串
     */
    public static String padAfter(CharSequence str, int minLength, char padChar) {
        if (null == str) {
            return null;
        }
        final int strLen = str.length();
        if (strLen == minLength) {
            return str.toString();
        } else if (strLen > minLength) {
            return sub(str, strLen - minLength, strLen);
        }

        return str.toString().concat(repeat(padChar, minLength - strLen));
    }

    /**
     * 补充字符串以满足最小长度
     *
     * <pre>
     *  gtcStrUtil.padAfter(null, *, *);//null
     *  gtcStrUtil.padAfter("1", 3, "ABC");//"1AB"
     *  gtcStrUtil.padAfter("123", 2, "ABC");//"23"
     * </pre>
     *
     * @param str 字符串，如果为<code>null</code>，直接返回null
     * @param minLength 最小长度
     * @param padStr 补充的字符
     * @return 补充后的字符串
     * @since 4.3.2
     */
    public static String padAfter(CharSequence str, int minLength, CharSequence padStr) {
        if (null == str) {
            return null;
        }
        final int strLen = str.length();
        if (strLen == minLength) {
            return str.toString();
        } else if (strLen > minLength) {
            return subSufByLength(str, minLength);
        }

        return str.toString().concat(repeatByLength(padStr, minLength - strLen));
    }

    /**
     * 居中字符串，两边补充指定字符串，如果指定长度小于字符串，则返回原字符串
     *
     * <pre>
     *  gtcStrUtil.center(null, *)   = null
     *  gtcStrUtil.center("", 4)     = "    "
     *  gtcStrUtil.center("ab", -1)  = "ab"
     *  gtcStrUtil.center("ab", 4)   = " ab "
     *  gtcStrUtil.center("abcd", 2) = "abcd"
     *  gtcStrUtil.center("a", 4)    = " a  "
     * </pre>
     *
     * @param str 字符串
     * @param size 指定长度
     * @return 补充后的字符串
     * @since 4.3.2
     */
    public static String center(CharSequence str, final int size) {
        return center(str, size, GutilChar.SPACE);
    }

    /**
     * 居中字符串，两边补充指定字符串，如果指定长度小于字符串，则返回原字符串
     *
     * <pre>
     *  gtcStrUtil.center(null, *, *)     = null
     *  gtcStrUtil.center("", 4, ' ')     = "    "
     *  gtcStrUtil.center("ab", -1, ' ')  = "ab"
     *  gtcStrUtil.center("ab", 4, ' ')   = " ab "
     *  gtcStrUtil.center("abcd", 2, ' ') = "abcd"
     *  gtcStrUtil.center("a", 4, ' ')    = " a  "
     *  gtcStrUtil.center("a", 4, 'y')   = "yayy"
     *  gtcStrUtil.center("abc", 7, ' ')   = "  abc  "
     * </pre>
     *
     * @param str 字符串
     * @param size 指定长度
     * @param padChar 两边补充的字符
     * @return 补充后的字符串
     * @since 4.3.2
     */
    public static String center(CharSequence str, final int size, char padChar) {
        if (str == null || size <= 0) {
            return str(str);
        }
        final int strLen = str.length();
        final int pads = size - strLen;
        if (pads <= 0) {
            return str.toString();
        }
        str = padPre(str, strLen + pads / 2, padChar);
        str = padAfter(str, size, padChar);
        return str.toString();
    }

    /**
     * 居中字符串，两边补充指定字符串，如果指定长度小于字符串，则返回原字符串
     *
     * <pre>
     *  gtcStrUtil.center(null, *, *)     = null
     *  gtcStrUtil.center("", 4, " ")     = "    "
     *  gtcStrUtil.center("ab", -1, " ")  = "ab"
     *  gtcStrUtil.center("ab", 4, " ")   = " ab "
     *  gtcStrUtil.center("abcd", 2, " ") = "abcd"
     *  gtcStrUtil.center("a", 4, " ")    = " a  "
     *  gtcStrUtil.center("a", 4, "yz")   = "yayz"
     *  gtcStrUtil.center("abc", 7, null) = "  abc  "
     *  gtcStrUtil.center("abc", 7, "")   = "  abc  "
     * </pre>
     *
     * @param str 字符串
     * @param size 指定长度
     * @param padStr 两边补充的字符串
     * @return 补充后的字符串
     */
    public static String center(CharSequence str, final int size, CharSequence padStr) {
        if (str == null || size <= 0) {
            return str(str);
        }
        if (isEmpty(padStr)) {
            padStr = SPACE;
        }
        final int strLen = str.length();
        final int pads = size - strLen;
        if (pads <= 0) {
            return str.toString();
        }
        str = padPre(str, strLen + pads / 2, padStr);
        str = padAfter(str, size, padStr);
        return str.toString();
    }

    /**
     * 创建StringBuilder对象
     *
     * @return StringBuilder对象
     */
    public static StringBuilder builder() {
        return new StringBuilder();
    }

    /**
     * 创建StringBuilder对象
     *
     * @param capacity 初始大小
     * @return StringBuilder对象
     */
    public static StringBuilder builder(int capacity) {
        return new StringBuilder(capacity);
    }

    /**
     * 创建StringBuilder对象
     *
     * @param strs 初始字符串列表
     * @return StringBuilder对象
     */
    public static StringBuilder builder(CharSequence... strs) {
        final StringBuilder sb = new StringBuilder();
        for (CharSequence str : strs) {
            sb.append(str);
        }
        return sb;
    }

    /**
     * 获得StringReader
     *
     * @param str 字符串
     * @return StringReader
     */
    public static StringReader getReader(CharSequence str) {
        if (null == str) {
            return null;
        }
        return new StringReader(str.toString());
    }

    /**
     * 获得StringWriter
     *
     * @return StringWriter
     */
    public static StringWriter getWriter() {
        return new StringWriter();
    }

    /**
     * 统计指定内容中包含指定字符串的数量<br>
     * 参数为 {@code null} 或者 "" 返回 {@code 0}.
     *
     * <pre>
     *  gtcStrUtil.count(null, *)       = 0
     *  gtcStrUtil.count("", *)         = 0
     *  gtcStrUtil.count("abba", null)  = 0
     *  gtcStrUtil.count("abba", "")    = 0
     *  gtcStrUtil.count("abba", "a")   = 2
     *  gtcStrUtil.count("abba", "ab")  = 1
     *  gtcStrUtil.count("abba", "xxx") = 0
     * </pre>
     *
     * @param content 被查找的字符串
     * @param strForSearch 需要查找的字符串
     * @return 查找到的个数
     */
    public static int count(CharSequence content, CharSequence strForSearch) {
        if (hasEmpty(content, strForSearch) || strForSearch.length() > content.length()) {
            return 0;
        }

        int count = 0;
        int idx = 0;
        final String content2 = content.toString();
        final String strForSearch2 = strForSearch.toString();
        while ((idx = content2.indexOf(strForSearch2, idx)) > -1) {
            count++;
            idx += strForSearch.length();
        }
        return count;
    }

    /**
     * 统计指定内容中包含指定字符的数量
     *
     * @param content 内容
     * @param charForSearch 被统计的字符
     * @return 包含数量
     */
    public static int count(CharSequence content, char charForSearch) {
        int count = 0;
        if (isEmpty(content)) {
            return 0;
        }
        int contentLength = content.length();
        for (int i = 0; i < contentLength; i++) {
            if (charForSearch == content.charAt(i)) {
                count++;
            }
        }
        return count;
    }

    /**
     * 将给定字符串，变成 "xxx...xxx" 形式的字符串<br>
     * 最大长度小于等于 0 时返回空串；最大长度小于等于 3（无法容纳省略号）时直接截取头部
     *
     * @param str 字符串
     * @param maxLength 最大长度
     * @return 截取后的字符串
     */
    public static String brief(CharSequence str, int maxLength) {
        if (null == str) {
            return null;
        }
        if (maxLength <= 0) {
            return EMPTY;
        }
        if (maxLength <= 3) {
            // 长度过小，无法容纳省略号，直接截取头部
            return str.toString().substring(0, Math.min(str.length(), maxLength));
        }
        if (str.length() <= maxLength) {
            return str.toString();
        }
        int w = maxLength / 2;
        int l = str.length() + 3;

        final String str2 = str.toString();
        return format("{}...{}", str2.substring(0, maxLength - w), str2.substring(l - w));
    }

    /**
     * 比较两个字符串，用于排序
     *
     * <pre>
     *  gtcStrUtil.compare(null, null, *)     = 0
     *  gtcStrUtil.compare(null , "a", true)  &lt; 0
     *  gtcStrUtil.compare(null , "a", false) &gt; 0
     *  gtcStrUtil.compare("a", null, true)   &gt; 0
     *  gtcStrUtil.compare("a", null, false)  &lt; 0
     *  gtcStrUtil.compare("abc", "abc", *)   = 0
     *  gtcStrUtil.compare("a", "b", *)       &lt; 0
     *  gtcStrUtil.compare("b", "a", *)       &gt; 0
     *  gtcStrUtil.compare("a", "B", *)       &gt; 0
     *  gtcStrUtil.compare("ab", "abc", *)    &lt; 0
     * </pre>
     *
     * @param str1 字符串1
     * @param str2 字符串2
     * @param nullIsLess {@code null} 值是否排在前（null是否小于非空值）
     * @return 排序值。负数：str1 &lt; str2，正数：str1 &gt; str2, 0：str1 == str2
     */
    public static int compare(
            final CharSequence str1, final CharSequence str2, final boolean nullIsLess) {
        if (str1 == str2) {
            return 0;
        }
        if (str1 == null) {
            return nullIsLess ? -1 : 1;
        }
        if (str2 == null) {
            return nullIsLess ? 1 : -1;
        }
        return str1.toString().compareTo(str2.toString());
    }

    /**
     * 比较两个字符串，用于排序，大小写不敏感
     *
     * <pre>
     *  gtcStrUtil.compareIgnoreCase(null, null, *)     = 0
     *  gtcStrUtil.compareIgnoreCase(null , "a", true)  &lt; 0
     *  gtcStrUtil.compareIgnoreCase(null , "a", false) &gt; 0
     *  gtcStrUtil.compareIgnoreCase("a", null, true)   &gt; 0
     *  gtcStrUtil.compareIgnoreCase("a", null, false)  &lt; 0
     *  gtcStrUtil.compareIgnoreCase("abc", "abc", *)   = 0
     *  gtcStrUtil.compareIgnoreCase("abc", "ABC", *)   = 0
     *  gtcStrUtil.compareIgnoreCase("a", "b", *)       &lt; 0
     *  gtcStrUtil.compareIgnoreCase("b", "a", *)       &gt; 0
     *  gtcStrUtil.compareIgnoreCase("a", "B", *)       &lt; 0
     *  gtcStrUtil.compareIgnoreCase("A", "b", *)       &lt; 0
     *  gtcStrUtil.compareIgnoreCase("ab", "abc", *)    &lt; 0
     * </pre>
     *
     * @param str1 字符串1
     * @param str2 字符串2
     * @param nullIsLess {@code null} 值是否排在前（null是否小于非空值）
     * @return 排序值。负数：str1 &lt; str2，正数：str1 &gt; str2, 0：str1 == str2
     */
    public static int compareIgnoreCase(CharSequence str1, CharSequence str2, boolean nullIsLess) {
        if (str1 == str2) {
            return 0;
        }
        if (str1 == null) {
            return nullIsLess ? -1 : 1;
        }
        if (str2 == null) {
            return nullIsLess ? 1 : -1;
        }
        return str1.toString().compareToIgnoreCase(str2.toString());
    }

    /**
     * 指定范围内查找指定字符
     *
     * @param str 字符串
     * @param searchChar 被查找的字符
     * @return 位置
     */
    public static int indexOf(final CharSequence str, char searchChar) {
        return indexOf(str, searchChar, 0);
    }

    /**
     * 指定范围内查找指定字符
     *
     * @param str 字符串
     * @param searchChar 被查找的字符
     * @param start 起始位置，如果小于0，从0开始查找
     * @return 位置
     */
    public static int indexOf(CharSequence str, char searchChar, int start) {
        if (str instanceof String) {
            return ((String) str).indexOf(searchChar, start);
        } else {
            return indexOf(str, searchChar, start, -1);
        }
    }

    /**
     * 指定范围内查找指定字符
     *
     * @param str 字符串
     * @param searchChar 被查找的字符
     * @param start 起始位置，如果小于0，从0开始查找
     * @param end 终止位置，如果超过str.length()则默认查找到字符串末尾
     * @return 位置
     */
    public static int indexOf(final CharSequence str, char searchChar, int start, int end) {
        if (isEmpty(str)) {
            return INDEX_NOT_FOUND;
        }
        final int len = str.length();
        if (start < 0 || start > len) {
            start = 0;
        }
        if (end > len || end < 0) {
            end = len;
        }
        for (int i = start; i < end; i++) {
            if (str.charAt(i) == searchChar) {
                return i;
            }
        }
        return INDEX_NOT_FOUND;
    }

    /**
     * 指定范围内查找字符串，忽略大小写<br>
     *
     * <pre>
     *  gtcStrUtil.indexOfIgnoreCase(null, *, *)          = -1
     *  gtcStrUtil.indexOfIgnoreCase(*, null, *)          = -1
     *  gtcStrUtil.indexOfIgnoreCase("", "", 0)           = 0
     *  gtcStrUtil.indexOfIgnoreCase("aabaabaa", "A", 0)  = 0
     *  gtcStrUtil.indexOfIgnoreCase("aabaabaa", "B", 0)  = 2
     *  gtcStrUtil.indexOfIgnoreCase("aabaabaa", "AB", 0) = 1
     *  gtcStrUtil.indexOfIgnoreCase("aabaabaa", "B", 3)  = 5
     *  gtcStrUtil.indexOfIgnoreCase("aabaabaa", "B", 9)  = -1
     *  gtcStrUtil.indexOfIgnoreCase("aabaabaa", "B", -1) = 2
     *  gtcStrUtil.indexOfIgnoreCase("aabaabaa", "", 2)   = 2
     *  gtcStrUtil.indexOfIgnoreCase("abc", "", 9)        = -1
     * </pre>
     *
     * @param str 字符串
     * @param searchStr 需要查找位置的字符串
     * @return 位置
     * @since 3.2.1
     */
    public static int indexOfIgnoreCase(final CharSequence str, final CharSequence searchStr) {
        return indexOfIgnoreCase(str, searchStr, 0);
    }

    /**
     * 指定范围内查找字符串
     *
     * <pre>
     *  gtcStrUtil.indexOfIgnoreCase(null, *, *)          = -1
     *  gtcStrUtil.indexOfIgnoreCase(*, null, *)          = -1
     *  gtcStrUtil.indexOfIgnoreCase("", "", 0)           = 0
     *  gtcStrUtil.indexOfIgnoreCase("aabaabaa", "A", 0)  = 0
     *  gtcStrUtil.indexOfIgnoreCase("aabaabaa", "B", 0)  = 2
     *  gtcStrUtil.indexOfIgnoreCase("aabaabaa", "AB", 0) = 1
     *  gtcStrUtil.indexOfIgnoreCase("aabaabaa", "B", 3)  = 5
     *  gtcStrUtil.indexOfIgnoreCase("aabaabaa", "B", 9)  = -1
     *  gtcStrUtil.indexOfIgnoreCase("aabaabaa", "B", -1) = 2
     *  gtcStrUtil.indexOfIgnoreCase("aabaabaa", "", 2)   = 2
     *  gtcStrUtil.indexOfIgnoreCase("abc", "", 9)        = -1
     * </pre>
     *
     * @param str 字符串
     * @param searchStr 需要查找位置的字符串
     * @param fromIndex 起始位置
     * @return 位置
     * @since 3.2.1
     */
    public static int indexOfIgnoreCase(
            final CharSequence str, final CharSequence searchStr, int fromIndex) {
        return indexOf(str, searchStr, fromIndex, true);
    }

    /**
     * 指定范围内查找字符串
     *
     * @param str 字符串
     * @param searchStr 需要查找位置的字符串
     * @param fromIndex 起始位置
     * @param ignoreCase 是否忽略大小写
     * @return 位置
     * @since 3.2.1
     */
    public static int indexOf(
            final CharSequence str, CharSequence searchStr, int fromIndex, boolean ignoreCase) {
        if (str == null || searchStr == null) {
            return INDEX_NOT_FOUND;
        }
        if (fromIndex < 0) {
            fromIndex = 0;
        }

        final int endLimit = str.length() - searchStr.length() + 1;
        if (fromIndex > endLimit) {
            return INDEX_NOT_FOUND;
        }
        if (searchStr.length() == 0) {
            return fromIndex;
        }

        if (false == ignoreCase) {
            // 不忽略大小写调用JDK方法
            return str.toString().indexOf(searchStr.toString(), fromIndex);
        }

        for (int i = fromIndex; i < endLimit; i++) {
            if (isSubEquals(str, i, searchStr, 0, searchStr.length(), true)) {
                return i;
            }
        }
        return INDEX_NOT_FOUND;
    }

    /**
     * 指定范围内查找字符串，忽略大小写<br>
     * 被查找字符串为 {@code null} 时返回 -1
     *
     * @param str 字符串
     * @param searchStr 需要查找位置的字符串
     * @return 位置
     * @since 3.2.1
     */
    public static int lastIndexOfIgnoreCase(final CharSequence str, final CharSequence searchStr) {
        return lastIndexOfIgnoreCase(str, searchStr, null == str ? 0 : str.length());
    }

    /**
     * 指定范围内查找字符串，忽略大小写<br>
     * fromIndex 为搜索起始位置，从后往前计数
     *
     * @param str 字符串
     * @param searchStr 需要查找位置的字符串
     * @param fromIndex 起始位置，从后往前计数
     * @return 位置
     * @since 3.2.1
     */
    public static int lastIndexOfIgnoreCase(
            final CharSequence str, final CharSequence searchStr, int fromIndex) {
        return lastIndexOf(str, searchStr, fromIndex, true);
    }

    /**
     * 指定范围内查找字符串<br>
     * fromIndex 为搜索起始位置，从后往前计数
     *
     * @param str 字符串
     * @param searchStr 需要查找位置的字符串
     * @param fromIndex 起始位置，从后往前计数
     * @param ignoreCase 是否忽略大小写
     * @return 位置
     * @since 3.2.1
     */
    public static int lastIndexOf(
            final CharSequence str,
            final CharSequence searchStr,
            int fromIndex,
            boolean ignoreCase) {
        if (str == null || searchStr == null) {
            return INDEX_NOT_FOUND;
        }
        if (fromIndex < 0) {
            fromIndex = 0;
        }
        fromIndex = Math.min(fromIndex, str.length());

        if (searchStr.length() == 0) {
            return fromIndex;
        }

        if (false == ignoreCase) {
            // 不忽略大小写调用JDK方法
            return str.toString().lastIndexOf(searchStr.toString(), fromIndex);
        }

        for (int i = fromIndex; i >= 0; i--) {
            if (isSubEquals(str, i, searchStr, 0, searchStr.length(), true)) {
                return i;
            }
        }
        return INDEX_NOT_FOUND;
    }

    /**
     * 返回字符串 searchStr 在字符串 str 中第 ordinal 次出现的位置。
     *
     * <p>如果 str=null 或 searchStr=null 或 ordinal&ge;0 则返回-1<br>
     * 此方法来自：Apache-Commons-Lang
     *
     * <p>例子（*代表任意字符）：
     *
     * <pre>
     *  gtcStrUtil.ordinalIndexOf(null, *, *)          = -1
     *  gtcStrUtil.ordinalIndexOf(*, null, *)          = -1
     *  gtcStrUtil.ordinalIndexOf("", "", *)           = 0
     *  gtcStrUtil.ordinalIndexOf("aabaabaa", "a", 1)  = 0
     *  gtcStrUtil.ordinalIndexOf("aabaabaa", "a", 2)  = 1
     *  gtcStrUtil.ordinalIndexOf("aabaabaa", "b", 1)  = 2
     *  gtcStrUtil.ordinalIndexOf("aabaabaa", "b", 2)  = 5
     *  gtcStrUtil.ordinalIndexOf("aabaabaa", "ab", 1) = 1
     *  gtcStrUtil.ordinalIndexOf("aabaabaa", "ab", 2) = 4
     *  gtcStrUtil.ordinalIndexOf("aabaabaa", "", 1)   = 0
     *  gtcStrUtil.ordinalIndexOf("aabaabaa", "", 2)   = 0
     * </pre>
     *
     * @param str 被检查的字符串，可以为null
     * @param searchStr 被查找的字符串，可以为null
     * @param ordinal 第几次出现的位置
     * @return 查找到的位置
     * @since 3.2.3
     */
    public static int ordinalIndexOf(String str, String searchStr, int ordinal) {
        if (str == null || searchStr == null || ordinal <= 0) {
            return INDEX_NOT_FOUND;
        }
        if (searchStr.length() == 0) {
            return 0;
        }
        int found = 0;
        int index = INDEX_NOT_FOUND;
        do {
            index = str.indexOf(searchStr, index + 1);
            if (index < 0) {
                return index;
            }
            found++;
        } while (found < ordinal);
        return index;
    }

    // ------------------------------------------------------------------------------------------------------------------
    // Append and prepend

    /**
     * 如果给定字符串不是以给定的一个或多个字符串为结尾，则在尾部添加结尾字符串<br>
     * 不忽略大小写
     *
     * @param str 被检查的字符串
     * @param suffix 需要添加到结尾的字符串
     * @param suffixes 需要额外检查的结尾字符串，如果以这些中的一个为结尾，则不再添加
     * @return 如果已经结尾，返回原字符串，否则返回添加结尾的字符串
     * @since 3.0.7
     */
    public static String appendIfMissing(
            final CharSequence str, final CharSequence suffix, final CharSequence... suffixes) {
        return appendIfMissing(str, suffix, false, suffixes);
    }

    /**
     * 如果给定字符串不是以给定的一个或多个字符串为结尾，则在尾部添加结尾字符串<br>
     * 忽略大小写
     *
     * @param str 被检查的字符串
     * @param suffix 需要添加到结尾的字符串
     * @param suffixes 需要额外检查的结尾字符串，如果以这些中的一个为结尾，则不再添加
     * @return 如果已经结尾，返回原字符串，否则返回添加结尾的字符串
     * @since 3.0.7
     */
    public static String appendIfMissingIgnoreCase(
            final CharSequence str, final CharSequence suffix, final CharSequence... suffixes) {
        return appendIfMissing(str, suffix, true, suffixes);
    }

    /**
     * 如果给定字符串不是以给定的一个或多个字符串为结尾，则在尾部添加结尾字符串
     *
     * @param str 被检查的字符串
     * @param suffix 需要添加到结尾的字符串
     * @param ignoreCase 检查结尾时是否忽略大小写
     * @param suffixes 需要额外检查的结尾字符串，如果以这些中的一个为结尾，则不再添加
     * @return 如果已经结尾，返回原字符串，否则返回添加结尾的字符串
     * @since 3.0.7
     */
    public static String appendIfMissing(
            final CharSequence str,
            final CharSequence suffix,
            final boolean ignoreCase,
            final CharSequence... suffixes) {
        if (str == null || isEmpty(suffix) || endWith(str, suffix, ignoreCase)) {
            return str(str);
        }
        if (suffixes != null && suffixes.length > 0) {
            for (final CharSequence s : suffixes) {
                if (endWith(str, s, ignoreCase)) {
                    return str.toString();
                }
            }
        }
        return str.toString().concat(suffix.toString());
    }

    /**
     * 如果给定字符串不是以给定的一个或多个字符串为开头，则在首部添加起始字符串<br>
     * 不忽略大小写
     *
     * @param str 被检查的字符串
     * @param prefix 需要添加到首部的字符串
     * @param prefixes 需要额外检查的首部字符串，如果以这些中的一个为起始，则不再添加
     * @return 如果已经结尾，返回原字符串，否则返回添加结尾的字符串
     * @since 3.0.7
     */
    public static String prependIfMissing(
            final CharSequence str, final CharSequence prefix, final CharSequence... prefixes) {
        return prependIfMissing(str, prefix, false, prefixes);
    }

    /**
     * 如果给定字符串不是以给定的一个或多个字符串为开头，则在首部添加起始字符串<br>
     * 忽略大小写
     *
     * @param str 被检查的字符串
     * @param prefix 需要添加到首部的字符串
     * @param prefixes 需要额外检查的首部字符串，如果以这些中的一个为起始，则不再添加
     * @return 如果已经结尾，返回原字符串，否则返回添加结尾的字符串
     * @since 3.0.7
     */
    public static String prependIfMissingIgnoreCase(
            final CharSequence str, final CharSequence prefix, final CharSequence... prefixes) {
        return prependIfMissing(str, prefix, true, prefixes);
    }

    /**
     * 如果给定字符串不是以给定的一个或多个字符串为开头，则在首部添加起始字符串
     *
     * @param str 被检查的字符串
     * @param prefix 需要添加到首部的字符串
     * @param ignoreCase 检查结尾时是否忽略大小写
     * @param prefixes 需要额外检查的首部字符串，如果以这些中的一个为起始，则不再添加
     * @return 如果已经结尾，返回原字符串，否则返回添加结尾的字符串
     * @since 3.0.7
     */
    public static String prependIfMissing(
            final CharSequence str,
            final CharSequence prefix,
            final boolean ignoreCase,
            final CharSequence... prefixes) {
        if (str == null || isEmpty(prefix) || startWith(str, prefix, ignoreCase)) {
            return str(str);
        }
        if (prefixes != null && prefixes.length > 0) {
            for (final CharSequence s : prefixes) {
                if (startWith(str, s, ignoreCase)) {
                    return str.toString();
                }
            }
        }
        return prefix.toString().concat(str.toString());
    }

    /**
     * 将已有字符串填充为规定长度，如果已有字符串超过这个长度则返回这个字符串<br>
     * 字符填充于字符串前
     *
     * @param str 被填充的字符串
     * @param filledChar 填充的字符
     * @param len 填充长度
     * @return 填充后的字符串
     * @since 3.1.2
     */
    public static String fillBefore(String str, char filledChar, int len) {
        return fill(str, filledChar, len, true);
    }

    /**
     * 将已有字符串填充为规定长度，如果已有字符串超过这个长度则返回这个字符串<br>
     * 字符填充于字符串后
     *
     * @param str 被填充的字符串
     * @param filledChar 填充的字符
     * @param len 填充长度
     * @return 填充后的字符串
     * @since 3.1.2
     */
    public static String fillAfter(String str, char filledChar, int len) {
        return fill(str, filledChar, len, false);
    }

    /**
     * 将已有字符串填充为规定长度，如果已有字符串超过这个长度则返回这个字符串
     *
     * @param str 被填充的字符串
     * @param filledChar 填充的字符
     * @param len 填充长度
     * @param isPre 是否填充在前
     * @return 填充后的字符串
     * @since 3.1.2
     */
    public static String fill(String str, char filledChar, int len, boolean isPre) {
        if (null == str) {
            return null;
        }
        final int strLen = str.length();
        if (strLen > len) {
            return str;
        }

        String filledStr = GutilStr.repeat(filledChar, len - strLen);
        return isPre ? filledStr.concat(str) : str.concat(filledStr);
    }

    /**
     * 截取两个字符串的不同部分（长度一致），判断截取的子串是否相同<br>
     * 任意一个字符串为null返回false
     *
     * @param str1 第一个字符串
     * @param start1 第一个字符串开始的位置
     * @param str2 第二个字符串
     * @param start2 第二个字符串开始的位置
     * @param length 截取长度
     * @param ignoreCase 是否忽略大小写
     * @return 子串是否相同
     * @since 3.2.1
     */
    public static boolean isSubEquals(
            CharSequence str1,
            int start1,
            CharSequence str2,
            int start2,
            int length,
            boolean ignoreCase) {
        if (null == str1 || null == str2) {
            return false;
        }

        return str1.toString().regionMatches(ignoreCase, start1, str2.toString(), start2, length);
    }

    /**
     * 替换指定字符串的指定区间内字符为固定字符
     *
     * @param str 字符串
     * @param startInclude 开始位置（包含）
     * @param endExclude 结束位置（不包含）
     * @param replacedChar 被替换的字符
     * @return 替换后的字符串
     * @since 3.2.1
     */
    public static String replace(
            CharSequence str, int startInclude, int endExclude, char replacedChar) {
        if (isEmpty(str)) {
            return str(str);
        }
        if (startInclude < 0) {
            throw new IllegalArgumentException("startInclude must be >= 0");
        }
        final int strLength = str.length();
        if (startInclude > strLength) {
            return str(str);
        }
        if (endExclude > strLength) {
            endExclude = strLength;
        }
        if (startInclude > endExclude) {
            // 如果起始位置大于结束位置，不替换
            return str(str);
        }

        final char[] chars = new char[strLength];
        for (int i = 0; i < strLength; i++) {
            if (i >= startInclude && i < endExclude) {
                chars[i] = replacedChar;
            } else {
                chars[i] = str.charAt(i);
            }
        }
        return new String(chars);
    }

    /**
     * 替换指定字符串的指定区间内字符为"*"
     *
     * @param str 字符串
     * @param startInclude 开始位置（包含）
     * @param endExclude 结束位置（不包含）
     * @return 替换后的字符串
     * @since 4.1.14
     */
    public static String hide(CharSequence str, int startInclude, int endExclude) {
        return replace(str, startInclude, endExclude, '*');
    }

    /**
     * 替换字符字符数组中所有的字符为replacedStr<br>
     * 提供的chars为所有需要被替换的字符，例如："\r\n"，则"\r"和"\n"都会被替换，哪怕他们单独存在
     *
     * @param str 被检查的字符串
     * @param chars 需要替换的字符列表，用一个字符串表示这个字符列表
     * @param replacedStr 替换成的字符串
     * @return 新字符串
     * @since 3.2.2
     */
    public static String replaceChars(CharSequence str, String chars, CharSequence replacedStr) {
        if (isEmpty(str) || isEmpty(chars)) {
            return str(str);
        }
        return replaceChars(str, chars.toCharArray(), replacedStr);
    }

    /**
     * 替换字符字符数组中所有的字符为replacedStr
     *
     * @param str 被检查的字符串
     * @param chars 需要替换的字符列表
     * @param replacedStr 替换成的字符串
     * @return 新字符串
     * @since 3.2.2
     */
    public static String replaceChars(CharSequence str, char[] chars, CharSequence replacedStr) {
        if (isEmpty(str) || GutilArray.isEmpty(chars)) {
            return str(str);
        }

        final Set<Character> set = new HashSet<>(chars.length);
        for (char c : chars) {
            set.add(c);
        }
        int strLen = str.length();
        final StringBuilder builder = builder();
        char c;
        for (int i = 0; i < strLen; i++) {
            c = str.charAt(i);
            builder.append(set.contains(c) ? replacedStr : c);
        }
        return builder.toString();
    }

    /**
     * 将对象转为字符串<br>
     *
     * <pre>
     * 1、Byte数组和ByteBuffer会被转换为对应字符串的数组
     * 2、对象数组会调用Arrays.toString方法
     * </pre>
     *
     * @param obj 对象
     * @return 字符串
     */
    public static String utf8Str(Object obj) {
        return str(obj, Charset.forName("UTF-8"));
    }

    /**
     * 字符串指定位置的字符是否与给定字符相同<br>
     * 如果字符串为null，返回false<br>
     * 如果给定的位置大于字符串长度，返回false<br>
     * 如果给定的位置小于0，返回false
     *
     * @param str 字符串
     * @param position 位置
     * @param c 需要对比的字符
     * @return 字符串指定位置的字符是否与给定字符相同
     * @since 3.3.1
     */
    public static boolean equalsCharAt(CharSequence str, int position, char c) {
        if (null == str || position < 0) {
            return false;
        }
        return str.length() > position && c == str.charAt(position);
    }

    /**
     * 给定字符串数组的总长度<br>
     * null字符长度定义为0
     *
     * @param strs 字符串数组
     * @return 总长度
     * @since 4.0.1
     */
    public static int totalLength(CharSequence... strs) {
        int totalLength = 0;
        for (CharSequence str : strs) {
            totalLength += (null == str ? 0 : str.length());
        }
        return totalLength;
    }

    /**
     * 给定字符串中的字母是否全部为大写，判断依据如下：
     *
     * <pre>
     * 1. 大写字母包括A-Z
     * 2. 其它非字母的Unicode符都算作大写
     * </pre>
     *
     * @param str 被检查的字符串
     * @return 是否全部为大写
     * @since 4.2.2
     */
    public static boolean isUpperCase(CharSequence str) {
        if (null == str) {
            return false;
        }
        final int len = str.length();
        for (int i = 0; i < len; i++) {
            if (Character.isLowerCase(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * 给定字符串中的字母是否全部为小写，判断依据如下：
     *
     * <pre>
     * 1. 小写字母包括a-z
     * 2. 其它非字母的Unicode符都算作小写
     * </pre>
     *
     * @param str 被检查的字符串
     * @return 是否全部为小写
     * @since 4.2.2
     */
    public static boolean isLowerCase(CharSequence str) {
        if (null == str) {
            return false;
        }
        final int len = str.length();
        for (int i = 0; i < len; i++) {
            if (Character.isUpperCase(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * 获取字符串的长度，如果为null返回0
     *
     * @param cs a 字符串
     * @return 字符串的长度，如果为null返回0
     * @since 4.3.2
     */
    public static int length(CharSequence cs) {
        return cs == null ? 0 : cs.length();
    }

    /**
     * 给定字符串转为bytes后的byte数（byte长度）
     *
     * @param cs 字符串
     * @param charset 编码
     * @return byte长度
     * @since 4.5.2
     */
    public static int byteLength(CharSequence cs, Charset charset) {
        if (null == charset) {
            // 字符集为空时回退为系统默认字符集
            charset = Charset.defaultCharset();
        }
        return cs == null ? 0 : cs.toString().getBytes(charset).length;
    }

    /**
     * 切换给定字符串中的大小写。大写转小写，小写转大写。
     *
     * <pre>
     *  gtcStrUtil.swapCase(null)                 = null
     *  gtcStrUtil.swapCase("")                   = ""
     *  gtcStrUtil.swapCase("The dog has a BONE") = "tHE DOG HAS A bone"
     * </pre>
     *
     * @param str 字符串
     * @return 交换后的字符串
     * @since 4.3.2
     */
    public static String swapCase(final String str) {
        if (isEmpty(str)) {
            return str;
        }

        final char[] buffer = str.toCharArray();

        for (int i = 0; i < buffer.length; i++) {
            final char ch = buffer[i];
            if (Character.isUpperCase(ch)) {
                buffer[i] = Character.toLowerCase(ch);
            } else if (Character.isTitleCase(ch)) {
                buffer[i] = Character.toLowerCase(ch);
            } else if (Character.isLowerCase(ch)) {
                buffer[i] = Character.toUpperCase(ch);
            }
        }
        return new String(buffer);
    }

    // Spring 风格字符串工具方法
    // --------------------------------------------------------------------------------------------------------------------------

    /** 文件夹分隔符（Unix 风格） */
    private static final String FOLDER_SEPARATOR = "/";

    /** 文件夹分隔符（Windows 风格） */
    private static final String WINDOWS_FOLDER_SEPARATOR = "\\";

    /** 上级路径标识 */
    private static final String TOP_PATH = "..";

    /** 当前路径标识 */
    private static final String CURRENT_PATH = ".";

    /** 文件扩展名分隔符 */
    private static final char EXTENSION_SEPARATOR = '.';

    // ---------------------------------------------------------------------
    // 自定义方法
    // ---------------------------------------------------------------------

    /** 整数校验正则：可选的符号位 + 至少一位数字 */
    private static final Pattern intPattern = Pattern.compile("^[+\\-]?\\d+$");

    /**
     * 判断给定字符串是否为整数（支持正负号）<br>
     * 字符串为 {@code null} 时返回 false
     *
     * @param str 被检测的字符串
     * @return 是否为整数
     */
    public static boolean isInteger(String str) {
        return null != str && intPattern.matcher(str).matches();
    }

    /**
     * 将列表中的元素以分隔符拼接为字符串，数字不加引号，其余元素按 isSqm 加单引号或双引号<br>
     * 元素为 {@code null} 时跳过；原始类型数组元素会先转为包装类型数组再拼接
     *
     * @param keys 列表
     * @param split 分隔符
     * @param isSqm 是否使用单引号，false 时使用双引号
     * @return 拼接后的字符串
     */
    public static String join(List<?> keys, String split, boolean isSqm) {
        return join(keys.toArray(), split, isSqm);
    }

    /**
     * 将数组中的元素以分隔符拼接为字符串，数字不加引号，其余元素按 isSqm 加单引号或双引号<br>
     * 元素为 {@code null} 时跳过；原始类型数组元素会先转为包装类型数组再拼接，避免输出类似 {@code [J@1a2b3c} 的对象地址
     *
     * @param keys 数组
     * @param split 分隔符
     * @param isSqm 是否使用单引号，false 时使用双引号
     * @return 拼接后的字符串
     */
    public static String join(Object[] keys, String split, boolean isSqm) {
        if (null == keys) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        int len = keys.length;
        boolean isFirst = true;
        for (int i = 0; i < len; i++) {
            Object item = keys[i];
            if (null == item) {
                // 跳过 null 元素
                continue;
            }
            if (GutilObject.isArray(item)) {
                // 原始类型数组直接 toString 会输出 "[J@哈希值"，先转为包装类型数组
                item = GutilObject.toObjectArray(item);
            }
            final boolean isNum = item instanceof Number;
            if (isFirst) {
                isFirst = false;
            } else {
                sb.append(split);
            }
            sb.append(isNum ? "" : (isSqm ? "'" : "\""))
                    .append(item.toString())
                    .append(isNum ? "" : (isSqm ? "'" : "\""));
        }
        return sb.toString();
    }

    /**
     * 以分隔符拼接多个元素，数字不加引号，其余元素按 isSqm 加单引号或双引号<br>
     * 注意：本方法与 {@link #join(Object[], String, boolean)} 的参数顺序不同——本方法第一个参数为 分隔符 split，最后一个参数为待拼接的元素
     * keys
     *
     * @param split 分隔符
     * @param isSqm 是否使用单引号，false 时使用双引号
     * @param keys 待拼接的元素
     * @return 拼接后的字符串
     */
    public static String join(String split, boolean isSqm, Object... keys) {
        return join(keys, split, isSqm);
    }

    public static String ifEmpty(String str, String defaultString) {
        if (isEmpty(str)) {
            return defaultString;
        }
        return str;
    }

    // ---------------------------------------------------------------------
    // 字符串通用便捷方法
    // ---------------------------------------------------------------------

    /**
     * Check whether the given {@code String} is empty.
     *
     * <p>This method accepts any Object as an argument, comparing it to {@code null} and the empty
     * String. As a consequence, this method will never return {@code true} for a non-null
     * non-String object.
     *
     * <p>The Object signature is useful for general attribute handling code that commonly deals
     * with Strings but generally has to iterate over Objects since attributes may e.g. be primitive
     * value objects as well.
     *
     * @param str the candidate String
     * @since 3.2.1
     */
    public static boolean isEmpty(Object str) {
        return (str == null || "".equals(str));
    }

    /**
     * Check that the given {@code CharSequence} is neither {@code null} nor of length 0.
     *
     * <p>Note: this method returns {@code true} for a {@code CharSequence} that purely consists of
     * whitespace.
     *
     * <p>
     *
     * <pre class="code">
     * StringUtils.hasLength(null) = false
     * StringUtils.hasLength("") = false
     * StringUtils.hasLength(" ") = true
     * StringUtils.hasLength("Hello") = true
     * </pre>
     *
     * @param str the {@code CharSequence} to check (may be {@code null})
     * @return {@code true} if the {@code CharSequence} is not {@code null} and has length
     * @see #hasText(String)
     */
    public static boolean hasLength(CharSequence str) {
        return (str != null && str.length() > 0);
    }

    /**
     * Check that the given {@code String} is neither {@code null} nor of length 0.
     *
     * <p>Note: this method returns {@code true} for a {@code String} that purely consists of
     * whitespace.
     *
     * @param str the {@code String} to check (may be {@code null})
     * @return {@code true} if the {@code String} is not {@code null} and has length
     * @see #hasLength(CharSequence)
     * @see #hasText(String)
     */
    public static boolean hasLength(String str) {
        return (str != null && !str.isEmpty());
    }

    /**
     * Check whether the given {@code CharSequence} contains actual <em>text</em>.
     *
     * <p>More specifically, this method returns {@code true} if the {@code CharSequence} is not
     * {@code null}, its length is greater than 0, and it contains at least one non-whitespace
     * character.
     *
     * <p>
     *
     * <pre class="code">
     * StringUtils.hasText(null) = false
     * StringUtils.hasText("") = false
     * StringUtils.hasText(" ") = false
     * StringUtils.hasText("12345") = true
     * StringUtils.hasText(" 12345 ") = true
     * </pre>
     *
     * @param str the {@code CharSequence} to check (may be {@code null})
     * @return {@code true} if the {@code CharSequence} is not {@code null}, its length is greater
     *     than 0, and it does not contain whitespace only
     * @see Character#isWhitespace
     */
    public static boolean hasText(CharSequence str) {
        return (str != null && str.length() > 0 && containsText(str));
    }

    /**
     * Check whether the given {@code String} contains actual <em>text</em>.
     *
     * <p>More specifically, this method returns {@code true} if the {@code String} is not {@code
     * null}, its length is greater than 0, and it contains at least one non-whitespace character.
     *
     * @param str the {@code String} to check (may be {@code null})
     * @return {@code true} if the {@code String} is not {@code null}, its length is greater than 0,
     *     and it does not contain whitespace only
     * @see #hasText(CharSequence)
     */
    public static boolean hasText(String str) {
        return (str != null && !str.isEmpty() && containsText(str));
    }

    private static boolean containsText(CharSequence str) {
        int strLen = str.length();
        for (int i = 0; i < strLen; i++) {
            if (!Character.isWhitespace(str.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check whether the given {@code CharSequence} contains any whitespace characters.
     *
     * @param str the {@code CharSequence} to check (may be {@code null})
     * @return {@code true} if the {@code CharSequence} is not empty and contains at least 1
     *     whitespace character
     * @see Character#isWhitespace
     */
    public static boolean containsWhitespace(CharSequence str) {
        if (!hasLength(str)) {
            return false;
        }

        int strLen = str.length();
        for (int i = 0; i < strLen; i++) {
            if (Character.isWhitespace(str.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check whether the given {@code String} contains any whitespace characters.
     *
     * @param str the {@code String} to check (may be {@code null})
     * @return {@code true} if the {@code String} is not empty and contains at least 1 whitespace
     *     character
     * @see #containsWhitespace(CharSequence)
     */
    public static boolean containsWhitespace(String str) {
        return containsWhitespace((CharSequence) str);
    }

    /**
     * 去除给定字符串首尾的空白字符
     *
     * @param str 要处理的字符串
     * @return 去除首尾空白后的字符串
     * @see java.lang.Character#isWhitespace
     */
    public static String trimWhitespace(String str) {
        if (!hasLength(str)) {
            return str;
        }

        int beginIndex = 0;
        int endIndex = str.length() - 1;

        while (beginIndex <= endIndex && Character.isWhitespace(str.charAt(beginIndex))) {
            beginIndex++;
        }

        while (endIndex > beginIndex && Character.isWhitespace(str.charAt(endIndex))) {
            endIndex--;
        }

        return str.substring(beginIndex, endIndex + 1);
    }

    /**
     * 去除给定字符串中的所有空白字符：包括首部、尾部及字符之间的空白
     *
     * @param str 要处理的字符串
     * @return 去除所有空白后的字符串
     * @see java.lang.Character#isWhitespace
     */
    public static String trimAllWhitespace(String str) {
        if (!hasLength(str)) {
            return str;
        }

        int len = str.length();
        StringBuilder sb = new StringBuilder(str.length());
        for (int i = 0; i < len; i++) {
            char c = str.charAt(i);
            if (!Character.isWhitespace(c)) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * 去除给定字符串首部的空白字符
     *
     * @param str 要处理的字符串
     * @return 去除首部空白后的字符串
     * @see java.lang.Character#isWhitespace
     */
    public static String trimLeadingWhitespace(String str) {
        if (!hasLength(str)) {
            return str;
        }

        // 先定位首个非空白字符的位置，再一次性截取，避免逐字符删除导致的 O(n^2)
        int beginIndex = 0;
        int strLen = str.length();
        while (beginIndex < strLen && Character.isWhitespace(str.charAt(beginIndex))) {
            beginIndex++;
        }
        return beginIndex == 0 ? str : str.substring(beginIndex);
    }

    /**
     * 去除给定字符串尾部的空白字符
     *
     * @param str 要处理的字符串
     * @return 去除尾部空白后的字符串
     * @see java.lang.Character#isWhitespace
     */
    public static String trimTrailingWhitespace(String str) {
        if (!hasLength(str)) {
            return str;
        }

        // 先定位末尾最后一个非空白字符的位置，再一次性截取，避免逐字符删除导致的 O(n^2)
        int endIndex = str.length() - 1;
        while (endIndex >= 0 && Character.isWhitespace(str.charAt(endIndex))) {
            endIndex--;
        }
        return endIndex == str.length() - 1 ? str : str.substring(0, endIndex + 1);
    }

    /**
     * 去除给定字符串首部出现的所有指定字符
     *
     * @param str 要处理的字符串
     * @param leadingCharacter 要去除的首部字符
     * @return 去除首部指定字符后的字符串
     */
    public static String trimLeadingCharacter(String str, char leadingCharacter) {
        if (!hasLength(str)) {
            return str;
        }

        // 用 indexOf 定位首个非指定字符的位置，再一次性截取，避免逐字符删除导致的 O(n^2)
        int beginIndex = 0;
        int strLen = str.length();
        while (beginIndex < strLen && str.charAt(beginIndex) == leadingCharacter) {
            beginIndex++;
        }
        return beginIndex == 0 ? str : str.substring(beginIndex);
    }

    /**
     * 去除给定字符串尾部出现的所有指定字符
     *
     * @param str 要处理的字符串
     * @param trailingCharacter 要去除的尾部字符
     * @return 去除尾部指定字符后的字符串
     */
    public static String trimTrailingCharacter(String str, char trailingCharacter) {
        if (!hasLength(str)) {
            return str;
        }

        // 先定位末尾最后一个非指定字符的位置，再一次性截取，避免逐字符删除导致的 O(n^2)
        int endIndex = str.length() - 1;
        while (endIndex >= 0 && str.charAt(endIndex) == trailingCharacter) {
            endIndex--;
        }
        return endIndex == str.length() - 1 ? str : str.substring(0, endIndex + 1);
    }

    /**
     * Test if the given {@code String} starts with the specified prefix, ignoring upper/lower case.
     *
     * @param str the {@code String} to check
     * @param prefix the prefix to look for
     * @see java.lang.String#startsWith
     */
    public static boolean startsWithIgnoreCase(String str, String prefix) {
        return (str != null
                && prefix != null
                && str.length() >= prefix.length()
                && str.regionMatches(true, 0, prefix, 0, prefix.length()));
    }

    /**
     * Test if the given {@code String} ends with the specified suffix, ignoring upper/lower case.
     *
     * @param str the {@code String} to check
     * @param suffix the suffix to look for
     * @see java.lang.String#endsWith
     */
    public static boolean endsWithIgnoreCase(String str, String suffix) {
        return (str != null
                && suffix != null
                && str.length() >= suffix.length()
                && str.regionMatches(
                        true, str.length() - suffix.length(), suffix, 0, suffix.length()));
    }

    /**
     * 判断给定字符串在指定位置是否与给定子串匹配<br>
     * 字符串为 {@code null}、索引为负数或子串超出范围时返回 false
     *
     * @param str 原始字符串（或 StringBuilder）
     * @param index 原始字符串中开始匹配的索引
     * @param substring 需要匹配的子串
     * @return 是否匹配
     */
    public static boolean substringMatch(CharSequence str, int index, CharSequence substring) {
        if (null == str || index < 0) {
            return false;
        }
        if (index + substring.length() > str.length()) {
            return false;
        }
        for (int i = 0; i < substring.length(); i++) {
            if (str.charAt(index + i) != substring.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Count the occurrences of the substring {@code sub} in string {@code str}.
     *
     * @param str string to search in
     * @param sub string to search for
     */
    public static int countOccurrencesOf(String str, String sub) {
        if (!hasLength(str) || !hasLength(sub)) {
            return 0;
        }

        int count = 0;
        int pos = 0;
        int idx;
        while ((idx = str.indexOf(sub, pos)) != -1) {
            ++count;
            pos = idx + sub.length();
        }
        return count;
    }

    /**
     * Replace all occurrences of a substring within a string with another string.
     *
     * @param inString {@code String} to examine
     * @param oldPattern {@code String} to replace
     * @param newPattern {@code String} to insert
     * @return a {@code String} with the replacements
     */
    public static String replace(String inString, String oldPattern, String newPattern) {
        if (!hasLength(inString) || !hasLength(oldPattern) || newPattern == null) {
            return inString;
        }
        int index = inString.indexOf(oldPattern);
        if (index == -1) {
            // no occurrence -> can return input as-is
            return inString;
        }

        int capacity = inString.length();
        if (newPattern.length() > oldPattern.length()) {
            capacity += 16;
        }
        StringBuilder sb = new StringBuilder(capacity);

        int pos = 0; // our position in the old string
        int patLen = oldPattern.length();
        while (index >= 0) {
            sb.append(inString.substring(pos, index));
            sb.append(newPattern);
            pos = index + patLen;
            index = inString.indexOf(oldPattern, pos);
        }

        // append any characters to the right of a match
        sb.append(inString.substring(pos));
        return sb.toString();
    }

    /**
     * Delete all occurrences of the given substring.
     *
     * @param inString the original {@code String}
     * @param pattern the pattern to delete all occurrences of
     * @return the resulting {@code String}
     */
    public static String delete(String inString, String pattern) {
        return replace(inString, pattern, "");
    }

    /**
     * Delete any character in a given {@code String}.
     *
     * @param inString the original {@code String}
     * @param charsToDelete a set of characters to delete. E.g. "az\n" will delete 'a's, 'z's and
     *     new lines.
     * @return the resulting {@code String}
     */
    public static String deleteAny(String inString, String charsToDelete) {
        if (!hasLength(inString) || !hasLength(charsToDelete)) {
            return inString;
        }

        StringBuilder sb = new StringBuilder(inString.length());
        for (int i = 0; i < inString.length(); i++) {
            char c = inString.charAt(i);
            if (charsToDelete.indexOf(c) == -1) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    // ---------------------------------------------------------------------
    // 格式化字符串的便捷方法
    // ---------------------------------------------------------------------

    /** 字符串加 单引号 */
    public static String quote(String str) {
        return (str != null ? "'" + str + "'" : null);
    }

    /**
     * Turn the given Object into a {@code String} with single quotes if it is a {@code String};
     * keeping the Object as-is else.
     *
     * @param obj the input Object (e.g. "myString")
     * @return the quoted {@code String} (e.g. "'myString'"), or the input object as-is if not a
     *     {@code String}
     */
    public static Object quoteIfString(Object obj) {
        return (obj instanceof String ? quote((String) obj) : obj);
    }

    /**
     * 去掉以点（'.'）分隔的限定名，例如 "this.name.is.qualified" 返回 "qualified"<br>
     * 字符串为 {@code null} 时返回 {@code null}
     *
     * @param qualifiedName 限定名
     * @return 去掉限定后的名称
     */
    public static String unqualify(String qualifiedName) {
        return unqualify(qualifiedName, '.');
    }

    /**
     * 去掉以指定分隔符分隔的限定名，例如使用 ':' 分隔时，"this:name:is:qualified" 返回 "qualified"<br>
     * 字符串为 {@code null} 时返回 {@code null}
     *
     * @param qualifiedName 限定名
     * @param separator 分隔符
     * @return 去掉限定后的名称
     */
    public static String unqualify(String qualifiedName, char separator) {
        if (null == qualifiedName) {
            return null;
        }
        return qualifiedName.substring(qualifiedName.lastIndexOf(separator) + 1);
    }

    /**
     * Capitalize a {@code String}, changing the first letter to upper case as per {@link
     * Character#toUpperCase(char)}. No other letters are changed.
     *
     * @param str the {@code String} to capitalize
     * @return the capitalized {@code String}
     */
    public static String capitalize(String str) {
        return changeFirstCharacterCase(str, true);
    }

    /**
     * Uncapitalize a {@code String}, changing the first letter to lower case as per {@link
     * Character#toLowerCase(char)}. No other letters are changed.
     *
     * @param str the {@code String} to uncapitalize
     * @return the uncapitalized {@code String}
     */
    public static String uncapitalize(String str) {
        return changeFirstCharacterCase(str, false);
    }

    private static String changeFirstCharacterCase(String str, boolean capitalize) {
        if (!hasLength(str)) {
            return str;
        }

        char baseChar = str.charAt(0);
        char updatedChar;
        if (capitalize) {
            updatedChar = Character.toUpperCase(baseChar);
        } else {
            updatedChar = Character.toLowerCase(baseChar);
        }
        if (baseChar == updatedChar) {
            return str;
        }

        char[] chars = str.toCharArray();
        chars[0] = updatedChar;
        return new String(chars, 0, chars.length);
    }

    /**
     * 从给定的 Java 资源路径中提取文件名，例如 {@code "mypath/myfile.txt" -> "myfile.txt"}<br>
     * 同时识别 "/" 与 "\" 两种路径分隔符；路径为 {@code null} 时返回 {@code null}
     *
     * @param path 文件路径（可能为 {@code null}）
     * @return 提取出的文件名，未找到时返回 {@code null}
     */
    public static String getFilename(String path) {
        if (path == null) {
            return null;
        }

        int separatorIndex =
                Math.max(
                        path.lastIndexOf(FOLDER_SEPARATOR),
                        path.lastIndexOf(WINDOWS_FOLDER_SEPARATOR));
        return (separatorIndex != -1 ? path.substring(separatorIndex + 1) : path);
    }

    /**
     * 从给定的 Java 资源路径中提取文件扩展名，例如 "mypath/myfile.txt" -> "txt"<br>
     * 同时识别 "/" 与 "\" 两种路径分隔符；路径为 {@code null} 时返回 {@code null}
     *
     * @param path 文件路径（可能为 {@code null}）
     * @return 提取出的文件扩展名，未找到时返回 {@code null}
     */
    public static String getFilenameExtension(String path) {
        if (path == null) {
            return null;
        }

        int extIndex = path.lastIndexOf(EXTENSION_SEPARATOR);
        if (extIndex == -1) {
            return null;
        }

        int folderIndex =
                Math.max(
                        path.lastIndexOf(FOLDER_SEPARATOR),
                        path.lastIndexOf(WINDOWS_FOLDER_SEPARATOR));
        if (folderIndex > extIndex) {
            return null;
        }

        return path.substring(extIndex + 1);
    }

    /**
     * 去除给定 Java 资源路径的文件扩展名，例如 "mypath/myfile.txt" -> "mypath/myfile"<br>
     * 路径为 {@code null} 时返回 {@code null}
     *
     * @param path 文件路径
     * @return 去除文件扩展名后的路径
     */
    public static String stripFilenameExtension(String path) {
        if (null == path) {
            return null;
        }
        int extIndex = path.lastIndexOf(EXTENSION_SEPARATOR);
        if (extIndex == -1) {
            return path;
        }

        int folderIndex = path.lastIndexOf(FOLDER_SEPARATOR);
        if (folderIndex > extIndex) {
            return path;
        }

        return path.substring(0, extIndex);
    }

    /**
     * Apply the given relative path to the given Java resource path, assuming standard Java folder
     * separation (i.e. "/" separators).
     *
     * @param path the path to start from (usually a full file path)
     * @param relativePath the relative path to apply (relative to the full file path above)
     * @return the full file path that results from applying the relative path
     */
    public static String applyRelativePath(String path, String relativePath) {
        int separatorIndex = path.lastIndexOf(FOLDER_SEPARATOR);
        if (separatorIndex != -1) {
            String newPath = path.substring(0, separatorIndex);
            if (!relativePath.startsWith(FOLDER_SEPARATOR)) {
                newPath += FOLDER_SEPARATOR;
            }
            return newPath + relativePath;
        } else {
            return relativePath;
        }
    }

    /**
     * Normalize the path by suppressing sequences like "path/.." and inner simple dots.
     *
     * <p>The result is convenient for path comparison. For other uses, notice that Windows
     * separators ("\") are replaced by simple slashes.
     *
     * @param path the original path
     * @return the normalized path
     */
    public static String cleanPath(String path) {
        if (!hasLength(path)) {
            return path;
        }
        String pathToUse = replace(path, WINDOWS_FOLDER_SEPARATOR, FOLDER_SEPARATOR);

        // Strip prefix from path to analyze, to not treat it as part of the
        // first path element. This is necessary to correctly parse paths like
        // "file:core/../core/io/Resource.class", where the ".." should just
        // strip the first "core" directory while keeping the "file:" prefix.
        int prefixIndex = pathToUse.indexOf(':');
        String prefix = "";
        if (prefixIndex != -1) {
            prefix = pathToUse.substring(0, prefixIndex + 1);
            if (prefix.contains(FOLDER_SEPARATOR)) {
                prefix = "";
            } else {
                pathToUse = pathToUse.substring(prefixIndex + 1);
            }
        }
        if (pathToUse.startsWith(FOLDER_SEPARATOR)) {
            prefix = prefix + FOLDER_SEPARATOR;
            pathToUse = pathToUse.substring(1);
        }

        String[] pathArray = delimitedListToStringArray(pathToUse, FOLDER_SEPARATOR);
        LinkedList<String> pathElements = new LinkedList<>();
        int tops = 0;

        for (int i = pathArray.length - 1; i >= 0; i--) {
            String element = pathArray[i];
            if (CURRENT_PATH.equals(element)) {
                // Points to current directory - drop it.
            } else if (TOP_PATH.equals(element)) {
                // Registering top path found.
                tops++;
            } else {
                if (tops > 0) {
                    // Merging path element with element corresponding to top path.
                    tops--;
                } else {
                    // Normal path element found.
                    pathElements.add(0, element);
                }
            }
        }

        // Remaining top paths need to be retained.
        for (int i = 0; i < tops; i++) {
            pathElements.add(0, TOP_PATH);
        }
        // If nothing else left, at least explicitly point to current path.
        if (pathElements.size() == 1
                && "".equals(pathElements.getLast())
                && !prefix.endsWith(FOLDER_SEPARATOR)) {
            pathElements.add(0, CURRENT_PATH);
        }

        return prefix + collectionToDelimitedString(pathElements, FOLDER_SEPARATOR);
    }

    /**
     * Compare two paths after normalization of them.
     *
     * @param path1 first path for comparison
     * @param path2 second path for comparison
     * @return whether the two paths are equivalent after normalization
     */
    public static boolean pathEquals(String path1, String path2) {
        return cleanPath(path1).equals(cleanPath(path2));
    }

    /**
     * 解码给定的 URI 编码字符串，遵循以下规则：
     *
     * <ul>
     *   <li>字母数字字符 {@code "a"} 到 {@code "z"}、{@code "A"} 到 {@code "Z"} 以及 {@code "0"} 到 {@code "9"}
     *       保持不变。
     *   <li>特殊字符 {@code "-"}、{@code "_"}、{@code "."} 和 {@code "*"} 保持不变。
     *   <li>序列 "{@code %<i>xy</i>}" 被解释为该字符的十六进制表示。
     *   <li>非 ASCII 字符（如中文）按 UTF-8 编码写入字节，避免只保留低 8 位导致乱码。
     * </ul>
     *
     * @param source 已编码的字符串
     * @param charset 字符集
     * @return 解码后的字符串
     * @throws IllegalArgumentException 当给定字符串包含无效的编码序列时抛出
     * @since 5.0
     * @see java.net.URLDecoder#decode(String, String)
     */
    public static String uriDecode(String source, Charset charset) {
        int length = source.length();
        if (length == 0) {
            return source;
        }
        GutilAssert.notNull(charset, () -> "Charset must not be null");

        ByteArrayOutputStream bos = new ByteArrayOutputStream(length);
        boolean changed = false;
        for (int i = 0; i < length; i++) {
            int ch = source.charAt(i);
            if (ch == '%') {
                if (i + 2 < length) {
                    char hex1 = source.charAt(i + 1);
                    char hex2 = source.charAt(i + 2);
                    int u = Character.digit(hex1, 16);
                    int l = Character.digit(hex2, 16);
                    if (u == -1 || l == -1) {
                        throw new IllegalArgumentException(
                                "Invalid encoded sequence \"" + source.substring(i) + "\"");
                    }
                    bos.write((char) ((u << 4) + l));
                    i += 2;
                    changed = true;
                } else {
                    throw new IllegalArgumentException(
                            "Invalid encoded sequence \"" + source.substring(i) + "\"");
                }
            } else if (ch > 0xFF) {
                // 非 ASCII 字符（如中文）按 UTF-8 编码写入完整字节，避免只保留低 8 位导致乱码
                byte[] utf8Bytes =
                        new String(Character.toChars(ch)).getBytes(StandardCharsets.UTF_8);
                bos.write(utf8Bytes, 0, utf8Bytes.length);
                changed = true;
            } else {
                bos.write(ch);
            }
        }
        return (changed ? new String(bos.toByteArray(), charset) : source);
    }

    /**
     * Parse the given {@code String} value into a {@link Locale}, accepting the {@link
     * Locale#toString} format as well as BCP 47 language tags.
     *
     * @param localeValue the locale value: following either {@code Locale's} {@code toString()}
     *     format ("en", "en_UK", etc), also accepting spaces as separators (as an alternative to
     *     underscores), or BCP 47 (e.g. "en-UK") as specified by {@link Locale#forLanguageTag} on
     *     Java 7+
     * @return a corresponding {@code Locale} instance, or {@code null} if none
     * @throws IllegalArgumentException in case of an invalid locale specification
     * @since 5.0.4
     * @see #parseLocaleString
     * @see Locale#forLanguageTag
     */
    public static Locale parseLocale(String localeValue) {
        String[] tokens = tokenizeLocaleSource(localeValue);
        if (tokens.length == 1) {
            Locale resolved = Locale.forLanguageTag(localeValue);
            return (resolved.getLanguage().length() > 0 ? resolved : null);
        }
        return parseLocaleTokens(localeValue, tokens);
    }

    /**
     * Parse the given {@code String} representation into a {@link Locale}.
     *
     * <p>For many parsing scenarios, this is an inverse operation of {@link Locale#toString
     * Locale's toString}, in a lenient sense. This method does not aim for strict {@code Locale}
     * design compliance; it is rather specifically tailored for typical Spring parsing needs.
     *
     * <p><b>Note: This delegate does not accept the BCP 47 language tag format. Please use {@link
     * #parseLocale} for lenient parsing of both formats.</b>
     *
     * @param localeString the locale {@code String}: following {@code Locale's} {@code toString()}
     *     format ("en", "en_UK", etc), also accepting spaces as separators (as an alternative to
     *     underscores)
     * @return a corresponding {@code Locale} instance, or {@code null} if none
     * @throws IllegalArgumentException in case of an invalid locale specification
     */
    public static Locale parseLocaleString(String localeString) {
        return parseLocaleTokens(localeString, tokenizeLocaleSource(localeString));
    }

    private static String[] tokenizeLocaleSource(String localeSource) {
        return tokenizeToStringArray(localeSource, "_ ", false, false);
    }

    private static Locale parseLocaleTokens(String localeString, String[] tokens) {
        String language = (tokens.length > 0 ? tokens[0] : "");
        String country = (tokens.length > 1 ? tokens[1] : "");
        validateLocalePart(language);
        validateLocalePart(country);

        String variant = "";
        if (tokens.length > 2) {
            // There is definitely a variant, and it is everything after the country
            // code sans the separator between the country code and the variant.
            int endIndexOfCountryCode =
                    localeString.indexOf(country, language.length()) + country.length();
            // Strip off any leading '_' and whitespace, what's left is the variant.
            variant = trimLeadingWhitespace(localeString.substring(endIndexOfCountryCode));
            if (variant.startsWith("_")) {
                variant = trimLeadingCharacter(variant, '_');
            }
        }

        if ("".equals(variant) && country.startsWith("#")) {
            variant = country;
            country = "";
        }

        return (language.length() > 0 ? new Locale(language, country, variant) : null);
    }

    private static void validateLocalePart(String localePart) {
        for (int i = 0; i < localePart.length(); i++) {
            char ch = localePart.charAt(i);
            if (ch != ' ' && ch != '_' && ch != '#' && !Character.isLetterOrDigit(ch)) {
                throw new IllegalArgumentException(
                        "Locale part \"" + localePart + "\" contains invalid characters");
            }
        }
    }

    /**
     * Parse the given {@code timeZoneString} value into a {@link TimeZone}.
     *
     * @param timeZoneString the time zone {@code String}, following {@link
     *     TimeZone#getTimeZone(String)} but throwing {@link IllegalArgumentException} in case of an
     *     invalid time zone specification
     * @return a corresponding {@link TimeZone} instance
     * @throws IllegalArgumentException in case of an invalid time zone specification
     */
    public static TimeZone parseTimeZoneString(String timeZoneString) {
        TimeZone timeZone = TimeZone.getTimeZone(timeZoneString);
        if ("GMT".equals(timeZone.getID()) && !timeZoneString.startsWith("GMT")) {
            // We don't want that GMT fallback...
            throw new IllegalArgumentException(
                    "Invalid time zone specification '" + timeZoneString + "'");
        }
        return timeZone;
    }

    // ---------------------------------------------------------------------
    // 字符串数组的便捷方法
    // ---------------------------------------------------------------------

    /**
     * Append the given {@code String} to the given {@code String} array, returning a new array
     * consisting of the input array contents plus the given {@code String}.
     *
     * @param array the array to append to (can be {@code null})
     * @param str the {@code String} to append
     * @return the new array (never {@code null})
     */
    public static String[] addStringToArray(String[] array, String str) {
        if (GutilObject.isEmpty(array)) {
            return new String[] {str};
        }

        String[] newArr = new String[array.length + 1];
        System.arraycopy(array, 0, newArr, 0, array.length);
        newArr[array.length] = str;
        return newArr;
    }

    /**
     * Concatenate the given {@code String} arrays into one, with overlapping array elements
     * included twice.
     *
     * <p>The order of elements in the original arrays is preserved.
     *
     * @param array1 the first array (can be {@code null})
     * @param array2 the second array (can be {@code null})
     * @return the new array ({@code null} if both given arrays were {@code null})
     */
    public static String[] concatenateStringArrays(String[] array1, String[] array2) {
        if (GutilObject.isEmpty(array1)) {
            return array2;
        }
        if (GutilObject.isEmpty(array2)) {
            return array1;
        }

        String[] newArr = new String[array1.length + array2.length];
        System.arraycopy(array1, 0, newArr, 0, array1.length);
        System.arraycopy(array2, 0, newArr, array1.length, array2.length);
        return newArr;
    }

    /**
     * Merge the given {@code String} arrays into one, with overlapping array elements only included
     * once.
     *
     * <p>The order of elements in the original arrays is preserved (with the exception of
     * overlapping elements, which are only included on their first occurrence).
     *
     * @param array1 the first array (can be {@code null})
     * @param array2 the second array (can be {@code null})
     * @return the new array ({@code null} if both given arrays were {@code null})
     * @deprecated as of 4.3.15, in favor of manual merging via {@link LinkedHashSet} (with every
     *     entry included at most once, even entries within the first array)
     */
    // @Deprecated

    public static String[] mergeStringArrays(String[] array1, String[] array2) {
        if (GutilObject.isEmpty(array1)) {
            return array2;
        }
        if (GutilObject.isEmpty(array2)) {
            return array1;
        }

        List<String> result = new ArrayList<>();
        result.addAll(Arrays.asList(array1));
        for (String str : array2) {
            if (!result.contains(str)) {
                result.add(str);
            }
        }
        return toStringArray(result);
    }

    /**
     * Turn given source {@code String} array into sorted array.
     *
     * @param array the source array
     * @return the sorted array (never {@code null})
     */
    public static String[] sortStringArray(String[] array) {
        if (GutilObject.isEmpty(array)) {
            return new String[0];
        }

        Arrays.sort(array);
        return array;
    }

    /**
     * Copy the given {@code Collection} into a {@code String} array.
     *
     * <p>The {@code Collection} must contain {@code String} elements only.
     *
     * @param collection the {@code Collection} to copy
     * @return the {@code String} array
     */
    public static String[] toStringArray(Collection<String> collection) {
        return collection.toArray(new String[0]);
    }

    /**
     * Copy the given Enumeration into a {@code String} array. The Enumeration must contain {@code
     * String} elements only.
     *
     * @param enumeration the Enumeration to copy
     * @return the {@code String} array
     */
    public static String[] toStringArray(Enumeration<String> enumeration) {
        return toStringArray(Collections.list(enumeration));
    }

    /**
     * Trim the elements of the given {@code String} array, calling {@code String.trim()} on each of
     * them.
     *
     * @param array the original {@code String} array (potentially empty)
     * @return the resulting array (of the same size) with trimmed elements
     */
    public static String[] trimArrayElements(String[] array) {
        if (GutilObject.isEmpty(array)) {
            return new String[0];
        }

        String[] result = new String[array.length];
        for (int i = 0; i < array.length; i++) {
            String element = array[i];
            result[i] = (element != null ? element.trim() : null);
        }
        return result;
    }

    /**
     * Remove duplicate strings from the given array.
     *
     * <p>As of 4.2, it preserves the original order, as it uses a {@link LinkedHashSet}.
     *
     * @param array the {@code String} array (potentially empty)
     * @return an array without duplicates, in natural sort order
     */
    public static String[] removeDuplicateStrings(String[] array) {
        if (GutilObject.isEmpty(array)) {
            return array;
        }

        Set<String> set = new LinkedHashSet<>(Arrays.asList(array));
        return toStringArray(set);
    }

    /**
     * Split a {@code String} at the first occurrence of the delimiter. Does not include the
     * delimiter in the result.
     *
     * @param toSplit the string to split (potentially {@code null} or empty)
     * @param delimiter to split the string up with (potentially {@code null} or empty)
     * @return a two element array with index 0 being before the delimiter, and index 1 being after
     *     the delimiter (neither element includes the delimiter); or {@code null} if the delimiter
     *     wasn't found in the given input {@code String}
     */
    public static String[] split(String toSplit, String delimiter) {
        if (!hasLength(toSplit) || !hasLength(delimiter)) {
            return null;
        }
        int offset = toSplit.indexOf(delimiter);
        if (offset < 0) {
            return null;
        }

        String beforeDelimiter = toSplit.substring(0, offset);
        String afterDelimiter = toSplit.substring(offset + delimiter.length());
        return new String[] {beforeDelimiter, afterDelimiter};
    }

    /**
     * Take an array of strings and split each element based on the given delimiter. A {@code
     * Properties} instance is then generated, with the left of the delimiter providing the key, and
     * the right of the delimiter providing the value.
     *
     * <p>Will trim both the key and value before adding them to the {@code Properties}.
     *
     * @param array the array to process
     * @param delimiter to split each element using (typically the equals symbol)
     * @return a {@code Properties} instance representing the array contents, or {@code null} if the
     *     array to process was {@code null} or empty
     */
    public static Properties splitArrayElementsIntoProperties(String[] array, String delimiter) {
        return splitArrayElementsIntoProperties(array, delimiter, null);
    }

    /**
     * Take an array of strings and split each element based on the given delimiter. A {@code
     * Properties} instance is then generated, with the left of the delimiter providing the key, and
     * the right of the delimiter providing the value.
     *
     * <p>Will trim both the key and value before adding them to the {@code Properties} instance.
     *
     * @param array the array to process
     * @param delimiter to split each element using (typically the equals symbol)
     * @param charsToDelete one or more characters to remove from each element prior to attempting
     *     the split operation (typically the quotation mark symbol), or {@code null} if no removal
     *     should occur
     * @return a {@code Properties} instance representing the array contents, or {@code null} if the
     *     array to process was {@code null} or empty
     */
    public static Properties splitArrayElementsIntoProperties(
            String[] array, String delimiter, String charsToDelete) {

        if (GutilObject.isEmpty(array)) {
            return null;
        }

        Properties result = new Properties();
        for (String element : array) {
            if (charsToDelete != null) {
                element = deleteAny(element, charsToDelete);
            }
            String[] splittedElement = split(element, delimiter);
            if (splittedElement == null) {
                continue;
            }
            result.setProperty(splittedElement[0].trim(), splittedElement[1].trim());
        }
        return result;
    }

    /**
     * Tokenize the given {@code String} into a {@code String} array via a {@link StringTokenizer}.
     *
     * <p>Trims tokens and omits empty tokens.
     *
     * <p>The given {@code delimiters} string can consist of any number of delimiter characters.
     * Each of those characters can be used to separate tokens. A delimiter is always a single
     * character; for multi-character delimiters, consider using {@link
     * #delimitedListToStringArray}.
     *
     * @param str the {@code String} to tokenize (potentially {@code null} or empty)
     * @param delimiters the delimiter characters, assembled as a {@code String} (each of the
     *     characters is individually considered as a delimiter)
     * @return an array of the tokens
     * @see java.util.StringTokenizer
     * @see String#trim()
     * @see #delimitedListToStringArray
     */
    public static String[] tokenizeToStringArray(String str, String delimiters) {
        return tokenizeToStringArray(str, delimiters, true, true);
    }

    /**
     * Tokenize the given {@code String} into a {@code String} array via a {@link StringTokenizer}.
     *
     * <p>The given {@code delimiters} string can consist of any number of delimiter characters.
     * Each of those characters can be used to separate tokens. A delimiter is always a single
     * character; for multi-character delimiters, consider using {@link
     * #delimitedListToStringArray}.
     *
     * @param str the {@code String} to tokenize (potentially {@code null} or empty)
     * @param delimiters the delimiter characters, assembled as a {@code String} (each of the
     *     characters is individually considered as a delimiter)
     * @param trimTokens trim the tokens via {@link String#trim()}
     * @param ignoreEmptyTokens omit empty tokens from the result array (only applies to tokens that
     *     are empty after trimming; StringTokenizer will not consider subsequent delimiters as
     *     token in the first place).
     * @return an array of the tokens
     * @see java.util.StringTokenizer
     * @see String#trim()
     * @see #delimitedListToStringArray
     */
    public static String[] tokenizeToStringArray(
            String str, String delimiters, boolean trimTokens, boolean ignoreEmptyTokens) {

        if (str == null) {
            return new String[0];
        }

        StringTokenizer st = new StringTokenizer(str, delimiters);
        List<String> tokens = new ArrayList<>();
        while (st.hasMoreTokens()) {
            String token = st.nextToken();
            if (trimTokens) {
                token = token.trim();
            }
            if (!ignoreEmptyTokens || token.length() > 0) {
                tokens.add(token);
            }
        }
        return toStringArray(tokens);
    }

    /**
     * 将给定的分隔列表字符串转换为字符串数组<br>
     * 空 token（如尾部分隔符产生的空元素）会被过滤
     *
     * <p>单个 {@code delimiter} 可以由多个字符组成，但它仍被视为单个分隔字符串，而非多个可能的分隔 字符，这一点与 {@link
     * #tokenizeToStringArray} 不同。
     *
     * @param str 输入的字符串（可能为 {@code null} 或空）
     * @param delimiter 元素之间的分隔符（是单个分隔字符串，而非一组单独的分隔字符）
     * @return 列表中的 token 数组
     * @see #tokenizeToStringArray
     */
    public static String[] delimitedListToStringArray(String str, String delimiter) {
        return delimitedListToStringArray(str, delimiter, null);
    }

    /**
     * 将给定的分隔列表字符串转换为字符串数组<br>
     * 空 token（如尾部分隔符产生的空元素）会被过滤
     *
     * <p>单个 {@code delimiter} 可以由多个字符组成，但它仍被视为单个分隔字符串，而非多个可能的分隔 字符，这一点与 {@link
     * #tokenizeToStringArray} 不同。
     *
     * @param str 输入的字符串（可能为 {@code null} 或空）
     * @param delimiter 元素之间的分隔符（是单个分隔字符串，而非一组单独的分隔字符）
     * @param charsToDelete 需要删除的一组字符；可用于删除不需要的换行符，例如 {@code "\r\n\f"} 会删除 字符串中的所有换行和换页符
     * @return 列表中的 token 数组
     * @see #tokenizeToStringArray
     */
    public static String[] delimitedListToStringArray(
            String str, String delimiter, String charsToDelete) {

        if (str == null) {
            return new String[0];
        }
        if (delimiter == null) {
            return new String[] {str};
        }

        List<String> result = new ArrayList<>();
        if ("".equals(delimiter)) {
            for (int i = 0; i < str.length(); i++) {
                result.add(deleteAny(str.substring(i, i + 1), charsToDelete));
            }
        } else {
            int pos = 0;
            int delPos;
            while ((delPos = str.indexOf(delimiter, pos)) != -1) {
                String token = deleteAny(str.substring(pos, delPos), charsToDelete);
                if (token.length() > 0) {
                    // 过滤空 token
                    result.add(token);
                }
                pos = delPos + delimiter.length();
            }
            if (str.length() > 0 && pos <= str.length()) {
                // 追加剩余部分（空输入时不追加；尾部空元素按语义过滤）
                String token = deleteAny(str.substring(pos), charsToDelete);
                if (token.length() > 0) {
                    result.add(token);
                }
            }
        }
        return toStringArray(result);
    }

    /**
     * Convert a comma delimited list (e.g., a row from a CSV file) into an array of strings.
     *
     * @param str the input {@code String} (potentially {@code null} or empty)
     * @return an array of strings, or the empty array in case of empty input
     */
    public static String[] commaDelimitedListToStringArray(String str) {
        return delimitedListToStringArray(str, ",");
    }

    /**
     * Convert a comma delimited list (e.g., a row from a CSV file) into a set.
     *
     * <p>Note that this will suppress duplicates, and as of 4.2, the elements in the returned set
     * will preserve the original order in a {@link LinkedHashSet}.
     *
     * @param str the input {@code String} (potentially {@code null} or empty)
     * @return a set of {@code String} entries in the list
     * @see #removeDuplicateStrings(String[])
     */
    public static Set<String> commaDelimitedListToSet(String str) {
        String[] tokens = commaDelimitedListToStringArray(str);
        return new LinkedHashSet<>(Arrays.asList(tokens));
    }

    /**
     * Convert a {@link Collection} to a delimited {@code String} (e.g. CSV).
     *
     * <p>Useful for {@code toString()} implementations.
     *
     * @param coll the {@code Collection} to convert (potentially {@code null} or empty)
     * @param delim the delimiter to use (typically a ",")
     * @param prefix the {@code String} to start each element with
     * @param suffix the {@code String} to end each element with
     * @return the delimited {@code String}
     */
    public static String collectionToDelimitedString(
            Collection<?> coll, String delim, String prefix, String suffix) {

        if (GutilCollection.isEmpty(coll)) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        Iterator<?> it = coll.iterator();
        while (it.hasNext()) {
            sb.append(prefix).append(it.next()).append(suffix);
            if (it.hasNext()) {
                sb.append(delim);
            }
        }
        return sb.toString();
    }

    /**
     * Convert a {@code Collection} into a delimited {@code String} (e.g. CSV).
     *
     * <p>Useful for {@code toString()} implementations.
     *
     * @param coll the {@code Collection} to convert (potentially {@code null} or empty)
     * @param delim the delimiter to use (typically a ",")
     * @return the delimited {@code String}
     */
    public static String collectionToDelimitedString(Collection<?> coll, String delim) {
        return collectionToDelimitedString(coll, delim, "", "");
    }

    /** 集合 拼接成字符串 */
    public static String collectionToCommaDelimitedString(Collection<?> coll) {
        return collectionToDelimitedString(coll, ",");
    }

    /** 用分隔符拼接字符串 */
    public static String arrayToDelimitedString(Object[] arr, String delim) {
        if (GutilObject.isEmpty(arr)) {
            return "";
        }
        if (arr.length == 1) {
            return GutilObject.nullSafeToString(arr[0]);
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < arr.length; i++) {
            if (i > 0) {
                sb.append(delim);
            }
            sb.append(arr[i]);
        }
        return sb.toString();
    }

    /** 用 逗号 拼接字符串 */
    public static String arrayToCommaDelimitedString(Object[] arr) {
        return arrayToDelimitedString(arr, ",");
    }

    public static String reverse(String str) {
        if (str == null) {
            return null;
        }
        return new StringBuilder(str).reverse().toString();
    }

    /**
     * Removes a substring only if it is at the end of a source string, otherwise returns the source
     * string.
     *
     * <p>A <code>null</code> source string will return <code>null</code>. An empty ("") source
     * string will return the empty string. A <code>null</code> search string will return the source
     * string.
     *
     * <pre>
     * StringUtils.removeEnd(null, *)      = null
     * StringUtils.removeEnd("", *)        = ""
     * StringUtils.removeEnd(*, null)      = *
     * StringUtils.removeEnd("www.domain.com", ".com.")  = "www.domain.com"
     * StringUtils.removeEnd("www.domain.com", ".com")   = "www.domain"
     * StringUtils.removeEnd("www.domain.com", "domain") = "www.domain.com"
     * StringUtils.removeEnd("abc", "")    = "abc"
     * </pre>
     *
     * @param str the source String to search, may be null
     * @param remove the String to search for and remove, may be null
     * @return the substring with the string removed if found, <code>null</code> if null String
     *     input
     * @since 2.1
     */
    public static String removeEnd(String str, String remove) {
        if (isEmpty(str) || isEmpty(remove)) {
            return str;
        }
        if (str.endsWith(remove)) {
            return str.substring(0, str.length() - remove.length());
        }
        return str;
    }

    /**
     * Case insensitive removal of a substring if it is at the end of a source string, otherwise
     * returns the source string.
     *
     * <p>A <code>null</code> source string will return <code>null</code>. An empty ("") source
     * string will return the empty string. A <code>null</code> search string will return the source
     * string.
     *
     * <pre>
     * StringUtils.removeEndIgnoreCase(null, *)      = null
     * StringUtils.removeEndIgnoreCase("", *)        = ""
     * StringUtils.removeEndIgnoreCase(*, null)      = *
     * StringUtils.removeEndIgnoreCase("www.domain.com", ".com.")  = "www.domain.com"
     * StringUtils.removeEndIgnoreCase("www.domain.com", ".com")   = "www.domain"
     * StringUtils.removeEndIgnoreCase("www.domain.com", "domain") = "www.domain.com"
     * StringUtils.removeEndIgnoreCase("abc", "")    = "abc"
     * StringUtils.removeEndIgnoreCase("www.domain.com", ".COM") = "www.domain")
     * StringUtils.removeEndIgnoreCase("www.domain.COM", ".com") = "www.domain")
     * </pre>
     *
     * @param str the source String to search, may be null
     * @param remove the String to search for (case insensitive) and remove, may be null
     * @return the substring with the string removed if found, <code>null</code> if null String
     *     input
     * @since 2.4
     */
    public static String removeEndIgnoreCase(String str, String remove) {
        if (isEmpty(str) || isEmpty(remove)) {
            return str;
        }
        if (endsWithIgnoreCase(str, remove)) {
            return str.substring(0, str.length() - remove.length());
        }
        return str;
    }
}
