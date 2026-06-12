package cn.geoair.web.session;

import cn.geoair.web.util.GutilCookie;

import jakarta.servlet.http.Cookie;

@GirSessionAn(catalog = " gir:session:cookie-sessions:")
public class GirCookieSession extends GirHttpSession {

    private static final long serialVersionUID = -9083487544487755650L;

    protected GirCookieSession(String id, GirSessionConfig cfg) {
        super(id, cfg);
    }

    /*
     * @Override public long getCreationTime() { return 0; }
     *
     *
     * @Override public long getLastAccessedTime() { return 0; }
     *
     *
     * @Override public ServletContext getServletContext() { return null; }
     *
     *
     * @Override public void setMaxInactiveInterval(int interval) { }
     *
     *
     * @Override public int getMaxInactiveInterval() { return 0; }
     *
     *
     * @Override public HttpSessionContext getSessionContext() { return null; }
     *
     *
     * @Override public Object getValue(String name) { return null; }
     *
     *
     * @Override public Enumeration<String> getAttributeNames() { return null; }
     *
     *
     * @Override public String[] getValueNames() { return null; }
     *
     *
     * @Override public void setAttribute(String name, Object value) {
     *
     * }
     *
     *
     * @Override public Object getAttribute(String name) { return null; }
     *
     * @Override public void putValue(String name, Object value) {
     *
     * }
     *
     * @Override public void removeAttribute(String name) {
     *
     * }
     *
     *
     * @Override public void removeValue(String name){
     *
     * }
     *
     * @Override public boolean isNew() { return false; }
     *
     */
    @Override
    public void invalidate() {
        super.invalidate();
        Cookie cookie = GutilCookie.getCookie(getSessionCfg().getCookieKey());
        if (cookie != null) {
            GutilCookie.removeCookie(getSessionCfg().getCookieKey());
        }
    }
}
