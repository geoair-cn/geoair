package cn.geoair.gtc.base.tool;



import java.util.Scanner;

import cn.geoair.gtc.base.util.GutilArray;
import cn.geoair.gtc.base.util.GutilAssert;
import cn.geoair.gtc.base.util.GutilChar;
import cn.geoair.gtc.base.util.GutilStr;

import static java.lang.System.err;
import static java.lang.System.out;

/**
 * 命令行（控制台）工具方法类<br>
 * 此类主要针对{@link System#out} 和 {@link System#err} 做封装。
 *
 */

public class GkConsole {

	private static final String TEMPLATE_VAR = "{}";

	// --------------------------------------------------------------------------------- Log

	/**
	 * 同 System.out.println()方法，打印控制台日志
	 */
	public static void log() {
		out.println();
	}

	/**
	 * 同 System.out.println()方法，打印控制台日志<br>
	 * 如果传入打印对象为{@link Throwable}对象，那么同时打印堆栈
	 *
	 * @param obj 要打印的对象
	 */
	public static void log(Object obj) {
		if (obj instanceof Throwable) {
			final Throwable e = (Throwable) obj;
			log(e, e.getMessage());
		} else {
			log(TEMPLATE_VAR, obj);
		}
	}

	/**
	 * 同 System.out.println()方法，打印控制台日志<br>
	 * 如果传入打印对象为{@link Throwable}对象，那么同时打印堆栈
	 *
	 * @param obj1      第一个要打印的对象
	 * @param otherObjs 其它要打印的对象
	 */
	public static void log(Object obj1, Object... otherObjs) {
		if (GutilArray.isEmpty(otherObjs)) {
			log(obj1);
		} else {
			log(buildTemplateSplitBySpace(otherObjs.length + 1), GutilArray.insert(otherObjs, 0, obj1));
		}
	}

	/**
	 * 同 System.out.println()方法，打印控制台日志<br>
	 * 当传入template无"{}"时，被认为非模板，直接打印多个参数以空格分隔
	 *
	 * @param template 文本模板，被替换的部分用 {} 表示
	 * @param values   值
	 */
	public static void log(String template, Object... values) {
		if (GutilArray.isEmpty(values) || GutilStr.contains(template, TEMPLATE_VAR)) {
			logInternal(template, values);
		} else {
			logInternal(buildTemplateSplitBySpace(values.length + 1), GutilArray.insert(values, 0, template));
		}
	}

	/**
	 * 同 System.out.println()方法，打印控制台日志
	 *
	 * @param t        异常对象
	 * @param template 文本模板，被替换的部分用 {} 表示
	 * @param values   值
	 */
	public static void log(Throwable t, String template, Object... values) {
		out.println(GutilStr.format(template, values));
		if (null != t) {
			t.printStackTrace();
			out.flush();
		}
	}

	/**
	 * 同 System.out.println()方法，打印控制台日志
	 *
	 * @param template 文本模板，被替换的部分用 {} 表示
	 * @param values   值
	 */
	private static void logInternal(String template, Object... values) {
		log(null, template, values);
	}

	// --------------------------------------------------------------------------------- print

	/**
	 * 打印表格到控制台
	 *
	 * @param consoleTable 控制台表格
	 */
	public static void table(GkConsoleTable consoleTable) {
		print(consoleTable.toString());
	}

	/**
	 * 同 System.out.print()方法，打印控制台日志
	 *
	 * @param obj 要打印的对象
	 */
	public static void print(Object obj) {
		print(TEMPLATE_VAR, obj);
	}

	/**
	 * 同 System.out.println()方法，打印控制台日志<br>
	 * 如果传入打印对象为{@link Throwable}对象，那么同时打印堆栈
	 *
	 * @param obj1      第一个要打印的对象
	 * @param otherObjs 其它要打印的对象
	 */
	public static void print(Object obj1, Object... otherObjs) {
		if (GutilArray.isEmpty(otherObjs)) {
			print(obj1);
		} else {
			print(buildTemplateSplitBySpace(otherObjs.length + 1), GutilArray.insert(otherObjs, 0, obj1));
		}
	}

	/**
	 * 同 System.out.print()方法，打印控制台日志
	 *
	 * @param template 文本模板，被替换的部分用 {} 表示
	 * @param values   值
	 */
	public static void print(String template, Object... values) {
		if (GutilArray.isEmpty(values) || GutilStr.contains(template, TEMPLATE_VAR)) {
			printInternal(template, values);
		} else {
			printInternal(buildTemplateSplitBySpace(values.length + 1), GutilArray.insert(values, 0, template));
		}
	}

	/**
	 * 打印进度条
	 *
	 * @param showChar 进度条提示字符，例如“#”
	 * @param len      打印长度
	 */
	public static void printProgress(char showChar, int len) {
		print("{}{}", GutilChar.CR, GutilStr.repeat(showChar, len));
	}

	/**
	 * 打印进度条
	 *
	 * @param showChar 进度条提示字符，例如“#”
	 * @param totalLen 总长度
	 * @param rate     总长度所占比取值0~1
	 */
	public static void printProgress(char showChar, int totalLen, double rate) {
		GutilAssert.isTrue(rate >= 0 && rate <= 1, "Rate must between 0 and 1 (both include)");
		printProgress(showChar, (int) (totalLen * rate));
	}

	/**
	 * 同 System.out.println()方法，打印控制台日志
	 *
	 * @param template 文本模板，被替换的部分用 {} 表示
	 * @param values   值
	 */
	private static void printInternal(String template, Object... values) {
		out.print(GutilStr.format(template, values));
	}

	// --------------------------------------------------------------------------------- Error

	/**
	 * 同 System.err.println()方法，打印控制台日志
	 */
	public static void error() {
		err.println();
	}

	/**
	 * 同 System.err.println()方法，打印控制台日志
	 *
	 * @param obj 要打印的对象
	 */
	public static void error(Object obj) {
		if (obj instanceof Throwable) {
			Throwable e = (Throwable) obj;
			error(e, e.getMessage());
		} else {
			error(TEMPLATE_VAR, obj);
		}
	}

	/**
	 * 同 System.error.println()方法，打印控制台日志<br>
	 * 如果传入打印对象为{@link Throwable}对象，那么同时打印堆栈
	 *
	 * @param obj1      第一个要打印的对象
	 * @param otherObjs 其它要打印的对象
	 */
	public static void error(Object obj1, Object... otherObjs) {
		if (GutilArray.isEmpty(otherObjs)) {
			error(obj1);
		} else {
			error(buildTemplateSplitBySpace(otherObjs.length + 1), GutilArray.insert(otherObjs, 0, obj1));
		}
	}

	/**
	 * 同 System.err.println()方法，打印控制台日志
	 *
	 * @param template 文本模板，被替换的部分用 {} 表示
	 * @param values   值
	 */
	public static void error(String template, Object... values) {
		if (GutilArray.isEmpty(values) || GutilStr.contains(template, TEMPLATE_VAR)) {
			errorInternal(template, values);
		} else {
			errorInternal(buildTemplateSplitBySpace(values.length + 1), GutilArray.insert(values, 0, template));
		}
	}

	/**
	 * 同 System.err.println()方法，打印控制台日志
	 *
	 * @param t        异常对象
	 * @param template 文本模板，被替换的部分用 {} 表示
	 * @param values   值
	 */
	public static void error(Throwable t, String template, Object... values) {
		err.println(GutilStr.format(template, values));
		if (null != t) {
			t.printStackTrace(err);
			err.flush();
		}
	}

	/**
	 * 同 System.err.println()方法，打印控制台日志
	 *
	 * @param template 文本模板，被替换的部分用 {} 表示
	 * @param values   值
	 */
	private static void errorInternal(String template, Object... values) {
		error(null, template, values);
	}

	// --------------------------------------------------------------------------------- in

	/**
	 * 创建从控制台读取内容的{@link Scanner}
	 *
	 * @return {@link Scanner}
	 */
	public static Scanner scanner() {
		return new Scanner(System.in);
	}

	/**
	 * 读取用户输入的内容（在控制台敲回车前的内容）
	 *
	 * @return 用户输入的内容
	 */
	public static String input() {
		return scanner().next();
	}

	// --------------------------------------------------------------------------------- console lineNumber

	/**
	 * 返回当前位置+行号 (不支持Lambda、内部类、递归内使用)
	 *
	 * @return 返回当前行号
	 */
	public static String where() {
		final StackTraceElement stackTraceElement = new Throwable().getStackTrace()[1];
		final String className = stackTraceElement.getClassName();
		final String methodName = stackTraceElement.getMethodName();
		final String fileName = stackTraceElement.getFileName();
		final Integer lineNumber = stackTraceElement.getLineNumber();
		return String.format("%s.%s(%s:%s)", className, methodName, fileName, lineNumber);
	}

	/**
	 * 返回当前行号 (不支持Lambda、内部类、递归内使用)
	 *
	 * @return 返回当前行号
	 */
	public static Integer lineNumber() {
		return new Throwable().getStackTrace()[1].getLineNumber();
	}

	/**
	 * 构建空格分隔的模板，类似于"{} {} {} {}"
	 *
	 * @param count 变量数量
	 * @return 模板
	 */
	private static String buildTemplateSplitBySpace(int count) {
		return GutilStr.repeatAndJoin(TEMPLATE_VAR, count, GutilStr.SPACE);
	}

}

