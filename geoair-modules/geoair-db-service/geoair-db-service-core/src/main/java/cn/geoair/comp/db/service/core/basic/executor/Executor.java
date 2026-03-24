package cn.geoair.comp.db.service.core.basic.executor;

import java.util.Map;

import com.alibaba.fastjson2.JSONObject;

public interface Executor {

	Object execute(JSONObject taskJson, Map<String, Object> param) throws Exception;

}
