package cn.geoair.map.tile.forge.core.zip;

@FunctionalInterface
public interface TerminatingConsumer<T> {
    /**
     * 消费条目，返回false表示终止遍历
     *
     * @param t            消费对象
     * @param allCount     总数
     * @param currentCount 当前消费数量
     * @return 是否继续遍历，一个停止条件
     */
    boolean accept(T t, Long allCount, Long currentCount);
}
