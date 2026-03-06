package cn.geoair.map.dynamic.geoserver.gss;

import org.geoserver.catalog.Catalog;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.security.impl.ServiceAccessRule;
import org.geoserver.security.impl.ServiceAccessRuleDAO;

import java.io.IOException;
import java.util.Properties;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * @author ：张逢吉
 * @date ：Created in 18:59 @description： TODO
 */
public class GssServiceDAO extends ServiceAccessRuleDAO {

	public GssServiceDAO(GeoServerDataDirectory dd, Catalog rawCatalog) throws IOException {
		super(dd, rawCatalog);
	}

	@Override
	protected void loadRules(Properties props) {
		SortedSet<ServiceAccessRule> result = new ConcurrentSkipListSet<>();
		result.add(new ServiceAccessRule(new ServiceAccessRule()));
		rules = result;
	}

	@Override
	protected Properties toProperties() {
		Properties props = new Properties();
		for (ServiceAccessRule rule : rules) {
			props.put(rule.getKey(), rule.getValue());
		}
		return props;
	}

}
