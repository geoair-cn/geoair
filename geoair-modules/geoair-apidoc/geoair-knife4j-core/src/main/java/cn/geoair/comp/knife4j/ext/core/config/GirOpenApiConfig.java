package cn.geoair.comp.knife4j.ext.core.config;

/**
 * @author ：张俊
 * @date ：Created in 2026/3/19 16:46
 * @description： TODO
 */
public abstract class GirOpenApiConfig implements IGirOpenApiConfig {


    /**
     * 是否加载完成
     */
    boolean isLoad;

    @Override
    public boolean isLoad() {
        return isLoad;
    }

    @Override
    public void doLoading() {
        isLoad = false;
    }

    @Override
    public void loadEnd() {
        isLoad = true;
    }
}
