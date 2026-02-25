package cn.geoair.map.dynamic.file.core.link;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author ：张逢吉
 * @date ：Created in 2022/2/9 14:47
 * @description： 文件链接的基本类
 */
@Data
@Accessors(chain = true)
public abstract class LinkInfo {


    /**
     * 检查链接是否可用
     */
    public abstract void checkLinkInfo();

}
