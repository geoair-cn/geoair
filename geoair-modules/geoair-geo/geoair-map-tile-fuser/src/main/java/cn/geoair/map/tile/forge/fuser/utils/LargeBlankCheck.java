package cn.geoair.map.tile.forge.fuser.utils;

import lombok.Data;
import lombok.experimental.Accessors;

import java.awt.image.BufferedImage;

/**
 * @author ：张俊
 * @date ：Created in 2026/6/24 09:00
 * @description： TODO
 */
@Data
@Accessors(chain = true)
public class LargeBlankCheck {

    public static LargeBlankCheck of() {
        return new LargeBlankCheck();
    }

    Boolean blankIs;

    BufferedImage image = null;
}
