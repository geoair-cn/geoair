// package cn.geoair.map.dynamic.dbservice.core.typehander.dm;
//
// import cn.geoair.map.dynamic.dbservice.core.typehander.BaseTypeHandler;
// import cn.hutool.db.Entity;
// import cn.hutool.db.meta.JdbcType;
//
// import com.dameng.geotools.util.DmGeo2Util;
//
// import dm.jdbc.driver.DmdbStruct;
//
// import org.locationtech.jts.geom.Geometry;
// import org.locationtech.jts.io.WKBReader;
//
// import java.sql.Blob;
// import java.sql.ResultSet;
// import java.sql.SQLException;
// import java.util.Map;
//
/// **
// * @author ：张俊
// * @date ：Created in 2024/4/18 17:46 @description： oracle空间字段的解析
// */
// public class DmGeomTypeHandler extends BaseTypeHandler<String> {
//
// @Override
// public String getNonNullParameter(Object parameter, JdbcType jdbcType) {
// return null;
// }
//
// @Override
// public String getResult(Entity entity, String columnName) {
// Object obj = entity.getObj(columnName);
// if (obj instanceof dm.jdbc.driver.DmdbStruct) {
// return toWkt((DmdbStruct) obj);
// }
// return String.valueOf(obj);
// }
//
// @Override
// public String getResult(ResultSet resultSet, String columnName) {
// Object obj = null;
// try {
// obj = resultSet.getObject(columnName);
// }
// catch (SQLException throwables) {
// throwables.printStackTrace();
// }
// if (obj instanceof DmdbStruct) {
// return toWkt((DmdbStruct) obj);
// }
// return String.valueOf(obj);
// }
//
// @Override
// public String getResult(ResultSet resultSet, Integer columnIndex) {
// Object obj = null;
// try {
// obj = resultSet.getObject(columnIndex);
// }
// catch (SQLException throwables) {
// throwables.printStackTrace();
// }
// if (obj instanceof DmdbStruct) {
// return toWkt((DmdbStruct) obj);
// }
// return String.valueOf(obj);
// }
//
// @Override
// public String getResult(Map<String, Object> row, String columnName) {
// Object obj = null;
// obj = row.get(columnName);
// if (obj instanceof DmdbStruct) {
// return toWkt((DmdbStruct) obj);
// }
// return String.valueOf(obj);
// }
//
// @Override
// public String getResult(Object obj) {
// if (obj instanceof DmdbStruct) {
// return toWkt((DmdbStruct) obj);
// }
// return String.valueOf(obj);
// }
//
// private final WKBReader wkbReader = new WKBReader();
//
// String toWkt(DmdbStruct value) {
// try {
// // 获取结构体中的成员
// Object[] attrs = value.getAttributes();
// Blob gSerObj = (Blob) attrs[0];
// int len = (int) gSerObj.length();
// // 获取 gserialized 为二进制数组
// byte[] gserialized = gSerObj.getBytes(1, len);
// // 将 gserialized 转换为
// byte[] wkb = DmGeo2Util.wkbFromGser(gserialized, DmGeo2Util.NDR);
// Geometry jtsGeom = wkbReader.read(wkb);
// return jtsGeom.toText();
// }
// catch (Exception e) {
// return "";
// }
// }
//
// }
