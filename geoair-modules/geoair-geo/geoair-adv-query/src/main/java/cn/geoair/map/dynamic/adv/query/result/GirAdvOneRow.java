package cn.geoair.map.dynamic.adv.query.result;

import cn.geoair.map.dynamic.tools.simple.collection.map.OptNullGeomAndBasicTypeFromObjectGetter;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.map.CamelCaseLinkedMap;
import cn.hutool.core.map.CaseInsensitiveLinkedMap;
import cn.hutool.db.Entity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author ：张逢吉
 * @date ：Created in 2025/9/30 15:20 @description： 一行数据
 */
public class GirAdvOneRow extends LinkedHashMap<String, Object>
        implements OptNullGeomAndBasicTypeFromObjectGetter, Serializable {

    public static GirAdvOneRow ofByMap(Map<String, Object> row) {
        return new GirAdvOneRow(row);
    }

    public static GirAdvOneRow ofByEntity(Entity row) {
        if (row == null) {
            return new GirAdvOneRow(new LinkedHashMap<>());
        }
        return new GirAdvOneRow(row);
    }

    public <T> T toBeanObj(Class<T> clazz) {
        T bean = BeanUtil.toBean(this, clazz);
        return bean;
    }

    /**
     * 对key转换成大小写不敏感
     *
     * @return
     */
    public GirAdvOneRow toCaseInsensitive() {
        return new GirAdvOneRow(new CaseInsensitiveLinkedMap<String, Object>(this));
    }

    /**
     * 对key转换成驼峰的key
     *
     * @return
     */
    public GirAdvOneRow toCamelCase() {
        return new GirAdvOneRow(
                new CamelCaseLinkedMap<String, Object>(
                        new CaseInsensitiveLinkedMap<String, Object>(this)));
    }

    /**
     * 转换成简单地map
     *
     * @param oneRow
     * @return
     */
    public static Map<String, Object> toMap(GirAdvOneRow oneRow) {
        if (oneRow == null) {
            return new LinkedHashMap<>();
        }
        return oneRow;
    }

    /**
     * 转换成简单地mapList
     *
     * @param rowList
     * @return
     */
    public static List<Map<String, Object>> toMapList(List<GirAdvOneRow> rowList) {
        if (rowList == null) {
            return new ArrayList<>();
        }
        List<Map<String, Object>> list = new ArrayList<>();
        for (GirAdvOneRow row : rowList) {
            list.add(toMap(row));
        }
        return list;
    }

    /**
     * 转换成驼峰的List
     *
     * @param rowList
     * @return
     */
    public static List<GirAdvOneRow> toCamelCaseList(List<GirAdvOneRow> rowList) {
        if (rowList == null) {
            return new ArrayList<>();
        }
        List<GirAdvOneRow> list = new ArrayList<>();
        for (GirAdvOneRow row : rowList) {
            list.add(row.toCamelCase());
        }
        return list;
    }

    /**
     * 转换成大小写不敏感的List
     *
     * @param rowList
     * @return
     */
    public static List<GirAdvOneRow> toCaseInsensitiveList(List<GirAdvOneRow> rowList) {
        if (rowList == null) {
            return new ArrayList<>();
        }
        List<GirAdvOneRow> list = new ArrayList<>();
        for (GirAdvOneRow row : rowList) {
            list.add(row.toCaseInsensitive());
        }
        return list;
    }

    /**
     * 转换成Bean的List
     *
     * @param rowList
     * @return
     */
    public static <T> List<T> toBeanObjList(List<GirAdvOneRow> rowList, Class<T> clazz) {
        if (rowList == null) {
            return new ArrayList<>();
        }
        List<T> list = new ArrayList<>();
        for (GirAdvOneRow row : rowList) {
            list.add(row.toBeanObj(clazz));
        }
        return list;
    }

    public static List<GirAdvOneRow> ofByEntityList(List<Entity> rows) {
        if (rows == null || rows.isEmpty()) {
            return ListUtil.empty();
        }
        List<GirAdvOneRow> list = new ArrayList<>(rows.size());
        for (Entity row : rows) {
            GirAdvOneRow girAdvOneRow = ofByEntity(row);
            if (!girAdvOneRow.isEmpty()) {
                list.add(girAdvOneRow);
            }
        }
        return list;
    }

    /**
     * 通过map的构造函数
     *
     * @param map
     */
    private GirAdvOneRow(Map<String, Object> map) {
        CopyOptions copyOptions = CopyOptions.create();
        copyOptions.setIgnoreNullValue(false);
        copyOptions.setAutoTransCamelCase(false);
        BeanUtil.copyProperties(map, this, copyOptions);
    }

    /**
     * 通过hutool的entity创建的
     *
     * @param map
     */
    private GirAdvOneRow(Entity map) {
        CopyOptions copyOptions = CopyOptions.create();
        copyOptions.setIgnoreNullValue(false);
        copyOptions.setAutoTransCamelCase(false);
        BeanUtil.copyProperties(map, this, copyOptions);
    }

    @Override
    public Object getObj(String key, Object defaultValue) {
        Object o = this.get(key);
        if (o == null) {
            return defaultValue;
        }
        return o;
    }
}
