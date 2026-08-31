package cn.geoair.base.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;

/**
 * 集合工具 来自spring CollectionUtil
 *
 * @author
 */
public abstract class GutilCollection {

    /**
     * 如果给定的 Collection 为 {@code null} 或空，返回 {@code true}；否则返回 {@code false}。
     *
     * @param collection 要检查的 Collection
     * @return 给定的 Collection 是否为空
     */
    public static boolean isEmpty(Collection<?> collection) {
        return (collection == null || collection.isEmpty());
    }

    /**
     * 如果给定的 Map 为 {@code null} 或空，返回 {@code true}；否则返回 {@code false}。
     *
     * @param map 要检查的 Map
     * @return 给定的 Map 是否为空
     */
    public static boolean isEmpty(Map<?, ?> map) {
        return (map == null || map.isEmpty());
    }

    /**
     * 如果给定的 Collection 既不为 {@code null} 也不为空，返回 {@code true}；否则返回 {@code false}。
     *
     * @param collection 要检查的 Collection
     * @return 给定的 Collection 是否不为空
     */
    public static boolean isNotEmpty(final Collection<?> collection) {
        return collection != null && !collection.isEmpty();
    }

    /**
     * 如果给定的 Map 既不为 {@code null} 也不为空，返回 {@code true}；否则返回 {@code false}。
     *
     * @param map 要检查的 Map
     * @return 给定的 Map 是否不为空
     */
    public static boolean isNotEmpty(final Map<?, ?> map) {
        return map != null && !map.isEmpty();
    }

    /**
     * 将给定的数组转换为 List。基本类型数组会被转换为对应包装类型组成的 List。
     *
     * <p><b>注意：</b>一般情况下推荐使用标准的 {@link Arrays#asList} 方法。本 {@code arrayToList} 方法仅用于处理运行时可能为 {@code
     * Object[]} 或基本类型数组的 Object 入参。
     *
     * <p>{@code null} 源值将被转换为空 List。
     *
     * <p><b>注意：</b> 与{@link java.util.Arrays#asList(Object[])}不同，本方法返回<b>可变</b>的{@link ArrayList}，
     * 可以安全地增删元素。
     *
     * @param source 待转换的（可能是基本类型的）数组
     * @param <E> 返回列表的元素类型
     * @return 转换后的 List 结果（可变ArrayList，永不为{@code null}）
     * @see GutilObject#toObjectArray(Object)
     * @see java.util.Arrays#asList(Object[])
     */
    @SuppressWarnings("unchecked")
    public static <E> List<E> arrayToList(Object source) {
        Object[] arr = GutilObject.toObjectArray(source);
        List<E> result = new ArrayList<>(arr.length);
        for (Object element : arr) {
            result.add((E) element);
        }
        return result;
    }

    /**
     * 将给定的数组合并到给定的 Collection 中。
     *
     * @param array 要合并的数组（可以为 {@code null}）
     * @param collection 合并数组的目标 Collection
     * @throws IllegalArgumentException 如果目标 Collection 为 {@code null}
     */
    @SuppressWarnings("unchecked")
    public static <E> void mergeArrayIntoCollection(Object array, Collection<E> collection) {
        if (collection == null) {
            throw new IllegalArgumentException("Target collection must not be null");
        }
        Object[] arr = GutilObject.toObjectArray(array);
        for (Object elem : arr) {
            collection.add((E) elem);
        }
    }

    /**
     * 将给定的 Properties 实例合并到给定的 Map 中，复制其全部属性（键值对）。
     *
     * <p>使用 {@code Properties.propertyNames()} 以覆盖原始 Properties 实例中关联的默认属性。
     *
     * @param props 要合并的 Properties 实例（可以为 {@code null}）
     * @param map 合并属性的目标 Map
     * @throws IllegalArgumentException 如果目标 Map 为 {@code null}
     */
    @SuppressWarnings("unchecked")
    public static <K, V> void mergePropertiesIntoMap(Properties props, Map<K, V> map) {
        if (map == null) {
            throw new IllegalArgumentException("Target map must not be null");
        }
        if (props != null) {
            for (Enumeration<?> en = props.propertyNames(); en.hasMoreElements(); ) {
                String key = (String) en.nextElement();
                Object value = props.get(key);
                if (value == null) {
                    // 允许默认值回退或使用可能被重写的访问器...
                    value = props.getProperty(key);
                }
                map.put((K) key, (V) value);
            }
        }
    }

    /**
     * 检查给定的 Iterator 是否包含给定的元素。
     *
     * @param iterator 要检查的 Iterator（可以为 {@code null}，此时返回 {@code false}）
     * @param element 要查找的元素
     * @return 找到返回 {@code true}，否则返回 {@code false}
     * @apiNote 本方法会<b>消费（耗尽）</b>传入的Iterator：无论是否找到，调用后该Iterator不再有剩余元素。
     */
    public static boolean contains(Iterator<?> iterator, Object element) {
        if (iterator != null) {
            while (iterator.hasNext()) {
                Object candidate = iterator.next();
                if (GutilObject.nullSafeEquals(candidate, element)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 检查给定的 Collection 是否包含给定的元素。
     *
     * @param collection 要检查的 Collection
     * @param element 要查找的元素
     * @return 找到返回 {@code true}，否则返回 {@code false}
     */
    public static <T> boolean contains(Collection<T> collection, T element) {
        return isNotEmpty(collection) && collection.contains(element);
    }

    /**
     * 检查给定的 Enumeration 是否包含给定的元素。
     *
     * @param enumeration 要检查的 Enumeration（可以为 {@code null}，此时返回 {@code false}）
     * @param element 要查找的元素
     * @return 找到返回 {@code true}，否则返回 {@code false}
     * @apiNote 本方法会<b>消费（耗尽）</b>传入的Enumeration：无论是否找到，调用后该Enumeration不再有 剩余元素。
     */
    public static boolean contains(Enumeration<?> enumeration, Object element) {
        if (enumeration != null) {
            while (enumeration.hasMoreElements()) {
                Object candidate = enumeration.nextElement();
                if (GutilObject.nullSafeEquals(candidate, element)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 检查给定的 Collection 是否包含给定的元素实例。
     *
     * <p>要求必须是同一个实例（按引用相等判断），而非与之 {@code equals} 相等的其他元素。
     *
     * @param collection 要检查的 Collection
     * @param element 要查找的元素
     * @return 找到返回 {@code true}，否则返回 {@code false}
     */
    public static boolean containsInstance(Collection<?> collection, Object element) {
        if (collection != null) {
            for (Object candidate : collection) {
                if (candidate == element) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 如果 '{@code candidates}' 中的任一元素包含在 '{@code source}' 中，返回 {@code true}；否则返回 {@code false}。
     *
     * <p>优化策略：始终迭代<b>较小的</b>集合做包含性查询；当较大的集合是{@link List}时，先将其转成 {@link HashSet}以摊平查询成本（O(min)次查询 +
     * O(max)次哈希构建）。
     *
     * @param source 源 Collection
     * @param candidates 要查找的候选元素集合
     * @return 是否找到任一候选元素
     * @apiNote 当较大的集合为{@link List}并走哈希优化路径时，元素必须满足{@code hashCode}与 {@code
     *     equals}一致性的契约，否则可能得到与{@code contains}逐一遍历不同的结果；元素为 {@code null}时始终按{@code
     *     contains}语义处理（HashSet支持null元素）。
     */
    public static boolean containsAny(Collection<?> source, Collection<?> candidates) {
        if (isEmpty(source) || isEmpty(candidates)) {
            return false;
        }
        Collection<?> larger = (source.size() >= candidates.size()) ? source : candidates;
        Collection<?> smaller = (source.size() >= candidates.size()) ? candidates : source;
        if (larger instanceof List) {
            Set<Object> largerSet = new HashSet<>(larger);
            for (Object element : smaller) {
                if (largerSet.contains(element)) {
                    return true;
                }
            }
            return false;
        }
        for (Object element : smaller) {
            if (larger.contains(element)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 返回 '{@code candidates}' 中第一个包含在 '{@code source}' 中的元素。如果 '{@code candidates}' 中没有元素存在于
     * '{@code source}' 中，返回 {@code null}。迭代顺序取决于 {@link Collection} 的具体实现。
     *
     * <p>优化策略：保持按'{@code candidates}'的迭代顺序查找（保证返回"第一个"的语义），当'{@code source}' 是{@link
     * List}时，先将其转成{@link HashSet}以摊平包含性查询成本。
     *
     * @param source 源 Collection
     * @param candidates 要查找的候选元素集合
     * @return 第一个匹配的元素，未找到返回 {@code null}
     * @apiNote 当'{@code source}'为{@link List}并走哈希优化路径时，元素必须满足{@code hashCode}与 {@code
     *     equals}一致性的契约，否则可能得到与{@code contains}逐一遍历不同的结果。
     */
    @SuppressWarnings("unchecked")
    public static <E> E findFirstMatch(Collection<?> source, Collection<E> candidates) {
        if (isEmpty(source) || isEmpty(candidates)) {
            return null;
        }
        Collection<?> membership = (source instanceof List) ? new HashSet<Object>(source) : source;
        for (Object candidate : candidates) {
            if (membership.contains(candidate)) {
                return (E) candidate;
            }
        }
        return null;
    }

    /**
     * 在给定的 Collection 中查找指定类型的单个值。
     *
     * @param collection 要搜索的 Collection
     * @param type 要查找的类型
     * @return 存在明确匹配时返回该类型的值；没有找到或找到多个这样的值时返回 {@code null}
     */
    @SuppressWarnings("unchecked")
    public static <T> T findValueOfType(Collection<?> collection, Class<T> type) {
        if (isEmpty(collection)) {
            return null;
        }
        T value = null;
        for (Object element : collection) {
            if (type == null || type.isInstance(element)) {
                if (value != null) {
                    // 找到多个值... 没有明确的单一值。
                    return null;
                }
                value = (T) element;
            }
        }
        return value;
    }

    /**
     * 在给定的 Collection 中查找给定类型之一的单个值：先按第一个类型搜索，再按第二个类型搜索，以此类推。
     *
     * @param collection 要搜索的集合
     * @param types 要查找的类型，按优先级排序
     * @return 存在明确匹配时返回其中一种类型的值；没有找到或找到多个这样的值时返回 {@code null}
     */
    public static Object findValueOfType(Collection<?> collection, Class<?>[] types) {
        if (isEmpty(collection) || GutilObject.isEmpty(types)) {
            return null;
        }
        for (Class<?> type : types) {
            Object value = findValueOfType(collection, type);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    /**
     * 判断给定的 Collection 是否只包含一个唯一的对象。
     *
     * @param collection 要检查的 Collection
     * @return 如果集合中只包含同一个实例（单个引用或对同一实例的多个引用），返回 {@code true}， 否则返回 {@code false}
     */
    public static boolean hasUniqueObject(Collection<?> collection) {
        if (isEmpty(collection)) {
            return false;
        }
        boolean hasCandidate = false;
        Object candidate = null;
        for (Object elem : collection) {
            if (!hasCandidate) {
                hasCandidate = true;
                candidate = elem;
            } else if (candidate != elem) {
                return false;
            }
        }
        return true;
    }

    /**
     * 查找给定 Collection 的公共元素类型（如果存在）。
     *
     * @param collection 要检查的 Collection
     * @return 公共元素类型；如果没有找到明确的公共类型（或集合为空）则返回 {@code null}
     * @apiNote 返回{@code null}存在三种情况，调用方需区分处理：<br>
     *     1) 集合为{@code null}或空集合，无元素可推断；<br>
     *     2) 集合中所有元素均为{@code null}，无任何类型信息；<br>
     *     3) 集合中元素类型不一致（出现两种及以上不同类型），不存在公共类型。<br>
     *     仅当集合存在非{@code null}元素且所有非{@code null}元素类型完全相同时，才返回该类型。
     */
    public static Class<?> findCommonElementType(Collection<?> collection) {
        if (isEmpty(collection)) {
            return null;
        }
        Class<?> candidate = null;
        for (Object val : collection) {
            if (val != null) {
                if (candidate == null) {
                    candidate = val.getClass();
                } else if (candidate != val.getClass()) {
                    return null;
                }
            }
        }
        return candidate;
    }

    /**
     * 获取给定 Set 的最后一个元素：优先使用 {@link SortedSet#last()}，否则遍历所有元素 （假定为有序集合）。
     *
     * @param set 要检查的 Set（可以为 {@code null} 或空）
     * @return 最后一个元素，没有则返回 {@code null}
     * @since 5.0.3
     * @see SortedSet
     * @see LinkedHashMap#keySet()
     * @see java.util.LinkedHashSet
     */
    public static <T> T lastElement(Set<T> set) {
        if (isEmpty(set)) {
            return null;
        }
        if (set instanceof SortedSet) {
            return ((SortedSet<T>) set).last();
        }

        // 必须完整遍历...
        Iterator<T> it = set.iterator();
        T last = null;
        while (it.hasNext()) {
            last = it.next();
        }
        return last;
    }

    /**
     * 获取给定 List 的最后一个元素，即访问最高索引。
     *
     * @param list 要检查的 List（可以为 {@code null} 或空）
     * @return 最后一个元素，没有则返回 {@code null}
     * @since 5.0.3
     */
    public static <T> T lastElement(List<T> list) {
        if (isEmpty(list)) {
            return null;
        }
        return list.get(list.size() - 1);
    }

    /**
     * 将给定枚举中的元素装入指定类型的数组中。枚举元素必须可赋值给给定数组的类型。 返回的数组是与传入数组不同的实例。
     *
     * @param enumeration 要装载的枚举，不能为 {@code null}
     * @param array 用于指定结果类型的数组，不能为 {@code null}
     * @param <A> 数组组件类型
     * @param <E> 枚举元素类型
     * @return 包含枚举元素的指定类型数组
     * @throws IllegalArgumentException 如果 {@code enumeration} 或 {@code array} 为 {@code null}
     */
    public static <A, E extends A> A[] toArray(Enumeration<E> enumeration, A[] array) {
        if (enumeration == null) {
            throw new IllegalArgumentException("Enumeration must not be null");
        }
        if (array == null) {
            throw new IllegalArgumentException("Array must not be null");
        }
        ArrayList<A> elements = new ArrayList<>();
        while (enumeration.hasMoreElements()) {
            elements.add(enumeration.nextElement());
        }
        return elements.toArray(array);
    }

    /**
     * 将枚举适配为迭代器。
     *
     * @param enumeration 枚举
     * @return 迭代器
     */
    public static <E> Iterator<E> toIterator(Enumeration<E> enumeration) {
        return new EnumerationIterator<>(enumeration);
    }

    /**
     * 将 {@code Map<K, List<V>>} 适配为 {@code MultiValueMap<K, V>}。
     *
     * @param map 原始 Map
     * @return 多值 Map
     * @since 3.1
     *     <p>public static <K, V> gtcMultiValueMap<K, V> toMultiValueMap(Map<K, List<V>> map) {
     *     return new MultiValueMapAdapter<>(map); }
     */

    /**
     * 返回指定多值 Map 的不可修改视图。
     *
     * @param map 需要返回不可修改视图的 Map
     * @return 指定多值 Map 的不可修改视图
     * @since 3.1 @SuppressWarnings("unchecked") public static <K, V> gtcMultiValueMap<K, V>
     *     unmodifiableMultiValueMap( gtcMultiValueMap<? extends K, ? extends V> map) {
     *     gtcAssert.notNull(map, "'map' must not be null"); Map<K, List<V>> result = new
     *     LinkedHashMap<>(map.size()); map.forEach((key, value) -> { List<? extends V> values =
     *     Collections.unmodifiableList(value); result.put(key, (List<V>) values); }); Map<K,
     *     List<V>> unmodifiableMap = Collections.unmodifiableMap(result); return
     *     toMultiValueMap(unmodifiableMap); }
     */

    /** 包装 Enumeration 的迭代器。 */
    private static class EnumerationIterator<E> implements Iterator<E> {

        private final Enumeration<E> enumeration;

        public EnumerationIterator(Enumeration<E> enumeration) {
            this.enumeration = enumeration;
        }

        @Override
        public boolean hasNext() {
            return this.enumeration.hasMoreElements();
        }

        @Override
        public E next() {
            return this.enumeration.nextElement();
        }

        @Override
        public void remove() throws UnsupportedOperationException {
            throw new UnsupportedOperationException("Not supported");
        }
    }

    /**
     * 将 Map 适配为 MultiValueMap 契约的实现（以下为注释掉的参考实现代码）。 @SuppressWarnings("serial") private static
     * class MultiValueMapAdapter<K, V> implements gtcMultiValueMap<K, V>, Serializable {
     *
     * <p>private final Map<K, List<V>> map;
     *
     * <p>public MultiValueMapAdapter(Map<K, List<V>> map) { gtcAssert.notNull(map, "'map' must not
     * be null"); this.map = map; } @Override
     *
     * <p>public V getFirst(K key) { List<V> values = this.map.get(key); return (values != null ?
     * values.get(0) : null); } @Override public void add(K key, V value) { List<V> values =
     * this.map.computeIfAbsent(key, k -> new LinkedList<>()); values.add(value); } @Override public
     * void addAll(K key, List<? extends V> values) { List<V> currentValues =
     * this.map.computeIfAbsent(key, k -> new LinkedList<>()); currentValues.addAll(values);
     * } @Override public void addAll( gtcMultiValueMap<K, V> values) { for (Entry<K, List<V>> entry
     * : values.entrySet()) { addAll(entry.getKey(), entry.getValue()); } } @Override public void
     * set(K key, V value) { List<V> values = new LinkedList<>(); values.add(value);
     * this.map.put(key, values); } @Override public void setAll(Map<K, V> values) {
     * values.forEach(this::set); } @Override public Map<K, V> toSingleValueMap() { LinkedHashMap<K,
     * V> singleValueMap = new LinkedHashMap<>(this.map.size()); this.map.forEach((key, value) ->
     * singleValueMap.put(key, value.get(0))); return singleValueMap; } @Override public int size()
     * { return this.map.size(); } @Override public boolean isEmpty() { return this.map.isEmpty();
     * } @Override public boolean containsKey(Object key) { return this.map.containsKey(key);
     * } @Override public boolean containsValue(Object value) { return
     * this.map.containsValue(value); } @Override public List<V> get(Object key) { return
     * this.map.get(key); } @Override public List<V> put(K key, List<V> value) { return
     * this.map.put(key, value); } @Override public List<V> remove(Object key) { return
     * this.map.remove(key); } @Override public void putAll(Map<? extends K, ? extends List<V>> map)
     * { this.map.putAll(map); } @Override public void clear() { this.map.clear(); } @Override
     * public Set<K> keySet() { return this.map.keySet(); } @Override public Collection<List<V>>
     * values() { return this.map.values(); } @Override public Set<Entry<K, List<V>>> entrySet() {
     * return this.map.entrySet(); } @Override public boolean equals(Object other) { if (this ==
     * other) { return true; } return map.equals(other); } @Override public int hashCode() { return
     * this.map.hashCode(); } @Override public String toString() { return this.map.toString(); } }
     */
}
