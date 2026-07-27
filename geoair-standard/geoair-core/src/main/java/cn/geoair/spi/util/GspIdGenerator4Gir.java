package cn.geoair.spi.util;

import cn.geoair.base.gpa.id.GirIdGenerator;
import cn.geoair.base.lang.invoke.GaMethodHandImpl;
import cn.geoair.base.lang.invoke.GaMethodHandImpl.ImplType;
import cn.geoair.base.lang.invoke.GkMethodHand;
import cn.geoair.base.tool.GkSnowflake;
import java.util.UUID;

public class GspIdGenerator4Gir {

    static {
        GkMethodHand.implFromClass(GspIdGenerator4Gir.class);
        GirIdGenerator.setIdGeneratorProvider(
                new GirIdGenerator.IdGeneratorProvider() {
                    @Override
                    public String randomUUID() {
                        return GspIdGenerator4Gir.randomUUID();
                    }

                    @Override
                    public String simpleUUID() {
                        return GspIdGenerator4Gir.simpleUUID();
                    }

                    @Override
                    public long timestampId() {
                        return GspIdGenerator4Gir.timestampId();
                    }

                    @Override
                    public String timestampId36() {
                        return GspIdGenerator4Gir.timestampId36();
                    }
                });
    }

    private static GkSnowflake snowflake = null;

    public static GkSnowflake getGfSnowflake() {
        if (snowflake == null) {
            snowflake = new GkSnowflake(1, 1, false);
        }
        return snowflake;
    }

    @GaMethodHandImpl(
        implClass = GirIdGenerator.class,
        implMethod = "randomUUID",
        type = ImplType.comity
    )
    public static String randomUUID() {
        return UUID.randomUUID().toString();
    }

    @GaMethodHandImpl(
        implClass = GirIdGenerator.class,
        implMethod = "simpleUUID",
        type = ImplType.comity
    )
    public static String simpleUUID() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }

    @GaMethodHandImpl(
        implClass = GirIdGenerator.class,
        implMethod = "timestampId",
        type = ImplType.comity
    )
    public static long timestampId() {
        return getGfSnowflake().nextId();
    }

    @GaMethodHandImpl(
        implClass = GirIdGenerator.class,
        implMethod = "timestampId36",
        type = ImplType.comity
    )
    public static String timestampId36() {
        return Long.toString(timestampId(), 36);
    }
}
