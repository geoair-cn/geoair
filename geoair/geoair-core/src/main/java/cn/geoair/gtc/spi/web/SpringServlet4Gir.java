package cn.geoair.gtc.spi.web;

import java.lang.ref.WeakReference;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import cn.geoair.gtc.web.util.GirHttpServletHelper;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import cn.geoair.gtc.base.lang.invoke.GkMethodHand;
import cn.geoair.gtc.base.lang.invoke.GaMethodHandImpl;
import  cn.geoair.gtc.base.lang.invoke.GaMethodHandImpl.ImplType;


public class SpringServlet4Gir {

	static {
		GkMethodHand.implFromClass(SpringServlet4Gir.class);
	}

    @GaMethodHandImpl(implClass= GirHttpServletHelper.class,implMethod="getRequest",type=ImplType.expectfirst)
    public static HttpServletRequest getRequest() {
        RequestAttributes ra = RequestContextHolder.getRequestAttributes();
        if (ra != null) {
            ServletRequestAttributes sra = (ServletRequestAttributes) ra;
            return sra.getRequest();
        } else {
            return null;
        }
    }

    @GaMethodHandImpl(implClass= GirHttpServletHelper.class,implMethod="getResponse",type=ImplType.expectfirst)
    public static HttpServletResponse getResponse() {
        RequestAttributes ra = RequestContextHolder.getRequestAttributes();
        if (ra != null) {
            ServletRequestAttributes sra = (ServletRequestAttributes) ra;
            return sra.getResponse();
        } else {
            return null;
        }
    }


    private static WeakReference<ServletContext> servletContextWarp = new WeakReference<ServletContext>(null);

    @GaMethodHandImpl(implClass= GirHttpServletHelper.class,implMethod="getServletContext",type=ImplType.expectfirst)
    public static ServletContext getServletContext() {

    	if(servletContextWarp.get() == null) {
    		ServletContext sc =  GirHttpServletHelper.getRequest().getServletContext();
			servletContextWarp = new WeakReference<ServletContext>(sc);
		}
		return servletContextWarp.get();

    	//return ContextLoader.getCurrentWebApplicationContext().getServletContext();
    }


}
