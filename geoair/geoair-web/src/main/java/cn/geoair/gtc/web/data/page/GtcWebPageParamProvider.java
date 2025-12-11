package cn.geoair.gtc.web.data.page;

import javax.servlet.http.HttpServletRequest;
import cn.geoair.gtc.base.data.page.GiPageParam;
import cn.geoair.gtc.base.data.page.GiPageParamProvider;
import cn.geoair.gtc.base.data.page.support.GtcPageParam;
import cn.geoair.gtc.base.env.property.GtcPropertyHelper;
import cn.geoair.gtc.base.util.GutilStr;
import cn.geoair.gtc.web.util.GtcHttpServletHelper;

/**
 * 分页参数默认提供者
 * @author Ray
 *
 */
public class GtcWebPageParamProvider implements GiPageParamProvider  {

	public GtcWebPageParamProvider() {}

	@Override
	public GiPageParam getPageParam() {
		HttpServletRequest req =  GtcHttpServletHelper.getRequest();

		String startKey =  GtcPropertyHelper.getPropertier().getProperty("gtc.data.page.param.start","start");
		String pageKey =  GtcPropertyHelper.getPropertier().getProperty("gtc.data.page.param.page","page");
		String limitKey =  GtcPropertyHelper.getPropertier().getProperty("gtc.data.page.param.limit","limit");

        String start = req.getParameter(startKey);
        String page = req.getParameter(pageKey);
        String limit = req.getParameter(limitKey);
        Integer pageSize = null;
        Integer pageNum = null;
        Long startRow = null;
        if(limit != null && GutilStr.isInteger(limit)) {
        	pageSize = Integer.valueOf(limit);
        }else {
        	pageSize =  GtcPropertyHelper.getPropertier().getProperty("gtc.data.page.param.pageSize",Integer.class,25);
        }
        if(page != null && GutilStr.isInteger(page)) {
        	pageNum = Integer.valueOf(page);
        }else if(start != null && GutilStr.isInteger(start)) {
        	startRow = Long.valueOf(start);
        }

         GtcPageParam pp = new GtcPageParam();

        return pp.putParam(pageSize, pageNum, startRow);
	}


}
