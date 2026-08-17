package cn.geoair.base.util;

import java.util.Collection;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 断言工具，来自于 spring Assert
 *
 * <p>所有参数/值校验断言失败时抛出{@link IllegalArgumentException}，仅 {@link #state} 系列方法抛出
 * {@link IllegalStateException}（用于状态不变量校验，而非参数校验）。
 *
 * @author
 */
public abstract class GutilAssert {

    /**
     * 断言一个布尔表达式，如果表达式为 {@code false}，抛出 {@code IllegalStateException}。
     *
     * <p>如果希望在断言失败时抛出 {@code IllegalArgumentException}，请调用 {@link #isTrue}。
     *
     * <pre class="code">
     * Assert.state(id == null, "The id property must not already be initialized");</pre>
     *
     * @param expression 布尔表达式
     * @param message 断言失败时使用的异常消息
     * @throws IllegalStateException 如果 {@code expression} 为 {@code false}
     */
    public static void state(boolean expression, String message) {
        if (!expression) {
            throw new IllegalStateException(message);
        }
    }

    /**
     * 断言一个布尔表达式，如果表达式为 {@code false}，抛出 {@code IllegalStateException}。
     *
     * <p>如果希望在断言失败时抛出 {@code IllegalArgumentException}，请调用 {@link #isTrue}。
     *
     * <pre class="code">
     * Assert.state(id == null,
     *     () -&gt; "ID for " + entity.getName() + " must not already be initialized");
     * </pre>
     *
     * @param expression 布尔表达式
     * @param messageSupplier 断言失败时使用的异常消息提供者
     * @throws IllegalStateException 如果 {@code expression} 为 {@code false}
     * @since 5.0
     */
    public static void state(boolean expression, Supplier<String> messageSupplier) {
        if (!expression) {
            throw new IllegalStateException(nullSafeGet(messageSupplier));
        }
    }

    /**
     * 断言一个布尔表达式，如果表达式为 {@code false}，抛出 {@code IllegalArgumentException}。
     *
     * <pre class=
     * "code">Assert.isTrue(i &gt; 0, "The value must be greater than zero");
     * </pre>
     *
     * @param expression 布尔表达式
     * @param message 断言失败时使用的异常消息
     * @throws IllegalArgumentException 如果 {@code expression} 为 {@code false}
     */
    public static void isTrue(boolean expression, String message) {
        isTrue(expression, () -> message);
    }

    /**
     * 断言一个布尔表达式，如果表达式为 {@code false}，抛出 {@code IllegalArgumentException}。
     *
     * <pre class="code">
     * Assert.isTrue(i &gt; 0, () -&gt; "The value '" + i + "' must be greater than zero");
     * </pre>
     *
     * @param expression 布尔表达式
     * @param messageSupplier 断言失败时使用的异常消息提供者
     * @throws IllegalArgumentException 如果 {@code expression} 为 {@code false}
     * @since 5.0
     */
    public static void isTrue(boolean expression, Supplier<String> messageSupplier) {
        if (!expression) {
            throw new IllegalArgumentException(nullSafeGet(messageSupplier));
        }
    }

    /**
     * 断言两个对象相等，使用 {@link GutilObject#equal(Object, Object)}（含
     * BigDecimal数值比较与数组内容深度比较）。
     *
     * @param object1 对象1
     * @param object2 对象2
     * @param message 断言失败时的异常消息
     * @throws IllegalArgumentException 两个对象不相等
     */
    public static void equal(Object object1, Object object2, String message) {
        if (GutilObject.notEqual(object1, object2)) {
            throw new IllegalArgumentException(nullSafeGet(() -> message));
        }
    }

    /**
     * 断言两个字符串相等，委托 {@link #equal(Object, Object, String)}。
     *
     * @param string1 字符串1
     * @param string2 字符串2
     * @param message 断言失败时的异常消息
     * @throws IllegalArgumentException 两个字符串不相等
     */
    public static void equalString(String string1, String string2, String message) {
        equal(string1, string2, message);
    }

    /**
     * 断言两个对象不相等，使用 {@link GutilObject#equal(Object, Object)}。
     *
     * @param object1 对象1
     * @param object2 对象2
     * @param message 断言失败时的异常消息
     * @throws IllegalArgumentException 两个对象相等
     */
    public static void notEqual(Object object1, Object object2, String message) {
        if (GutilObject.equal(object1, object2)) {
            throw new IllegalArgumentException(nullSafeGet(() -> message));
        }
    }

    /**
     * 断言一个对象为 {@code null}。
     *
     * @param object 要检查的对象
     * @param message 断言失败时使用的异常消息
     * @throws IllegalArgumentException 如果对象不为 {@code null}
     */
    public static void isNull(Object object, String message) {
        isNull(object, () -> message);
    }

    /**
     * 断言一个对象为 {@code null}。
     *
     * <pre class="code">
     * Assert.isNull(value, () -&gt; "The value '" + value + "' must be null");
     * </pre>
     *
     * @param object 要检查的对象
     * @param messageSupplier 断言失败时使用的异常消息提供者
     * @throws IllegalArgumentException 如果对象不为 {@code null}
     * @since 5.0
     */
    public static void isNull(Object object, Supplier<String> messageSupplier) {
        if (object != null) {
            throw new IllegalArgumentException(nullSafeGet(messageSupplier));
        }
    }

    /**
     * 断言一个对象为 {@code null}。
     *
     * @param object 要检查的对象
     * @throws IllegalArgumentException 如果对象不为 {@code null}
     */
    public static void isNull(Object object) {
        isNull(object, () -> "[Assertion failed] - the object argument must be null");
    }

    /**
     * 断言一个对象不为 {@code null}。
     *
     * <pre class="code">Assert.notNull(clazz, "The class must not be null");</pre>
     *
     * @param object 要检查的对象
     * @param message 断言失败时使用的异常消息
     * @throws IllegalArgumentException 如果对象为 {@code null}
     */
    public static void notNull(Object object, String message) {
        if (object == null) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * 断言一个对象不为 {@code null}。
     *
     * <pre class="code">
     * Assert.notNull(clazz, () -&gt; "The class '" + clazz.getName() + "' must not be null");
     * </pre>
     *
     * @param object 要检查的对象
     * @param messageSupplier 断言失败时使用的异常消息提供者
     * @throws IllegalArgumentException 如果对象为 {@code null}
     * @since 5.0
     */
    public static void notNull(Object object, Supplier<String> messageSupplier) {
        if (object == null) {
            throw new IllegalArgumentException(nullSafeGet(messageSupplier));
        }
    }

    /**
     * 断言给定的字符串不为空；即它必须不为 {@code null} 且不是空字符串。
     *
     * <pre class="code">
     * Assert.hasLength(name, () -&gt; "Name for account '" + account.getId() + "' must not be empty");
     * </pre>
     *
     * @param text 要检查的字符串
     * @param message 断言失败时使用的异常消息
     * @see GutilStr#hasLength
     * @throws IllegalArgumentException 如果文本为空
     */
    public static void hasLength(String text, String message) {
        hasLength(text, () -> message);
    }

    /**
     * 断言给定的字符串不为空；即它必须不为 {@code null} 且不是空字符串。
     *
     * @param text 要检查的字符串
     * @param messageSupplier 断言失败时使用的异常消息提供者
     * @see GutilStr#hasLength
     * @throws IllegalArgumentException 如果文本为空
     * @since 5.0
     */
    public static void hasLength(String text, Supplier<String> messageSupplier) {
        if (!GutilStr.hasLength(text)) {
            throw new IllegalArgumentException(nullSafeGet(messageSupplier));
        }
    }

    /**
     * 断言给定的字符串包含有效的文本内容；即它必须不为 {@code null} 且至少包含一个非空白字符。
     *
     * <pre class="code">
     * Assert.hasText(name, () -&gt; "Name for account '" + account.getId() + "' must not be empty");
     * </pre>
     *
     * @param text 要检查的字符串
     * @param message 断言失败时使用的异常消息
     * @see GutilStr#hasText
     * @throws IllegalArgumentException 如果文本不包含有效的文本内容
     */
    public static void hasText(String text, String message) {
        hasText(text, () -> message);
    }

    /**
     * 断言给定的字符串包含有效的文本内容；即它必须不为 {@code null} 且至少包含一个非空白字符。
     *
     * @param text 要检查的字符串
     * @param messageSupplier 断言失败时使用的异常消息提供者
     * @see GutilStr#hasText
     * @throws IllegalArgumentException 如果文本不包含有效的文本内容
     * @since 5.0
     */
    public static void hasText(String text, Supplier<String> messageSupplier) {
        if (!GutilStr.hasText(text)) {
            throw new IllegalArgumentException(nullSafeGet(messageSupplier));
        }
    }

    /**
     * 断言给定的文本不包含给定的子串。
     *
     * <pre class=
     * "code">Assert.doesNotContain(name, "rod", "Name must not contain 'rod'");
     * </pre>
     *
     * @param textToSearch 要搜索的文本
     * @param substring 要在文本中查找的子串
     * @param message 断言失败时使用的异常消息
     * @throws IllegalArgumentException 如果文本包含该子串
     */
    public static void doesNotContain(String textToSearch, String substring, String message) {
        if (GutilStr.hasLength(textToSearch)
                && GutilStr.hasLength(substring)
                && textToSearch.contains(substring)) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * 断言给定的文本不包含给定的子串。
     *
     * <pre class="code">
     * Assert.doesNotContain(name, forbidden, () -&gt; "Name must not contain '" + forbidden + "'");
     * </pre>
     *
     * @param textToSearch 要搜索的文本
     * @param substring 要在文本中查找的子串
     * @param messageSupplier 断言失败时使用的异常消息提供者
     * @throws IllegalArgumentException 如果文本包含该子串
     * @since 5.0
     */
    public static void doesNotContain(
            String textToSearch, String substring, Supplier<String> messageSupplier) {
        if (GutilStr.hasLength(textToSearch)
                && GutilStr.hasLength(substring)
                && textToSearch.contains(substring)) {
            throw new IllegalArgumentException(nullSafeGet(messageSupplier));
        }
    }

    /**
     * 断言数组包含元素；即它必须不为 {@code null} 且至少包含一个元素。
     *
     * @param array 要检查的数组
     * @param message 断言失败时使用的异常消息
     * @throws IllegalArgumentException 如果对象数组为 {@code null} 或不包含任何元素
     */
    public static void notEmpty(Object[] array, String message) {
        notEmpty(array, () -> message);
    }

    /**
     * 断言数组包含元素；即它必须不为 {@code null} 且至少包含一个元素。
     *
     * <pre class="code">
     * Assert.notEmpty(array, () -&gt; "The " + arrayType + " array must contain elements");
     * </pre>
     *
     * @param array 要检查的数组
     * @param messageSupplier 断言失败时使用的异常消息提供者
     * @throws IllegalArgumentException 如果对象数组为 {@code null} 或不包含任何元素
     * @since 5.0
     */
    public static void notEmpty(Object[] array, Supplier<String> messageSupplier) {
        if (GutilObject.isEmpty(array)) {
            throw new IllegalArgumentException(nullSafeGet(messageSupplier));
        }
    }

    /**
     * 断言数组中不包含 {@code null} 元素。
     *
     * <p>注意：如果数组为空，本方法不会报错！
     *
     * @param array 要检查的数组
     * @param message 断言失败时使用的异常消息
     * @throws IllegalArgumentException 如果数组为 {@code null} 或包含 {@code null} 元素
     */
    public static void noNullElements(Object[] array, String message) {
        noNullElements(array, () -> message);
    }

    /**
     * 断言数组中不包含 {@code null} 元素。
     *
     * <p>注意：如果数组为空，本方法不会报错！
     *
     * <pre class="code">
     * Assert.noNullElements(array, () -&gt; "The " + arrayType + " array must contain non-null elements");
     * </pre>
     *
     * @param array 要检查的数组
     * @param messageSupplier 断言失败时使用的异常消息提供者
     * @throws IllegalArgumentException 如果数组为 {@code null} 或包含 {@code null} 元素
     * @since 5.0
     */
    public static void noNullElements(Object[] array, Supplier<String> messageSupplier) {
        if (array == null) {
            throw new IllegalArgumentException(nullSafeGet(messageSupplier));
        }
        for (Object element : array) {
            if (element == null) {
                throw new IllegalArgumentException(nullSafeGet(messageSupplier));
            }
        }
    }

    /**
     * 断言集合包含元素；即它必须不为 {@code null} 且至少包含一个元素。
     *
     * @param collection 要检查的集合
     * @param message 断言失败时使用的异常消息
     * @throws IllegalArgumentException 如果集合为 {@code null} 或不包含任何元素
     */
    public static void notEmpty(Collection<?> collection, String message) {
        notEmpty(collection, () -> message);
    }

    /**
     * 断言集合包含元素；即它必须不为 {@code null} 且至少包含一个元素。
     *
     * <pre class="code">
     * Assert.notEmpty(collection, () -&gt; "The " + collectionType + " collection must contain elements");
     * </pre>
     *
     * @param collection 要检查的集合
     * @param messageSupplier 断言失败时使用的异常消息提供者
     * @throws IllegalArgumentException 如果集合为 {@code null} 或不包含任何元素
     * @since 5.0
     */
    public static void notEmpty(Collection<?> collection, Supplier<String> messageSupplier) {
        if (GutilCollection.isEmpty(collection)) {
            throw new IllegalArgumentException(nullSafeGet(messageSupplier));
        }
    }

    /**
     * 断言 Map 包含条目；即它必须不为 {@code null} 且至少包含一个条目。
     *
     * @param map 要检查的 Map
     * @param message 断言失败时使用的异常消息
     * @throws IllegalArgumentException 如果 Map 为 {@code null} 或不包含任何条目
     */
    public static void notEmpty(Map<?, ?> map, String message) {
        notEmpty(map, () -> message);
    }

    /**
     * 断言 Map 包含条目；即它必须不为 {@code null} 且至少包含一个条目。
     *
     * <pre class="code">
     * Assert.notEmpty(map, () -&gt; "The " + mapType + " map must contain entries");
     * </pre>
     *
     * @param map 要检查的 Map
     * @param messageSupplier 断言失败时使用的异常消息提供者
     * @throws IllegalArgumentException 如果 Map 为 {@code null} 或不包含任何条目
     * @since 5.0
     */
    public static void notEmpty(Map<?, ?> map, Supplier<String> messageSupplier) {
        if (GutilCollection.isEmpty(map)) {
            throw new IllegalArgumentException(nullSafeGet(messageSupplier));
        }
    }

    /**
     * 断言给定的对象是指定类的实例。
     *
     * <pre class="code">
     * Assert.instanceOf(Foo.class, foo, () -&gt; "Processing " + Foo.class.getSimpleName() + ":");
     * </pre>
     *
     * @param type 要检查的目标类型
     * @param obj 要检查的对象
     * @param messageSupplier 断言失败时使用的异常消息提供者。详情参见
     *     {@link #isInstanceOf(Class, Object, String)}。
     * @throws IllegalArgumentException 如果对象不是指定类型的实例
     * @since 5.0
     */
    public static void isInstanceOf(Class<?> type, Object obj, Supplier<String> messageSupplier) {
        notNull(type, () -> "Type to check against must not be null");
        if (!type.isInstance(obj)) {
            instanceCheckFailed(type, obj, nullSafeGet(messageSupplier));
        }
    }

    /**
     * 断言给定的对象是指定类的实例。
     *
     * <pre class="code">Assert.instanceOf(Foo.class, foo);</pre>
     *
     * @param type 要检查的目标类型
     * @param obj 要检查的对象
     * @throws IllegalArgumentException 如果对象不是指定类型的实例
     */
    public static void isInstanceOf(Class<?> type, Object obj) {
        isInstanceOf(type, obj, () -> "");
    }

    /**
     * 断言 {@code superType.isAssignableFrom(subType)} 为 {@code true}。
     *
     * <pre class=
     * "code">Assert.isAssignable(Number.class, myClass, "Number expected");
     * </pre>
     *
     * @param superType 要检查的父类型
     * @param subType 要检查的子类型
     * @param message 附加在异常消息前提供更多上下文的消息。如果它为空或以 ":"、";"、","、"." 结尾，
     *     则会在其后追加完整的异常消息；如果它以空格结尾，则追加违规子类型的名称；其他情况下，
     *     追加 ":" 加空格及违规子类型的名称。
     * @throws IllegalArgumentException 如果类型之间不可赋值
     */
    public static void isAssignable(Class<?> superType, Class<?> subType, String message) {
        notNull(superType, () -> "Super type to check against must not be null");
        if (subType == null || !superType.isAssignableFrom(subType)) {
            assignableCheckFailed(superType, subType, message);
        }
    }

    /**
     * 断言 {@code superType.isAssignableFrom(subType)} 为 {@code true}。
     *
     * <pre class="code">
     * Assert.isAssignable(Number.class, myClass, () -&gt; "Processing " + myAttributeName + ":");
     * </pre>
     *
     * @param superType 要检查的父类型
     * @param subType 要检查的子类型
     * @param messageSupplier 断言失败时使用的异常消息提供者。详情参见
     *     {@link #isAssignable(Class, Class, String)}。
     * @throws IllegalArgumentException 如果类型之间不可赋值
     * @since 5.0
     */
    public static void isAssignable(
            Class<?> superType, Class<?> subType, Supplier<String> messageSupplier) {
        notNull(superType, () -> "Super type to check against must not be null");
        if (subType == null || !superType.isAssignableFrom(subType)) {
            assignableCheckFailed(superType, subType, nullSafeGet(messageSupplier));
        }
    }

    /**
     * 断言 {@code superType.isAssignableFrom(subType)} 为 {@code true}。
     *
     * <pre class="code">Assert.isAssignable(Number.class, myClass);</pre>
     *
     * @param superType 要检查的父类型
     * @param subType 要检查的子类型
     * @throws IllegalArgumentException 如果类型之间不可赋值
     */
    public static void isAssignable(Class<?> superType, Class<?> subType) {
        isAssignable(superType, subType, "");
    }

    private static void instanceCheckFailed(Class<?> type, Object obj, String msg) {
        String className = (obj != null ? obj.getClass().getName() : "null");
        String result = "";
        boolean defaultMessage = true;
        if (GutilStr.hasLength(msg)) {
            if (endsWithSeparator(msg)) {
                result = msg + " ";
            } else {
                result = messageWithTypeName(msg, className);
                defaultMessage = false;
            }
        }
        if (defaultMessage) {
            result =
                    result + ("Object of class [" + className + "] must be an instance of " + type);
        }
        throw new IllegalArgumentException(result);
    }

    private static void assignableCheckFailed(Class<?> superType, Class<?> subType, String msg) {
        String result = "";
        boolean defaultMessage = true;
        if (GutilStr.hasLength(msg)) {
            if (endsWithSeparator(msg)) {
                result = msg + " ";
            } else {
                result = messageWithTypeName(msg, subType);
                defaultMessage = false;
            }
        }
        if (defaultMessage) {
            result = result + (subType + " is not assignable to " + superType);
        }
        throw new IllegalArgumentException(result);
    }

    private static boolean endsWithSeparator(String msg) {
        return (msg.endsWith(":") || msg.endsWith(";") || msg.endsWith(",") || msg.endsWith("."));
    }

    private static String messageWithTypeName(String msg, Object typeName) {
        return msg + (msg.endsWith(" ") ? "" : ": ") + typeName;
    }

    private static String nullSafeGet(Supplier<String> messageSupplier) {
        return (messageSupplier != null ? messageSupplier.get() : null);
    }
}