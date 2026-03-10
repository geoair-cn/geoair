package cn.geoair.map.dynamic.dbservice.core.basic.domain;

import cn.geoair.map.dynamic.dbservice.core.model.dbapi.dto.DbApiDataSourceDto;
import cn.geoair.map.dynamic.dbservice.core.model.dbapi.entity.DbApiDataSourcePo;

import lombok.Data;

import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @program: dbApi
 * @description:
 * @author: 武汉刘德华
 * @create: 2021-01-20 09:52
 */
@Data
public class DataSource implements Serializable {

    public DbApiDataSourcePo toPo() {
        DbApiDataSourcePo thisPo = new DbApiDataSourcePo();
        BeanUtils.copyProperties(this, thisPo);
        return thisPo;
    }

    public static DataSource fromPo(DbApiDataSourcePo po) {
        DataSource thisVo = new DataSource();
        BeanUtils.copyProperties(po, thisVo);
        return thisVo;
    }

    public static List<DataSource> fromPos(List<DbApiDataSourcePo> pos) {
        List<DataSource> list = new ArrayList<>();
        for (DbApiDataSourcePo po : pos) {
            DataSource thisVo = fromPo(po);
            list.add(thisVo);
        }
        return list;
    }

    public static DataSource fromDto(DbApiDataSourceDto dto) {
        DataSource thisVo = new DataSource();
        BeanUtils.copyProperties(dto, thisVo);
        return thisVo;
    }

    public static List<DataSource> fromDtos(List<DbApiDataSourceDto> dtos) {
        List<DataSource> list = new ArrayList<>();
        for (DbApiDataSourceDto dto : dtos) {
            DataSource thisVo = fromDto(dto);
            list.add(thisVo);
        }
        return list;
    }

    String id;

    String name;

    String note;

    String url;

    String username;

    String password;

    /** true 修改密码 false不修改 */
    boolean edit_password;

    String type;

    String driver;

    String tableSql;

    String createUserId;

    String createTime;

    String updateTime;
}
