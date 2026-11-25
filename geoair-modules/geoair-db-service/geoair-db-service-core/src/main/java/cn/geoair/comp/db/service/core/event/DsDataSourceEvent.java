package cn.geoair.comp.db.service.core.event;

import cn.geoair.base.Gir;
import cn.geoair.comp.db.service.core.basic.apo.DsDataSourceApo;
import cn.hutool.core.collection.ListUtil;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * @author ：张俊
 * @date ：Created in 2026/4/22 09:08
 * @description： 数据源的事件处理器
 */
public interface DsDataSourceEvent {
    static List<DsDataSourceEvent> getInstances() {
        List<DsDataSourceEvent> events = new ArrayList<>();
        Map<String, DsDataSourceEvent> beans = Gir.beans.getBeans(DsDataSourceEvent.class);
        for (Map.Entry<String, DsDataSourceEvent> event : beans.entrySet()) {
            events.add(event.getValue());
        }
        return ListUtil.sort(
                events,
                new Comparator<DsDataSourceEvent>() {
                    @Override
                    public int compare(DsDataSourceEvent o1, DsDataSourceEvent o2) {
                        return o1.getOrder() - o2.getOrder();
                    }
                });
    }

    /**
     * 获取排序,bean的顺序根据 1->无穷大排列
     *
     * @return
     */
    default Integer getOrder() {
        return 10;
    }

    /**
     * 新增前事件：校验、填充、权限判断
     *
     * @param dsDataSourceApo 待新增对象
     */
    default void addBeforeDsDataSourceEvent(DsDataSourceApo dsDataSourceApo) {}

    /**
     * 新增后事件：日志、通知、缓存、同步
     *
     * @param dsDataSourceApo 新增成功后的对象
     */
    default void addAfterDsDataSourceEvent(DsDataSourceApo dsDataSourceApo) {}

    /**
     * 更新前事件：更新校验、旧数据快照、防并发
     *
     * @param oldPo 更新前数据
     * @param newPo 待更新数据
     */
    default void updateBeforeDsDataSourceEvent(DsDataSourceApo oldPo, DsDataSourceApo newPo) {};

    /**
     * 更新后事件：记录变更、刷新缓存、通知
     *
     * @param oldPo 更新前数据
     * @param newPo 更新后数据
     * @param success 是否更新成功
     */
    default void updateAfterDsDataSourceEvent(
            DsDataSourceApo oldPo, DsDataSourceApo newPo, boolean success) {};

    /**
     * 删除前事件：删除校验、关联检查
     *
     * @param dsDataSourceApo 待删除对象
     */
    default void deleteBeforeDsDataSourceEvent(DsDataSourceApo dsDataSourceApo) {};

    /**
     * 删除后事件：清理关联数据、日志
     *
     * @param dsDataSourceApo 已删除对象
     * @param success 是否删除成功
     */
    default void deleteAfterDsDataSourceEvent(DsDataSourceApo dsDataSourceApo, boolean success) {};

    /**
     * 获取当前的PO的关联信息。关联信息放到relationObj里面
     *
     * @param po 当前的po对象
     * @param relationObj 一个汇总的关联信息
     */
    void getDsDataSourceRelationEvent(DsDataSourceApo po, Map<String, Object> relationObj);

    // ==================== 事件触发统一封装 ====================

    /** 触发新增前事件 */
    public static void triggerAddBeforeEvent(DsDataSourceApo po) {
        List<DsDataSourceEvent> events = DsDataSourceEvent.getInstances();
        for (DsDataSourceEvent event : events) {
            try {
                event.addBeforeDsDataSourceEvent(po);
            } catch (Exception e) {
                Gir.log.error("触发【新增前】事件失败", e);
            }
        }
    }

    /** 触发新增后事件 */
    static void triggerAddAfterEvent(DsDataSourceApo po) {
        List<DsDataSourceEvent> events = DsDataSourceEvent.getInstances();
        for (DsDataSourceEvent event : events) {
            try {
                event.addAfterDsDataSourceEvent(po);
            } catch (Exception e) {
                Gir.log.error("触发【新增后】事件失败", e);
            }
        }
    }

    /** 触发更新前事件 */
    static void triggerUpdateBeforeEvent(DsDataSourceApo oldPo, DsDataSourceApo newPo) {
        List<DsDataSourceEvent> events = DsDataSourceEvent.getInstances();
        for (DsDataSourceEvent event : events) {
            try {
                event.updateBeforeDsDataSourceEvent(oldPo, newPo);
            } catch (Exception e) {
                Gir.log.error("触发【更新前】事件失败", e);
            }
        }
    }

    /** 触发更新后事件 */
    static void triggerUpdateAfterEvent(
            DsDataSourceApo oldPo, DsDataSourceApo newPo, boolean success) {
        List<DsDataSourceEvent> events = DsDataSourceEvent.getInstances();
        for (DsDataSourceEvent event : events) {
            try {
                event.updateAfterDsDataSourceEvent(oldPo, newPo, success);
            } catch (Exception e) {
                Gir.log.error("触发【更新后】事件失败", e);
            }
        }
    }

    /** 触发删除前事件 */
    static void triggerDeleteBeforeEvent(DsDataSourceApo po) {
        List<DsDataSourceEvent> events = DsDataSourceEvent.getInstances();
        for (DsDataSourceEvent event : events) {
            try {
                event.deleteBeforeDsDataSourceEvent(po);
            } catch (Exception e) {
                Gir.log.error("触发【删除前】事件失败", e);
            }
        }
    }

    /** 触发删除后事件 */
    static void triggerDeleteAfterEvent(DsDataSourceApo po, boolean success) {
        List<DsDataSourceEvent> events = DsDataSourceEvent.getInstances();
        for (DsDataSourceEvent event : events) {
            try {
                event.deleteAfterDsDataSourceEvent(po, success);
            } catch (Exception e) {
                Gir.log.error("触发【删除后】事件失败", e);
            }
        }
    }
}
