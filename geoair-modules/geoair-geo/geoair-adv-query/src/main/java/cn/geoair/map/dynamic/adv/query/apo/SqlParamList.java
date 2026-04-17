package cn.geoair.map.dynamic.adv.query.apo;

import java.io.Serializable;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * 用于保存预编译之后的值
 * <p>使用这个参数的时候，表示前面传的sql是 select * from name == ？ 这样的问号占位符</p>
 *
 * @author 张逢吉
 * @date Created in 17:47
 */
public class SqlParamList extends ArrayList<Object> implements Serializable ,GirSqlParam{

    /**
     * 创建一个空的 SqlParamList
     *
     * @return SqlParamList实例
     */
    public static SqlParamList of() {
        return new SqlParamList();
    }

    /**
     * 从可变参数创建 SqlParamList
     *
     * @param params 参数数组
     * @return SqlParamList实例
     */
    public static SqlParamList of(Object... params) {
        SqlParamList list = new SqlParamList();
        if (params != null && params.length > 0) {
            list.addAll(Arrays.asList(params));
        }
        return list;
    }

    /**
     * 从Collection创建 SqlParamList
     *
     * @param collection 参数集合
     * @return SqlParamList实例
     */
    public static SqlParamList of(Collection<?> collection) {
        SqlParamList list = new SqlParamList();
        if (collection != null && !collection.isEmpty()) {
            list.addAll(collection);
        }
        return list;
    }

    /**
     * 从数组创建 SqlParamList
     *
     * @param array 参数数组
     * @return SqlParamList实例
     */
    public static SqlParamList ofArray(Object[] array) {
        return of(array);
    }

    /**
     * 从List创建 SqlParamList
     *
     * @param list 参数列表
     * @return SqlParamList实例
     */
    public static SqlParamList ofList(List<?> list) {
        return of(list);
    }

    /**
     * 添加单个参数
     *
     * @param value 参数值
     * @return 当前实例
     */
    public SqlParamList addParam(Object value) {
        super.add(value);
        return this;
    }

    /**
     * 批量添加参数
     *
     * @param values 参数数组
     * @return 当前实例
     */
    public SqlParamList addParams(Object... values) {
        if (values != null && values.length > 0) {
            addAll(Arrays.asList(values));
        }
        return this;
    }

    /**
     * 批量添加参数
     *
     * @param collection 参数集合
     * @return 当前实例
     */
    public SqlParamList addAllParams(Collection<?> collection) {
        if (collection != null && !collection.isEmpty()) {
            addAll(collection);
        }
        return this;
    }

    /**
     * 转换为数组
     *
     * @return Object数组
     */
    public Object[] toArrayValue() {
        return super.toArray();
    }

    /**
     * 转换为指定类型的数组
     *
     * @param a   目标数组
     * @param <T> 数组元素类型
     * @return 转换后的数组
     */
    public <T> T[] toArrayValue(T[] a) {
        return super.toArray(a);
    }

    /**
     * 转换为List
     *
     * @return List对象
     */
    public List<Object> toList() {
        return new ArrayList<>(this);
    }

    /**
     * 获取指定位置的参数
     *
     * @param index 索引位置
     * @return 参数值
     */
    public Object getParam(int index) {
        if (index < 0 || index >= super.size()) {
            return null;
        }
        return super.get(index);
    }

    /**
     * 获取指定位置的参数并转换为指定类型
     *
     * @param index 索引位置
     * @param clazz 目标类型
     * @param <T>   目标类型泛型
     * @return 转换后的参数值
     */
    public <T> T getParam(int index, Class<T> clazz) {
        Object value = getParam(index);
        if (value == null) {
            return null;
        }
        if (clazz.isInstance(value)) {
            return clazz.cast(value);
        }
        return null;
    }

    /**
     * 获取第一个参数
     *
     * @return 第一个参数值
     */
    public Object getFirst() {
        return isEmpty() ? null : get(0);
    }

    /**
     * 获取最后一个参数
     *
     * @return 最后一个参数值
     */
    public Object getLast() {
        return isEmpty() ? null : get(size() - 1);
    }

    /**
     * 清除所有参数
     *
     * @return 当前实例
     */
    public SqlParamList clearParams() {
        super.clear();
        return this;
    }

    /**
     * 移除指定位置的参数
     *
     * @param index 索引位置
     * @return 移除的参数值
     */
    public Object removeParam(int index) {
        if (index < 0 || index >= super.size()) {
            return null;
        }
        return super.remove(index);
    }

    /**
     * 批量移除参数
     *
     * @param indices 要移除的索引数组
     * @return 移除的参数列表
     */
    public List<Object> removeParams(int... indices) {
        List<Object> removed = new ArrayList<>();
        if (indices != null && indices.length > 0) {
            // 从大到小排序，避免索引错位
            int[] sortedIndices = Arrays.stream(indices)
                    .boxed()
                    .sorted(Comparator.reverseOrder())
                    .mapToInt(Integer::intValue)
                    .toArray();
            for (int index : sortedIndices) {
                if (index >= 0 && index < size()) {
                    removed.add(0, super.remove(index));
                }
            }
        }
        return removed;
    }

    /**
     * 替换指定位置的参数
     *
     * @param index 索引位置
     * @param value 新参数值
     * @return 原来的参数值
     */
    public Object replaceParam(int index, Object value) {
        if (index < 0 || index >= super.size()) {
            return null;
        }
        return super.set(index, value);
    }

    /**
     * 遍历所有参数
     *
     * @param consumer 参数消费器
     * @return 当前实例
     */
    public SqlParamList forEachParam(Consumer<Object> consumer) {
        if (consumer != null) {
            forEach(consumer);
        }
        return this;
    }

    /**
     * 判断是否包含指定参数
     *
     * @param value 参数值
     * @return true=包含，false=不包含
     */
    public boolean containsParam(Object value) {
        return contains(value);
    }

    /**
     * 获取参数索引
     *
     * @param value 参数值
     * @return 索引位置，不存在返回-1
     */
    public int indexOfParam(Object value) {
        return indexOf(value);
    }

    /**
     * 获取最后一个参数索引
     *
     * @param value 参数值
     * @return 索引位置，不存在返回-1
     */
    public int lastIndexOfParam(Object value) {
        return lastIndexOf(value);
    }

    /**
     * 复制当前参数列表
     *
     * @return 新的SqlParamList实例
     */
    public SqlParamList copy() {
        SqlParamList newList = new SqlParamList();
        newList.addAll(this);
        return newList;
    }

    /**
     * 转换为字符串表示（用于调试）
     *
     * @return 参数字符串
     */
    public String toParamString() {
        return this.stream()
                .map(Object::toString)
                .collect(Collectors.joining(", ", "[", "]"));
    }

    /**
     * 获取预编译SQL的占位符字符串
     *
     * @param count 参数个数
     * @return 占位符字符串，如 "?, ?, ?"
     */
    public static String getPlaceholders(int count) {
        if (count <= 0) {
            return "";
        }
        return String.join(", ", Collections.nCopies(count, "?"));
    }

    /**
     * 获取当前参数的占位符字符串
     *
     * @return 占位符字符串
     */
    public String getPlaceholders() {
        return getPlaceholders(size());
    }

    @Override
    public String toString() {
        return "SqlParamList{" +
                "size=" + size() +
                ", params=" + toParamString() +
                '}';
    }

    public static void main(String[] args) {

        // 示例1：创建空列表
        SqlParamList empty = SqlParamList.of();
        System.out.println("空列表: " + empty);

        // 示例2：从可变参数创建
        SqlParamList params1 = SqlParamList.of("张三", 18, new Date(), 1);
        System.out.println("可变参数创建: " + params1);
        System.out.println("参数数量: " + params1.size());
        System.out.println("占位符: " + params1.getPlaceholders());

        // 示例3：从数组创建
        Object[] arr = {"李四", 20, 2};
        SqlParamList params2 = SqlParamList.ofArray(arr);
        System.out.println("从数组创建: " + params2);

        // 示例4：从List创建
        List<Object> list = Arrays.asList("王五", 22, 3);
        SqlParamList params3 = SqlParamList.ofList(list);
        System.out.println("从List创建: " + params3);

        // 示例5：链式添加参数
        SqlParamList params4 = SqlParamList.of()
                .addParam("赵六")
                .addParam(25)
                .addParam(4)
                .addParams("额外", "参数");
        System.out.println("链式添加: " + params4);

        // 示例6：获取参数
        System.out.println("第一个参数: " + params4.getFirst());
        System.out.println("最后一个参数: " + params4.getLast());
        System.out.println("索引1的参数: " + params4.getParam(1));
        System.out.println("索引2的参数(转String): " + params4.getParam(2, String.class));

        // 示例7：转换为数组和List
        Object[] array = params4.toArrayValue();
        List<Object> paramList = params4.toList();
        System.out.println("转数组长度: " + array.length);
        System.out.println("转List大小: " + paramList.size());

        // 示例8：替换和移除
        params4.replaceParam(0, "赵六替换");
        System.out.println("替换后: " + params4);

        Object removed = params4.removeParam(0);
        System.out.println("移除: " + removed);
        System.out.println("移除后: " + params4);

        // 示例9：批量移除
        SqlParamList params5 = SqlParamList.of("a", "b", "c", "d", "e");
        List<Object> removedList = params5.removeParams(1, 3);
        System.out.println("批量移除后: " + params5);
        System.out.println("移除的元素: " + removedList);

        // 示例10：遍历参数
        params5.forEachParam(param -> System.out.println("参数值: " + param));

        // 示例11：复制
        SqlParamList copy = params5.copy();
        System.out.println("复制: " + copy);

        // 示例12：判断包含
        System.out.println("是否包含'c': " + params5.containsParam("c"));
        System.out.println("'c'的索引: " + params5.indexOfParam("c"));

        // 示例13：清空
        params5.clearParams();
        System.out.println("清空后是否为空: " + params5.isEmpty());

        // 示例14：静态占位符方法
        System.out.println("5个参数的占位符: " + SqlParamList.getPlaceholders(5));

        // 示例15：结合SQL使用
        String sql = "SELECT * FROM user WHERE name = ? AND age = ? AND status = ?";
        SqlParamList params = SqlParamList.of("张三", 18, 1);
        System.out.println("SQL: " + sql);
        System.out.println("参数: " + params.toParamString());


    }

}
