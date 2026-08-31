package cn.geoair.base.gpa.id;

import cn.geoair.base.lang.invoke.GaMethodHandDefine;
import cn.geoair.base.lang.invoke.GaMethodHandImpl;
import cn.geoair.base.lang.invoke.GkMethodHand;
import java.util.UUID;

public class GirIdGenerator {

    private static volatile IdGeneratorProvider idGeneratorProvider;

    public interface IdGeneratorProvider {
        String randomUUID();

        String simpleUUID();

        long timestampId();

        String timestampId36();
    }

    public static void setIdGeneratorProvider(IdGeneratorProvider idGeneratorProvider) {
        GirIdGenerator.idGeneratorProvider = idGeneratorProvider;
    }

    static {
        GkMethodHand.implFromClass(GirIdGenerator.class);
    }

    /**
     * 获取一个uuid
     *
     * @return 例如 0dd3de63-0fa7-4fea-9923-39fe198e1f58
     */
    @GaMethodHandDefine()
    public static String randomUUID() {
        IdGeneratorProvider provider = idGeneratorProvider;
        if (provider != null) {
            return provider.randomUUID();
        }
        return (String) GkMethodHand.invokeSelf();
    }

    @GaMethodHandImpl(
            implClass = GirIdGenerator.class,
            implMethod = "randomUUID",
            type = GaMethodHandImpl.ImplType.comity)
    private static String _randomUUID() {
        return UUID.randomUUID().toString();
    }

    /**
     * 获取一个32位的uuid
     *
     * @return 例如 0dd3de630fa74fea992339fe198e1f58
     */
    @GaMethodHandDefine()
    public static String simpleUUID() {
        IdGeneratorProvider provider = idGeneratorProvider;
        if (provider != null) {
            return provider.simpleUUID();
        }
        return (String) GkMethodHand.invokeSelf();
    }

    @GaMethodHandImpl(
            implClass = GirIdGenerator.class,
            implMethod = "simpleUUID",
            type = GaMethodHandImpl.ImplType.comity)
    private static String _simpleUUID() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }

    /**
     * 获取19位的时间戳id(默认雪花算法的id)
     *
     * @return
     */
    @GaMethodHandDefine()
    public static long timestampId() {
        IdGeneratorProvider provider = idGeneratorProvider;
        if (provider != null) {
            return provider.timestampId();
        }
        return (long) GkMethodHand.invokeSelf();
    }

    /**
     * 获取19位的时间戳id
     *
     * @return
     */
    @GaMethodHandDefine()
    public static String timestampId36() {
        IdGeneratorProvider provider = idGeneratorProvider;
        if (provider != null) {
            return provider.timestampId36();
        }
        return (String) GkMethodHand.invokeSelf();
    }

    /*
     * private static int DEVICE_NUMBER = 20;
     *
     *
     * public static void setDeviceNumber(int dn) {
     *
     * if(dn > 9 && dn < 100) { DEVICE_NUMBER = dn; }else { throw new
     * gtcException("DEVICE_NUMBER 是一个10-100之间的数."); } }
     *
     * private static String LAST_ID; private static int LAST_INC = 0;
     *
     * private static synchronized int getLastInc(String millStr) {
     * if(millStr.equals(LAST_ID)) { LAST_INC++; }else { LAST_INC = 0; LAST_ID = millStr;
     * } return LAST_INC; }
     *
     *
     * @MethodHandImpl(implClass=
     * gtcIdGenerator.class,implMethod="timestampId",type=ImplType.comity) private static
     * String _timestampId(String prefix,int len){ StringBuilder sb = new StringBuilder();
     * sb.append(DEVICE_NUMBER); Calendar calendar = Calendar.getInstance();
     * sb.append(String.valueOf(calendar.get(Calendar.YEAR)).substring(2));
     * appendTime2(sb,calendar.get(Calendar.MONTH) + 1);
     * appendTime2(sb,calendar.get(Calendar.DAY_OF_MONTH));
     * appendTime2(sb,calendar.get(Calendar.HOUR_OF_DAY));
     * appendTime2(sb,calendar.get(Calendar.MINUTE));
     * appendTime2(sb,calendar.get(Calendar.SECOND));
     * appendTime3(sb,calendar.get(Calendar.MILLISECOND));
     * appendTime3(sb,getLastInc(sb.toString()));
     *
     *
     * int strLen = sb.length(); if (prefix == null || "".equals(prefix)){ if (strLen <
     * len){ for (int i = 0; i < len - strLen; i++) { sb.append(0); } } }else { strLen =
     * strLen + prefix.length(); if (strLen < len){ for (int i = 0; i < len - strLen; i++)
     * { sb.insert(0,0); } } sb.insert(0, prefix); } return sb.toString(); }
     *
     * private static void appendTime2(StringBuilder sb,int time) { if(time < 10) {
     * sb.append(0); } sb.append(time); } private static void appendTime3(StringBuilder
     * sb,int time) { if(time < 10) { sb.append(0).append(0); }else if(time < 100) {
     * sb.append(0); } sb.append(time); }
     */

}
