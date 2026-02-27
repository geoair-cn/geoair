package cn.geoair.map.dynamic.adv.mybatis;

import cn.geoair.map.dynamic.adv.mybatis.node.SqlNode;

import java.util.concurrent.ConcurrentHashMap;


public class Cache {

    ConcurrentHashMap<String, SqlNode> nodeCache = new ConcurrentHashMap<>();

    public ConcurrentHashMap<String, SqlNode> getNodeCache() {
        return nodeCache;
    }
}
