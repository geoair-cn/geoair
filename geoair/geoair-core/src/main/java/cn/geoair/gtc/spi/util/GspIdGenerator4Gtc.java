package cn.geoair.gtc.spi.util;

import cn.geoair.gtc.base.gpa.id.GtcIdGenerator;
import cn.geoair.gtc.base.lang.invoke.GkMethodHand;
import cn.geoair.gtc.base.lang.invoke.GaMethodHandImpl;
import  cn.geoair.gtc.base.lang.invoke.GaMethodHandImpl.ImplType;
import cn.geoair.gtc.base.tool.GkSnowflake;

public class GspIdGenerator4Gtc {

	static {
		GkMethodHand.implFromClass(GspIdGenerator4Gtc.class);
	}

	private static GkSnowflake snowflake = null;
	public static GkSnowflake getGfSnowflake() {
		if(snowflake == null) {
			snowflake = new GkSnowflake(1,1,false);
		}
		return snowflake;
	}

	@GaMethodHandImpl(implClass= GtcIdGenerator.class,implMethod="timestampId",type=ImplType.comity)
    public static long timestampId(){
		return getGfSnowflake().nextId();
    }

}
