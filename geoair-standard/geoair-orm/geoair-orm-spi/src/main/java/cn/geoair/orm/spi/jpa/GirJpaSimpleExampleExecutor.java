package cn.geoair.orm.spi.jpa;

import java.util.List;

import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.hibernate.SQLQuery;
import org.hibernate.transform.Transformers;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import cn.geoair.base.data.page.GiPageParam;
import cn.geoair.base.data.page.GiPager;
import cn.geoair.base.data.page.support.GirPager;
import cn.geoair.base.data.tuples.GkPair;
import cn.geoair.base.json.GirJSON;
import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.geoair.orm.spi.GirExampleExecutor;
// import javafx.util.GkPair;
import cn.geoair.orm.spi.support.GirExample;
import cn.geoair.orm.spi.support.GirSimpleExampleParser;

/** 简单的 jpa Example 查询条件器的 执行器 */
@Component
public class GirJpaSimpleExampleExecutor implements GirExampleExecutor {

	private final GiLogger log = GirLoggerFactory.getLogger(GirJpaSimpleExampleExecutor.class);

	@Resource
	EntityManagerProvider entityManagerProvider;

	// 转换器
	GirSimpleExampleParser girSimpleExampleParser = new GirSimpleExampleParser();

	@Override
	public Number selectCountByExample(GirExample girExample) {
		GkPair<String, List<Object>> pair = girSimpleExampleParser.selectCountByExample(girExample);
		String key = pair.getValue0();
		log.debug(" selectCountByExample sql语句:{}", pair.getValue0());
		log.debug("执行参数:{}", GirJSON.toJson(pair.getValue1()).toJSONString());
		EntityManager entityManager = entityManagerProvider.getEntityManager();
		Query nativeQuery = entityManager.createNativeQuery(key);
		for (int i = 0; i < pair.getValue1().size(); i++) {
			nativeQuery.setParameter(i + 1, pair.getValue1().get(i));
		}

		return (Number) nativeQuery.getSingleResult();
	}

	@Override
	@Transactional
	public Integer deleteByExample(GirExample girExample) {
		GkPair<String, List<Object>> pair = girSimpleExampleParser.deleteByExample(girExample);
		String key = pair.getValue0();
		log.debug(" deleteByExample sql语句:{}", pair.getValue0());
		log.debug("执行参数:{}", GirJSON.toJson(pair.getValue1()).toJSONString());
		EntityManager entityManager = entityManagerProvider.getEntityManager();
		Query nativeQuery = entityManager.createNativeQuery(key);
		for (int i = 0; i < pair.getValue1().size(); i++) {
			nativeQuery.setParameter(i + 1, pair.getValue1().get(i));
		}
		return nativeQuery.executeUpdate();
	}

	@Override
	public <E> List<E> selectByExample(GirExample girExample) {
		GkPair<String, List<Object>> pair = girSimpleExampleParser.selectByExample(girExample);
		String key = pair.getValue0();
		log.debug(" selectByExample sql语句:{}", pair.getValue0());
		log.debug("执行参数:{}", GirJSON.toJson(pair.getValue1()).toJSONString());
		EntityManager entityManager = entityManagerProvider.getEntityManager();
		Query nativeQuery = entityManager.createNativeQuery(key);
		nativeQuery.unwrap(SQLQuery.class).setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP);
		for (int i = 0; i < pair.getValue1().size(); i++) {
			nativeQuery.setParameter(i + 1, pair.getValue1().get(i));
		}
		return nativeQuery.getResultList();
	}

	@Override
	public <E> List<E> selectByExample(E type, GirExample girExample) {
		GkPair<String, List<Object>> pair = girSimpleExampleParser.selectByExample(girExample);
		String key = pair.getValue0();
		log.info(" selectByExample sql语句:{}", pair.getValue0());
		log.info("执行参数:{}", GirJSON.toJson(pair.getValue1()).toJSONString());
		EntityManager entityManager = entityManagerProvider.getEntityManager();
		Query nativeQuery = entityManager.createNativeQuery(key, type.getClass());

		for (int i = 0; i < pair.getValue1().size(); i++) {
			nativeQuery.setParameter(i + 1, pair.getValue1().get(i));
		}
		List<E> resultList = nativeQuery.getResultList();
		return resultList;
	}

	@Override
	public <E> GiPager<E> selectPageByExample(E type, GirExample girExample, GiPageParam pageParam) {
		Number count = selectCountByExample(girExample);
		GkPair<String, List<Object>> pair = girSimpleExampleParser.selectByExample(girExample);
		String key = pair.getValue0();
		log.info(" selectByExample sql语句:{}", pair.getValue0());
		log.info("执行参数:{}", GirJSON.toJson(pair.getValue1()).toJSONString());
		EntityManager entityManager = entityManagerProvider.getEntityManager();
		Query nativeQuery = entityManager.createNativeQuery(key, type.getClass());
		for (int i = 0; i < pair.getValue1().size(); i++) {
			nativeQuery.setParameter(i + 1, pair.getValue1().get(i));
		}
		nativeQuery.setFirstResult(pageParam.pageSize() * (pageParam.pageNum() - 1));
		nativeQuery.setMaxResults(pageParam.pageSize());
		List resultList = nativeQuery.getResultList();
		return new GirPager<>().put(resultList, count.longValue(), pageParam);
	}

	@Override
	public <E> GiPager<E> selectPageByExample(GirExample girExample, GiPageParam pageParam) {
		Number count = selectCountByExample(girExample);
		GkPair<String, List<Object>> pair = girSimpleExampleParser.selectByExample(girExample);
		String key = pair.getValue0();
		log.info(" selectByExample sql语句:{}", pair.getValue0());
		log.info("执行参数:{}", GirJSON.toJson(pair.getValue1()).toJSONString());
		EntityManager entityManager = entityManagerProvider.getEntityManager();
		Query nativeQuery = entityManager.createNativeQuery(key);
		for (int i = 0; i < pair.getValue1().size(); i++) {
			nativeQuery.setParameter(i + 1, pair.getValue1().get(i));
		}
		nativeQuery.setFirstResult(pageParam.pageSize() * (pageParam.pageNum() - 1));
		nativeQuery.setMaxResults(pageParam.pageSize());
		nativeQuery.unwrap(SQLQuery.class).setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP);
		List resultList = nativeQuery.getResultList();
		return new GirPager<>().put(resultList, count.longValue(), pageParam);
	}

	@Override
	public <E> List<E> selectByExampleAndRowBounds(GirExample girExample) {
		return selectByExample(girExample);
	}

	@Override
	@Transactional
	public Integer updateByExampleSelective(Object updateEntity, GirExample girExample) {
		GkPair<String, List<Object>> pair = girSimpleExampleParser.updateByExampleSelective(updateEntity, girExample);
		String key = pair.getValue0();
		log.info(" updateByExampleSelective sql语句:{}", pair.getValue0());
		log.info("执行参数:{}", GirJSON.toJson(pair.getValue1()).toJSONString());
		EntityManager entityManager = entityManagerProvider.getEntityManager();
		Query nativeQuery = entityManager.createNativeQuery(key);
		for (int i = 0; i < pair.getValue1().size(); i++) {
			nativeQuery.setParameter(i + 1, pair.getValue1().get(i));
		}
		return nativeQuery.executeUpdate();
	}

	@Override
	@Transactional
	public Integer updateByExample(Object updateEntity, GirExample girExample) {
		GkPair<String, List<Object>> pair = girSimpleExampleParser.updateByExample(updateEntity, girExample);
		String key = pair.getValue0();
		log.info(" updateByExample sql语句:{}", pair.getValue0());
		log.info("执行参数:{}", GirJSON.toJson(pair.getValue1()).toJSONString());
		EntityManager entityManager = entityManagerProvider.getEntityManager();
		Query nativeQuery = entityManager.createNativeQuery(key);
		for (int i = 0; i < pair.getValue1().size(); i++) {
			nativeQuery.setParameter(i + 1, pair.getValue1().get(i));
		}
		return nativeQuery.executeUpdate();
	}

}
