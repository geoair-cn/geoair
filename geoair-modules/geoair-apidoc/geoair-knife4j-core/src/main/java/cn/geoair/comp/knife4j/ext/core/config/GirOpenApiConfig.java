package cn.geoair.comp.knife4j.ext.core.config;

/**
 * <p>Abstract GirOpenApiConfig class.</p>
 *
 * @author ：张俊
 * @date ：Created in 2026/3/19 16:46 @description： TODO
 * @version $Id: $Id
 */
public abstract class GirOpenApiConfig implements IGirOpenApiConfig {

	/**
	 * 是否加载完成
	 */
	boolean isLoad;

	/** {@inheritDoc} */
	@Override
	public boolean isLoad() {
		return isLoad;
	}

	/** {@inheritDoc} */
	@Override
	public void doLoading() {
		isLoad = false;
	}

	/** {@inheritDoc} */
	@Override
	public void loadEnd() {
		isLoad = true;
	}

}
