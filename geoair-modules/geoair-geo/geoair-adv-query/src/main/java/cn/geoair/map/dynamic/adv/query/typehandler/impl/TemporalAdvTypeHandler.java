package cn.geoair.map.dynamic.adv.query.typehandler.impl;

import cn.geoair.map.dynamic.adv.query.typehandler.AdvBaseTypeHandler;
import cn.geoair.map.dynamic.adv.query.typehandler.AdvTypeHandlerContext;

import java.sql.Time;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Date;

/**
 * @author ：张逢吉
 * @date ：Created in 2026/7/22
 * @description： 日期时间类型处理器
 */
public class TemporalAdvTypeHandler extends AdvBaseTypeHandler<Object> {

    @Override
    public boolean supports(Class<?> javaType, Object value) {
        if (javaType == null) {
            return false;
        }
        return javaType == Date.class
                || javaType == java.sql.Date.class
                || javaType == Time.class
                || javaType == Timestamp.class
                || javaType == LocalDate.class
                || javaType == LocalTime.class
                || javaType == LocalDateTime.class
                || javaType == OffsetDateTime.class;
    }

    @Override
    protected Object convertNonNullForRead(
            Object value, Class<?> javaType, AdvTypeHandlerContext context) {
        if (javaType.isInstance(value)) {
            return value;
        }
        Date date = toDate(value);
        if (date == null) {
            return value;
        }
        if (javaType == Date.class) {
            return date;
        }
        if (javaType == java.sql.Date.class) {
            return new java.sql.Date(date.getTime());
        }
        if (javaType == Time.class) {
            return new Time(date.getTime());
        }
        if (javaType == Timestamp.class) {
            return new Timestamp(date.getTime());
        }
        Instant instant = Instant.ofEpochMilli(date.getTime());
        ZoneId zoneId = ZoneId.systemDefault();
        if (javaType == LocalDate.class) {
            return instant.atZone(zoneId).toLocalDate();
        }
        if (javaType == LocalTime.class) {
            return instant.atZone(zoneId).toLocalTime();
        }
        if (javaType == LocalDateTime.class) {
            return instant.atZone(zoneId).toLocalDateTime();
        }
        if (javaType == OffsetDateTime.class) {
            return instant.atZone(zoneId).toOffsetDateTime();
        }
        return value;
    }

    private Date toDate(Object value) {
        if (value instanceof Date) {
            return (Date) value;
        }
        if (value instanceof Timestamp) {
            return new Date(((Timestamp) value).getTime());
        }
        if (value instanceof Time) {
            return new Date(((Time) value).getTime());
        }
        if (value instanceof LocalDateTime) {
            LocalDateTime dateTime = (LocalDateTime) value;
            return Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant());
        }
        if (value instanceof LocalDate) {
            LocalDate localDate = (LocalDate) value;
            return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        }
        if (value instanceof LocalTime) {
            LocalTime localTime = (LocalTime) value;
            return Date.from(localTime.atDate(LocalDate.now()).atZone(ZoneId.systemDefault()).toInstant());
        }
        if (value instanceof OffsetDateTime) {
            return Date.from(((OffsetDateTime) value).toInstant());
        }
        if (value instanceof Number) {
            return new Date(((Number) value).longValue());
        }
        try {
            return Timestamp.valueOf(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }
}
