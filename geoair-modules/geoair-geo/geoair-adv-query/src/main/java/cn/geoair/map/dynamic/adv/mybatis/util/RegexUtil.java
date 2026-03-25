package cn.geoair.map.dynamic.adv.mybatis.util;

import cn.geoair.base.Gir;
import java.util.regex.Pattern;

public class RegexUtil {

    public static String replace(String content, String item, String newItem) {
        return content.replaceFirst("^\\s*" + item + "(?![^.,:\\s])", newItem);
    }

    public static void main(String[] args) {
        boolean matches = "item".matches("item" + "[.,:\\s\\[]");

        boolean item = Pattern.compile("item[.,:\\s\\[]").matcher("item").matches();

        // String aa = "item[0].name".replaceFirst("^\\s*" + "item" + "(?![^.,:\\s])",
        // "aa");
        // Gir.log.info(aa);
        Gir.log.info(item + "");
    }
}
