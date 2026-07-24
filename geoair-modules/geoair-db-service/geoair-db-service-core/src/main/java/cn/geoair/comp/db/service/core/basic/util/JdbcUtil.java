package cn.geoair.comp.db.service.core.basic.util;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.geoair.comp.db.service.core.basic.apo.DsDataSourceApo;
import cn.geoair.map.dynamic.adv.query.apo.DataFieldsApo;
import cn.geoair.map.dynamic.adv.query.apo.FieldBySchemaApo;
import com.alibaba.fastjson2.JSONObject;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class JdbcUtil {

    public static GiLogger log = GirLoggerFactory.getLogger();

    public static Connection getConnection(DsDataSourceApo ds) throws Exception {
        try {
            Class.forName(ds.getDriver());
            String password =
                    ds.isEdit_password() ? ds.getPassword() : DESUtils.decrypt(ds.getPassword());
            Connection connection =
                    DriverManager.getConnection(ds.getUrl(), ds.getUsername(), password);
            log.info("successfully connected");
            return connection;
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(
                    "Please check whether the jdbc driver jar is missing, if missed copy the jdbc jar file to lib dir. "
                            + e.getMessage());
        }
    }

    public static List<JSONObject> getRDBMSColumnProperties(DataFieldsApo dataFieldsApo) {
        List<JSONObject> list = new ArrayList<>();
        PreparedStatement pst = null;
        try {
            dataFieldsApo.getFieldList(
                    new Function<FieldBySchemaApo, JSONObject>() {
                        @Override
                        public JSONObject apply(FieldBySchemaApo fieldBySchemaApo) {
                            String javaClassName = fieldBySchemaApo.getJavaClassName();
                            JSONObject jsonObject = new JSONObject();
                            String udtName = fieldBySchemaApo.getUdtName();
                            String columnName1 = fieldBySchemaApo.getColumnName();
                            jsonObject.put("fieldTypeName", udtName);
                            jsonObject.put("TypeName", udtName);
                            jsonObject.put("fieldJavaTypeName", javaClassName);
                            jsonObject.put("label", columnName1);
                            return jsonObject;
                        }
                    },
                    true);
            return list;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }
}
