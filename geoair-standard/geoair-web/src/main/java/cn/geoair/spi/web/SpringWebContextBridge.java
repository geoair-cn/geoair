package cn.geoair.spi.web;

import cn.geoair.base.lang.invoke.GaMethodHandImpl;
import cn.geoair.base.lang.invoke.GaMethodHandImpl.ImplType;
import cn.geoair.base.lang.invoke.GkMethodHand;
import cn.geoair.web.util.GiWebContextProvider;
import cn.geoair.web.util.GirHttpServletHelper;
import java.lang.ref.WeakReference;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public class SpringWebContextBridge {

    private static final GiWebContextProvider CONTEXT_PROVIDER =
            new GiWebContextProvider() {
                @Override
                public HttpServletRequest getRequest() {
                    return currentRequest();
                }

                @Override
                public HttpServletResponse getResponse() {
                    return currentResponse();
                }

                @Override
                public ServletContext getServletContext() {
                    return currentServletContext();
                }
            };

    static {
        GkMethodHand.implFromClass(SpringWebContextBridge.class);
        GirHttpServletHelper.setContextProvider(CONTEXT_PROVIDER);
    }

    @GaMethodHandImpl(
        implClass = GirHttpServletHelper.class,
        implMethod = "getRequest",
        type = ImplType.expectfirst
    )
    public static HttpServletRequest getRequest() {
        return currentRequest();
    }

    @GaMethodHandImpl(
        implClass = GirHttpServletHelper.class,
        implMethod = "getResponse",
        type = ImplType.expectfirst
    )
    public static HttpServletResponse getResponse() {
        return currentResponse();
    }

    private static WeakReference<ServletContext> servletContextWarp =
            new WeakReference<ServletContext>(null);

    @GaMethodHandImpl(
        implClass = GirHttpServletHelper.class,
        implMethod = "getServletContext",
        type = ImplType.expectfirst
    )
    public static ServletContext getServletContext() {
        return currentServletContext();
    }

    private static HttpServletRequest currentRequest() {
        RequestAttributes ra = RequestContextHolder.getRequestAttributes();
        if (ra != null) {
            ServletRequestAttributes sra = (ServletRequestAttributes) ra;
            return sra.getRequest();
        } else {
            return null;
        }
    }

    private static HttpServletResponse currentResponse() {
        RequestAttributes ra = RequestContextHolder.getRequestAttributes();
        if (ra != null) {
            ServletRequestAttributes sra = (ServletRequestAttributes) ra;
            return sra.getResponse();
        } else {
            return null;
        }
    }

    private static ServletContext currentServletContext() {

        if (servletContextWarp.get() == null) {
            HttpServletRequest request = currentRequest();
            if (request == null) {
                return null;
            }
            ServletContext sc = request.getServletContext();
            servletContextWarp = new WeakReference<ServletContext>(sc);
        }
        return servletContextWarp.get();

        // return ContextLoader.getCurrentWebApplicationContext().getServletContext();
    }
}
