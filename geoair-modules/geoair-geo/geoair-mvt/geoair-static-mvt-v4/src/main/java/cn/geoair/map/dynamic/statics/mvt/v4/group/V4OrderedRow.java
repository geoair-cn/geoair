package cn.geoair.map.dynamic.statics.mvt.v4.group;

import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;

import java.io.Serializable;

/**
 * 带输入序号的要素行。
 *
 * <p>V4 需要两个行以外的信息，都放在这一层而不是写进行对象里：</p>
 * <ul>
 *   <li>{@code sequence}：这一行是输入里的第几个要素，用于 {@code --preserve-input-order}
 *       以及削减时的并列打破；</li>
 *   <li>{@code rowIndex}：同一个要素落在这块瓦片上的第几行。一个线/面要素在一块瓦片里
 *       可能被切成多行，它们的 {@code sequence} 相同、身份也相同，只有这个下标能把它们区分开。</li>
 * </ul>
 * <p>两者合起来让「一个瓦片保留哪些行」成为一个<b>全序</b>下的确定性结果：
 * 不依赖到达顺序、不依赖溢写把内存清空过几次（见 {@link V4IdentitySelector}）。</p>
 *
 * <p>为什么不写进行对象：行里的非几何字段会被编码器当属性输出
 * （{@code includeFields} 留空时是全字段输出），往里塞私有字段会污染 PBF 属性。</p>
 *
 * @author 张逢吉
 */
public final class V4OrderedRow implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 输入里的全局顺序（从 0 递增）。同一要素被分到多个瓦片时共用同一个序号。 */
    private final long sequence;

    /** 同一要素落在这块瓦片上的行下标（从 0 起）。 */
    private final int rowIndex;

    private final GirAdvOneRow row;

    public V4OrderedRow(long sequence, int rowIndex, GirAdvOneRow row) {
        this.sequence = sequence;
        this.rowIndex = rowIndex;
        this.row = row;
    }

    public long getSequence() {
        return sequence;
    }

    public int getRowIndex() {
        return rowIndex;
    }

    public GirAdvOneRow getRow() {
        return row;
    }
}
