package cn.geoair.web.session;

import cn.geoair.web.util.GirHttpServletHelper;
import java.util.Enumeration;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpSession;


@SuppressWarnings("deprecation")
@GirSessionAn(catalog = " gtc:session:spring-sessions:")
public class GirSpringSession extends GirHttpSession {

    private static final long serialVersionUID = -7622639754610367089L;

    protected GirSpringSession(String id, GirSessionConfig cfg) {
        super(id, cfg);
    }

    public HttpSession getHttpSession(boolean autoCreate) {
        return GirHttpServletHelper.getRequest().getSession(autoCreate);
    }

    @Override
    public long getCreationTime() {
        return getHttpSession(true).getCreationTime();
    }

    @Override
    public long getLastAccessedTime() {
        return getHttpSession(true).getLastAccessedTime();
    }

    @Override
    public ServletContext getServletContext() {
        return getHttpSession(true).getServletContext();
    }

    @Override
    public void setMaxInactiveInterval(int interval) {
        getHttpSession(true).setMaxInactiveInterval(interval);
    }

    @Override
    public int getMaxInactiveInterval() {
        return getHttpSession(true).getMaxInactiveInterval();
    }


    @Override
    public Enumeration<String> getAttributeNames() {
        return getHttpSession(true).getAttributeNames();
    }


    @Override
    public boolean isNew() {
        return getHttpSession(true).isNew();
    }

    //

    @Override
    public void setAttribute(String name, Object value) {
        getHttpSession(true).setAttribute(name, value);
    }

    @Override
    public Object getAttribute(String name) {
        HttpSession session = getHttpSession(false);
        if (session != null) {
            return session.getAttribute(name);
        }
        return null;
    }



    @Override
    public void removeAttribute(String name) {
        HttpSession session = getHttpSession(false);
        if (session != null) {
            session.removeAttribute(name);
        }
    }



    @Override
    public void invalidate() {
        super.invalidate();
        HttpSession session = getHttpSession(false);
        if (session != null) {
            session.invalidate();
        }
    }
}
