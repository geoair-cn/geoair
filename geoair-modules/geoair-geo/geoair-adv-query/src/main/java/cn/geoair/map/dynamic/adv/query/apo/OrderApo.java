package cn.geoair.map.dynamic.adv.query.apo;

import cn.geoair.map.dynamic.adv.query.enums.AdvEnumsOrder;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import java.io.Serializable;

/** 创建人: 张逢吉 创建时间: 2025/10/10 12:48 描述: 排序 条件接收参数 */
public class OrderApo implements Serializable {

    /** 字段名称 */
    String fieldName;

    /** 排序函数 类似于这样 CAST(gtc_id AS numeric) */
    String function;

    /** 条件 */
    AdvEnumsOrder advEnumsOrder;

    public OrderApo() {}

    public static OrderApo empty() {
        return new OrderApo();
    }

    public OrderApo copy() {
        OrderApo po = empty();
        BeanUtil.copyProperties(this, po);
        return po;
    }

    public boolean isFunction() {
        return ObjectUtil.isNotEmpty(function);
    }

    public String getFunction() {
        return function;
    }

    public String getFieldName() {
        return fieldName;
    }

    public OrderApo setFieldName(String fieldName) {
        this.fieldName = fieldName;
        return this;
    }

    public OrderApo setFunction(String function) {
        this.function = function;
        return this;
    }

    public AdvEnumsOrder getAdvEnumsOrder() {
        return advEnumsOrder;
    }

    public OrderApo setAdvEnumsOrder(AdvEnumsOrder advEnumsOrder) {
        this.advEnumsOrder = advEnumsOrder;
        return this;
    }

    public static OrderApo create(String fieldName, AdvEnumsOrder advEnumsOrder) {
        return new OrderApo().setFieldName(fieldName).setAdvEnumsOrder(advEnumsOrder);
    }
}
