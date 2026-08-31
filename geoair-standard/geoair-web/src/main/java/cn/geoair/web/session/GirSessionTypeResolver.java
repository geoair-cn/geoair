package cn.geoair.web.session;

import jakarta.servlet.http.HttpSession;

final class GirSessionTypeResolver {

    private GirSessionTypeResolver() {}

    static boolean isNativeHttpSessionClass(GirSessionConfig sessionConfig) {
        return sessionConfig.getSessionClass() == HttpSession.class;
    }

    static boolean isServletSessionClass(GirSessionConfig sessionConfig) {
        Class<? extends HttpSession> sessionClass = sessionConfig.getSessionClass();
        return sessionClass == GirSpringSession.class || sessionClass == HttpSession.class;
    }

    static boolean isCookieSessionClass(GirSessionConfig sessionConfig) {
        return sessionConfig.getSessionClass() == GirCookieSession.class;
    }

    static boolean isTokenSessionClass(GirSessionConfig sessionConfig) {
        return sessionConfig.getSessionClass() == GirTokenSession.class;
    }

    static boolean isManagedSessionClass(GirSessionConfig sessionConfig) {
        return !isNativeHttpSessionClass(sessionConfig);
    }
}
