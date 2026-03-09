package cn.geoair.map.dynamic.dbservice.basic.executor;

import com.alibaba.fastjson.JSONObject;

import java.util.Map;

public interface Executor {

    Object execute(JSONObject taskJson, Map<String, Object> param) throws Exception;
}
