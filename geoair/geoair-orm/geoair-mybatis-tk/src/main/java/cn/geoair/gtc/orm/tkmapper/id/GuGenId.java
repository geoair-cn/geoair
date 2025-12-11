package cn.geoair.gtc.orm.tkmapper.id;

import org.apache.ibatis.reflection.MetaObject;
import cn.geoair.gtc.base.gpa.id.GiGenId;
import tk.mybatis.mapper.MapperException;
import tk.mybatis.mapper.genid.GenId;
import tk.mybatis.mapper.util.MetaObjectUtil;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 *
 */
public class GuGenId {

    public static final Map<Class<? extends GenId<?>>, GenId<?>> CACHE = new ConcurrentHashMap<Class<? extends GenId<?>>, GenId<?>>();

    public static final ReentrantLock LOCK = new ReentrantLock();

    /**
     * 生成 Id
     *
     * @param target
     * @param property
     * @param genClass
     * @param table
     * @param column
     * @throws MapperException
     */
    public static void genId(Object target, String property, Class<? extends GenId<?>> genClass,String table, String column) throws MapperException {

    	try {
    		GenId<?> genId;
            if (CACHE.containsKey(genClass)) {
                genId = CACHE.get(genClass);
            } else {
                LOCK.lock();
                try {
                    if (!CACHE.containsKey(genClass)) {
                        CACHE.put(genClass, genClass.newInstance());
                    }
                    genId = CACHE.get(genClass);
                } finally {
                    LOCK.unlock();
                }
            }
            MetaObject metaObject = MetaObjectUtil.forObject(target);
            if (metaObject.getValue(property) == null) {
            	Object id;
            	if(GiGenId.class.isAssignableFrom(genClass)) {
            		id = ((GiGenId<?>)genId).genId(target, table, column);
            	}else {
            		id = genId.genId(table, column);
            	}
                metaObject.setValue(property, id);
            }
        } catch (Exception e) {
            throw new MapperException("生成 ID 失败!", e);
        }
    }


}
