package cn.geoair.web.session;

import jakarta.servlet.http.HttpSession;

import java.lang.reflect.Constructor;

final class GirHttpSessionFactory {

    private GirHttpSessionFactory() {}

    static HttpSession create(
            Class<? extends HttpSession> sessionClass,
            String code,
            GirSessionConfig sessionConfig) {
        HttpSession session = null;
        try {
            Constructor<? extends HttpSession> cont =
                    sessionClass.getDeclaredConstructor(String.class, GirSessionConfig.class);
            session = (GirHttpSession) cont.newInstance(code, sessionConfig);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return session;
    }

    static HttpSession create(GirSessionConfig sessionConfig, String code) {
        return create(sessionConfig.getSessionClass(), code, sessionConfig);
    }
}
