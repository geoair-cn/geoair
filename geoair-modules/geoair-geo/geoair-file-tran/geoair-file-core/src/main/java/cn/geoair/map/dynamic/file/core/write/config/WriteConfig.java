package cn.geoair.map.dynamic.file.core.write.config;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author ：张逢吉
 * @date ：Created in 2022/2/9 15:06 @description： 写入相关配置
 */
@Data
@Accessors(chain = true)
public class WriteConfig {

    /** 输出的srid */
    private int outPutSrid = 4326;
}
