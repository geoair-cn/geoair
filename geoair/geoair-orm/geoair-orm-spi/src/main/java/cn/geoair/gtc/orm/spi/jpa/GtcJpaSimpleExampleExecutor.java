package cn.geoair.gtc.orm.spi.jpa;

import cn.geoair.gtc.base.data.page.GiPageParam;
import cn.geoair.gtc.base.data.page.GiPager;
import cn.geoair.gtc.base.data.page.support.GtcPager;
import cn.geoair.gtc.base.json.GtcJSON;
import cn.geoair.gtc.base.log.GiLoger;
import cn.geoair.gtc.base.log.GtcLoger;
import cn.geoair.gtc.orm.spi.support.GtcExample;
import cn.geoair.gtc.orm.spi.support.GtcSimpleExampleParser;
import cn.geoair.gtc.orm.spi.GtcExampleExecutor;
//import javafx.util.GkPair;

import cn.geoair.gtc.base.data.tuples.GkPair;
import org.hibernate.SQLQuery;
import org.hibernate.transform.Transformers;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.List;

/**
 * 简单的 jpa Example 查询条件器的 执行器
 */
@Component
public class GtcJpaSimpleExampleExecutor implements GtcExampleExecutor {

    private final GiLoger log =  GtcLoger.getLoger( GtcJpaSimpleExampleExecutor.class);


    @Resource
    EntityManagerProvider entityManagerProvider;

    // 转换器
     GtcSimpleExampleParser gtcSimpleExampleParser = new GtcSimpleExampleParser();

    @Override
    public Number selectCountByExample( GtcExample gtcExample) {
        GkPair<String, List<Object>> pair =  gtcSimpleExampleParser.selectCountByExample( gtcExample);
        String key = pair.getValue0();
        log.info(" selectCountByExample sql语句:{}", pair.getValue0());
        log.info("执行参数:{}",  GtcJSON.toJson(pair.getValue1()).toJSONString());
        EntityManager entityManager = entityManagerProvider.getEntityManager();
        Query nativeQuery = entityManager.createNativeQuery(key);
        for (int i = 0; i < pair.getValue1().size(); i++) {
            nativeQuery.setParameter(i + 1, pair.getValue1().get(i));
        }

        return (Number) nativeQuery.getSingleResult();
    }

    @Override
    @Transactional
    public Integer deleteByExample( GtcExample gtcExample) {
        GkPair<String, List<Object>> pair =  gtcSimpleExampleParser.deleteByExample( gtcExample);
        String key =  pair.getValue0();
        log.info(" deleteByExample sql语句:{}",  pair.getValue0());
        log.info("执行参数:{}",  GtcJSON.toJson(pair.getValue1()).toJSONString());
        EntityManager entityManager = entityManagerProvider.getEntityManager();
        Query nativeQuery = entityManager.createNativeQuery(key);
        for (int i = 0; i < pair.getValue1().size(); i++) {
            nativeQuery.setParameter(i + 1, pair.getValue1().get(i));
        }
        return nativeQuery.executeUpdate();
    }

    @Override
    public <E> List<E> selectByExample( GtcExample gtcExample) {
        GkPair<String, List<Object>> pair =  gtcSimpleExampleParser.selectByExample( gtcExample);
        String key =  pair.getValue0();
        log.info(" selectByExample sql语句:{}",  pair.getValue0());
        log.info("执行参数:{}",  GtcJSON.toJson(pair.getValue1()).toJSONString());
        EntityManager entityManager = entityManagerProvider.getEntityManager();
        Query nativeQuery = entityManager.createNativeQuery(key);
        nativeQuery.unwrap(SQLQuery.class).setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP);
        for (int i = 0; i < pair.getValue1().size(); i++) {
            nativeQuery.setParameter(i + 1, pair.getValue1().get(i));
        }
        return nativeQuery.getResultList();
    }

    @Override
    public <E> List<E> selectByExample(E type,  GtcExample gtcExample) {
        GkPair<String, List<Object>> pair =  gtcSimpleExampleParser.selectByExample( gtcExample);
        String key =  pair.getValue0();
        log.info(" selectByExample sql语句:{}",  pair.getValue0());
        log.info("执行参数:{}",  GtcJSON.toJson(pair.getValue1()).toJSONString());
        EntityManager entityManager = entityManagerProvider.getEntityManager();
        Query nativeQuery = entityManager.createNativeQuery(key, type.getClass());

        for (int i = 0; i < pair.getValue1().size(); i++) {
            nativeQuery.setParameter(i + 1, pair.getValue1().get(i));
        }
        List<E> resultList = nativeQuery.getResultList();
        return resultList;
    }

    @Override
    public <E> GiPager<E> selectPageByExample(E type, GtcExample gtcExample, GiPageParam pageParam) {
        Number count = selectCountByExample( gtcExample);
        GkPair<String, List<Object>> pair =  gtcSimpleExampleParser.selectByExample( gtcExample);
        String key =  pair.getValue0();
        log.info(" selectByExample sql语句:{}",  pair.getValue0());
        log.info("执行参数:{}",  GtcJSON.toJson(pair.getValue1()).toJSONString());
        EntityManager entityManager = entityManagerProvider.getEntityManager();
        Query nativeQuery = entityManager.createNativeQuery(key, type.getClass());
        for (int i = 0; i < pair.getValue1().size(); i++) {
            nativeQuery.setParameter(i + 1, pair.getValue1().get(i));
        }
        nativeQuery.setFirstResult(pageParam.pageSize() * (pageParam.pageNum() - 1));
        nativeQuery.setMaxResults(pageParam.pageSize());
        List resultList = nativeQuery.getResultList();
        return new GtcPager<>().put(resultList, count.longValue(), pageParam);
    }

    @Override
    public <E> GiPager<E> selectPageByExample(GtcExample gtcExample, GiPageParam pageParam) {
        Number count = selectCountByExample( gtcExample);
        GkPair<String, List<Object>> pair =  gtcSimpleExampleParser.selectByExample( gtcExample);
        String key =  pair.getValue0();
        log.info(" selectByExample sql语句:{}",  pair.getValue0());
        log.info("执行参数:{}",  GtcJSON.toJson(pair.getValue1()).toJSONString());
        EntityManager entityManager = entityManagerProvider.getEntityManager();
        Query nativeQuery = entityManager.createNativeQuery(key);
        for (int i = 0; i < pair.getValue1().size(); i++) {
            nativeQuery.setParameter(i + 1, pair.getValue1().get(i));
        }
        nativeQuery.setFirstResult(pageParam.pageSize() * (pageParam.pageNum() - 1));
        nativeQuery.setMaxResults(pageParam.pageSize());
        nativeQuery.unwrap(SQLQuery.class).setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP);
        List resultList = nativeQuery.getResultList();
        return new GtcPager<>().put(resultList, count.longValue(), pageParam);
    }

    @Override
    public <E> List<E> selectByExampleAndRowBounds( GtcExample gtcExample) {
        return selectByExample( gtcExample);
    }

    @Override
    @Transactional
    public Integer updateByExampleSelective(Object updateEntity,  GtcExample gtcExample) {
        GkPair<String, List<Object>> pair =  gtcSimpleExampleParser.updateByExampleSelective(updateEntity,  gtcExample);
        String key =  pair.getValue0();
        log.info(" updateByExampleSelective sql语句:{}",  pair.getValue0());
        log.info("执行参数:{}",  GtcJSON.toJson(pair.getValue1()).toJSONString());
        EntityManager entityManager = entityManagerProvider.getEntityManager();
        Query nativeQuery = entityManager.createNativeQuery(key);
        for (int i = 0; i < pair.getValue1().size(); i++) {
            nativeQuery.setParameter(i + 1, pair.getValue1().get(i));
        }
        return nativeQuery.executeUpdate();
    }

    @Override
    @Transactional
    public Integer updateByExample(Object updateEntity,  GtcExample gtcExample) {
        GkPair<String, List<Object>> pair =  gtcSimpleExampleParser.updateByExample(updateEntity,  gtcExample);
        String key =  pair.getValue0();
        log.info(" updateByExample sql语句:{}",  pair.getValue0());
        log.info("执行参数:{}",  GtcJSON.toJson(pair.getValue1()).toJSONString());
        EntityManager entityManager = entityManagerProvider.getEntityManager();
        Query nativeQuery = entityManager.createNativeQuery(key);
        for (int i = 0; i < pair.getValue1().size(); i++) {
            nativeQuery.setParameter(i + 1, pair.getValue1().get(i));
        }
        return nativeQuery.executeUpdate();
    }
}
