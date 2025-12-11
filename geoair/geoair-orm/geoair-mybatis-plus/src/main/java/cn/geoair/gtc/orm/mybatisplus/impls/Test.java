package cn.geoair.gtc.orm.mybatisplus.impls;

import com.baomidou.mybatisplus.core.toolkit.sql.StringEscape;

/**
 * @author ：张俊
 * @date ：Created in 2023/6/14 16:12
 * @description： TODO
 */
public class Test {

    public static void main(String[] args) {
        String string = StringEscape.escapeString("{};");
        System.out.println(string);
    }

}
