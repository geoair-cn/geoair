package cn.geoair.map.tile.forge.fuser.fuser;

import cn.geoair.map.dynamic.tools.grid.dto.RangeApo;
import cn.geoair.map.tile.forge.core.bygwc.core.mime.ImageMime;

/**
 * @author ：zhangjun
 * @date ：Created in 2026/6/15 12:39
 * @description： 合并的具体实现
 */
public interface FuserExec {

    byte[] toImageBytes() throws Exception;

    ImageMime getOutputFormat();

    ImageMime getSrcFormat();

    RangeApo getSrcRange();

}
