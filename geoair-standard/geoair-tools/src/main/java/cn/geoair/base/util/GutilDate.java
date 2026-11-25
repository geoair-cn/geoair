package cn.geoair.base.util;

import cn.geoair.base.text.GuFastDateFormat;

import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.TimeZone;

/** 日期与时间工具类（来自 common lang）。 */
public class GutilDate {

    /** UTC 时区（即 GMT）。 */
    public static final TimeZone UTC_TIME_ZONE = TimeZone.getTimeZone("GMT");

    /**
     * 标准秒的毫秒数。
     *
     * @since 2.1
     */
    public static final long MILLIS_PER_SECOND = 1000;

    /**
     * 标准分钟的毫秒数。
     *
     * @since 2.1
     */
    public static final long MILLIS_PER_MINUTE = 60 * MILLIS_PER_SECOND;

    /**
     * 标准小时的毫秒数。
     *
     * @since 2.1
     */
    public static final long MILLIS_PER_HOUR = 60 * MILLIS_PER_MINUTE;

    /**
     * 标准天的毫秒数。
     *
     * @since 2.1
     */
    public static final long MILLIS_PER_DAY = 24 * MILLIS_PER_HOUR;

    /** 半个月，用于表示日期位于月份的上半月还是下半月。 */
    public static final int SEMI_MONTH = 1001;

    private static final int[][] fields = {
        {Calendar.MILLISECOND},
        {Calendar.SECOND},
        {Calendar.MINUTE},
        {Calendar.HOUR_OF_DAY, Calendar.HOUR},
        {Calendar.DATE, Calendar.DAY_OF_MONTH, Calendar.AM_PM
            /*
             * Calendar.DAY_OF_YEAR, Calendar.DAY_OF_WEEK, Calendar.DAY_OF_WEEK_IN_MONTH
             */
        },
        {Calendar.MONTH, GutilDate.SEMI_MONTH},
        {Calendar.YEAR},
        {Calendar.ERA}
    };

    /** 周范围样式：从星期日开始。 */
    public static final int RANGE_WEEK_SUNDAY = 1;

    /** 周范围样式：从星期一开始。 */
    public static final int RANGE_WEEK_MONDAY = 2;

    /** 周范围样式：从焦点日期当天开始。 */
    public static final int RANGE_WEEK_RELATIVE = 3;

    /** 周范围样式：以焦点日期当天为中心。 */
    public static final int RANGE_WEEK_CENTER = 4;

    /** 月范围样式：包含的周从星期日开始。 */
    public static final int RANGE_MONTH_SUNDAY = 5;

    /** 月范围样式：包含的周从星期一开始。 */
    public static final int RANGE_MONTH_MONDAY = 6;

    /** 截断模式常量 */
    private static final int MODIFY_TRUNCATE = 0;

    /** 四舍五入模式常量 */
    private static final int MODIFY_ROUND = 1;

    /** 向上取整模式常量 */
    private static final int MODIFY_CEILING = 2;

    /**
     * <code>GutilDate</code> 实例不应在标准编程中构造，应作为静态工具类使用，例如 <code>GutilDate.parse(str)</code>。
     *
     * <p>构造函数设为 public 以支持需要 JavaBean 实例的工具。
     */
    public GutilDate() {
        super();
    }

    // -----------------------------------------------------------------------
    /**
     * 判断两个日期对象是否在同一天（忽略时间部分）。
     *
     * <p>例如 2002-03-28 13:45 与 2002-03-28 06:01 返回 true；2002-03-28 13:45 与 2002-03-12 13:45 返回
     * false。
     *
     * <p>在 JVM 默认时区（{@link TimeZone#getDefault()}）下比较。
     *
     * @param date1 第一个日期，不被修改，不可为 null
     * @param date2 第二个日期，不被修改，不可为 null
     * @return 如果它们表示同一天则返回 true
     * @throws IllegalArgumentException 如果任一日期为 <code>null</code>
     * @since 2.1
     */
    public static boolean isSameDay(Date date1, Date date2) {
        if (date1 == null || date2 == null) {
            throw new IllegalArgumentException("The date must not be null");
        }
        Calendar cal1 = Calendar.getInstance();
        cal1.setTime(date1);
        Calendar cal2 = Calendar.getInstance();
        cal2.setTime(date2);
        return isSameDay(cal1, cal2);
    }

    /**
     * 判断两个日历对象是否在同一天（忽略时间部分）。
     *
     * <p>例如 2002-03-28 13:45 与 2002-03-28 06:01 返回 true；2002-03-28 13:45 与 2002-03-12 13:45 返回
     * false。
     *
     * <p>比较的是两个日历各自时区下的本地日期，时区不同的日历仍可能判为同一天。
     *
     * @param cal1 第一个日历，不被修改，不可为 null
     * @param cal2 第二个日历，不被修改，不可为 null
     * @return 如果它们表示同一天则返回 true
     * @throws IllegalArgumentException 如果任一日历为 <code>null</code>
     * @since 2.1
     */
    public static boolean isSameDay(Calendar cal1, Calendar cal2) {
        if (cal1 == null || cal2 == null) {
            throw new IllegalArgumentException("The date must not be null");
        }
        return (cal1.get(Calendar.ERA) == cal2.get(Calendar.ERA)
                && cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR)
                && cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR));
    }

    // -----------------------------------------------------------------------
    /**
     * 判断两个日期对象是否表示同一时刻。
     *
     * <p>该方法比较两个对象的长整型毫秒时间。
     *
     * @param date1 第一个日期，不被修改，不可为 null
     * @param date2 第二个日期，不被修改，不可为 null
     * @return 如果它们表示同一毫秒时刻则返回 true
     * @throws IllegalArgumentException 如果任一日期为 <code>null</code>
     * @since 2.1
     */
    public static boolean isSameInstant(Date date1, Date date2) {
        if (date1 == null || date2 == null) {
            throw new IllegalArgumentException("The date must not be null");
        }
        return date1.getTime() == date2.getTime();
    }

    /**
     * 判断两个日历对象是否表示同一时刻。
     *
     * <p>该方法比较两个对象的长整型毫秒时间。
     *
     * @param cal1 第一个日历，不被修改，不可为 null
     * @param cal2 第二个日历，不被修改，不可为 null
     * @return 如果它们表示同一毫秒时刻则返回 true
     * @throws IllegalArgumentException 如果任一日历为 <code>null</code>
     * @since 2.1
     */
    public static boolean isSameInstant(Calendar cal1, Calendar cal2) {
        if (cal1 == null || cal2 == null) {
            throw new IllegalArgumentException("The date must not be null");
        }
        return cal1.getTime().getTime() == cal2.getTime().getTime();
    }

    // -----------------------------------------------------------------------
    /**
     * 判断两个日历对象是否表示相同的本地时间。
     *
     * <p>该方法比较两个对象的各字段值。此外，两个日历必须是相同的类型（子类）。
     *
     * @param cal1 第一个日历，不被修改，不可为 null
     * @param cal2 第二个日历，不被修改，不可为 null
     * @return 如果它们表示相同的本地时间则返回 true
     * @throws IllegalArgumentException 如果任一日历为 <code>null</code>
     * @since 2.1
     */
    public static boolean isSameLocalTime(Calendar cal1, Calendar cal2) {
        if (cal1 == null || cal2 == null) {
            throw new IllegalArgumentException("The date must not be null");
        }
        return (cal1.get(Calendar.MILLISECOND) == cal2.get(Calendar.MILLISECOND)
                && cal1.get(Calendar.SECOND) == cal2.get(Calendar.SECOND)
                && cal1.get(Calendar.MINUTE) == cal2.get(Calendar.MINUTE)
                && cal1.get(Calendar.HOUR) == cal2.get(Calendar.HOUR)
                && cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
                && cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR)
                && cal1.get(Calendar.ERA) == cal2.get(Calendar.ERA)
                && cal1.getClass() == cal2.getClass());
    }

    // -----------------------------------------------------------------------
    /**
     * 解析日期字符串，依次尝试多个解析模式。
     *
     * <p>按顺序尝试每个解析模式，只有当整个输入字符串都被成功解析时才算解析成功；若所有模式均无法 匹配，抛出 {@link
     * ParseException}。解析采用宽松（lenient）模式，允许类似 "February 942, 1996" 的日期。
     *
     * <p><b>时区语义：</b>本方法使用 JVM 默认时区（{@link TimeZone#getDefault()}）解析，而 {@link #formatUTC(long,
     * String)} 使用 UTC 格式化，两者并非互为逆运算。若需与 formatUTC 往返 一致，请使用 {@link #parseDate(String, String[],
     * TimeZone)} 并显式传入 {@link #UTC_TIME_ZONE}。
     *
     * @param str 待解析的日期字符串，不可为 null
     * @param parsePatterns 日期格式模式数组，参见 SimpleDateFormat，不可为 null
     * @return 解析得到的日期
     * @throws IllegalArgumentException 如果日期字符串或模式数组为 null
     * @throws ParseException 如果没有（或没有任何）日期模式适合
     * @see #parseDate(String, String[], TimeZone)
     */
    public static Date parseDate(String str, String[] parsePatterns) throws ParseException {
        return parseDateWithLeniency(str, parsePatterns, null, null, true);
    }

    // -----------------------------------------------------------------------
    /**
     * 解析日期字符串，依次尝试多个解析模式。
     *
     * <p>按顺序尝试每个解析模式，只有当整个输入字符串都被成功解析时才算解析成功；若所有模式均无法 匹配，抛出 {@link
     * ParseException}。解析采用宽松（lenient）模式。
     *
     * <p><b>时区语义：</b>按指定的时区解析；若 {@code timeZone} 为 null，则使用 JVM 默认时区 （{@link
     * TimeZone#getDefault()}）。与 {@link #formatUTC(long, String)} 往返一致时请传入 {@link #UTC_TIME_ZONE}。
     *
     * @param str 待解析的日期字符串，不可为 null
     * @param parsePatterns 日期格式模式数组，参见 SimpleDateFormat，不可为 null
     * @param timeZone 解析使用的时区，可以为 null（null 表示 JVM 默认时区）
     * @return 解析得到的日期
     * @throws IllegalArgumentException 如果日期字符串或模式数组为 null
     * @throws ParseException 如果没有（或没有任何）日期模式适合
     * @see #parseDate(String, String[])
     * @see #formatUTC(long, String)
     */
    public static Date parseDate(String str, String[] parsePatterns, TimeZone timeZone)
            throws ParseException {
        return parseDateWithLeniency(str, parsePatterns, timeZone, null, true);
    }

    // -----------------------------------------------------------------------
    /**
     * 解析日期字符串，依次尝试多个解析模式。
     *
     * <p>按顺序尝试每个解析模式，只有当整个输入字符串都被成功解析时才算解析成功；若所有模式均无法 匹配，抛出 {@link
     * ParseException}。解析采用宽松（lenient）模式。
     *
     * <p><b>时区语义：</b>按指定的时区与语言环境解析；若 {@code timeZone} 为 null，则使用 JVM 默认 时区（{@link
     * TimeZone#getDefault()}）。与 {@link #formatUTC(long, String)} 往返一致时请传入 {@link #UTC_TIME_ZONE}。
     *
     * @param str 待解析的日期字符串，不可为 null
     * @param parsePatterns 日期格式模式数组，参见 SimpleDateFormat，不可为 null
     * @param timeZone 解析使用的时区，可以为 null（null 表示 JVM 默认时区）
     * @param locale 解析使用的语言环境，可以为 null（null 表示 JVM 默认语言环境）； 当模式包含 "MMM"（月份缩写）等文本元素时建议显式指定
     * @return 解析得到的日期
     * @throws IllegalArgumentException 如果日期字符串或模式数组为 null
     * @throws ParseException 如果没有（或没有任何）日期模式适合
     * @see #parseDate(String, String[])
     * @see #formatUTC(long, String)
     */
    public static Date parseDate(
            String str, String[] parsePatterns, TimeZone timeZone, Locale locale)
            throws ParseException {
        return parseDateWithLeniency(str, parsePatterns, timeZone, locale, true);
    }

    // -----------------------------------------------------------------------
    /**
     * 严格解析日期字符串，依次尝试多个解析模式。
     *
     * <p>按顺序尝试每个解析模式，只有当整个输入字符串都被成功解析时才算解析成功；若所有模式均无法 匹配，抛出 {@link
     * ParseException}。解析采用严格（非宽松）模式，不允许类似 "February 942, 1996" 或 "2 月 30 日" 之类的非法日期。
     *
     * <p><b>时区语义：</b>本方法使用 JVM 默认时区（{@link TimeZone#getDefault()}）解析，而 {@link #formatUTC(long,
     * String)} 使用 UTC 格式化，两者并非互为逆运算。若需与 formatUTC 往返 一致，请使用 {@link #parseDateStrictly(String,
     * String[], TimeZone)} 并显式传入 {@link #UTC_TIME_ZONE}。
     *
     * @param str 待解析的日期字符串，不可为 null
     * @param parsePatterns 日期格式模式数组，参见 SimpleDateFormat，不可为 null
     * @return 解析得到的日期
     * @throws IllegalArgumentException 如果日期字符串或模式数组为 null
     * @throws ParseException 如果没有（或没有任何）日期模式适合
     * @see #parseDateStrictly(String, String[], TimeZone)
     */
    public static Date parseDateStrictly(String str, String[] parsePatterns) throws ParseException {
        return parseDateWithLeniency(str, parsePatterns, null, null, false);
    }

    // -----------------------------------------------------------------------
    /**
     * 严格解析日期字符串，依次尝试多个解析模式。
     *
     * <p>按顺序尝试每个解析模式，只有当整个输入字符串都被成功解析时才算解析成功；若所有模式均无法 匹配，抛出 {@link
     * ParseException}。解析采用严格（非宽松）模式，不允许类似 "February 942, 1996" 或 "2 月 30 日" 之类的非法日期。
     *
     * <p><b>时区语义：</b>按指定的时区解析；若 {@code timeZone} 为 null，则使用 JVM 默认时区 （{@link
     * TimeZone#getDefault()}）。与 {@link #formatUTC(long, String)} 往返一致时请传入 {@link #UTC_TIME_ZONE}。
     *
     * @param str 待解析的日期字符串，不可为 null
     * @param parsePatterns 日期格式模式数组，参见 SimpleDateFormat，不可为 null
     * @param timeZone 解析使用的时区，可以为 null（null 表示 JVM 默认时区）
     * @return 解析得到的日期
     * @throws IllegalArgumentException 如果日期字符串或模式数组为 null
     * @throws ParseException 如果没有（或没有任何）日期模式适合
     * @see #parseDateStrictly(String, String[])
     * @see #formatUTC(long, String)
     */
    public static Date parseDateStrictly(String str, String[] parsePatterns, TimeZone timeZone)
            throws ParseException {
        return parseDateWithLeniency(str, parsePatterns, timeZone, null, false);
    }

    // -----------------------------------------------------------------------
    /**
     * 严格解析日期字符串，依次尝试多个解析模式。
     *
     * <p>按顺序尝试每个解析模式，只有当整个输入字符串都被成功解析时才算解析成功；若所有模式均无法 匹配，抛出 {@link
     * ParseException}。解析采用严格（非宽松）模式，不允许类似 "February 942, 1996" 或 "2 月 30 日" 之类的非法日期。
     *
     * <p><b>时区语义：</b>按指定的时区与语言环境解析；若 {@code timeZone} 为 null，则使用 JVM 默认 时区（{@link
     * TimeZone#getDefault()}）。与 {@link #formatUTC(long, String)} 往返一致时请传入 {@link #UTC_TIME_ZONE}。
     *
     * @param str 待解析的日期字符串，不可为 null
     * @param parsePatterns 日期格式模式数组，参见 SimpleDateFormat，不可为 null
     * @param timeZone 解析使用的时区，可以为 null（null 表示 JVM 默认时区）
     * @param locale 解析使用的语言环境，可以为 null（null 表示 JVM 默认语言环境）； 当模式包含 "MMM"（月份缩写）等文本元素时建议显式指定
     * @return 解析得到的日期
     * @throws IllegalArgumentException 如果日期字符串或模式数组为 null
     * @throws ParseException 如果没有（或没有任何）日期模式适合
     * @see #parseDateStrictly(String, String[])
     * @see #formatUTC(long, String)
     */
    public static Date parseDateStrictly(
            String str, String[] parsePatterns, TimeZone timeZone, Locale locale)
            throws ParseException {
        return parseDateWithLeniency(str, parsePatterns, timeZone, locale, false);
    }

    /**
     * 解析日期字符串的内部实现。
     *
     * <p>按顺序尝试每个解析模式，只有当整个输入字符串都被成功解析时才算解析成功；若所有模式均无法 匹配，抛出 {@link ParseException}。若 {@code
     * timeZone} 或 {@code locale} 为 null，则分别使用 JVM 默认时区与默认语言环境。
     *
     * @param str 待解析的日期字符串，不可为 null
     * @param parsePatterns 日期格式模式数组，参见 SimpleDateFormat，不可为 null
     * @param timeZone 解析使用的时区，可以为 null（null 表示 JVM 默认时区）
     * @param locale 解析使用的语言环境，可以为 null（null 表示 JVM 默认语言环境）
     * @param lenient 是否宽松解析日期/时间
     * @return 解析得到的日期
     * @throws IllegalArgumentException 如果日期字符串或模式数组为 null
     * @throws ParseException 如果没有（或没有任何）日期模式适合
     * @see java.util.Calendar#isLenient()
     */
    private static Date parseDateWithLeniency(
            String str, String[] parsePatterns, TimeZone timeZone, Locale locale, boolean lenient)
            throws ParseException {
        if (str == null || parsePatterns == null) {
            throw new IllegalArgumentException("Date and Patterns must not be null");
        }

        SimpleDateFormat parser =
                locale == null ? new SimpleDateFormat() : new SimpleDateFormat("", locale);
        parser.setLenient(lenient);
        if (timeZone != null) {
            parser.setTimeZone(timeZone);
        }
        ParsePosition pos = new ParsePosition(0);
        for (int i = 0; i < parsePatterns.length; i++) {

            String pattern = parsePatterns[i];

            // LANG-530 - 需要确保 'ZZ' 输出不会被传给 SimpleDateFormat
            if (parsePatterns[i].endsWith("ZZ")) {
                pattern = pattern.substring(0, pattern.length() - 1);
            }

            parser.applyPattern(pattern);
            pos.setIndex(0);

            String str2 = str;
            // LANG-530 - 需要确保 'ZZ' 输出不会进入 SimpleDateFormat，否则会抛 ParseException
            if (parsePatterns[i].endsWith("ZZ")) {
                int signIdx = indexOfSignChars(str2, 0);
                while (signIdx >= 0) {
                    str2 = reformatTimezone(str2, signIdx);
                    signIdx = indexOfSignChars(str2, ++signIdx);
                }
            }

            Date date = parser.parse(str2, pos);
            if (date != null && pos.getIndex() == str2.length()) {
                return date;
            }
        }
        throw new ParseException("Unable to parse the date: " + str, -1);
    }

    /**
     * 符号字符（即 '+' 或 '-'）的索引。
     *
     * @param str 要搜索的字符串
     * @param startPos 起始位置
     * @return 第一个符号字符的索引，未找到返回 -1
     */
    private static int indexOfSignChars(String str, int startPos) {
        int idx = GutilStr.indexOf(str, '+', startPos);
        if (idx < 0) {
            idx = GutilStr.indexOf(str, '-', startPos);
        }
        return idx;
    }

    /**
     * 重新格式化日期字符串中的时区。
     *
     * @param str 输入字符串
     * @param signIdx 符号字符的索引位置
     * @return 重新格式化后的字符串
     */
    private static String reformatTimezone(String str, int signIdx) {
        String str2 = str;
        if (signIdx >= 0
                && signIdx + 5 < str.length()
                && Character.isDigit(str.charAt(signIdx + 1))
                && Character.isDigit(str.charAt(signIdx + 2))
                && str.charAt(signIdx + 3) == ':'
                && Character.isDigit(str.charAt(signIdx + 4))
                && Character.isDigit(str.charAt(signIdx + 5))) {
            str2 = str.substring(0, signIdx + 3) + str.substring(signIdx + 4);
        }
        return str2;
    }

    // -----------------------------------------------------------------------
    /**
     * 在日期上增加指定年数并返回新的日期对象，原日期对象保持不变。
     *
     * <p>在默认时区（{@link TimeZone#getDefault()}）下进行计算。
     *
     * @param date 原日期，不可为 null
     * @param amount 增加的年数，可以为负数
     * @return 增加后的新日期对象
     * @throws IllegalArgumentException 如果日期为 null
     */
    public static Date addYears(Date date, int amount) {
        return add(date, Calendar.YEAR, amount);
    }

    // -----------------------------------------------------------------------
    /**
     * 在日期上增加指定月数并返回新的日期对象，原日期对象保持不变。
     *
     * <p>在默认时区（{@link TimeZone#getDefault()}）下进行计算。
     *
     * @param date 原日期，不可为 null
     * @param amount 增加的月数，可以为负数
     * @return 增加后的新日期对象
     * @throws IllegalArgumentException 如果日期为 null
     */
    public static Date addMonths(Date date, int amount) {
        return add(date, Calendar.MONTH, amount);
    }

    // -----------------------------------------------------------------------
    /**
     * 在日期上增加指定周数并返回新的日期对象，原日期对象保持不变。
     *
     * <p>在默认时区（{@link TimeZone#getDefault()}）下进行计算。
     *
     * @param date 原日期，不可为 null
     * @param amount 增加的周数，可以为负数
     * @return 增加后的新日期对象
     * @throws IllegalArgumentException 如果日期为 null
     */
    public static Date addWeeks(Date date, int amount) {
        return add(date, Calendar.WEEK_OF_YEAR, amount);
    }

    // -----------------------------------------------------------------------
    /**
     * 在日期上增加指定天数并返回新的日期对象，原日期对象保持不变。
     *
     * <p>在默认时区（{@link TimeZone#getDefault()}）下进行计算。
     *
     * @param date 原日期，不可为 null
     * @param amount 增加的天数，可以为负数
     * @return 增加后的新日期对象
     * @throws IllegalArgumentException 如果日期为 null
     */
    public static Date addDays(Date date, int amount) {
        return add(date, Calendar.DAY_OF_MONTH, amount);
    }

    // -----------------------------------------------------------------------
    /**
     * 在日期上增加指定小时数并返回新的日期对象，原日期对象保持不变。
     *
     * <p>在默认时区（{@link TimeZone#getDefault()}）下进行计算。
     *
     * @param date 原日期，不可为 null
     * @param amount 增加的小时数，可以为负数
     * @return 增加后的新日期对象
     * @throws IllegalArgumentException 如果日期为 null
     */
    public static Date addHours(Date date, int amount) {
        return add(date, Calendar.HOUR_OF_DAY, amount);
    }

    // -----------------------------------------------------------------------
    /**
     * 在日期上增加指定分钟数并返回新的日期对象，原日期对象保持不变。
     *
     * <p>在默认时区（{@link TimeZone#getDefault()}）下进行计算。
     *
     * @param date 原日期，不可为 null
     * @param amount 增加的分钟数，可以为负数
     * @return 增加后的新日期对象
     * @throws IllegalArgumentException 如果日期为 null
     */
    public static Date addMinutes(Date date, int amount) {
        return add(date, Calendar.MINUTE, amount);
    }

    // -----------------------------------------------------------------------
    /**
     * 在日期上增加指定秒数并返回新的日期对象，原日期对象保持不变。
     *
     * <p>在默认时区（{@link TimeZone#getDefault()}）下进行计算。
     *
     * @param date 原日期，不可为 null
     * @param amount 增加的秒数，可以为负数
     * @return 增加后的新日期对象
     * @throws IllegalArgumentException 如果日期为 null
     */
    public static Date addSeconds(Date date, int amount) {
        return add(date, Calendar.SECOND, amount);
    }

    // -----------------------------------------------------------------------
    /**
     * 在日期上增加指定毫秒数并返回新的日期对象，原日期对象保持不变。
     *
     * <p>在默认时区（{@link TimeZone#getDefault()}）下进行计算。
     *
     * @param date 原日期，不可为 null
     * @param amount 增加的毫秒数，可以为负数
     * @return 增加后的新日期对象
     * @throws IllegalArgumentException 如果日期为 null
     */
    public static Date addMilliseconds(Date date, int amount) {
        return add(date, Calendar.MILLISECOND, amount);
    }

    // -----------------------------------------------------------------------
    /**
     * 在日期上按指定日历字段增加指定数值并返回新的日期对象，原日期对象保持不变。
     *
     * <p>在默认时区（{@link TimeZone#getDefault()}）下进行计算。
     *
     * @param date 原日期，不可为 null
     * @param calendarField 要增加的日历字段，参见 {@link Calendar}
     * @param amount 增加的数值，可以为负数
     * @return 增加后的新日期对象
     * @throws IllegalArgumentException 如果日期为 null
     * @see #addYears(Date, int)
     * @see #addMonths(Date, int)
     * @see #addDays(Date, int)
     * @deprecated 将在 3.0 中改为私有作用域
     */
    public static Date add(Date date, int calendarField, int amount) {
        if (date == null) {
            throw new IllegalArgumentException("The date must not be null");
        }
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.add(calendarField, amount);
        return c.getTime();
    }

    // -----------------------------------------------------------------------
    /**
     * 将日期对象的年份字段设置为指定值并返回新的日期对象，原日期对象保持不变。
     *
     * <p>在默认时区（{@link TimeZone#getDefault()}）下进行计算。
     *
     * @param date 原日期，不可为 null
     * @param amount 要设置的年份值
     * @return 设置后的新日期对象
     * @throws IllegalArgumentException 如果日期为 null，或设置后的日期非法（如闰年 2 月 30 日）
     * @since 2.4
     */
    public static Date setYears(Date date, int amount) {
        return set(date, Calendar.YEAR, amount);
    }

    // -----------------------------------------------------------------------
    /**
     * 将日期对象的月份字段设置为指定值并返回新的日期对象，原日期对象保持不变。
     *
     * <p>在默认时区（{@link TimeZone#getDefault()}）下进行计算。
     *
     * @param date 原日期，不可为 null
     * @param amount 要设置的月份值
     * @return 设置后的新日期对象
     * @throws IllegalArgumentException 如果日期为 null，或设置后的日期非法（如闰年 2 月 30 日）
     * @since 2.4
     */
    public static Date setMonths(Date date, int amount) {
        return set(date, Calendar.MONTH, amount);
    }

    // -----------------------------------------------------------------------
    /**
     * 将日期对象的日（月中第几天）字段设置为指定值并返回新的日期对象，原日期对象保持不变。
     *
     * <p>在默认时区（{@link TimeZone#getDefault()}）下进行计算。
     *
     * @param date 原日期，不可为 null
     * @param amount 要设置的日值
     * @return 设置后的新日期对象
     * @throws IllegalArgumentException 如果日期为 null，或设置后的日期非法 （如把 2 月设置为 30 日、平年设置为 2 月 29 日等）
     * @since 2.4
     */
    public static Date setDays(Date date, int amount) {
        return set(date, Calendar.DAY_OF_MONTH, amount);
    }

    // -----------------------------------------------------------------------
    /**
     * 将日期对象的小时字段设置为指定值并返回新的日期对象，原日期对象保持不变。小时取值范围为 0-23。
     *
     * <p>在默认时区（{@link TimeZone#getDefault()}）下进行计算。
     *
     * @param date 原日期，不可为 null
     * @param amount 要设置的小时值
     * @return 设置后的新日期对象
     * @throws IllegalArgumentException 如果日期为 null，或设置后的日期非法
     * @since 2.4
     */
    public static Date setHours(Date date, int amount) {
        return set(date, Calendar.HOUR_OF_DAY, amount);
    }

    // -----------------------------------------------------------------------
    /**
     * 将日期对象的分钟字段设置为指定值并返回新的日期对象，原日期对象保持不变。
     *
     * <p>在默认时区（{@link TimeZone#getDefault()}）下进行计算。
     *
     * @param date 原日期，不可为 null
     * @param amount 要设置的分钟值
     * @return 设置后的新日期对象
     * @throws IllegalArgumentException 如果日期为 null，或设置后的日期非法
     * @since 2.4
     */
    public static Date setMinutes(Date date, int amount) {
        return set(date, Calendar.MINUTE, amount);
    }

    // -----------------------------------------------------------------------
    /**
     * 将日期对象的秒字段设置为指定值并返回新的日期对象，原日期对象保持不变。
     *
     * <p>在默认时区（{@link TimeZone#getDefault()}）下进行计算。
     *
     * @param date 原日期，不可为 null
     * @param amount 要设置的秒值
     * @return 设置后的新日期对象
     * @throws IllegalArgumentException 如果日期为 null，或设置后的日期非法
     * @since 2.4
     */
    public static Date setSeconds(Date date, int amount) {
        return set(date, Calendar.SECOND, amount);
    }

    // -----------------------------------------------------------------------
    /**
     * 将日期对象的毫秒字段设置为指定值并返回新的日期对象，原日期对象保持不变。
     *
     * <p>在默认时区（{@link TimeZone#getDefault()}）下进行计算。
     *
     * @param date 原日期，不可为 null
     * @param amount 要设置的毫秒值
     * @return 设置后的新日期对象
     * @throws IllegalArgumentException 如果日期为 null，或设置后的日期非法
     * @since 2.4
     */
    public static Date setMilliseconds(Date date, int amount) {
        return set(date, Calendar.MILLISECOND, amount);
    }

    // -----------------------------------------------------------------------
    /**
     * 将日期对象按指定日历字段设置为指定值并返回新的日期对象，原日期对象保持不变。
     *
     * <p>本方法使用非宽松（lenient=false）日历：当设置后的日期非法时抛出 {@link IllegalArgumentException}，而不是像宽松日历那样自动进位。
     * 例如把 2 月设置为 30 日、平年设置为 2 月 29 日、月末设置为不存在的日期（如 2 月 31 日）都会 抛出异常，不会滚动到下一个月份。闰年（如 2000 年、2024 年）中
     * 2 月 29 日是合法日期，请按 实际年份判断边界。
     *
     * <p>在默认时区（{@link TimeZone#getDefault()}）下进行计算。
     *
     * @param date 原日期，不可为 null
     * @param calendarField 要设置的日历字段，参见 {@link Calendar}
     * @param amount 要设置的值
     * @return 设置后的新日期对象
     * @throws IllegalArgumentException 如果日期为 null，或设置后的日期非法（日历处于非宽松模式）
     * @since 2.4
     */
    private static Date set(Date date, int calendarField, int amount) {
        if (date == null) {
            throw new IllegalArgumentException("The date must not be null");
        }
        // getInstance() 返回新对象，因此本方法是线程安全的。
        Calendar c = Calendar.getInstance();
        c.setLenient(false);
        c.setTime(date);
        c.set(calendarField, amount);
        return c.getTime();
    }

    // -----------------------------------------------------------------------
    /**
     * 将 {@link Date} 转换为 {@link Calendar} 对象。
     *
     * <p>返回的日历使用 JVM 默认时区（{@link TimeZone#getDefault()}）与默认语言环境。
     *
     * @param date 要转换的日期，不可为 null
     * @return 转换得到的日历对象
     * @throws IllegalArgumentException 如果传入 null
     * @since 2.6
     */
    public static Calendar toCalendar(Date date) {
        if (date == null) {
            throw new IllegalArgumentException("The date must not be null");
        }
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        return c;
    }

    // -----------------------------------------------------------------------
    /**
     * 对日期执行四舍五入，保留指定字段作为最高有效字段。
     *
     * <p>例如 2002-03-28 13:45:01.231，若字段为 HOUR，则返回 2002-03-28 14:00:00.000； 若字段为 MONTH，则返回
     * 2002-04-01 00:00:00.000。
     *
     * <p>对于实行夏令时切换的时区，按 {@link Calendar#HOUR_OF_DAY} 取整时可能产生非整点结果， 详见 {@link Calendar#add(int,
     * int)} 的说明。
     *
     * <p>在默认时区（{@link TimeZone#getDefault()}）下进行计算。
     *
     * @param date 要处理的日期
     * @param field 来自 {@link Calendar} 的字段或 {@link #SEMI_MONTH}
     * @return 取整后的日期
     * @throws IllegalArgumentException 如果日期为 null 或字段不支持
     * @throws ArithmeticException 如果年份超过 2 亿 8 千万
     */
    public static Date round(Date date, int field) {
        if (date == null) {
            throw new IllegalArgumentException("The date must not be null");
        }
        Calendar gval = Calendar.getInstance();
        gval.setTime(date);
        modify(gval, field, MODIFY_ROUND);
        return gval.getTime();
    }

    /**
     * 对日历执行四舍五入，保留指定字段作为最高有效字段，返回一个新的日历对象，原对象保持不变。
     *
     * <p>例如 2002-03-28 13:45:01.231，若字段为 HOUR，则返回 2002-03-28 14:00:00.000； 若字段为 MONTH，则返回
     * 2002-04-01 00:00:00.000。
     *
     * <p>对于实行夏令时切换的时区，按 {@link Calendar#HOUR_OF_DAY} 取整时可能产生非整点结果。
     *
     * <p>计算时保留原日历对象的时区。
     *
     * @param date 要处理的日历
     * @param field 来自 {@link Calendar} 的字段或 {@link #SEMI_MONTH}
     * @return 取整后的日历对象（新对象）
     * @throws IllegalArgumentException 如果日历为 null 或字段不支持
     * @throws ArithmeticException 如果年份超过 2 亿 8 千万
     */
    public static Calendar round(Calendar date, int field) {
        if (date == null) {
            throw new IllegalArgumentException("The date must not be null");
        }
        Calendar rounded = (Calendar) date.clone();
        modify(rounded, field, MODIFY_ROUND);
        return rounded;
    }

    /**
     * 对日期或日历执行四舍五入，保留指定字段作为最高有效字段。
     *
     * <p>例如 2002-03-28 13:45:01.231，若字段为 HOUR，则返回 2002-03-28 14:00:00.000； 若字段为 MONTH，则返回
     * 2002-04-01 00:00:00.000。
     *
     * @param date 要处理的日期，类型为 {@link Date} 或 {@link Calendar}
     * @param field 来自 {@link Calendar} 的字段或 {@link #SEMI_MONTH}
     * @return 取整后的日期
     * @throws IllegalArgumentException 如果日期为 null 或字段不支持
     * @throws ClassCastException 如果对象类型不是 {@link Date} 或 {@link Calendar}
     * @throws ArithmeticException 如果年份超过 2 亿 8 千万
     */
    public static Date round(Object date, int field) {
        if (date == null) {
            throw new IllegalArgumentException("The date must not be null");
        }
        if (date instanceof Date) {
            return round((Date) date, field);
        } else if (date instanceof Calendar) {
            return round((Calendar) date, field).getTime();
        } else {
            throw new ClassCastException("Could not round " + date);
        }
    }

    // -----------------------------------------------------------------------
    /**
     * 对日期执行截断，保留指定字段作为最高有效字段。
     *
     * <p>例如 2002-03-28 13:45:01.231，若字段为 HOUR，则返回 2002-03-28 13:00:00.000； 若字段为 MONTH，则返回
     * 2002-03-01 00:00:00.000。
     *
     * <p>在默认时区（{@link TimeZone#getDefault()}）下进行计算。
     *
     * @param date 要处理的日期
     * @param field 来自 {@link Calendar} 的字段或 {@link #SEMI_MONTH}
     * @return 截断后的日期
     * @throws IllegalArgumentException 如果日期为 null 或字段不支持
     * @throws ArithmeticException 如果年份超过 2 亿 8 千万
     */
    public static Date truncate(Date date, int field) {
        if (date == null) {
            throw new IllegalArgumentException("The date must not be null");
        }
        Calendar gval = Calendar.getInstance();
        gval.setTime(date);
        modify(gval, field, MODIFY_TRUNCATE);
        return gval.getTime();
    }

    /**
     * 对日历执行截断，保留指定字段作为最高有效字段，返回一个新的日历对象，原对象保持不变。
     *
     * <p>例如 2002-03-28 13:45:01.231，若字段为 HOUR，则返回 2002-03-28 13:00:00.000； 若字段为 MONTH，则返回
     * 2002-03-01 00:00:00.000。
     *
     * <p>计算时保留原日历对象的时区。
     *
     * @param date 要处理的日历
     * @param field 来自 {@link Calendar} 的字段或 {@link #SEMI_MONTH}
     * @return 截断后的日历对象（新对象）
     * @throws IllegalArgumentException 如果日历为 null 或字段不支持
     * @throws ArithmeticException 如果年份超过 2 亿 8 千万
     */
    public static Calendar truncate(Calendar date, int field) {
        if (date == null) {
            throw new IllegalArgumentException("The date must not be null");
        }
        Calendar truncated = (Calendar) date.clone();
        modify(truncated, field, MODIFY_TRUNCATE);
        return truncated;
    }

    /**
     * 对日期或日历执行截断，保留指定字段作为最高有效字段。
     *
     * <p>例如 2002-03-28 13:45:01.231，若字段为 HOUR，则返回 2002-03-28 13:00:00.000； 若字段为 MONTH，则返回
     * 2002-03-01 00:00:00.000。
     *
     * @param date 要处理的日期，类型为 {@link Date} 或 {@link Calendar}
     * @param field 来自 {@link Calendar} 的字段或 {@link #SEMI_MONTH}
     * @return 截断后的日期
     * @throws IllegalArgumentException 如果日期为 null 或字段不支持
     * @throws ClassCastException 如果对象类型不是 {@link Date} 或 {@link Calendar}
     * @throws ArithmeticException 如果年份超过 2 亿 8 千万
     */
    public static Date truncate(Object date, int field) {
        if (date == null) {
            throw new IllegalArgumentException("The date must not be null");
        }
        if (date instanceof Date) {
            return truncate((Date) date, field);
        } else if (date instanceof Calendar) {
            return truncate((Calendar) date, field).getTime();
        } else {
            throw new ClassCastException("Could not truncate " + date);
        }
    }

    // -----------------------------------------------------------------------
    /**
     * 对日期执行向上取整（ceiling），保留指定字段作为最高有效字段。
     *
     * <p>例如 2002-03-28 13:45:01.231，若字段为 HOUR，则返回 2002-03-28 14:00:00.000； 若字段为 MONTH，则返回
     * 2002-04-01 00:00:00.000。
     *
     * <p>在默认时区（{@link TimeZone#getDefault()}）下进行计算。
     *
     * @param date 要处理的日期
     * @param field 来自 {@link Calendar} 的字段或 {@link #SEMI_MONTH}
     * @return 向上取整后的日期
     * @throws IllegalArgumentException 如果日期为 null 或字段不支持
     * @throws ArithmeticException 如果年份超过 2 亿 8 千万
     * @since 2.5
     */
    public static Date ceiling(Date date, int field) {
        if (date == null) {
            throw new IllegalArgumentException("The date must not be null");
        }
        Calendar gval = Calendar.getInstance();
        gval.setTime(date);
        modify(gval, field, MODIFY_CEILING);
        return gval.getTime();
    }

    /**
     * 对日历执行向上取整（ceiling），保留指定字段作为最高有效字段，返回一个新的日历对象，原对象 保持不变。
     *
     * <p>例如 2002-03-28 13:45:01.231，若字段为 HOUR，则返回 2002-03-28 14:00:00.000； 若字段为 MONTH，则返回
     * 2002-04-01 00:00:00.000。
     *
     * <p>计算时保留原日历对象的时区。
     *
     * @param date 要处理的日历
     * @param field 来自 {@link Calendar} 的字段或 {@link #SEMI_MONTH}
     * @return 向上取整后的日历对象（新对象）
     * @throws IllegalArgumentException 如果日历为 null 或字段不支持
     * @throws ArithmeticException 如果年份超过 2 亿 8 千万
     * @since 2.5
     */
    public static Calendar ceiling(Calendar date, int field) {
        if (date == null) {
            throw new IllegalArgumentException("The date must not be null");
        }
        Calendar ceiled = (Calendar) date.clone();
        modify(ceiled, field, MODIFY_CEILING);
        return ceiled;
    }

    /**
     * 对日期或日历执行向上取整（ceiling），保留指定字段作为最高有效字段。
     *
     * <p>例如 2002-03-28 13:45:01.231，若字段为 HOUR，则返回 2002-03-28 14:00:00.000； 若字段为 MONTH，则返回
     * 2002-04-01 00:00:00.000。
     *
     * @param date 要处理的日期，类型为 {@link Date} 或 {@link Calendar}
     * @param field 来自 {@link Calendar} 的字段或 {@link #SEMI_MONTH}
     * @return 向上取整后的日期
     * @throws IllegalArgumentException 如果日期为 null 或字段不支持
     * @throws ClassCastException 如果对象类型不是 {@link Date} 或 {@link Calendar}
     * @throws ArithmeticException 如果年份超过 2 亿 8 千万
     * @since 2.5
     */
    public static Date ceiling(Object date, int field) {
        if (date == null) {
            throw new IllegalArgumentException("The date must not be null");
        }
        if (date instanceof Date) {
            return ceiling((Date) date, field);
        } else if (date instanceof Calendar) {
            return ceiling((Calendar) date, field).getTime();
        } else {
            throw new ClassCastException("Could not find ceiling of for type: " + date.getClass());
        }
    }

    // -----------------------------------------------------------------------
    /**
     * 内部计算方法。
     *
     * @param val 日历
     * @param field 字段常量
     * @param modType 截断、取整或取上界的方式
     * @throws ArithmeticException 如果年份超过 2.8 亿
     */
    private static void modify(Calendar val, int field, int modType) {
        if (val.get(Calendar.YEAR) > 280000000) {
            throw new ArithmeticException("Calendar value too large for accurate calculations");
        }

        if (field == Calendar.MILLISECOND) {
            return;
        }

        // ----------------- Fix for LANG-59 ---------------------- START ---------------
        // 参见 http://issues.apache.org/jira/browse/LANG-59
        //
        // 手动截断毫秒、秒和分钟，而不使用 Calendar 的方法。

        Date date = val.getTime();
        long time = date.getTime();
        boolean done = false;

        // 截断毫秒
        int millisecs = val.get(Calendar.MILLISECOND);
        if (MODIFY_TRUNCATE == modType || millisecs < 500) {
            time = time - millisecs;
        }
        if (field == Calendar.SECOND) {
            done = true;
        }

        // 截断秒
        int seconds = val.get(Calendar.SECOND);
        if (!done && (MODIFY_TRUNCATE == modType || seconds < 30)) {
            time = time - (seconds * 1000L);
        }
        if (field == Calendar.MINUTE) {
            done = true;
        }

        // 截断分钟
        int minutes = val.get(Calendar.MINUTE);
        if (!done && (MODIFY_TRUNCATE == modType || minutes < 30)) {
            time = time - (minutes * 60000L);
        }

        // 重置时间
        if (date.getTime() != time) {
            date.setTime(time);
            val.setTime(date);
        }
        // ----------------- Fix for LANG-59 ----------------------- END ----------------

        boolean roundUp = false;
        for (int i = 0; i < fields.length; i++) {
            for (int j = 0; j < fields[i].length; j++) {
                if (fields[i][j] == field) {
                    // 这就是我们要找的字段...停止循环
                    if (modType == MODIFY_CEILING || (modType == MODIFY_ROUND && roundUp)) {
                        if (field == GutilDate.SEMI_MONTH) {
                            // 这是一个难以归纳的特殊情况
                            // 如果日期为 1，则向上取整到 16，否则
                            // 减去 15 天并加 1 个月
                            if (val.get(Calendar.DATE) == 1) {
                                val.add(Calendar.DATE, 15);
                            } else {
                                val.add(Calendar.DATE, -15);
                                val.add(Calendar.MONTH, 1);
                            }
                            // ----------------- Fix for LANG-440 ----------------------
                            // START ---------------
                        } else if (field == Calendar.AM_PM) {
                            // 这是一个特殊情况
                            // 如果时间为 0，则向上取整到 12，否则
                            // 减去 12 小时并加 1 天
                            if (val.get(Calendar.HOUR_OF_DAY) == 0) {
                                val.add(Calendar.HOUR_OF_DAY, 12);
                            } else {
                                val.add(Calendar.HOUR_OF_DAY, -12);
                                val.add(Calendar.DATE, 1);
                            }
                            // ----------------- Fix for LANG-440 ----------------------
                            // END ---------------
                        } else {
                            // 需要对该字段加 1，因为
                            // 最后一个数字导致我们向上取整
                            val.add(fields[i][0], 1);
                        }
                    }
                    return;
                }
            }
            // 有些字段无法简单取整
            int offset = 0;
            boolean offsetSet = false;
            // 这些是需要不同取整规则的特殊字段类型
            switch (field) {
                case GutilDate.SEMI_MONTH:
                    if (fields[i][0] == Calendar.DATE) {
                        // 如果我们要丢弃 DATE 字段的值，
                        // 需要按自己的方式处理。
                        // 需要减 1，因为日期的下限是 1
                        offset = val.get(Calendar.DATE) - 1;
                        // 如果超过 15 天的调整值，说明处于
                        // 月份的下半段，应据此保留。
                        if (offset >= 15) {
                            offset -= 15;
                        }
                        // 记录我们处于该范围的上半段还是下半段
                        roundUp = offset > 7;
                        offsetSet = true;
                    }
                    break;
                case Calendar.AM_PM:
                    if (fields[i][0] == Calendar.HOUR_OF_DAY) {
                        // 如果我们要丢弃 HOUR 字段的值，
                        // 需要按自己的方式处理。
                        offset = val.get(Calendar.HOUR_OF_DAY);
                        if (offset >= 12) {
                            offset -= 12;
                        }
                        roundUp = offset >= 6;
                        offsetSet = true;
                    }
                    break;
            }
            if (!offsetSet) {
                int min = val.getActualMinimum(fields[i][0]);
                int max = val.getActualMaximum(fields[i][0]);
                // 计算距最小允许值的偏移量
                offset = val.get(fields[i][0]) - min;
                // 如果超过最小值和最大值之间的一半，则设置 roundUp
                roundUp = offset > ((max - min) / 2);
            }
            // 需要移除该字段
            if (offset != 0) {
                val.set(fields[i][0], val.get(fields[i][0]) - offset);
            }
        }
        throw new IllegalArgumentException("The field " + field + " is not supported");
    }

    // -----------------------------------------------------------------------
    /**
     * 构造按日期范围迭代每一天的迭代器，范围由焦点日期与范围样式定义。
     *
     * <p>例如传入 2002-07-04（星期四）与 {@link #RANGE_MONTH_SUNDAY}，返回的迭代器从 2002-06-30（星期日）开始，到
     * 2002-08-03（星期六）结束，每个中间日期返回一个 {@link Calendar} 实例。
     *
     * <p>迭代器通过 {@link Calendar#add(int, int)} 推进日期。
     *
     * <p>在默认时区（{@link TimeZone#getDefault()}）下进行计算。
     *
     * @param focus 焦点日期，不可为 null
     * @param rangeStyle 范围样式常量，必须是 {@link GutilDate#RANGE_MONTH_SUNDAY}、 {@link
     *     GutilDate#RANGE_MONTH_MONDAY}、{@link GutilDate#RANGE_WEEK_SUNDAY}、 {@link
     *     GutilDate#RANGE_WEEK_MONDAY}、{@link GutilDate#RANGE_WEEK_RELATIVE} 或 {@link
     *     GutilDate#RANGE_WEEK_CENTER} 之一
     * @return 日期迭代器，总是返回 {@link Calendar} 实例
     * @throws IllegalArgumentException 如果焦点日期为 null 或范围样式非法
     */
    public static Iterator iterator(Date focus, int rangeStyle) {
        if (focus == null) {
            throw new IllegalArgumentException("The date must not be null");
        }
        Calendar gval = Calendar.getInstance();
        gval.setTime(focus);
        return iterator(gval, rangeStyle);
    }

    /**
     * 构造按日期范围迭代每一天的迭代器，范围由焦点日历与范围样式定义。
     *
     * <p>例如传入 2002-07-04（星期四）与 {@link #RANGE_MONTH_SUNDAY}，返回的迭代器从 2002-06-30（星期日）开始，到
     * 2002-08-03（星期六）结束，每个中间日期返回一个 {@link Calendar} 实例。
     *
     * <p>迭代器通过 {@link Calendar#add(int, int)} 推进日期，并保留焦点日历的时区。
     *
     * @param focus 焦点日历
     * @param rangeStyle 范围样式常量，必须是 {@link GutilDate#RANGE_MONTH_SUNDAY}、 {@link
     *     GutilDate#RANGE_MONTH_MONDAY}、{@link GutilDate#RANGE_WEEK_SUNDAY}、 {@link
     *     GutilDate#RANGE_WEEK_MONDAY}、{@link GutilDate#RANGE_WEEK_RELATIVE} 或 {@link
     *     GutilDate#RANGE_WEEK_CENTER} 之一
     * @return 日期迭代器
     * @throws IllegalArgumentException 如果焦点日历为 null 或范围样式非法
     */
    public static Iterator iterator(Calendar focus, int rangeStyle) {
        if (focus == null) {
            throw new IllegalArgumentException("The date must not be null");
        }
        Calendar start = null;
        Calendar end = null;
        int startCutoff = Calendar.SUNDAY;
        int endCutoff = Calendar.SATURDAY;
        switch (rangeStyle) {
            case RANGE_MONTH_SUNDAY:
            case RANGE_MONTH_MONDAY:
                // 将 start 设置为该月的第一天
                start = truncate(focus, Calendar.MONTH);
                // 将 end 设置为该月的最后一天
                end = (Calendar) start.clone();
                end.add(Calendar.MONTH, 1);
                end.add(Calendar.DATE, -1);
                // 将 start 循环回上一个星期日或星期一
                if (rangeStyle == RANGE_MONTH_MONDAY) {
                    startCutoff = Calendar.MONDAY;
                    endCutoff = Calendar.SUNDAY;
                }
                break;
            case RANGE_WEEK_SUNDAY:
            case RANGE_WEEK_MONDAY:
            case RANGE_WEEK_RELATIVE:
            case RANGE_WEEK_CENTER:
                // 将 start 和 end 设置为当前日期
                start = truncate(focus, Calendar.DATE);
                end = truncate(focus, Calendar.DATE);
                switch (rangeStyle) {
                    case RANGE_WEEK_SUNDAY:
                        // 默认已设置
                        break;
                    case RANGE_WEEK_MONDAY:
                        startCutoff = Calendar.MONDAY;
                        endCutoff = Calendar.SUNDAY;
                        break;
                    case RANGE_WEEK_RELATIVE:
                        startCutoff = focus.get(Calendar.DAY_OF_WEEK);
                        endCutoff = startCutoff - 1;
                        break;
                    case RANGE_WEEK_CENTER:
                        startCutoff = focus.get(Calendar.DAY_OF_WEEK) - 3;
                        endCutoff = focus.get(Calendar.DAY_OF_WEEK) + 3;
                        break;
                }
                break;
            default:
                throw new IllegalArgumentException(
                        "The range style " + rangeStyle + " is not valid.");
        }
        if (startCutoff < Calendar.SUNDAY) {
            startCutoff += 7;
        }
        if (startCutoff > Calendar.SATURDAY) {
            startCutoff -= 7;
        }
        if (endCutoff < Calendar.SUNDAY) {
            endCutoff += 7;
        }
        if (endCutoff > Calendar.SATURDAY) {
            endCutoff -= 7;
        }
        while (start.get(Calendar.DAY_OF_WEEK) != startCutoff) {
            start.add(Calendar.DATE, -1);
        }
        while (end.get(Calendar.DAY_OF_WEEK) != endCutoff) {
            end.add(Calendar.DATE, 1);
        }
        return new DateIterator(start, end);
    }

    /**
     * 构造按日期范围迭代每一天的迭代器，范围由焦点日期（{@link Date} 或 {@link Calendar}）与范围 样式定义。
     *
     * <p>例如传入 2002-07-04（星期四）与 {@link #RANGE_MONTH_SUNDAY}，返回的迭代器从 2002-06-30（星期日）开始，到
     * 2002-08-03（星期六）结束，每个中间日期返回一个 {@link Calendar} 实例。
     *
     * @param focus 要处理的焦点日期，类型为 {@link Date} 或 {@link Calendar}
     * @param rangeStyle 范围样式常量，参见 {@link #iterator(Calendar, int)} 的说明
     * @return 日期迭代器
     * @throws IllegalArgumentException 如果焦点日期为 null
     * @throws ClassCastException 如果对象类型不是 {@link Date} 或 {@link Calendar}
     */
    public static Iterator iterator(Object focus, int rangeStyle) {
        if (focus == null) {
            throw new IllegalArgumentException("The date must not be null");
        }
        if (focus instanceof Date) {
            return iterator((Date) focus, rangeStyle);
        } else if (focus instanceof Calendar) {
            return iterator((Calendar) focus, rangeStyle);
        } else {
            throw new ClassCastException("Could not iterate based on " + focus);
        }
    }

    /**
     * 返回日期在指定片段内的毫秒数，所有大于该片段的日期字段将被忽略。
     *
     * <p>查询任意日期的毫秒数只会返回当前秒内的毫秒数（结果为 0-999）。本方法可获取任意片段的 毫秒数。例如计算今天已过去的毫秒数时，fragment 为 {@link
     * Calendar#DATE} 或 {@link Calendar#DAY_OF_YEAR}，结果为过去所有小时、分钟和秒的毫秒之和。
     *
     * <p>合法的 fragment 为：{@link Calendar#YEAR}、{@link Calendar#MONTH}、 {@link Calendar#DAY_OF_YEAR}
     * 与 {@link Calendar#DATE}、{@link Calendar#HOUR_OF_DAY}、 {@link Calendar#MINUTE}、{@link
     * Calendar#SECOND} 与 {@link Calendar#MILLISECOND}。 小于等于 SECOND 的片段返回 0。
     *
     * <ul>
     *   <li>2008-01-01 07:15:10.538，fragment 为 Calendar.SECOND，返回 538
     *   <li>2008-01-06 07:15:10.538，fragment 为 Calendar.SECOND，返回 538
     *   <li>2008-01-06 07:15:10.538，fragment 为 Calendar.MINUTE，返回 10538（10*1000 + 538）
     *   <li>2008-01-16 07:15:10.538，fragment 为 Calendar.MILLISECOND，返回 0（毫秒无法再细分）
     * </ul>
     *
     * <p>在默认时区（{@link TimeZone#getDefault()}）下进行计算。
     *
     * @param date 要处理的日期，不可为 null
     * @param fragment 用于计算的 {@link Calendar} 字段
     * @return 日期在指定片段内的毫秒数
     * @throws IllegalArgumentException 如果日期为 null 或 fragment 不受支持
     * @since 2.4
     */
    public static long getFragmentInMilliseconds(Date date, int fragment) {
        return getFragment(date, fragment, Calendar.MILLISECOND);
    }

    /**
     * 返回日期在指定片段内的秒数，所有大于该片段的日期字段将被忽略。
     *
     * <p>查询任意日期的秒数只会返回当前分钟内的秒数（结果为 0-59）。本方法可获取任意片段的秒数。 例如计算今天已过去的秒数时，fragment 为 {@link
     * Calendar#DATE} 或 {@link Calendar#DAY_OF_YEAR}，结果为过去所有小时和分钟的秒之和。
     *
     * <p>合法的 fragment 为：{@link Calendar#YEAR}、{@link Calendar#MONTH}、 {@link Calendar#DAY_OF_YEAR}
     * 与 {@link Calendar#DATE}、{@link Calendar#HOUR_OF_DAY}、 {@link Calendar#MINUTE}、{@link
     * Calendar#SECOND} 与 {@link Calendar#MILLISECOND}。 小于等于 SECOND 的片段返回 0。
     *
     * <ul>
     *   <li>2008-01-01 07:15:10.538，fragment 为 Calendar.MINUTE，返回 10 （等价于已废弃的 date.getSeconds()）
     *   <li>2008-01-06 07:15:10.538，fragment 为 Calendar.MINUTE，返回 10 （等价于已废弃的 date.getSeconds()）
     *   <li>2008-01-06 07:15:10.538，fragment 为 Calendar.DAY_OF_YEAR，返回 26110（7*3600 + 15*60 + 10）
     *   <li>2008-01-16 07:15:10.538，fragment 为 Calendar.MILLISECOND，返回 0（毫秒无法再细分）
     * </ul>
     *
     * <p>在默认时区（{@link TimeZone#getDefault()}）下进行计算。
     *
     * @param date 要处理的日期，不可为 null
     * @param fragment 用于计算的 {@link Calendar} 字段
     * @return 日期在指定片段内的秒数
     * @throws IllegalArgumentException 如果日期为 null 或 fragment 不受支持
     * @since 2.4
     */
    public static long getFragmentInSeconds(Date date, int fragment) {
        return getFragment(date, fragment, Calendar.SECOND);
    }

    /**
     * 返回日期在指定片段内的分钟数，所有大于该片段的日期字段将被忽略。
     *
     * <p>查询任意日期的分钟数只会返回当前小时内的分钟数（结果为 0-59）。本方法可获取任意片段的 分钟数。例如计算本月已过去的分钟数时，fragment 为 {@link
     * Calendar#MONTH}，结果为过去所有 天和小时的分钟之和。
     *
     * <p>合法的 fragment 为：{@link Calendar#YEAR}、{@link Calendar#MONTH}、 {@link Calendar#DAY_OF_YEAR}
     * 与 {@link Calendar#DATE}、{@link Calendar#HOUR_OF_DAY}、 {@link Calendar#MINUTE}、{@link
     * Calendar#SECOND} 与 {@link Calendar#MILLISECOND}。 小于等于 MINUTE 的片段返回 0。
     *
     * <ul>
     *   <li>2008-01-01 07:15:10.538，fragment 为 Calendar.HOUR_OF_DAY，返回 15 （等价于已废弃的
     *       date.getMinutes()）
     *   <li>2008-01-06 07:15:10.538，fragment 为 Calendar.HOUR_OF_DAY，返回 15 （等价于已废弃的
     *       date.getMinutes()）
     *   <li>2008-01-01 07:15:10.538，fragment 为 Calendar.MONTH，返回 15
     *   <li>2008-01-06 07:15:10.538，fragment 为 Calendar.MONTH，返回 435（7*60 + 15）
     *   <li>2008-01-16 07:15:10.538，fragment 为 Calendar.MILLISECOND，返回 0（毫秒无法再细分）
     * </ul>
     *
     * <p>在默认时区（{@link TimeZone#getDefault()}）下进行计算。
     *
     * @param date 要处理的日期，不可为 null
     * @param fragment 用于计算的 {@link Calendar} 字段
     * @return 日期在指定片段内的分钟数
     * @throws IllegalArgumentException 如果日期为 null 或 fragment 不受支持
     * @since 2.4
     */
    public static long getFragmentInMinutes(Date date, int fragment) {
        return getFragment(date, fragment, Calendar.MINUTE);
    }

    /**
     * 返回日期在指定片段内的小时数，所有大于该片段的日期字段将被忽略。
     *
     * <p>查询任意日期的小时数只会返回当前天内的小时数（结果为 0-23）。本方法可获取任意片段的小时数。 例如计算本月已过去的小时数时，fragment 为 {@link
     * Calendar#MONTH}，结果为过去所有天的小时之和。
     *
     * <p>合法的 fragment 为：{@link Calendar#YEAR}、{@link Calendar#MONTH}、 {@link Calendar#DAY_OF_YEAR}
     * 与 {@link Calendar#DATE}、{@link Calendar#HOUR_OF_DAY}、 {@link Calendar#MINUTE}、{@link
     * Calendar#SECOND} 与 {@link Calendar#MILLISECOND}。 小于等于 HOUR 的片段返回 0。
     *
     * <ul>
     *   <li>2008-01-01 07:15:10.538，fragment 为 Calendar.DAY_OF_YEAR，返回 7 （等价于已废弃的 date.getHours()）
     *   <li>2008-01-06 07:15:10.538，fragment 为 Calendar.DAY_OF_YEAR，返回 7 （等价于已废弃的 date.getHours()）
     *   <li>2008-01-01 07:15:10.538，fragment 为 Calendar.MONTH，返回 7
     *   <li>2008-01-06 07:15:10.538，fragment 为 Calendar.MONTH，返回 127（5*24 + 7）
     *   <li>2008-01-16 07:15:10.538，fragment 为 Calendar.MILLISECOND，返回 0（毫秒无法再细分）
     * </ul>
     *
     * <p>在默认时区（{@link TimeZone#getDefault()}）下进行计算。
     *
     * @param date 要处理的日期，不可为 null
     * @param fragment 用于计算的 {@link Calendar} 字段
     * @return 日期在指定片段内的小时数
     * @throws IllegalArgumentException 如果日期为 null 或 fragment 不受支持
     * @since 2.4
     */
    public static long getFragmentInHours(Date date, int fragment) {
        return getFragment(date, fragment, Calendar.HOUR_OF_DAY);
    }

    /**
     * 返回日期在指定片段内的天数，所有大于该片段的日期字段将被忽略。
     *
     * <p>查询任意日期的天数只会返回当前月内的天数（结果为 1-31）。本方法可获取任意片段的天数。 例如计算今年已过去的天数时，fragment 为 {@link
     * Calendar#YEAR}，结果为过去所有月的天数之和。
     *
     * <p>合法的 fragment 为：{@link Calendar#YEAR}、{@link Calendar#MONTH}、 {@link Calendar#DAY_OF_YEAR}
     * 与 {@link Calendar#DATE}、{@link Calendar#HOUR_OF_DAY}、 {@link Calendar#MINUTE}、{@link
     * Calendar#SECOND} 与 {@link Calendar#MILLISECOND}。 小于等于 DAY 的片段返回 0。
     *
     * <ul>
     *   <li>2008-01-28，fragment 为 Calendar.MONTH，返回 28 （等价于已废弃的 date.getDay()）
     *   <li>2008-02-28，fragment 为 Calendar.MONTH，返回 28 （等价于已废弃的 date.getDay()）
     *   <li>2008-01-28，fragment 为 Calendar.YEAR，返回 28
     *   <li>2008-02-28，fragment 为 Calendar.YEAR，返回 59
     *   <li>2008-01-28，fragment 为 Calendar.MILLISECOND，返回 0（毫秒无法再细分）
     * </ul>
     *
     * <p>在默认时区（{@link TimeZone#getDefault()}）下进行计算。
     *
     * @param date 要处理的日期，不可为 null
     * @param fragment 用于计算的 {@link Calendar} 字段
     * @return 日期在指定片段内的天数
     * @throws IllegalArgumentException 如果日期为 null 或 fragment 不受支持
     * @since 2.4
     */
    public static long getFragmentInDays(Date date, int fragment) {
        return getFragment(date, fragment, Calendar.DAY_OF_YEAR);
    }

    /**
     * 返回日历在指定片段内的毫秒数，所有大于该片段的日期字段将被忽略。
     *
     * <p>查询任意日历的毫秒数只会返回当前秒内的毫秒数（结果为 0-999）。本方法可获取任意片段的 毫秒数。例如计算今天已过去的毫秒数时，fragment 为 {@link
     * Calendar#DATE} 或 {@link Calendar#DAY_OF_YEAR}，结果为过去所有小时、分钟和秒的毫秒之和。
     *
     * <p>合法的 fragment 为：{@link Calendar#YEAR}、{@link Calendar#MONTH}、 {@link Calendar#DAY_OF_YEAR}
     * 与 {@link Calendar#DATE}、{@link Calendar#HOUR_OF_DAY}、 {@link Calendar#MINUTE}、{@link
     * Calendar#SECOND} 与 {@link Calendar#MILLISECOND}。 小于等于 SECOND 的片段返回 0。
     *
     * <ul>
     *   <li>2008-01-01 07:15:10.538，fragment 为 Calendar.SECOND，返回 538 （等价于
     *       calendar.get(Calendar.MILLISECOND)）
     *   <li>2008-01-06 07:15:10.538，fragment 为 Calendar.SECOND，返回 538 （等价于
     *       calendar.get(Calendar.MILLISECOND)）
     *   <li>2008-01-06 07:15:10.538，fragment 为 Calendar.MINUTE，返回 10538（10*1000 + 538）
     *   <li>2008-01-16 07:15:10.538，fragment 为 Calendar.MILLISECOND，返回 0（毫秒无法再细分）
     * </ul>
     *
     * <p>计算时保留原日历对象的时区。
     *
     * @param calendar 要处理的日历，不可为 null
     * @param fragment 用于计算的 {@link Calendar} 字段
     * @return 日历在指定片段内的毫秒数
     * @throws IllegalArgumentException 如果日历为 null 或 fragment 不受支持
     * @since 2.4
     */
    public static long getFragmentInMilliseconds(Calendar calendar, int fragment) {
        return getFragment(calendar, fragment, Calendar.MILLISECOND);
    }

    /**
     * 返回日历在指定片段内的秒数，所有大于该片段的日期字段将被忽略。
     *
     * <p>查询任意日历的秒数只会返回当前分钟内的秒数（结果为 0-59）。本方法可获取任意片段的秒数。 例如计算今天已过去的秒数时，fragment 为 {@link
     * Calendar#DATE} 或 {@link Calendar#DAY_OF_YEAR}，结果为过去所有小时和分钟的秒之和。
     *
     * <p>合法的 fragment 为：{@link Calendar#YEAR}、{@link Calendar#MONTH}、 {@link Calendar#DAY_OF_YEAR}
     * 与 {@link Calendar#DATE}、{@link Calendar#HOUR_OF_DAY}、 {@link Calendar#MINUTE}、{@link
     * Calendar#SECOND} 与 {@link Calendar#MILLISECOND}。 小于等于 SECOND 的片段返回 0。
     *
     * <ul>
     *   <li>2008-01-01 07:15:10.538，fragment 为 Calendar.MINUTE，返回 10 （等价于
     *       calendar.get(Calendar.SECOND)）
     *   <li>2008-01-06 07:15:10.538，fragment 为 Calendar.MINUTE，返回 10 （等价于
     *       calendar.get(Calendar.SECOND)）
     *   <li>2008-01-06 07:15:10.538，fragment 为 Calendar.DAY_OF_YEAR，返回 26110（7*3600 + 15*60 + 10）
     *   <li>2008-01-16 07:15:10.538，fragment 为 Calendar.MILLISECOND，返回 0（毫秒无法再细分）
     * </ul>
     *
     * <p>计算时保留原日历对象的时区。
     *
     * @param calendar 要处理的日历，不可为 null
     * @param fragment 用于计算的 {@link Calendar} 字段
     * @return 日历在指定片段内的秒数
     * @throws IllegalArgumentException 如果日历为 null 或 fragment 不受支持
     * @since 2.4
     */
    public static long getFragmentInSeconds(Calendar calendar, int fragment) {
        return getFragment(calendar, fragment, Calendar.SECOND);
    }

    /**
     * 返回日历在指定片段内的分钟数，所有大于该片段的日期字段将被忽略。
     *
     * <p>查询任意日历的分钟数只会返回当前小时内的分钟数（结果为 0-59）。本方法可获取任意片段的 分钟数。例如计算本月已过去的分钟数时，fragment 为 {@link
     * Calendar#MONTH}，结果为过去所有 天和小时的分钟之和。
     *
     * <p>合法的 fragment 为：{@link Calendar#YEAR}、{@link Calendar#MONTH}、 {@link Calendar#DAY_OF_YEAR}
     * 与 {@link Calendar#DATE}、{@link Calendar#HOUR_OF_DAY}、 {@link Calendar#MINUTE}、{@link
     * Calendar#SECOND} 与 {@link Calendar#MILLISECOND}。 小于等于 MINUTE 的片段返回 0。
     *
     * <ul>
     *   <li>2008-01-01 07:15:10.538，fragment 为 Calendar.HOUR_OF_DAY，返回 15 （等价于
     *       calendar.get(Calendar.MINUTE)）
     *   <li>2008-01-06 07:15:10.538，fragment 为 Calendar.HOUR_OF_DAY，返回 15 （等价于
     *       calendar.get(Calendar.MINUTE)）
     *   <li>2008-01-01 07:15:10.538，fragment 为 Calendar.MONTH，返回 15
     *   <li>2008-01-06 07:15:10.538，fragment 为 Calendar.MONTH，返回 435（7*60 + 15）
     *   <li>2008-01-16 07:15:10.538，fragment 为 Calendar.MILLISECOND，返回 0（毫秒无法再细分）
     * </ul>
     *
     * <p>计算时保留原日历对象的时区。
     *
     * @param calendar 要处理的日历，不可为 null
     * @param fragment 用于计算的 {@link Calendar} 字段
     * @return 日历在指定片段内的分钟数
     * @throws IllegalArgumentException 如果日历为 null 或 fragment 不受支持
     * @since 2.4
     */
    public static long getFragmentInMinutes(Calendar calendar, int fragment) {
        return getFragment(calendar, fragment, Calendar.MINUTE);
    }

    /**
     * 返回日历在指定片段内的小时数，所有大于该片段的日期字段将被忽略。
     *
     * <p>查询任意日历的小时数只会返回当前天内的小时数（结果为 0-23）。本方法可获取任意片段的小时数。 例如计算本月已过去的小时数时，fragment 为 {@link
     * Calendar#MONTH}，结果为过去所有天的小时之和。
     *
     * <p>合法的 fragment 为：{@link Calendar#YEAR}、{@link Calendar#MONTH}、 {@link Calendar#DAY_OF_YEAR}
     * 与 {@link Calendar#DATE}、{@link Calendar#HOUR_OF_DAY}、 {@link Calendar#MINUTE}、{@link
     * Calendar#SECOND} 与 {@link Calendar#MILLISECOND}。 小于等于 HOUR 的片段返回 0。
     *
     * <ul>
     *   <li>2008-01-01 07:15:10.538，fragment 为 Calendar.DAY_OF_YEAR，返回 7 （等价于
     *       calendar.get(Calendar.HOUR_OF_DAY)）
     *   <li>2008-01-06 07:15:10.538，fragment 为 Calendar.DAY_OF_YEAR，返回 7 （等价于
     *       calendar.get(Calendar.HOUR_OF_DAY)）
     *   <li>2008-01-01 07:15:10.538，fragment 为 Calendar.MONTH，返回 7
     *   <li>2008-01-06 07:15:10.538，fragment 为 Calendar.MONTH，返回 127（5*24 + 7）
     *   <li>2008-01-16 07:15:10.538，fragment 为 Calendar.MILLISECOND，返回 0（毫秒无法再细分）
     * </ul>
     *
     * <p>计算时保留原日历对象的时区。
     *
     * @param calendar 要处理的日历，不可为 null
     * @param fragment 用于计算的 {@link Calendar} 字段
     * @return 日历在指定片段内的小时数
     * @throws IllegalArgumentException 如果日历为 null 或 fragment 不受支持
     * @since 2.4
     */
    public static long getFragmentInHours(Calendar calendar, int fragment) {
        return getFragment(calendar, fragment, Calendar.HOUR_OF_DAY);
    }

    /**
     * 返回日历在指定片段内的天数，所有大于该片段的日期字段将被忽略。
     *
     * <p>查询任意日历的天数只会返回当前月内的天数（结果为 1-31）。本方法可获取任意片段的天数。 例如计算今年已过去的天数时，fragment 为 {@link
     * Calendar#YEAR}，结果为过去所有月的天数之和。
     *
     * <p>合法的 fragment 为：{@link Calendar#YEAR}、{@link Calendar#MONTH}、 {@link Calendar#DAY_OF_YEAR}
     * 与 {@link Calendar#DATE}、{@link Calendar#HOUR_OF_DAY}、 {@link Calendar#MINUTE}、{@link
     * Calendar#SECOND} 与 {@link Calendar#MILLISECOND}。 小于等于 DAY 的片段返回 0。
     *
     * <ul>
     *   <li>2008-01-28，fragment 为 Calendar.MONTH，返回 28 （等价于 calendar.get(Calendar.DAY_OF_MONTH)）
     *   <li>2008-02-28，fragment 为 Calendar.MONTH，返回 28 （等价于 calendar.get(Calendar.DAY_OF_MONTH)）
     *   <li>2008-01-28，fragment 为 Calendar.YEAR，返回 28 （等价于 calendar.get(Calendar.DAY_OF_YEAR)）
     *   <li>2008-02-28，fragment 为 Calendar.YEAR，返回 59 （等价于 calendar.get(Calendar.DAY_OF_YEAR)）
     *   <li>2008-01-28，fragment 为 Calendar.MILLISECOND，返回 0（毫秒无法再细分）
     * </ul>
     *
     * <p>计算时保留原日历对象的时区。
     *
     * @param calendar 要处理的日历，不可为 null
     * @param fragment 用于计算的 {@link Calendar} 字段
     * @return 日历在指定片段内的天数
     * @throws IllegalArgumentException 如果日历为 null 或 fragment 不受支持
     * @since 2.4
     */
    public static long getFragmentInDays(Calendar calendar, int fragment) {
        return getFragment(calendar, fragment, Calendar.DAY_OF_YEAR);
    }

    /**
     * 按指定单位计算日期在片段内的数量的 Date 版本实现。
     *
     * @param date 要处理的日期，不可为 null
     * @param fragment 用于计算的 {@link Calendar} 字段
     * @param unit 定义单位的 {@link Calendar} 字段
     * @return 日期在指定片段内的单位数量
     * @throws IllegalArgumentException 如果日期为 null 或 fragment 不受支持
     * @since 2.4
     */
    private static long getFragment(Date date, int fragment, int unit) {
        if (date == null) {
            throw new IllegalArgumentException("The date must not be null");
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return getFragment(calendar, fragment, unit);
    }

    /**
     * 按指定单位计算日历在片段内的数量的 Calendar 版本实现。
     *
     * @param calendar 要处理的日历，不可为 null
     * @param fragment 用于计算的 {@link Calendar} 字段
     * @param unit 定义单位的 {@link Calendar} 字段
     * @return 日历在指定片段内的单位数量
     * @throws IllegalArgumentException 如果日历为 null 或 fragment 不受支持
     * @since 2.4
     */
    private static long getFragment(Calendar calendar, int fragment, int unit) {
        if (calendar == null) {
            throw new IllegalArgumentException("The date must not be null");
        }
        long millisPerUnit = getMillisPerUnit(unit);
        long result = 0;

        // 大于天的片段需要拆解为天
        switch (fragment) {
            case Calendar.YEAR:
                result += (calendar.get(Calendar.DAY_OF_YEAR) * MILLIS_PER_DAY) / millisPerUnit;
                break;
            case Calendar.MONTH:
                result += (calendar.get(Calendar.DAY_OF_MONTH) * MILLIS_PER_DAY) / millisPerUnit;
                break;
        }

        switch (fragment) {
            // 这些情况下天数已计算过
            case Calendar.YEAR:
            case Calendar.MONTH:

            // 其余合法的情况
            case Calendar.DAY_OF_YEAR:
            case Calendar.DATE:
                result += (calendar.get(Calendar.HOUR_OF_DAY) * MILLIS_PER_HOUR) / millisPerUnit;
            // $FALL-THROUGH$
            case Calendar.HOUR_OF_DAY:
                result += (calendar.get(Calendar.MINUTE) * MILLIS_PER_MINUTE) / millisPerUnit;
            // $FALL-THROUGH$
            case Calendar.MINUTE:
                result += (calendar.get(Calendar.SECOND) * MILLIS_PER_SECOND) / millisPerUnit;
            // $FALL-THROUGH$
            case Calendar.SECOND:
                result += (calendar.get(Calendar.MILLISECOND) * 1) / millisPerUnit;
                break;
            case Calendar.MILLISECOND:
                break; // 永远用不到
            default:
                throw new IllegalArgumentException(
                        "The fragment " + fragment + " is not supported");
        }
        return result;
    }

    /**
     * 判断两个日历是否在指定最高有效字段范围内相等。
     *
     * @param cal1 第一个日历，不可为 null
     * @param cal2 第二个日历，不可为 null
     * @param field 来自 {@link Calendar} 的字段
     * @return 相等返回 <code>true</code>，否则返回 <code>false</code>
     * @throws IllegalArgumentException 如果任一参数为 null
     * @see #truncate(Calendar, int)
     * @see #truncatedEquals(Date, Date, int)
     * @since 2.6
     */
    public static boolean truncatedEquals(Calendar cal1, Calendar cal2, int field) {
        return truncatedCompareTo(cal1, cal2, field) == 0;
    }

    /**
     * 判断两个日期是否在指定最高有效字段范围内相等。
     *
     * @param date1 第一个日期，不可为 null
     * @param date2 第二个日期，不可为 null
     * @param field 来自 {@link Calendar} 的字段
     * @return 相等返回 <code>true</code>，否则返回 <code>false</code>
     * @throws IllegalArgumentException 如果任一参数为 null
     * @see #truncate(Date, int)
     * @see #truncatedEquals(Calendar, Calendar, int)
     * @since 2.6
     */
    public static boolean truncatedEquals(Date date1, Date date2, int field) {
        return truncatedCompareTo(date1, date2, field) == 0;
    }

    /**
     * 比较两个日历在指定最高有效字段范围内的顺序。
     *
     * @param cal1 第一个日历，不可为 null
     * @param cal2 第二个日历，不可为 null
     * @param field 来自 {@link Calendar} 的字段
     * @return 负整数、零或正整数，分别表示第一个日历小于、等于或大于第二个日历
     * @throws IllegalArgumentException 如果任一参数为 null
     * @see #truncate(Calendar, int)
     * @see #truncatedCompareTo(Date, Date, int)
     * @since 2.6
     */
    public static int truncatedCompareTo(Calendar cal1, Calendar cal2, int field) {
        Calendar truncatedCal1 = truncate(cal1, field);
        Calendar truncatedCal2 = truncate(cal2, field);
        return truncatedCal1.getTime().compareTo(truncatedCal2.getTime());
    }

    /**
     * 比较两个日期在指定最高有效字段范围内的顺序。
     *
     * @param date1 第一个日期，不可为 null
     * @param date2 第二个日期，不可为 null
     * @param field 来自 {@link Calendar} 的字段
     * @return 负整数、零或正整数，分别表示第一个日期小于、等于或大于第二个日期
     * @throws IllegalArgumentException 如果任一参数为 null
     * @see #truncate(Date, int)
     * @see #truncatedCompareTo(Calendar, Calendar, int)
     * @since 2.6
     */
    public static int truncatedCompareTo(Date date1, Date date2, int field) {
        Date truncatedDate1 = truncate(date1, field);
        Date truncatedDate2 = truncate(date2, field);
        return truncatedDate1.compareTo(truncatedDate2);
    }

    /**
     * 返回日期字段的毫秒数（当该字段是常量值时）。
     *
     * @param unit 作为片段合法单位的 {@link Calendar} 字段
     * @return 毫秒数
     * @throws IllegalArgumentException 如果单位不能用毫秒表示
     * @since 2.4
     */
    private static long getMillisPerUnit(int unit) {
        long result = Long.MAX_VALUE;
        switch (unit) {
            case Calendar.DAY_OF_YEAR:
            case Calendar.DATE:
                result = MILLIS_PER_DAY;
                break;
            case Calendar.HOUR_OF_DAY:
                result = MILLIS_PER_HOUR;
                break;
            case Calendar.MINUTE:
                result = MILLIS_PER_MINUTE;
                break;
            case Calendar.SECOND:
                result = MILLIS_PER_SECOND;
                break;
            case Calendar.MILLISECOND:
                result = 1;
                break;
            default:
                throw new IllegalArgumentException(
                        "The unit " + unit + " cannot be represented is milleseconds");
        }
        return result;
    }

    /** 日期迭代器。 */
    static class DateIterator implements Iterator {

        private final Calendar endFinal;

        private final Calendar spot;

        /**
         * 构造一个日期从一个日期到另一个日期的迭代器。
         *
         * @param startFinal 开始日期（含）
         * @param endFinal 结束日期（不含）
         */
        DateIterator(Calendar startFinal, Calendar endFinal) {
            super();
            this.endFinal = endFinal;
            spot = startFinal;
            spot.add(Calendar.DATE, -1);
        }

        /**
         * 迭代器是否尚未到达结束日期？
         *
         * @return 如果迭代器尚未到达结束日期则返回 <code>true</code>
         */
        public boolean hasNext() {
            return spot.before(endFinal);
        }

        /**
         * 返回迭代中的下一个日历。
         *
         * @return 下一个日期的 Object 日历
         */
        public Object next() {
            if (spot.equals(endFinal)) {
                throw new NoSuchElementException();
            }
            spot.add(Calendar.DATE, 1);
            return spot.clone();
        }

        /**
         * 总是抛出 UnsupportedOperationException。
         *
         * @throws UnsupportedOperationException
         * @see java.util.Iterator#remove()
         */
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    // 日期格式化
    // ----------------------------------------------------------------------------------------------------------------------------------

    /**
     * ISO8601 格式的日期时间格式化器（不带时区）。使用模式 <tt>yyyy-MM-dd'T'HH:mm:ss</tt>。
     *
     * <p>该常量为公共 API，供需要 ISO 日期时间文本（如接口报文、日志字段）的场景复用。 <b>时区语义：</b>未指定时区，格式化时使用 JVM 默认时区（{@link
     * TimeZone#getDefault()}）； 若需 UTC 输出请使用 {@link #ISO_DATETIME_TIME_ZONE_FORMAT} 或自行指定时区。
     */
    public static final GuFastDateFormat ISO_DATETIME_FORMAT =
            GuFastDateFormat.getInstance("yyyy-MM-dd'T'HH:mm:ss");

    /**
     * ISO8601 格式的日期时间格式化器（带时区）。使用模式 <tt>yyyy-MM-dd'T'HH:mm:ssZZ</tt>。
     *
     * <p>该常量为公共 API，供需要带时区偏移的 ISO 日期时间文本（如 HTTP/JSON 时间戳）的场景复用。 <b>时区语义：</b>未指定时区，格式化时使用 JVM
     * 默认时区（{@link TimeZone#getDefault()}）。
     */
    public static final GuFastDateFormat ISO_DATETIME_TIME_ZONE_FORMAT =
            GuFastDateFormat.getInstance("yyyy-MM-dd'T'HH:mm:ssZZ");

    /**
     * ISO8601 格式的日期格式化器（不带时区）。使用模式 <tt>yyyy-MM-dd</tt>。
     *
     * <p>该常量为公共 API，供需要 ISO 日期文本（如按天分桶的键）的场景复用。 <b>时区语义：</b>未指定时区，格式化时使用 JVM 默认时区（{@link
     * TimeZone#getDefault()}）。
     */
    public static final GuFastDateFormat ISO_DATE_FORMAT =
            GuFastDateFormat.getInstance("yyyy-MM-dd");

    /**
     * ISO8601 风格的日期格式化器（带时区）。使用模式 <tt>yyyy-MM-ddZZ</tt>。
     *
     * <p>该模式不符合正式的 ISO8601 规范（标准不允许只有时区而没有时间）。 <b>时区语义：</b>未指定时区，格式化时使用 JVM 默认时区（{@link
     * TimeZone#getDefault()}）。
     */
    public static final GuFastDateFormat ISO_DATE_TIME_ZONE_FORMAT =
            GuFastDateFormat.getInstance("yyyy-MM-ddZZ");

    /**
     * ISO8601 格式的时间格式化器（不带时区）。使用模式 <tt>'T'HH:mm:ss</tt>。
     *
     * <p><b>时区语义：</b>未指定时区，格式化时使用 JVM 默认时区（{@link TimeZone#getDefault()}）。
     */
    public static final GuFastDateFormat ISO_TIME_FORMAT =
            GuFastDateFormat.getInstance("'T'HH:mm:ss");

    /**
     * ISO8601 格式的时间格式化器（带时区）。使用模式 <tt>'T'HH:mm:ssZZ</tt>。
     *
     * <p><b>时区语义：</b>未指定时区，格式化时使用 JVM 默认时区（{@link TimeZone#getDefault()}）。
     */
    public static final GuFastDateFormat ISO_TIME_TIME_ZONE_FORMAT =
            GuFastDateFormat.getInstance("'T'HH:mm:ssZZ");

    /**
     * ISO8601 风格的时间格式化器（不带时区）。使用模式 <tt>HH:mm:ss</tt>。
     *
     * <p>该模式不符合正式的 ISO8601 规范（标准要求时间带 'T' 前缀）。 <b>时区语义：</b>未指定时区，格式化时使用 JVM 默认时区（{@link
     * TimeZone#getDefault()}）。
     */
    public static final GuFastDateFormat ISO_TIME_NO_T_FORMAT =
            GuFastDateFormat.getInstance("HH:mm:ss");

    /**
     * ISO8601 风格的时间格式化器（带时区）。使用模式 <tt>HH:mm:ssZZ</tt>。
     *
     * <p>该模式不符合正式的 ISO8601 规范（标准要求时间带 'T' 前缀）。 <b>时区语义：</b>未指定时区，格式化时使用 JVM 默认时区（{@link
     * TimeZone#getDefault()}）。
     */
    public static final GuFastDateFormat ISO_TIME_NO_T_TIME_ZONE_FORMAT =
            GuFastDateFormat.getInstance("HH:mm:ssZZ");

    /**
     * SMTP（以及其它类似协议）日期头格式化器。使用模式 <tt>EEE, dd MMM yyyy HH:mm:ss Z</tt>， 语言环境为 {@link Locale#US}。
     *
     * <p>该常量为公共 API，供邮件协议头等需要英文星期/月份缩写的场景复用。 <b>时区语义：</b>未指定时区，格式化时使用 JVM 默认时区（{@link
     * TimeZone#getDefault()}）。
     */
    public static final GuFastDateFormat SMTP_DATETIME_FORMAT =
            GuFastDateFormat.getInstance("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US);

    /**
     * 按指定模式格式化毫秒时间戳，使用 UTC 时区。
     *
     * <p><b>时区语义：</b>固定使用 UTC（{@link #UTC_TIME_ZONE}），不随 JVM 默认时区变化； 与 {@link #parseDate(String,
     * String[], TimeZone)}（传入 {@link #UTC_TIME_ZONE}）可构成 往返一致。
     *
     * @param millis 以毫秒表示的要格式化的时间
     * @param pattern 格式化模式
     * @return 格式化后的日期字符串
     * @throws IllegalArgumentException 如果模式为 null 或非法
     */
    public static String formatUTC(long millis, String pattern) {
        return format(new Date(millis), pattern, GutilDate.UTC_TIME_ZONE, null);
    }

    /**
     * 按指定模式格式化日期，使用 UTC 时区。
     *
     * <p><b>时区语义：</b>固定使用 UTC（{@link #UTC_TIME_ZONE}），不随 JVM 默认时区变化； 与 {@link #parseDate(String,
     * String[], TimeZone)}（传入 {@link #UTC_TIME_ZONE}）可构成 往返一致。
     *
     * @param date 要格式化的日期，不可为 null
     * @param pattern 格式化模式
     * @return 格式化后的日期字符串
     * @throws IllegalArgumentException 如果日期为 null，或模式为 null/非法
     */
    public static String formatUTC(Date date, String pattern) {
        return format(date, pattern, GutilDate.UTC_TIME_ZONE, null);
    }

    /**
     * 按指定模式格式化毫秒时间戳，使用 UTC 时区与指定语言环境。
     *
     * <p><b>时区语义：</b>固定使用 UTC（{@link #UTC_TIME_ZONE}），不随 JVM 默认时区变化。
     *
     * @param millis 以毫秒表示的要格式化的时间
     * @param pattern 格式化模式
     * @param locale 使用的语言环境，可以为 <code>null</code>（null 表示 JVM 默认语言环境）； 当模式包含
     *     "MMM"（月份缩写）等文本元素时建议显式指定
     * @return 格式化后的日期字符串
     * @throws IllegalArgumentException 如果模式为 null 或非法
     */
    public static String formatUTC(long millis, String pattern, Locale locale) {
        return format(new Date(millis), pattern, GutilDate.UTC_TIME_ZONE, locale);
    }

    /**
     * 按指定模式格式化日期，使用 UTC 时区与指定语言环境。
     *
     * <p><b>时区语义：</b>固定使用 UTC（{@link #UTC_TIME_ZONE}），不随 JVM 默认时区变化。
     *
     * @param date 要格式化的日期，不可为 null
     * @param pattern 格式化模式
     * @param locale 使用的语言环境，可以为 <code>null</code>（null 表示 JVM 默认语言环境）； 当模式包含
     *     "MMM"（月份缩写）等文本元素时建议显式指定
     * @return 格式化后的日期字符串
     * @throws IllegalArgumentException 如果日期为 null，或模式为 null/非法
     */
    public static String formatUTC(Date date, String pattern, Locale locale) {
        return format(date, pattern, GutilDate.UTC_TIME_ZONE, locale);
    }

    /**
     * 按指定模式格式化毫秒时间戳。
     *
     * <p><b>时区语义：</b>使用 JVM 默认时区（{@link TimeZone#getDefault()}）与默认语言环境； 需要 UTC 时请使用 {@link
     * #formatUTC(long, String)}。
     *
     * @param millis 以毫秒表示的要格式化的时间
     * @param pattern 格式化模式
     * @return 格式化后的日期字符串
     * @throws IllegalArgumentException 如果模式为 null 或非法
     */
    public static String format(long millis, String pattern) {
        return format(new Date(millis), pattern, null, null);
    }

    /**
     * 按指定模式格式化日期。
     *
     * <p><b>时区语义：</b>使用 JVM 默认时区（{@link TimeZone#getDefault()}）与默认语言环境； 需要 UTC 时请使用 {@link
     * #formatUTC(Date, String)}。
     *
     * @param date 要格式化的日期，不可为 null
     * @param pattern 格式化模式
     * @return 格式化后的日期字符串
     * @throws IllegalArgumentException 如果日期为 null，或模式为 null/非法
     */
    public static String format(Date date, String pattern) {
        return format(date, pattern, null, null);
    }

    /**
     * 按指定模式格式化日历。
     *
     * <p><b>时区语义：</b>未指定时区，格式化时保留日历自身的时区；语言环境使用 JVM 默认语言环境。
     *
     * @param calendar 要格式化的日历
     * @param pattern 格式化模式
     * @return 格式化后的日历字符串
     * @throws IllegalArgumentException 如果日历为 null，或模式为 null/非法
     * @see GuFastDateFormat#format(Calendar)
     * @since 2.4
     */
    public static String format(Calendar calendar, String pattern) {
        return format(calendar, pattern, null, null);
    }

    /**
     * 按指定模式与指定时区格式化毫秒时间戳。
     *
     * <p><b>时区语义：</b>按指定的时区格式化；若 {@code timeZone} 为 null，则使用 JVM 默认时区 （{@link
     * TimeZone#getDefault()}）。需要 UTC 时请传入 {@link #UTC_TIME_ZONE}。
     *
     * @param millis 以毫秒表示的要格式化的时间
     * @param pattern 格式化模式
     * @param timeZone 使用的时区，可以为 <code>null</code>（null 表示 JVM 默认时区）
     * @return 格式化后的日期字符串
     * @throws IllegalArgumentException 如果模式为 null 或非法
     */
    public static String format(long millis, String pattern, TimeZone timeZone) {
        return format(new Date(millis), pattern, timeZone, null);
    }

    /**
     * 按指定模式与指定时区格式化日期。
     *
     * <p><b>时区语义：</b>按指定的时区格式化；若 {@code timeZone} 为 null，则使用 JVM 默认时区 （{@link
     * TimeZone#getDefault()}）。需要 UTC 时请传入 {@link #UTC_TIME_ZONE}。
     *
     * @param date 要格式化的日期，不可为 null
     * @param pattern 格式化模式
     * @param timeZone 使用的时区，可以为 <code>null</code>（null 表示 JVM 默认时区）
     * @return 格式化后的日期字符串
     * @throws IllegalArgumentException 如果日期为 null，或模式为 null/非法
     */
    public static String format(Date date, String pattern, TimeZone timeZone) {
        return format(date, pattern, timeZone, null);
    }

    /**
     * 按指定模式与指定时区格式化日历。
     *
     * <p><b>时区语义：</b>若 {@code timeZone} 非 null，则优先使用该时区；否则保留日历自身的时区。
     *
     * @param calendar 要格式化的日历
     * @param pattern 格式化模式
     * @param timeZone 使用的时区，可以为 <code>null</code>
     * @return 格式化后的日历字符串
     * @throws IllegalArgumentException 如果日历为 null，或模式为 null/非法
     * @see GuFastDateFormat#format(Calendar)
     * @since 2.4
     */
    public static String format(Calendar calendar, String pattern, TimeZone timeZone) {
        return format(calendar, pattern, timeZone, null);
    }

    /**
     * 按指定模式与指定语言环境格式化毫秒时间戳。
     *
     * <p><b>时区语义：</b>使用 JVM 默认时区（{@link TimeZone#getDefault()}）。
     *
     * @param millis 以毫秒表示的要格式化的时间
     * @param pattern 格式化模式
     * @param locale 使用的语言环境，可以为 <code>null</code>（null 表示 JVM 默认语言环境）； 当模式包含
     *     "MMM"（月份缩写）等文本元素时建议显式指定
     * @return 格式化后的日期字符串
     * @throws IllegalArgumentException 如果模式为 null 或非法
     */
    public static String format(long millis, String pattern, Locale locale) {
        return format(new Date(millis), pattern, null, locale);
    }

    /**
     * 按指定模式与指定语言环境格式化日期。
     *
     * <p><b>时区语义：</b>使用 JVM 默认时区（{@link TimeZone#getDefault()}）。
     *
     * @param date 要格式化的日期，不可为 null
     * @param pattern 格式化模式
     * @param locale 使用的语言环境，可以为 <code>null</code>（null 表示 JVM 默认语言环境）； 当模式包含
     *     "MMM"（月份缩写）等文本元素时建议显式指定
     * @return 格式化后的日期字符串
     * @throws IllegalArgumentException 如果日期为 null，或模式为 null/非法
     */
    public static String format(Date date, String pattern, Locale locale) {
        return format(date, pattern, null, locale);
    }

    /**
     * 按指定模式与指定语言环境格式化日历。
     *
     * <p><b>时区语义：</b>未指定时区，格式化时保留日历自身的时区。
     *
     * @param calendar 要格式化的日历
     * @param pattern 格式化模式
     * @param locale 使用的语言环境，可以为 <code>null</code>（null 表示 JVM 默认语言环境）
     * @return 格式化后的日历字符串
     * @throws IllegalArgumentException 如果日历为 null，或模式为 null/非法
     * @see GuFastDateFormat#format(Calendar)
     * @since 2.4
     */
    public static String format(Calendar calendar, String pattern, Locale locale) {
        return format(calendar, pattern, null, locale);
    }

    /**
     * 按指定模式、时区与语言环境格式化毫秒时间戳。
     *
     * <p><b>时区语义：</b>按指定的时区格式化；若 {@code timeZone} 为 null，则使用 JVM 默认时区 （{@link
     * TimeZone#getDefault()}）。需要 UTC 时请传入 {@link #UTC_TIME_ZONE}。
     *
     * @param millis 以毫秒表示的要格式化的时间
     * @param pattern 格式化模式
     * @param timeZone 使用的时区，可以为 <code>null</code>（null 表示 JVM 默认时区）
     * @param locale 使用的语言环境，可以为 <code>null</code>（null 表示 JVM 默认语言环境）
     * @return 格式化后的日期字符串
     * @throws IllegalArgumentException 如果模式为 null 或非法
     */
    public static String format(long millis, String pattern, TimeZone timeZone, Locale locale) {
        return format(new Date(millis), pattern, timeZone, locale);
    }

    /**
     * 按指定模式、时区与语言环境格式化日期。
     *
     * <p><b>时区语义：</b>按指定的时区格式化；若 {@code timeZone} 为 null，则使用 JVM 默认时区 （{@link
     * TimeZone#getDefault()}）。需要 UTC 时请传入 {@link #UTC_TIME_ZONE}。
     *
     * <p><b>性能说明：</b>本方法通过 {@link GuFastDateFormat#getInstance(String, TimeZone, Locale)}
     * 获取格式化器，该方法是 synchronized 的，但内部按（模式、时区、语言环境）缓存实例，重复调用 相同参数时命中缓存，无需重复解析模式。
     *
     * @param date 要格式化的日期，不可为 null
     * @param pattern 格式化模式，不可为 null
     * @param timeZone 使用的时区，可以为 <code>null</code>（null 表示 JVM 默认时区）
     * @param locale 使用的语言环境，可以为 <code>null</code>（null 表示 JVM 默认语言环境）
     * @return 格式化后的日期字符串
     * @throws IllegalArgumentException 如果日期或模式为 null，或模式非法
     * @see GuFastDateFormat#format(Date)
     */
    public static String format(Date date, String pattern, TimeZone timeZone, Locale locale) {
        if (date == null) {
            throw new IllegalArgumentException("The date must not be null");
        }
        if (pattern == null) {
            throw new IllegalArgumentException("The pattern must not be null");
        }
        GuFastDateFormat df = GuFastDateFormat.getInstance(pattern, timeZone, locale);
        return df.format(date);
    }

    /**
     * 按指定模式、时区与语言环境格式化日历。
     *
     * <p><b>时区语义：</b>若 {@code timeZone} 非 null，则优先使用该时区；否则保留日历自身的时区。
     *
     * <p><b>性能说明：</b>本方法通过 {@link GuFastDateFormat#getInstance(String, TimeZone, Locale)}
     * 获取格式化器，该方法是 synchronized 的，但内部按（模式、时区、语言环境）缓存实例，重复调用 相同参数时命中缓存，无需重复解析模式。
     *
     * @param calendar 要格式化的日历，不可为 null
     * @param pattern 格式化模式，不可为 null
     * @param timeZone 使用的时区，可以为 <code>null</code>
     * @param locale 使用的语言环境，可以为 <code>null</code>（null 表示 JVM 默认语言环境）
     * @return 格式化后的日历字符串
     * @throws IllegalArgumentException 如果日历或模式为 null，或模式非法
     * @see GuFastDateFormat#format(Calendar)
     * @since 2.4
     */
    public static String format(
            Calendar calendar, String pattern, TimeZone timeZone, Locale locale) {
        if (calendar == null) {
            throw new IllegalArgumentException("The calendar must not be null");
        }
        if (pattern == null) {
            throw new IllegalArgumentException("The pattern must not be null");
        }
        GuFastDateFormat df = GuFastDateFormat.getInstance(pattern, timeZone, locale);
        return df.format(calendar);
    }
}
