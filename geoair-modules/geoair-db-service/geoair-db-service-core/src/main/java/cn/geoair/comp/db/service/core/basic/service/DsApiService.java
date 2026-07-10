package cn.geoair.comp.db.service.core.basic.service;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.geoair.comp.db.service.core.basic.apo.ApiConfigApo;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
 
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/**
 * @program: dbApi
 * @description:
 * @author: 武汉刘德华
 * @create: 2021-01-20 15:36
 */
@Service
 
public class DsApiService {
    public static GiLogger log = GirLoggerFactory.getLogger();
    public Map<String, Object> getSqlParam(HttpServletRequest request, ApiConfigApo config) {
        Map<String, Object> map = new HashMap<>();

        JSONArray requestParams = JSON.parseArray(config.getParams());
        for (int i = 0; i < requestParams.size(); i++) {
            JSONObject jo = requestParams.getJSONObject(i);
            String name = jo.getString("name");
            String type = jo.getString("type");

            // 数组类型参数
            if (type.startsWith("Array")) {
                String[] values = request.getParameterValues(name);
                if (values != null) {
                    List<String> list = Arrays.asList(values);
                    if (values.length > 0) {
                        switch (type) {
                            case "Array<double>":
                                List<Double> collect =
                                        list.stream()
                                                .map(value -> Double.valueOf(value))
                                                .collect(Collectors.toList());
                                map.put(name, collect);
                                break;
                            case "Array<bigint>":
                                List<Long> longs =
                                        list.stream()
                                                .map(value -> Long.valueOf(value))
                                                .collect(Collectors.toList());
                                map.put(name, longs);
                                break;
                            case "Array<string>":
                            case "Array<date>":
                                map.put(name, list);
                                break;
                        }
                    } else {
                        map.put(name, list);
                    }
                } else {
                    map.put(name, null);
                }
            } else {

                String value = request.getParameter(name);
                if (StringUtils.isNotBlank(value)) {

                    switch (type) {
                        case "double":
                            Double v = Double.valueOf(value);
                            map.put(name, v);
                            break;
                        case "bigint":
                            Long longV = Long.valueOf(value);
                            map.put(name, longV);
                            break;
                        case "string":
                        case "date":
                            map.put(name, value);
                            break;
                    }
                } else {
                    map.put(name, value);
                }
            }
        }
        return map;
    }
}
