package cn.geoair.map.tile.forge.fuser.test;

import cn.geoair.map.dynamic.tools.grid.dto.RangeApo;
import cn.geoair.map.tile.forge.core.bygwc.core.mime.ImageMime;
import cn.geoair.map.tile.forge.fuser.fuser.FuserExec;

/**
 * FuserExec 契约示例
 */
public class FuserExecContractExample {

    public static void main(String[] args) throws Exception {
        FuserExec exec = new FuserExec() {
            @Override
            public byte[] toImageBytes() {
                return new byte[] {1, 2, 3};
            }

            @Override
            public ImageMime getOutputFormat() {
                return ImageMime.png;
            }

            @Override
            public ImageMime getSrcFormat() {
                return ImageMime.png;
            }

            @Override
            public RangeApo getSrcRange() {
                return new RangeApo(0, 10, 0, 10, 8);
            }
        };

        System.out.println("outputFormat = " + exec.getOutputFormat());
        System.out.println("srcFormat = " + exec.getSrcFormat());
        System.out.println("srcRange z = " + exec.getSrcRange().getZ());
        System.out.println("imageBytes length = " + exec.toImageBytes().length);
    }
}
