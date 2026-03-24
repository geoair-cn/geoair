package cn.geoair.map.dynamic.adv.mybatis;

import java.util.concurrent.ConcurrentHashMap;

import cn.geoair.map.dynamic.adv.mybatis.node.SqlNode;

public class Cache {

	ConcurrentHashMap<String, SqlNode> nodeCache = new ConcurrentHashMap<>();

	public ConcurrentHashMap<String, SqlNode> getNodeCache() {
		return nodeCache;
	}

}
