package cn.geoair.base.bean;

import cn.geoair.base.util.GutilArray;
import cn.geoair.base.util.GutilChar;
import cn.geoair.base.util.GutilClass;
import cn.geoair.base.util.GutilReflection;
import cn.geoair.base.util.GutilStr;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GkBeanPath implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 表达式边界符号数组 */
    private static final char[] EXP_CHARS = {
        GutilChar.DOT, GutilChar.BRACKET_START, GutilChar.BRACKET_END
    };

    private boolean isStartWith = false;

    protected List<String> patternParts;

    /**
     * 解析Bean路径表达式为Bean模式<br>
     * Bean表达式，用于获取多层嵌套Bean中的字段值或Bean对象<br>
     * 根据给定的表达式，查找Bean中对应的属性值对象。 表达式分为两种：
     *
     * <ol>
     *   <li>.表达式，可以获取Bean对象中的属性（字段）值或者Map中key对应的值
     *   <li>[]表达式，可以获取集合等对象中对应index的值
     * </ol>
     *
     * 表达式栗子：
     *
     * <pre>
     * persion
     * persion.name
     * persons[3]
     * person.friends[5].name
     * ['person']['friends'][5]['name']
     * </pre>
     *
     * @param expression 表达式
     * @return gtcBeanPath
     */
    public static GkBeanPath create(String expression) {
        return new GkBeanPath(expression);
    }

    /**
     * 构造
     *
     * @param expression 表达式
     */
    public GkBeanPath(String expression) {
        init(expression);
    }

    /**
     * 获取Bean中对应表达式的值
     *
     * @param bean Bean对象或Map或List等
     * @return 值，如果对应值不存在，则返回null
     */
    public Object get(Object bean) {
        return get(this.patternParts, bean, false);
    }

    public List<String> getPatternParts() {

        List<String> parts = new ArrayList<>();

        for (String item : patternParts) {
            parts.add(item);
        }

        return parts;
    }

    /**
     * 设置表达式指定位置（或filed对应）的值<br>
     * 若表达式指向一个List则设置其坐标对应位置的值，若指向Map则put对应key的值，Bean则设置字段的值<br>
     * 注意：
     *
     * <pre>
     * 1. 如果为List，如果下标不大于List长度，则替换原有值，否则追加值
     * 2. 如果为数组，如果下标不大于数组长度，则替换原有值，否则追加值
     * </pre>
     *
     * @param bean Bean、Map或List
     * @param value 值
     */
    public void set(Object bean, Object value) {
        set(bean, this.patternParts, value);
    }

    /**
     * 设置表达式指定位置（或filed对应）的值<br>
     * 若表达式指向一个List则设置其坐标对应位置的值，若指向Map则put对应key的值，Bean则设置字段的值<br>
     * 注意：
     *
     * <pre>
     * 1. 如果为List，如果下标不大于List长度，则替换原有值，否则追加值
     * 2. 如果为数组，如果下标不大于数组长度，则替换原有值，否则追加值
     * </pre>
     *
     * @param bean Bean、Map或List
     * @param patternParts 表达式块列表
     * @param value 值
     */
    private void set(Object bean, List<String> patternParts, Object value) {
        Object subBean = get(patternParts, bean, true);
        if (null == subBean) {
            set(bean, patternParts.subList(0, patternParts.size() - 1), new HashMap<>());
            // set中有可能做过转换，因此此处重新获取bean
            subBean = get(patternParts, bean, true);
        }
        GutilReflection.setFieldValue(subBean, patternParts.get(patternParts.size() - 1), value);
    }

    // -------------------------------------------------------------------------------------------------------------------------------------
    // Private method start
    /**
     * 获取Bean中对应表达式的值
     *
     * @param patternParts 表达式分段列表
     * @param bean Bean对象或Map或List等
     * @param ignoreLast 是否忽略最后一个值，忽略最后一个值则用于set，否则用于read
     * @return 值，如果对应值不存在，则返回null
     */
    private Object get(List<String> patternParts, Object bean, boolean ignoreLast) {
        int length = patternParts.size();
        if (ignoreLast) {
            length--;
        }
        Object subBean = bean;
        boolean isFirst = true;
        String patternPart;
        for (int i = 0; i < length; i++) {
            patternPart = patternParts.get(i);
            subBean = getFieldValue(subBean, patternPart);
            if (null == subBean) {
                // 支持表达式的第一个对象为Bean本身（若用户定义表达式$开头，则不做此操作）
                if (isFirst && false == this.isStartWith && isMatchName(bean, patternPart, true)) {
                    subBean = bean;
                    isFirst = false;
                } else {
                    return null;
                }
            }
        }
        return subBean;
    }

    private static boolean isMatchName(Object bean, String beanClassName, boolean isSimple) {
        return GutilClass.getClassName(bean, isSimple)
                .equals(isSimple ? GutilStr.upperFirst(beanClassName) : beanClassName);
    }

    @SuppressWarnings("unchecked")
    private static Object getFieldValue(Object bean, String expression) {
        if (GutilStr.isBlank(expression)) {
            return null;
        }

        if (GutilStr.contains(expression, ':')) {
            // [start:end:step] 模式
            final String[] parts = GutilStr.split(expression, ":");
            int start = Integer.parseInt(parts[0]);
            int end = Integer.parseInt(parts[1]);
            int step = 1;
            if (3 == parts.length) {
                step = Integer.parseInt(parts[2]);
            }
            if (bean instanceof Collection) {
                return sub(new ArrayList<>((Collection<?>) bean), start, end, step);
            } else if (GutilArray.isArray(bean)) {
                return GutilArray.sub(bean, start, end, step);
            }
        } else if (GutilStr.contains(expression, ',')) {
            // [num0,num1,num2...]模式或者['key0','key1']模式
            final String[] keys = GutilStr.split(expression, ",");
            if (bean instanceof Collection) {

                final int[] intKeys = new int[keys.length];
                for (int i = 0; i < intKeys.length; i++) {
                    intKeys[i] = Integer.parseInt(keys[i]);
                }

                return getAny((Collection<?>) bean, intKeys);
            } else if (GutilArray.isArray(bean)) {

                final int[] intKeys = new int[keys.length];
                for (int i = 0; i < intKeys.length; i++) {
                    intKeys[i] = Integer.parseInt(keys[i]);
                }

                return GutilArray.getAny(bean, intKeys);
            } else {
                final String[] unWrappedKeys = new String[keys.length];
                for (int i = 0; i < unWrappedKeys.length; i++) {
                    unWrappedKeys[i] = GutilStr.unWrap(keys[i], '\'');
                }
                if (bean instanceof Map) {
                    // 只支持String为key的Map

                    Map<String, ?> ct = (Map<String, ?>) bean;

                    Map<String, Object> map = new HashMap<String, Object>();

                    for (String key : keys) {
                        String keyR = GutilStr.unWrap(key, '\'');

                        if (ct.containsKey(keyR)) {
                            map.put(keyR, ct.get(keyR));
                        }
                    }

                    return map;
                } else {
                    Map<String, Object> map = new HashMap<String, Object>();

                    for (String key : keys) {
                        String keyR = GutilStr.unWrap(key, '\'');

                        Object obj = GutilReflection.getFieldValue(bean, keyR);
                        if (obj != null) {
                            map.put(keyR, obj);
                        }
                    }
                    return map;
                }
            }
        } else {
            // 数字或普通字符串
            if (bean instanceof Map) {
                return ((Map<?, ?>) bean).get(expression);
            } else if (bean instanceof Collection) {
                return getCollectionIndex((Collection<?>) bean, Integer.parseInt(expression));
            } else if (GutilArray.isArray(bean)) {
                return GutilArray.get(bean, Integer.parseInt(expression));
            } else { // 普通Bean对象
                return GutilReflection.getFieldValue(bean, expression);
            }
        }

        return null;
    }

    private static <T> T getCollectionIndex(Collection<T> collection, int index) {
        if (null == collection) {
            return null;
        }

        final int size = collection.size();
        if (0 == size) {
            return null;
        }

        if (index < 0) {
            index += size;
        }

        // 检查越界
        if (index >= size) {
            return null;
        }

        if (collection instanceof List) {
            final List<T> list = ((List<T>) collection);
            return list.get(index);
        } else {
            int i = 0;
            for (T t : collection) {
                if (i > index) {
                    break;
                } else if (i == index) {
                    return t;
                }
                i++;
            }
        }
        return null;
    }

    private static <T> List<T> sub(List<T> list, int start, int end, int step) {
        if (list == null) {
            return null;
        }

        if (list.isEmpty()) {
            return new ArrayList<>(0);
        }

        final int size = list.size();
        if (start < 0) {
            start += size;
        }
        if (end < 0) {
            end += size;
        }
        if (start == size) {
            return new ArrayList<>(0);
        }
        if (start > end) {
            int tmp = start;
            start = end;
            end = tmp;
        }
        if (end > size) {
            if (start >= size) {
                return new ArrayList<>(0);
            }
            end = size;
        }

        if (step <= 1) {
            return list.subList(start, end);
        }

        final List<T> result = new ArrayList<>();
        for (int i = start; i < end; i += step) {
            result.add(list.get(i));
        }
        return result;
    }

    /**
     * 获取集合中指定多个下标的元素值，下标可以为负数，例如-1表示最后一个元素
     *
     * @param <T> 元素类型
     * @param collection 集合
     * @param indexes 下标，支持负数
     * @return 元素值列表
     * @since 4.0.6
     */
    @SuppressWarnings("unchecked")
    private static <T> List<T> getAny(Collection<T> collection, int... indexes) {
        final int size = collection.size();
        final ArrayList<T> result = new ArrayList<>();
        if (collection instanceof List) {
            final List<T> list = ((List<T>) collection);
            for (int index : indexes) {
                if (index < 0) {
                    index += size;
                }
                result.add(list.get(index));
            }
        } else {
            final Object[] array = collection.toArray();
            for (int index : indexes) {
                if (index < 0) {
                    index += size;
                }
                result.add((T) array[index]);
            }
        }
        return result;
    }

    /**
     * 初始化
     *
     * @param expression 表达式
     */
    private void init(String expression) {
        List<String> localPatternParts = new ArrayList<>();
        int length = expression.length();

        final StringBuilder builder = new StringBuilder();
        char c;
        boolean isNumStart = false; // 下标标识符开始
        for (int i = 0; i < length; i++) {
            c = expression.charAt(i);
            if (0 == i && '$' == c) {
                // 忽略开头的$符，表示当前对象
                isStartWith = true;
                continue;
            }

            if (GutilArray.contains(EXP_CHARS, c)) {
                // 处理边界符号
                if (GutilChar.BRACKET_END == c) {
                    // 中括号（数字下标）结束
                    if (false == isNumStart) {
                        throw new IllegalArgumentException(
                                GutilStr.format(
                                        "Bad expression '{}':{}, we find ']' but no '[' !",
                                        expression,
                                        i));
                    }
                    isNumStart = false;
                    // 中括号结束加入下标
                } else {
                    if (isNumStart) {
                        // 非结束中括号情况下发现起始中括号报错（中括号未关闭）
                        throw new IllegalArgumentException(
                                GutilStr.format(
                                        "Bad expression '{}':{}, we find '[' but no ']' !",
                                        expression,
                                        i));
                    } else if (GutilChar.BRACKET_START == c) {
                        // 数字下标开始
                        isNumStart = true;
                    }
                    // 每一个边界符之前的表达式是一个完整的KEY，开始处理KEY
                }
                if (builder.length() > 0) {
                    localPatternParts.add(unWrapIfPossible(builder));
                }
                builder.delete(0, builder.length());
            } else {
                // 非边界符号，追加字符
                builder.append(c);
            }
        }

        // 末尾边界符检查
        if (isNumStart) {
            throw new IllegalArgumentException(
                    GutilStr.format(
                            "Bad expression '{}':{}, we find '[' but no ']' !",
                            expression,
                            length - 1));
        } else {
            if (builder.length() > 0) {
                localPatternParts.add(unWrapIfPossible(builder));
            }
        }

        // 不可变List
        this.patternParts = Collections.unmodifiableList(localPatternParts);
    }

    /**
     * 对于非表达式去除单引号
     *
     * @param expression 表达式
     * @return 表达式
     */
    private static String unWrapIfPossible(CharSequence expression) {
        if (GutilStr.containsAny(expression, " = ", " > ", " < ", " like ", ",")) {
            return expression.toString();
        }
        return GutilStr.unWrap(expression, '\'');
    }
    // -------------------------------------------------------------------------------------------------------------------------------------
    // Private method end

}
