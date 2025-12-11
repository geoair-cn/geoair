package cn.geoair.gtc.web;

import cn.geoair.gtc.base.Gtc;

public abstract class GtcWeb {



	public static void main(String[] args) {

		//System.out.print( gtc.Env.getPropertier().getProperty("abc", "1"));

		 Gtc.log.error("df");
	}


	/*
	public static GiPageParam getPageParam() {
		GiPageParam param = GiPageConfig.getDefaultPageParam();
		HttpServletRequest req =  gtcHttpServletHelper.getRequest();

		String startKey =  gtcPropertyHelper.getPropertier().getProperty(" gtc.data.page.param.start","start");
		String pageKey =  gtcPropertyHelper.getPropertier().getProperty(" gtc.data.page.param.page","page");
		String limitKey =  gtcPropertyHelper.getPropertier().getProperty(" gtc.data.page.param.limit","limit");

        String start = req.getParameter(startKey);
        String page = req.getParameter(pageKey);
        String limit = req.getParameter(limitKey);
        Integer pageSize = null;
        Integer pageNum = null;
        Long startRow = null;
        if(limit != null && GutilStr.isInteger(limit)) {
        	pageSize = Integer.valueOf(limit);
        }else {
        	pageSize =  gtcPropertyHelper.getPropertier().getProperty(" gtc.data.page.param.pageSize",Integer.class,25);
        }
        if(page != null && GutilStr.isInteger(page)) {
        	pageNum = Integer.valueOf(page);
        }else if(start != null && GutilStr.isInteger(start)) {
        	startRow = Long.valueOf(start);
        }
        param.putParam(pageSize, pageNum, startRow);

        return param;
	}
	*/
}
