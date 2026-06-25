package cn.geoair.map.tile.forge.fuser.enums;


import cn.geoair.base.data.GiVisualValuable;

import java.awt.*;

/**
 * @author ：张俊
 * @date ：Created in 2026/6/12 17:15
 * @description： 枚举存储与三种配置（速度、质量、默认）之一相关的提示
 */
public enum HintsLevel  implements GiVisualValuable<String> {
    QUALITY(0, "quality"),
    DEFAULT(1, "default"),
    SPEED(2, "speed");

    private RenderingHints hints;

    private String mode;

    HintsLevel(int numHint, String mode) {
        this.mode = mode;
        switch (numHint) {
            // QUALITY HINTS
            case 0:
                hints =
                        new RenderingHints(
                                RenderingHints.KEY_COLOR_RENDERING,
                                RenderingHints.VALUE_COLOR_RENDER_QUALITY);
                hints.add(
                        new RenderingHints(
                                RenderingHints.KEY_ANTIALIASING,
                                RenderingHints.VALUE_ANTIALIAS_ON));
                hints.add(
                        new RenderingHints(
                                RenderingHints.KEY_FRACTIONALMETRICS,
                                RenderingHints.VALUE_FRACTIONALMETRICS_ON));
                hints.add(
                        new RenderingHints(
                                RenderingHints.KEY_ALPHA_INTERPOLATION,
                                RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY));
                hints.add(
                        new RenderingHints(
                                RenderingHints.KEY_INTERPOLATION,
                                RenderingHints.VALUE_INTERPOLATION_BICUBIC));
                hints.add(
                        new RenderingHints(
                                RenderingHints.KEY_RENDERING,
                                RenderingHints.VALUE_RENDER_QUALITY));
                hints.add(
                        new RenderingHints(
                                RenderingHints.KEY_TEXT_ANTIALIASING,
                                RenderingHints.VALUE_TEXT_ANTIALIAS_ON));
                hints.add(
                        new RenderingHints(
                                RenderingHints.KEY_STROKE_CONTROL,
                                RenderingHints.VALUE_STROKE_NORMALIZE));
                break;
            // DEFAULT HINTS
            case 1:
                hints =
                        new RenderingHints(
                                RenderingHints.KEY_COLOR_RENDERING,
                                RenderingHints.VALUE_COLOR_RENDER_DEFAULT);
                hints.add(
                        new RenderingHints(
                                RenderingHints.KEY_ANTIALIASING,
                                RenderingHints.VALUE_ANTIALIAS_DEFAULT));
                hints.add(
                        new RenderingHints(
                                RenderingHints.KEY_FRACTIONALMETRICS,
                                RenderingHints.VALUE_FRACTIONALMETRICS_DEFAULT));
                hints.add(
                        new RenderingHints(
                                RenderingHints.KEY_ALPHA_INTERPOLATION,
                                RenderingHints.VALUE_ALPHA_INTERPOLATION_DEFAULT));
                hints.add(
                        new RenderingHints(
                                RenderingHints.KEY_INTERPOLATION,
                                RenderingHints.VALUE_INTERPOLATION_BILINEAR));
                hints.add(
                        new RenderingHints(
                                RenderingHints.KEY_RENDERING,
                                RenderingHints.VALUE_RENDER_DEFAULT));
                hints.add(
                        new RenderingHints(
                                RenderingHints.KEY_TEXT_ANTIALIASING,
                                RenderingHints.VALUE_TEXT_ANTIALIAS_DEFAULT));
                hints.add(
                        new RenderingHints(
                                RenderingHints.KEY_STROKE_CONTROL,
                                RenderingHints.VALUE_STROKE_DEFAULT));
                break;
            // SPEED HINTS
            case 2:
                hints =
                        new RenderingHints(
                                RenderingHints.KEY_COLOR_RENDERING,
                                RenderingHints.VALUE_COLOR_RENDER_SPEED);
                hints.add(
                        new RenderingHints(
                                RenderingHints.KEY_ANTIALIASING,
                                RenderingHints.VALUE_ANTIALIAS_OFF));
                hints.add(
                        new RenderingHints(
                                RenderingHints.KEY_FRACTIONALMETRICS,
                                RenderingHints.VALUE_FRACTIONALMETRICS_OFF));
                hints.add(
                        new RenderingHints(
                                RenderingHints.KEY_ALPHA_INTERPOLATION,
                                RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED));
                hints.add(
                        new RenderingHints(
                                RenderingHints.KEY_INTERPOLATION,
                                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR));
                hints.add(
                        new RenderingHints(
                                RenderingHints.KEY_RENDERING,
                                RenderingHints.VALUE_RENDER_SPEED));
                hints.add(
                        new RenderingHints(
                                RenderingHints.KEY_TEXT_ANTIALIASING,
                                RenderingHints.VALUE_TEXT_ANTIALIAS_OFF));
                hints.add(
                        new RenderingHints(
                                RenderingHints.KEY_STROKE_CONTROL,
                                RenderingHints.VALUE_STROKE_PURE));
                break;
        }
    }

    public RenderingHints getRenderingHints() {
        return hints;
    }

    public String getModeName() {
        return mode;
    }

    public static HintsLevel getHintsForMode(String mode) {

        if (mode != null) {
            if (mode.equalsIgnoreCase(QUALITY.getModeName())) {
                return QUALITY;
            } else if (mode.equalsIgnoreCase(SPEED.getModeName())) {
                return SPEED;
            } else {
                return DEFAULT;
            }
        } else {
            return DEFAULT;
        }
    }
}
