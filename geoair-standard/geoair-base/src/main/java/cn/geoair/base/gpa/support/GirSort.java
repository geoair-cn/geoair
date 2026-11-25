package cn.geoair.base.gpa.support;

import cn.geoair.base.gpa.support.GirOrder.Direction;
import cn.geoair.base.util.GutilAssert;
import cn.geoair.base.util.GutilStr;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

/**
 * 排序条件对象，用于封装排序规则
 *
 * <p>该类表示一组排序条件，每个条件由{@link GirOrder}定义，支持链式调用和组合排序。 提供了静态方法用于创建排序实例，以及各种操作排序条件的方法。
 */
public class GirSort implements Serializable {

    /** 序列化版本标识符 */
    private static final long serialVersionUID = -1586646056446727841L;

    /** 表示未排序的实例 */
    private static final GirSort UNSORTED = GirSort.by(new GirOrder[0]);

    /** 默认排序方向，升序 */
    public static final Direction DEFAULT_DIRECTION = Direction.ASC;

    /** 存储排序条件列表 */
    private final List<GirOrder<?>> orders;

    /**
     * 构造函数，使用指定的排序条件列表创建排序对象
     *
     * @param orders 排序条件列表，不能为空
     */
    protected GirSort(List<GirOrder<?>> orders) {
        this.orders = orders;
    }

    /**
     * 根据给定的排序条件列表创建一个新的排序对象
     *
     * <p>如果传入的列表为空，则返回未排序实例
     *
     * @param orders 排序条件列表，不能为空
     * @return 新创建的排序对象或未排序实例
     */
    public static GirSort by(List<GirOrder<?>> orders) {

        GutilAssert.notNull(orders, " gtcOrders must not be null!");

        return orders.isEmpty() ? GirSort.unsorted() : new GirSort(orders);
    }

    /**
     * 根据给定的排序条件数组创建一个新的排序对象
     *
     * @param orders 排序条件数组，不能为空
     * @return 新创建的排序对象
     */
    public static GirSort by(GirOrder... orders) {

        GutilAssert.notNull(orders, " gtcOrders must not be null!");

        return new GirSort(Arrays.asList(orders));
    }

    /**
     * 返回表示无排序设置的排序实例
     *
     * @return 未排序的排序实例
     */
    public static GirSort unsorted() {
        return UNSORTED;
    }

    /**
     * 检查排序条件是否为空
     *
     * @return 如果没有排序条件则返回true，否则返回false
     */
    public boolean isEmpty() {
        return orders.isEmpty();
    }

    /** 清空所有排序条件 */
    public void clear() {
        this.orders.clear();
    }

    /**
     * 将当前排序条件与给定的排序条件合并，返回新的排序对象
     *
     * <p>新的排序对象包含当前排序的所有条件以及给定排序的条件
     *
     * @param sort 要合并的排序对象，不能为空
     * @return 包含合并后排序条件的新排序对象
     */
    public GirSort and(GirSort sort) {

        GutilAssert.notNull(sort, "Sort must not be null!");

        ArrayList<GirOrder<?>> these = new ArrayList<>();

        for (GirOrder<?> order : orders) {
            these.add(order);
        }

        // 添加传入的排序条件
        for (GirOrder<?> order : orders) {
            these.add(order);
        }

        return GirSort.by(these);
    }

    /**
     * 根据属性名称获取对应的排序条件
     *
     * @param property 属性名称
     * @return 对应的排序条件，如果不存在则返回null
     */
    public GirOrder<?> getOrderFor(String property) {

        for (GirOrder<?> order : orders) {
            if (order.getProperty().equals(property)) {
                return order;
            }
        }

        return null;
    }

    /**
     * 获取排序条件迭代器
     *
     * @return 排序条件迭代器
     */
    public Iterator<GirOrder<?>> iterator() {
        return this.orders.iterator();
    }

    /**
     * 对每个排序条件执行指定操作
     *
     * @param action 要对每个排序条件执行的操作
     */
    public void forEach(Consumer<? super GirOrder> action) {
        this.orders.forEach(action);
    }

    /**
     * 比较当前对象与另一个对象是否相等
     *
     * <p>如果两个排序对象包含相同的排序条件列表，则认为它们相等
     *
     * @param obj 要比较的对象
     * @return 如果相等返回true，否则返回false
     */
    @Override
    public boolean equals(Object obj) {

        if (this == obj) {
            return true;
        }

        if (!(obj instanceof GirSort)) {
            return false;
        }

        GirSort that = (GirSort) obj;

        return orders.equals(that.orders);
    }

    /**
     * 计算对象的哈希值
     *
     * @return 对象的哈希值
     */
    @Override
    public int hashCode() {

        int result = 17;
        result = 31 * result + orders.hashCode();
        return result;
    }

    /**
     * 返回对象的字符串表示形式
     *
     * <p>如果没有排序条件，返回"UNSORTED"；否则返回逗号分隔的排序条件字符串
     *
     * @return 对象的字符串表示
     */
    @Override
    public String toString() {
        return isEmpty() ? "UNSORTED" : GutilStr.collectionToCommaDelimitedString(orders);
    }
}
