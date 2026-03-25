package cn.geoair.map.dynamic.geoserver.gss;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.impl.RESTAccessRuleDAO;
import org.geotools.util.logging.Logging;

/**
 * @author ：张逢吉
 * @date ：Created in 19:07 @description： TODO
 */
public class GssRESTAccessRuleDAO extends RESTAccessRuleDAO {

    private static final Logger LOGGER = Logging.getLogger(RESTAccessRuleDAO.class);

    protected GssRESTAccessRuleDAO(GeoServerDataDirectory dd) throws IOException {
        super(dd);
    }

    public static RESTAccessRuleDAO get() {
        return GeoServerExtensions.bean(RESTAccessRuleDAO.class);
    }

    /** rule pattern */
    static final Pattern PATTERN =
            Pattern.compile(
                    "\\S+;(GET|POST|PUT|DELETE|HEAD)(,(GET|POST|PUT|DELETE|HEAD))*=\\S+(, ?\\S+)*");

    @Override
    protected void loadRules(Properties props) {
        // no concurrent version of LinkedHashMap, making it synchronized to preserve rule
        // order
        rules = Collections.synchronizedSet(new LinkedHashSet<>());
        for (Map.Entry<Object, Object> entry : props.entrySet()) {
            String key = (String) entry.getKey();
            String val = (String) entry.getValue();

            String rule = key + "=" + val;
            if (!PATTERN.matcher(rule).matches()) {
                LOGGER.severe("Ignoring '" + rule + "' not matching " + PATTERN);
                continue;
            }
            rule = rule.replaceAll(";", ":");
            rules.add(rule);
        }
    }

    @Override
    protected Properties toProperties() {
        Properties props = new Properties();
        for (String rule : rules) {
            rule = rule.replaceAll(":", ";");
            if (!PATTERN.matcher(rule).matches()) {
                LOGGER.severe("Invalid '" + rule + "' not matching " + PATTERN);
                continue;
            }
            String[] parts = rule.split("=");
            props.setProperty(parts[0], parts[1]);
        }
        return props;
    }
}
