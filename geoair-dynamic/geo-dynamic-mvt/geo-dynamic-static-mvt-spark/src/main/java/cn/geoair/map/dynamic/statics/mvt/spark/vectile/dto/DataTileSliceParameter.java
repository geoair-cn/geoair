package cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto;

import cn.hutool.core.bean.BeanUtil;
import lombok.Data;

/**
 * @author ：张逢吉
 * @date ：Created in 2025/12/31 13:30
 * @description：  由于spark任务中无法获取到spring的环境，所以这里需要把连接信息等信息通过对象的方式交给spark
 */
@Data
public class DataTileSliceParameter extends TileSliceParameter {

    /**
     * 成果物输出的表名
     */
    private String outPutUrl;


    /**
     * 数据库连接信息的输入信息
     */
    private String inputUrl;


    @Override
    public PgConnectInfo getOutPutConnectInfo() {
        if (super.getOutPutConnectInfo() == null && outPutUrl != null) {
            setOutPutConnectInfo(new PgConnectInfo(outPutUrl));
        }
        return super.getOutPutConnectInfo();
    }

    @Override
    public PgConnectInfo getInputConnectInfo() {
        if (super.getInputConnectInfo() == null && inputUrl != null) {
            setInputConnectInfo(new PgConnectInfo(inputUrl));
        }
        return super.getInputConnectInfo();
    }

    public DataTileSliceParameter copy() {
        DataTileSliceParameter dataTileSliceParameter = new DataTileSliceParameter();
        BeanUtil.copyProperties(this, dataTileSliceParameter);
        dataTileSliceParameter.setOutPutConnectInfo(new PgConnectInfo(outPutUrl));
        dataTileSliceParameter.setInputConnectInfo(new PgConnectInfo(inputUrl));
        return dataTileSliceParameter;
    }
}
