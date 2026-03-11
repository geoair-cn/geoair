package cn.geoair.map.dynamic.mvt.dto;

import cn.geoair.base.data.model.annotation.GaModelField;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.codec.Base32;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.URLUtil;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@Data
public class TileRequestParams {

    @GaModelField(text = "数据库资源ID")
    private Integer dsId;

    @GaModelField(text = "当前的切面名称")
    private String schemaName;

    @GaModelField(text = "表名或者sql")
    private String tbNameOrSql;

    @GaModelField(text = "当前表的srid")
    private String srid;

    @GaModelField(text = "空间字段名称")
    private String geomFieldName;

    @GaModelField(text = "字段列表")
    @Deprecated
    private String keepFields;

    @GaModelField(text = "响应所有字段")
    private boolean keepFieldAll;

    @GaModelField(text = "保留字段")
    private String filter;

    @GaModelField(text = "最小层级")
    private int minZoom;

    @GaModelField(text = "true就是4490网格，false就是3857网格")
    private boolean isGeo = false;

    @GaModelField(text = "坐标系转换，与isGeo相匹配，保证数据与网格的坐标系一致")
    private String transform;

    @GaModelField(text = "字段列表")
    private List<String> keepFieldList;

    public static TileRequestParams fromBase32(String baseString) {
        try {
            // 缓存未命中，执行原逻辑
            String encode = URLUtil.decode(baseString);
            String s = Base32.decodeStr(encode);
            TileRequestParams params = JSON.parseObject(s, TileRequestParams.class);
            return params;
        } catch (Exception e) {
            // 处理异常（如解码失败）
            throw new RuntimeException(
                    "Failed to parse TileRequestParams from base32: " + baseString, e);
        }
    }

    public String toBase32() {
        // 序列化为JSON时，忽略值为null的字段
        String jsonStr = JSON.toJSONString(this);
        // 移除空值，压缩体积
        JSONObject jsonObject = JSON.parseObject(jsonStr);
        jsonObject.entrySet().removeIf(entry -> ObjectUtil.isEmpty(entry.getValue()));
        // 对处理后的JSON字符串进行Base32编码
        String encode = Base32.encode(jsonObject.toString());
        return encode;
    }

    public static void main(String[] args) {
        System.out.println(
                fromBase32(
                        "PMRGOZLPEI5GMYLMONSSYITUOJQW443GN5ZG2IR2EI2DGMRWEIWCEZDTJFSCEORTGIWCE3LJNZNG633NEI5DSLBCORRE4YLNMVHXEU3RNQRDUITTMVWGKY3UEAVCAZTSN5WSA53POJVWM3DPO4XFYITQMFZGGZLML4YDQMBRLQRCELBCM5SW63KGNFSWYZCOMFWWKIR2EJTWK33NEIWCE43DNBSW2YKOMFWWKIR2EJ3W64TLMZWG65ZCFQRGWZLFOBDGSZLMMRGGS43UEI5FWITJMQRCYIS2IRBE2IRMEJNEITKDEIWCERCXIJGSELBCIRLU2QZCFQRFQWSRJBBE2IRMEJMFUUKIJVBSELBCKNDFQWRCFQRFGRSCJARCYISCJBMVSIRMEJNFUTKKEIWCEU2OIRNFUTKKEIWCETSZIRGUUIRMEJDUITKKEIWCER2CLJHFITKKEIWCEWKKJJBE4VCNJIRCYISKKNMUITKKEIWCEV2MLFGUUIRMEJDFUWSUEIWCEWSTJVBSELBCKFGEYWBCFQRFCTCYLIRCYISEJJFEOIRMEJCEUUSREIWCEWSTIJECELBCKFNEQIRMEJIUYURCFQREOWKRJMRCYIS2JQRCYISCIRBUIWKIEIWCEWKUEIWCEUKTKFJVEUJCFQRFCU22LJJFCIRMEJDEUIRMEJHEWSSZJVFCELBCLJDUUVCNJIRCYIS2I5NE4TKKEIWCEV2MKJMU2SRCFQREQWSKLFGUUIRMEJLUYR2TJVFCELBCKFKFUVCNJIRCYISXJJMU2SRCFQRFAR2SLJGUUIRMEJIEOUS2JJCSELBCINJE2SRCFQREGUSKIURCYIS2JRGUUIRMEJNEYSSFEIWCEWSKINNE2SRCFQRFUSSDLJFEKIRMEJCFSRCCJVFCELBCIRMUIQSKIURCYISZKBNFSRC2IZMUUIRMEJJUUTSLLFFCELBCKRME2SRCFQRFSRCNJIRCYISMIRGUUIRMEJBUITKKEIWCEU2ZJVFCELBCKFKFIRCNJIRCYISRLJJU2SRCFQRFUWSBJFGUUIRMEJJU4RC2LJAUSTKKEIWCEUKRIZNEEQ2TJURCYISRKRNFIIRMEJLUUWKZLERCYISTKFFFSTKKEIWCEU2RJJMUURJCFQRFCVCGKMRCYISRKRDFGTKKEIWCEUKUIZJUURJCFQRFCU22LFGUUIRMEJIVGWSZJZMUITKKEIWCEUKTLJMUORCNJIRCYISRKNNFSSSTLFCE2SRCFQRFCU22LFLUYWKNJIRCYISRKNNFSWSNINGCELBCKFNE2SRCFQRFCWSOLFCE2SRCFQRFCWSHIRGUUIRMEJIVUSSTLFCE2SRCFQRFCWSXJRMU2SRCFQRFCWS2JVBUYIRMEJJUQTKKEIWCEU2IJZMUITKKEIWCEU2IJJJVSRCNJIRCYISTJBLUYWKNJIRCYISEIZNEMWKKEIWCERCGLJDEMV2SKERCYISCJBJVIRCGJIRCYITSMVWWC4TLEIWCE5LJMQRF2LBCONZGSZBCHIRDIMZSGYRH2"));
    }

    public TileRequestParams copy() {
        TileRequestParams params = new TileRequestParams();
        BeanUtil.copyProperties(this, params);
        return params;
    }
}
