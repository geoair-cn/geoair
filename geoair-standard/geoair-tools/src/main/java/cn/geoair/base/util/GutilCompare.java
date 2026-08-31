package cn.geoair.base.util;

import java.util.Comparator;

/**
 * 对象比较工具
 *
 * <p>提供三种比较策略：
 *
 * <ul>
 *   <li>自定义{@link Comparator}比较：{@link #compare(Object, Object, Comparator)}
 *   <li>自然比较（对象必须实现{@link Comparable}）：{@link #compare(Object, Object)} 与 {@link #compare(Object,
 *       Object, boolean)}
 * </ul>
 *
 * <p>自然比较策略：{@code null} 被特殊处理（可配置排在末尾或排在前列），非 {@code null} 对象按 {@code compareTo} 比较；比较结果与 {@code
 * equals} 的一致性取决于被比较对象的 {@code compareTo} 与 {@code equals} 是否一致（即是否满足 Comparable 契约）。
 *
 * @author
 */
public class GutilCompare {

    /**
     * 对象比较，比较结果取决于comparator，如果被比较对象为null，传入的comparator对象应处理此情况<br>
     * 如果传入comparator为null，则使用默认规则比较（此时被比较对象必须实现Comparable接口）
     *
     * <p>一般而言，如果c1 &lt; c2，返回数小于0，c1==c2返回0，c1 &gt; c2 大于0
     *
     * @param <T> 被比较对象类型
     * @param c1 对象1
     * @param c2 对象2
     * @param comparator 比较器，可以为{@code null}；为{@code null}时按{@link #compare(Object, Object,
     *     boolean)}的自然比较规则比较
     * @return 比较结果
     * @throws IllegalArgumentException comparator为{@code null}且对象未实现{@link Comparable}接口
     * @see java.util.Comparator#compare(Object, Object)
     * @since 4.6.9
     */
    public static <T> int compare(T c1, T c2, Comparator<T> comparator) {
        if (null == comparator) {
            return compare(c1, c2, false);
        }
        return comparator.compare(c1, c2);
    }

    /**
     * {@code null}安全的对象比较，{@code null}对象小于任何对象（即{@code null}排在末尾）
     *
     * <p>比较策略：两个引用相同（含均为{@code null}）返回0；一侧为{@code null}时按规则排后； 否则要求对象实现{@link Comparable}并按{@code
     * compareTo}比较。不提供 hashCode/toString 兜底比较，以保证比较的传递性。
     *
     * @param <T> 被比较对象类型（必须实现Comparable接口）
     * @param c1 对象1，可以为{@code null}
     * @param c2 对象2，可以为{@code null}
     * @return 比较结果，如果c1 &lt; c2，返回数小于0，c1==c2返回0，c1 &gt; c2 大于0
     * @throws IllegalArgumentException 对象未实现{@link Comparable}接口
     * @see java.util.Comparator#compare(Object, Object)
     */
    public static <T extends Comparable<? super T>> int compare(T c1, T c2) {
        return compare(c1, c2, false);
    }

    /**
     * {@code null}安全的对象比较
     *
     * <p>比较策略：两个引用相同（含均为{@code null}）返回0；一侧为{@code null}时按{@code isNullGreater}排前或排后；否则要求对象实现{@link
     * Comparable}并按{@code compareTo}比较。 不提供 hashCode/toString 兜底比较，以保证比较的传递性。
     *
     * @param <T> 被比较对象类型（必须实现Comparable接口）
     * @param c1 对象1，可以为{@code null}
     * @param c2 对象2，可以为{@code null}
     * @param isNullGreater 当被比较对象为null时是否排在前面，true表示null大于任何对象，false反之
     * @return 比较结果，如果c1 &lt; c2，返回数小于0，c1==c2返回0，c1 &gt; c2 大于0
     * @throws IllegalArgumentException 对象未实现{@link Comparable}接口
     * @see java.util.Comparator#compare(Object, Object)
     */
    public static <T extends Comparable<? super T>> int compare(T c1, T c2, boolean isNullGreater) {
        return compare((Object) c1, (Object) c2, isNullGreater);
    }

    /**
     * 自然比较两个对象的大小，主实现
     *
     * <p>比较规则如下：
     *
     * <pre>
     * 1、两个引用相同（含均为null）返回0
     * 2、null按isNullGreater配置排前（true）或排后（false）
     * 3、要求对象实现Comparable，按compareTo比较；未实现时抛出IllegalArgumentException
     * </pre>
     *
     * <p>与equals的一致性：当被比较对象的{@code compareTo}与{@code equals}一致时，比较结果为0等价于 {@code
     * equals}为true；否则仅保证排序语义，不保证与equals一致。
     *
     * @param <T> 被比较对象类型
     * @param o1 对象1，可以为{@code null}
     * @param o2 对象2，可以为{@code null}
     * @param isNullGreater null值是否做为最大值（true表示null排在前面）
     * @return 比较结果，如果o1 &lt; o2，返回数小于0，o1==o2返回0，o1 &gt; o2 大于0
     * @throws IllegalArgumentException 对象未实现{@link Comparable}接口（携带对象类型信息）
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static <T> int compare(T o1, T o2, boolean isNullGreater) {
        if (o1 == o2) {
            return 0;
        } else if (null == o1) { // null 排在后面
            return isNullGreater ? 1 : -1;
        } else if (null == o2) {
            return isNullGreater ? -1 : 1;
        }
        if (!(o1 instanceof Comparable)) {
            throw new IllegalArgumentException(
                    "Object of type ["
                            + o1.getClass().getName()
                            + "] does not implement Comparable, cannot compare naturally: "
                            + o1);
        }
        if (!(o2 instanceof Comparable)) {
            throw new IllegalArgumentException(
                    "Object of type ["
                            + o2.getClass().getName()
                            + "] does not implement Comparable, cannot compare naturally: "
                            + o2);
        }
        return ((Comparable) o1).compareTo(o2);
    }
}
