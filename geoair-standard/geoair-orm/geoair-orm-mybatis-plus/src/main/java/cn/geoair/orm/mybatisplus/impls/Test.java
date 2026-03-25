package cn.geoair.orm.mybatisplus.impls;

import cn.geoair.base.Gir;
import com.baomidou.mybatisplus.core.toolkit.sql.StringEscape;

/**
 * @author ：张俊
 * @date ：Created in 2023/6/14 16:12 @description： TODO
 */
public class Test {

    public static void main(String[] args) {
        String string = StringEscape.escapeString("{};");
        Gir.log.info(string);
    }
}
