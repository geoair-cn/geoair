package cn.geoair.map.dynamic.statics.mvt.v4.dto;

import cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.v3.MultiLayerTileSliceParameter;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.v3.MvtLayerSliceParameter;
import cn.hutool.core.codec.Base32;
import cn.hutool.core.util.URLUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * V4 任务参数：<b>把 V3 的参数整包装进来</b>，再加上 V4 自己的扩展项。
 *
 * <p>为什么不把 V3 的字段抄一份：抄一份就意味着以后 V3 加字段必须有人记得同步，
 * 漏一个就变成"V4 少了 V3 的能力"。装在里面之后，"V4 完整保留 V3 能力"这件事
 * 由结构本身保证 —— V3 的 DTO 加了什么，V4 立刻就有。</p>
 *
 * <p>票据编解码沿用 V3 的做法（fastjson2 + Base32），只是外层多包一层 options。</p>
 *
 * @author 张逢吉
 */
@Data
@NoArgsConstructor
public class V4TileSliceParameter implements Serializable {

    private static final long serialVersionUID = 1L;

    /** V3 的全部任务参数，字段一个不少。 */
    private MultiLayerTileSliceParameter base;

    /** V4 扩展项（新增能力 + 单机运行时参数）。 */
    private V4Options options = new V4Options();

    /** 以一份 V3 参数构造 V4 参数，扩展项取默认值。 */
    public static V4TileSliceParameter of(MultiLayerTileSliceParameter base) {
        V4TileSliceParameter parameter = new V4TileSliceParameter();
        parameter.base = base;
        parameter.options = new V4Options();
        return parameter;
    }

    /** 取扩展项；为空时返回默认值，避免调用方到处判空。 */
    public V4Options resolveOptions() {
        if (options == null) {
            options = new V4Options();
        }
        return options;
    }

    /**
     * 编码为可传递的 Base32 票据。
     * <p>内层复用 V3 自己的 {@link MultiLayerTileSliceParameter#toBase32()}：先按 V3 的
     * 编码走一遍、再解回 JSON 嵌进外层，这样 V3 那部分参数的字段裁剪规则（剔除空值、
     * 字段名）与 V3 完全一致，不靠人工对齐。</p>
     */
    public String toBase32() {
        JSONObject wrapper = new JSONObject();
        if (base != null) {
            wrapper.put("base", JSON.parseObject(URLUtil.decode(Base32.decodeStr(base.toBase32()))));
        }
        wrapper.put("options", JSON.parseObject(JSON.toJSONString(resolveOptions())));
        wrapper.entrySet().removeIf(entry -> entry.getValue() == null);
        return Base32.encode(URLUtil.encode(wrapper.toString()));
    }

    /** 从 {@link #toBase32()} 的结果还原 V4 参数。 */
    public static V4TileSliceParameter fromBase32(String baseString) {
        try {
            String json = Base32.decodeStr(URLUtil.decode(baseString));
            JSONObject wrapper = JSON.parseObject(json);
            V4TileSliceParameter parameter = new V4TileSliceParameter();
            Object baseNode = wrapper.get("base");
            if (baseNode != null) {
                parameter.base = JSON.parseObject(baseNode.toString(), MultiLayerTileSliceParameter.class);
            }
            Object optionsNode = wrapper.get("options");
            parameter.options = optionsNode == null
                    ? new V4Options()
                    : JSON.parseObject(optionsNode.toString(), V4Options.class);
            return parameter;
        } catch (Exception e) {
            throw new IllegalArgumentException("无法解析 V4TileSliceParameter Base32 参数", e);
        }
    }

    // ------------------------------------------------------------------
    // 便捷委托：调用方读 V3 参数时不必先解包
    // ------------------------------------------------------------------

    public List<MvtLayerSliceParameter> getLayers() {
        return base == null ? null : base.getLayers();
    }

    public String getTileSetName() {
        return base == null ? null : base.getTileSetName();
    }

    public String getEdition() {
        return base == null ? null : base.getEdition();
    }

    public int getOutGridSrid() {
        return base == null ? 3857 : base.getOutGridSrid();
    }

    public int getMinZoom() {
        return base == null ? 4 : base.getMinZoom();
    }

    public int getMaxZoom() {
        return base == null ? 15 : base.getMaxZoom();
    }

    public String getTrackId() {
        return base == null ? null : base.getTrackId();
    }
}
