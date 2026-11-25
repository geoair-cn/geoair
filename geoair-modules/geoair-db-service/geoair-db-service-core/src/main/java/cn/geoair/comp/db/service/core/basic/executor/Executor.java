package cn.geoair.comp.db.service.core.basic.executor;

import com.alibaba.fastjson2.JSONObject;

import java.util.Map;

public interface Executor {

    Object execute(JSONObject taskJson, Map<String, Object> param) throws Exception;
}
