package cn.geoair.web.session;

import cn.geoair.web.util.GirHttpServletHelper;
import java.io.Serializable;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpSession;


@SuppressWarnings("deprecation")
public abstract class GirHttpSession implements HttpSession, Serializable {

    private static final long serialVersionUID = 7733215043083700732L;

    private static final String REQUEST_SESSION_CACHE_KEY = " geoair-session-request-cache";

    public static HttpSession getSession(GirSessionConfig sessionConfig, boolean autoCreate) {

        if (sessionConfig.isNativeHttpSessionClass()) {
            return GirHttpServletHelper.getSession(autoCreate);
        }

        GirHttpSession requestCachedSession = resolveRequestCachedSession(sessionConfig);
        if (requestCachedSession != null) {
            return requestCachedSession;
        }

        String code = resolveSessionCode(sessionConfig, autoCreate);
        if (code == null) {
            return null;
        }

        String sessionKey = buildSessionKey(sessionConfig, code);
        HttpSession cachedSession = resolveBackingCachedSession(sessionConfig, sessionKey);
        if (cachedSession != null) {
            cacheInRequestIfEnabled(sessionConfig, cachedSession);
            return cachedSession;
        }

        HttpSession session = GirHttpSessionFactory.create(sessionConfig, code);
        putBackingCache(sessionConfig, sessionKey, session);
        cacheInRequestIfEnabled(sessionConfig, session);
        return session;
    }

    public static HttpSession getSession(String sessionCode, GirSessionConfig sessionConfig) {

        if (sessionConfig.isNativeHttpSessionClass()) {
            return null;
        }
        String sessionKey = buildSessionKey(sessionConfig, sessionCode);
        return resolveBackingCachedSession(sessionConfig, sessionKey);
    }

    private static GirHttpSession resolveRequestCachedSession(GirSessionConfig sessionConfig) {
        if (!sessionConfig.isUseCache()) {
            return null;
        }
        Object session = GirHttpServletHelper.getRequestAttribute(REQUEST_SESSION_CACHE_KEY);
        if (session instanceof GirHttpSession) {
            GirHttpSession requestCachedSession = (GirHttpSession) session;
            if (sessionConfig.equals(requestCachedSession.getSessionCfg())) {
                return requestCachedSession;
            }
        }
        return null;
    }

    private static String resolveSessionCode(GirSessionConfig sessionConfig, boolean autoCreate) {
        return sessionConfig.getRequestSessionCode(autoCreate);
    }

    private static String buildSessionKey(GirSessionConfig sessionConfig, String code) {
        return sessionConfig.getCatalog() + code;
    }

    private static HttpSession resolveBackingCachedSession(
            GirSessionConfig sessionConfig, String sessionKey) {
        Object session = sessionConfig.getSessionCache().getObject(sessionKey);
        if (session instanceof HttpSession) {
            return (HttpSession) session;
        }
        return null;
    }

    private static void putBackingCache(
            GirSessionConfig sessionConfig, String sessionKey, HttpSession session) {
        sessionConfig
                .getSessionCache()
                .put(sessionKey, session, sessionConfig.getTokenTimeout()); // 一次登录最长登录时间
    }

    private static void cacheInRequestIfEnabled(
            GirSessionConfig sessionConfig, HttpSession session) {
        if (sessionConfig.isUseCache()) {
            GirHttpServletHelper.setRequestAttribute(REQUEST_SESSION_CACHE_KEY, session);
            // gtcSessionConfig.sessionThreadLocal.set(session);
        }
    }

    protected GirHttpSession(String id, GirSessionConfig cfg) {
        this.id = id;
        this.sessionCfg = cfg;
    }

    private String id;

    private GirSessionConfig sessionCfg;

    public GirSessionConfig getSessionCfg() {
        return sessionCfg;
    }

    protected void freshCache() {
        getSessionCfg()
                .getSessionCache()
                .put(buildSessionKey(sessionCfg, id), this, sessionCfg.getTokenTimeout());
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public long getCreationTime() {
        return 0;
    }

    @Override
    public long getLastAccessedTime() {
        return 0;
    }

    @Override
    public ServletContext getServletContext() {
        return null;
    }

    @Override
    public void setMaxInactiveInterval(int interval) {}

    @Override
    public int getMaxInactiveInterval() {
        return 0;
    }



    @Override
    public Enumeration<String> getAttributeNames() {
        return null;
    }



    private Map<String, Object> attributes = new HashMap<String, Object>();

    @Override
    public void setAttribute(String name, Object value) {
        attributes.put(name, value);
        this.freshCache();
    }

    @Override
    public Object getAttribute(String name) {
        return attributes.get(name);
    }


    @Override
    public void removeAttribute(String name) {
        attributes.remove(name);
        this.freshCache();
    }



    @Override
    public void invalidate() {
        getSessionCfg().getSessionCache().evict(buildSessionKey(sessionCfg, this.getId()));
    }

    @Override
    public boolean isNew() {
        return false;
    }
}
