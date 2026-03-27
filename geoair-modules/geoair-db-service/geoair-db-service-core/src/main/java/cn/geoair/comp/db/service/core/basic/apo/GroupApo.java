package cn.geoair.comp.db.service.core.basic.apo;

import java.io.Serializable;
import lombok.Data;

@Data
public class GroupApo implements Serializable {

    String id;

    String name;

    String createUserId;

    String createUserName;

    String createTime;

    String updateTime;
}
