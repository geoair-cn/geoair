// package com.gtc.orm.spi.dynamictable;
//
// import com.gtc.orm.spi.entity. gtcEntityColumn;
//
/// **
// * 空间数据类型的 EntityColumn
// */
// public class gtcGeometryEntityColumn extends gtcEntityColumn {
//
// public String getSelectColumn() {
// return "st_asewkt(" + column + ")";
// }
//
// public Object wrapColumnValue(Object value) {
// return "ST_GeometryFromText((" + value + " ||''),4326)";
// }
// }
