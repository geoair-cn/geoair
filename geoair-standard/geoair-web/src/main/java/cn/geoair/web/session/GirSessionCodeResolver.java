package cn.geoair.web.session;

import cn.geoair.base.util.GutilStr;
import cn.geoair.web.util.GirHttpServletHelper;
import cn.geoair.web.util.GutilCookie;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpSession;

import java.util.UUID;

final class GirSessionCodeResolver {

    private static final String REQUEST_TOKEN_RANDOM_KEY = " geoair-tokenKey-random";

    private GirSessionCodeResolver() {}

    static String resolve(GirSessionConfig sessionConfig, boolean autoCreate) {
        if (sessionConfig.isServletSessionClass()) {
            return resolveServletSessionCode(autoCreate);
        } else if (sessionConfig.isCookieSessionClass()) {
            return resolveCookieSessionCode(sessionConfig, autoCreate);
        } else if (sessionConfig.isTokenSessionClass()) {
            return resolveTokenSessionCode(sessionConfig, autoCreate);
        }
        return null;
    }

    private static String resolveServletSessionCode(boolean autoCreate) {
        HttpSession hs = GirHttpServletHelper.getSession(false);
        if (hs != null) {
            return hs.getId();
        } else if (autoCreate) {
            HttpSession session = GirHttpServletHelper.getSession(true);
            if (session != null) {
                return session.getId();
            }
        }
        return null;
    }

    private static String resolveCookieSessionCode(
            GirSessionConfig sessionConfig, boolean autoCreate) {
        Cookie cookie = GutilCookie.getCookie(sessionConfig.getCookieKey());
        if (cookie != null) {
            return cookie.getValue();
        } else if (autoCreate) {
            String code = UUID.randomUUID().toString();
            GutilCookie.addCookie(
                    sessionConfig.getCookieKey(), code, sessionConfig.getCookieTimeout());
            return code;
        }
        return null;
    }

    private static String resolveTokenSessionCode(
            GirSessionConfig sessionConfig, boolean autoCreate) {
        if (!sessionConfig.isUseCache()) {
            String requestScopedTokenReuse = resolveRequestScopedTokenReuse();
            if (requestScopedTokenReuse != null) {
                return requestScopedTokenReuse;
            }
        }
        String token = resolveIncomingToken(sessionConfig);
        if (GutilStr.hasText(token)) {
            return token;
        } else if (autoCreate) {
            String code = UUID.randomUUID().toString();
            if (!sessionConfig.isUseCache()) {
                cacheRequestScopedToken(code);
            }
            return code;
        }
        return null;
    }

    private static String resolveRequestScopedTokenReuse() {
        Object oldToken = GirHttpServletHelper.getRequestAttribute(REQUEST_TOKEN_RANDOM_KEY);
        if (oldToken != null) {
            return (String) oldToken;
        }
        return null;
    }

    private static String resolveIncomingToken(GirSessionConfig sessionConfig) {
        if (sessionConfig.isTokenInHeader()) {
            return GirHttpServletHelper.getHeader(sessionConfig.getTokenKey());
        }
        return GirHttpServletHelper.getParameter(sessionConfig.getTokenKey());
    }

    private static void cacheRequestScopedToken(String code) {
        GirHttpServletHelper.setRequestAttribute(REQUEST_TOKEN_RANDOM_KEY, code);
    }
}
