package cn.geoair.map.dynamic.statics.mvt.v4.group;

import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;

import java.io.Serializable;

/**
 * 带输入序号的要素行。
 *
 * <p>V4 需要知道"这一行是输入里的第几行"，用来实现 {@code --preserve-input-order}。
 * 序号<b>不写进行对象</b>：行对象里的非几何字段会被编码器当属性输出，
 * 往里塞私有字段会污染 PBF 属性（`includeFields` 留空时是全字段输出）。所以放在外面这一层。</p>
 *
 * @author 张逢吉
 */
public final class V4OrderedRow implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 输入里的全局顺序（从 0 递增）。同一要素被分到多个瓦片时共用同一个序号。 */
    private final long sequence;

    private final GirAdvOneRow row;

    public V4OrderedRow(long sequence, GirAdvOneRow row) {
        this.sequence = sequence;
        this.row = row;
    }

    public long getSequence() {
        return sequence;
    }

    public GirAdvOneRow getRow() {
        return row;
    }
}
