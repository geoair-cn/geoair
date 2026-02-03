package cn.geoair.gtc.orm.mybatisplus.impls;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.List;

import cn.geoair.gtc.base.util.GutilObject;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.TableFieldInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import cn.geoair.gtc.base.Gir;
import cn.geoair.gtc.base.exception.GirException;
import cn.geoair.gtc.base.gpa.dao.GiUpdateDao;
import cn.geoair.gtc.base.gpa.entity.GiEntityAlterable;

public interface PlusUpdateMapper<T extends GiEntityAlterable<PK>, PK extends Serializable> extends GiUpdateDao<T, PK>, BaseMapper<T> {

    @Override
    default int gtcUpdateByPK(T t) {
        TableInfo tableInfo = TableInfoHelper.getTableInfo(t.getClass());
        List<TableFieldInfo> fieldList = tableInfo.getFieldList();
        String keyColumn = tableInfo.getKeyColumn();
        UpdateWrapper<T> updateWrapper = new UpdateWrapper<>();
        for (TableFieldInfo tableFieldInfo : fieldList) {
            Field field = tableFieldInfo.getField();
            Object o = null;
            try {
                o = field.get(t);
            } catch (IllegalAccessException e) {
                Gir.log.debug("不可访问的字段，不做处理{}", tableFieldInfo.getColumn());
            }
            if (tableFieldInfo.getColumn().equals(keyColumn)) {
                if (o == null) {
                    throw new GirException("主键不能为空！");
                }
                updateWrapper.eq(tableFieldInfo.getColumn(), o);
            } else {
                updateWrapper.set(tableFieldInfo.getColumn(), o);
            }
        }
        return this.update(null, updateWrapper);
    }

    @Override
    default int gtcUpdateByPKSelective(T t) {
        TableInfo tableInfo = TableInfoHelper.getTableInfo(t.getClass());
        List<TableFieldInfo> fieldList = tableInfo.getFieldList();
        String keyColumn = tableInfo.getKeyColumn();
        UpdateWrapper<T> updateWrapper = new UpdateWrapper<>();
        for (TableFieldInfo tableFieldInfo : fieldList) {
            Field field = tableFieldInfo.getField();
            Object o = null;
            try {
                o = field.get(t);
            } catch (IllegalAccessException e) {
                Gir.log.debug("不可访问的字段，不做处理{}", tableFieldInfo.getColumn());
            }
            if (tableFieldInfo.getColumn().equals(keyColumn)) {
                if (o == null) {
                    throw new GirException("主键不能为空！");
                }
                updateWrapper.eq(tableFieldInfo.getColumn(), o);
            } else {
                if (o != null) {
                    updateWrapper.set(tableFieldInfo.getColumn(), o);
                }
            }
        }
        return this.update(null, updateWrapper);
    }

    /**
     * 根据主键批量更新
     *
     * @param records
     * @return
     */
    default int gtcUpdateByPK(List<T> records) {
        if (GutilObject.isNotEmpty(records)) {
            for (T record : records) {
                gtcUpdateByPK(record);
            }
            return records.size();
        }
        return 0;


    }

    /**
     * 根据主键批量更新(更新不为Null的字段)
     *
     * @param records
     * @return
     */
    default int gtcUpdateByPKSelective(List<T> records) {
        if (GutilObject.isNotEmpty(records)) {
            for (T record : records) {
                this.gtcUpdateByPKSelective(record);
            }
            return records.size();
        }
        return 0;


    }

}
