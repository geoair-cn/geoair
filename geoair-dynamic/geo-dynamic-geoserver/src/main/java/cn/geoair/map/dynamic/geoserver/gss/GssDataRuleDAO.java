package cn.geoair.map.dynamic.geoserver.gss;

import org.geoserver.catalog.Catalog;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.security.CatalogMode;
import org.geoserver.security.impl.DataAccessRule;
import org.geoserver.security.impl.DataAccessRuleDAO;

import java.io.IOException;
import java.util.Properties;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.regex.Pattern;

/**
 * @author ：张逢吉
 * @date ：Created in 18:59
 * @description： TODO
 */
public class GssDataRuleDAO extends DataAccessRuleDAO {

    CatalogMode catalogMode = CatalogMode.HIDE;

    private static Pattern DOT = Pattern.compile("\\.");

    /**
     * Builds a new dao
     *
     * @param dd
     * @param rawCatalog
     */
    public GssDataRuleDAO(GeoServerDataDirectory dd, Catalog rawCatalog) throws IOException {
        super(dd, rawCatalog);
    }

    @Override
    protected void loadRules(Properties props) {
        SortedSet<DataAccessRule> result = new ConcurrentSkipListSet<>();
        CatalogMode catalogMode = CatalogMode.HIDE;
        if (result.isEmpty()) {
            result.add(new DataAccessRule(DataAccessRule.READ_ALL));
            result.add(new DataAccessRule(DataAccessRule.WRITE_ALL));
        }

        this.catalogMode = catalogMode;
        this.rules = result;
    }

    @Override
    protected Properties toProperties() {
        Properties props = new Properties();
        props.put("mode", catalogMode.toString());
        for (DataAccessRule rule : rules) {
            StringBuilder sbKey =
                    new StringBuilder(DOT.matcher(rule.getRoot()).replaceAll("\\\\."));
            if (!rule.isGlobalGroupRule()) {
                sbKey.append(".").append(DOT.matcher(rule.getLayer()).replaceAll("\\\\."));
            }
            sbKey.append(".").append(rule.getAccessMode().getAlias());
            props.put(sbKey.toString(), rule.getValue());
        }
        return props;
    }
}
