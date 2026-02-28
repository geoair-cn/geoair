package cn.geoair.gtc.orm.mybatis.plugins;

import java.sql.Statement;
import java.util.List;
import java.util.Properties;

import org.apache.ibatis.executor.resultset.ResultSetHandler;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.springframework.stereotype.Component;

@Intercepts(
		value = { @Signature(args = { Statement.class }, method = "handleResultSets", type = ResultSetHandler.class) })
@Component
public class HandleResultPlugin implements Interceptor {

	public Object intercept(Invocation invocation) throws Throwable {
		Object obj = invocation.proceed();
		if (obj instanceof List) {
			// gtcEntitySelecter.afterSelectModel(null,null,obj);
		}
		return obj;

		/*
		 * MappedStatement state = (MappedStatement) invocation.getArgs()[0];
		 * SqlCommandType sqlType = state.getSqlCommandType(); Object parameter =
		 * invocation.getArgs()[1]; Class<?> entityClass =
		 * HandleUpdatePlugin.getEntityClass(((MappedStatement) invocation.getArgs()[0]));
		 * gtcModelSelecter.beforeSelectModel(parameter, entityClass); Object obj =
		 * invocation.proceed(); gtcModelSelecter.afterSelectModel(obj, entityClass,obj);
		 * return obj;
		 */
	}

	@Override
	public Object plugin(Object target) {
		if (target instanceof ResultSetHandler) {
			return Plugin.wrap(target, this);
		}
		return target;
	}

	@Override
	public void setProperties(Properties properties) {
	}

}
