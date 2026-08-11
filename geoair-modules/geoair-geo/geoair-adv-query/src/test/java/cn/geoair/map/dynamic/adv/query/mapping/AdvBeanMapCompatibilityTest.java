package cn.geoair.map.dynamic.adv.query.mapping;

import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;
import cn.geoair.map.dynamic.adv.query.typehandler.AdvTypeHandlerRegistry;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;

public class AdvBeanMapCompatibilityTest {

    private final AdvTypeHandlerRegistry registry = AdvTypeHandlerRegistry.defaultInstance();

    @Test
    public void shouldResolveColumnNameForMapClass() {
        AdvBeanMappingMeta mappingMeta = AdvBeanMappingMeta.of(Map.class);
        Assert.assertTrue(mappingMeta.isMapType());
        Assert.assertEquals("user_name", mappingMeta.resolveColumnName("userName", true));
        Assert.assertEquals("userName", mappingMeta.resolveColumnName("userName", false));
        Assert.assertNull(mappingMeta.resolvePropertyByColumnOrProperty("user_name"));
    }

    @Test
    public void shouldConvertMapToColumnValueMap() {
        AdvBeanColumnMapper mapper = new AdvBeanColumnMapper(registry);
        LinkedHashMap<String, Object> input = new LinkedHashMap<>();
        input.put("userName", "Alice");
        input.put("age", 18);
        input.put("emptyValue", "   ");
        input.put("skipMe", "ignored");
        input.put("nullValue", null);

        Map<String, Object> rowData = mapper.toColumnValueMap(
                input,
                true,
                true,
                true,
                Arrays.asList("skipMe"));

        Assert.assertEquals("Alice", rowData.get("user_name"));
        Assert.assertEquals(18, rowData.get("age"));
        Assert.assertFalse(rowData.containsKey("empty_value"));
        Assert.assertFalse(rowData.containsKey("skip_me"));
        Assert.assertFalse(rowData.containsKey("null_value"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldMapResultSetRowToMap() throws SQLException {
        ResultSet rs = buildSingleRowResultSet(
                new String[] {"user_name", "age"},
                new Object[] {"Alice", 18});

        AdvBeanMapper mapper = new AdvBeanMapper(registry);
        Assert.assertTrue(rs.next());
        Map<String, Object> mapped = mapper.mapRow(rs, Map.class);

        Assert.assertTrue(mapped instanceof LinkedHashMap);
        Assert.assertEquals("Alice", mapped.get("user_name"));
        Assert.assertEquals(18, mapped.get("age"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldMapResultSetListToMapList() throws SQLException {
        ResultSet rs = buildMultiRowResultSet(
                new String[] {"user_name", "age"},
                new Object[][] {
                        {"Alice", 18},
                        {"Bob", 20}
                });

        AdvBeanMapper mapper = new AdvBeanMapper(registry);
        List<Map> mapped = mapper.mapList(rs, Map.class);

        Assert.assertEquals(2, mapped.size());
        Assert.assertEquals("Alice", mapped.get(0).get("user_name"));
        Assert.assertEquals(18, mapped.get(0).get("age"));
        Assert.assertEquals("Bob", mapped.get(1).get("user_name"));
        Assert.assertEquals(20, mapped.get(1).get("age"));
    }

    private ResultSet buildSingleRowResultSet(String[] columns, Object[] values) {
        return buildMultiRowResultSet(columns, new Object[][] {values});
    }

    private ResultSet buildMultiRowResultSet(String[] columns, Object[][] rows) {
        ResultSetMetaData metaData = buildMetaData(columns);
        InvocationHandler handler = new InvocationHandler() {
            private int cursor = -1;

            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                String methodName = method.getName();
                if ("next".equals(methodName)) {
                    cursor++;
                    return cursor < rows.length;
                }
                if ("getMetaData".equals(methodName)) {
                    return metaData;
                }
                if ("getObject".equals(methodName)) {
                    if (args[0] instanceof Integer) {
                        return rows[cursor][((Integer) args[0]) - 1];
                    }
                    String columnLabel = String.valueOf(args[0]);
                    int idx = findColumnIndex(columns, columnLabel);
                    return idx >= 0 ? rows[cursor][idx] : null;
                }
                if ("close".equals(methodName)) {
                    return null;
                }
                if ("wasNull".equals(methodName)) {
                    return false;
                }
                return defaultValue(method.getReturnType());
            }
        };
        return (ResultSet) Proxy.newProxyInstance(
                ResultSet.class.getClassLoader(),
                new Class<?>[] {ResultSet.class},
                handler);
    }

    private ResultSetMetaData buildMetaData(String[] columns) {
        InvocationHandler handler = (proxy, method, args) -> {
            String methodName = method.getName();
            if ("getColumnCount".equals(methodName)) {
                return columns.length;
            }
            if ("getColumnLabel".equals(methodName) || "getColumnName".equals(methodName)) {
                return columns[((Integer) args[0]) - 1];
            }
            return defaultValue(method.getReturnType());
        };
        return (ResultSetMetaData) Proxy.newProxyInstance(
                ResultSetMetaData.class.getClassLoader(),
                new Class<?>[] {ResultSetMetaData.class},
                handler);
    }

    private int findColumnIndex(String[] columns, String columnLabel) {
        for (int i = 0; i < columns.length; i++) {
            if (columns[i].equalsIgnoreCase(columnLabel)) {
                return i;
            }
        }
        return -1;
    }

    private Object defaultValue(Class<?> returnType) {
        if (returnType == null || !returnType.isPrimitive()) {
            return null;
        }
        if (boolean.class.equals(returnType)) {
            return false;
        }
        if (char.class.equals(returnType)) {
            return '\0';
        }
        if (byte.class.equals(returnType)) {
            return (byte) 0;
        }
        if (short.class.equals(returnType)) {
            return (short) 0;
        }
        if (int.class.equals(returnType)) {
            return 0;
        }
        if (long.class.equals(returnType)) {
            return 0L;
        }
        if (float.class.equals(returnType)) {
            return 0F;
        }
        if (double.class.equals(returnType)) {
            return 0D;
        }
        return null;
    }
}
