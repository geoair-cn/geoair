package cn.geoair.gtc.web.data.page;

import javax.servlet.http.HttpServletRequest;
import cn.geoair.gtc.base.data.page.GiPageParam;
import cn.geoair.gtc.base.data.page.GiPageParamProvider;
import cn.geoair.gtc.base.data.page.support.GirPageParam;
import cn.geoair.gtc.base.env.property.GirPropertyHelper;
import cn.geoair.gtc.base.util.GutilStr;
import cn.geoair.gtc.web.util.GirHttpServletHelper;

/**
 * 分页参数默认提供者
 *
 * @author Ray
 *
 */
public class GirWebPageParamProvider implements GiPageParamProvider {

	public GirWebPageParamProvider() {
	}

	@Override
	public GiPageParam getPageParam() {
		HttpServletRequest req = GirHttpServletHelper.getRequest();

		String startKey = GirPropertyHelper.getPropertier().getProperty("gtc.data.page.param.start", "start");
		String pageKey = GirPropertyHelper.getPropertier().getProperty("gtc.data.page.param.page", "page");
		String limitKey = GirPropertyHelper.getPropertier().getProperty("gtc.data.page.param.limit", "limit");

		String start = req.getParameter(startKey);
		String page = req.getParameter(pageKey);
		String limit = req.getParameter(limitKey);
		Integer pageSize = null;
		Integer pageNum = null;
		Long startRow = null;
		if (limit != null && GutilStr.isInteger(limit)) {
			pageSize = Integer.valueOf(limit);
		}
		else {
			pageSize = GirPropertyHelper.getPropertier().getProperty("gtc.data.page.param.pageSize", Integer.class, 25);
		}
		if (page != null && GutilStr.isInteger(page)) {
			pageNum = Integer.valueOf(page);
		}
		else if (start != null && GutilStr.isInteger(start)) {
			startRow = Long.valueOf(start);
		}

		GirPageParam pp = new GirPageParam();

		return pp.putParam(pageSize, pageNum, startRow);
	}

}
