package cn.geoair.gtc.web.session;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpSession;

import cn.geoair.gtc.base.cache.GiCache;
import cn.geoair.gtc.base.cache.GirCacheHelper;
import cn.geoair.gtc.base.util.GutilStr;
import cn.geoair.gtc.web.util.GirHttpServletHelper;
import cn.geoair.gtc.web.util.GutilCookie;

public class GirSessionConfig implements Serializable {

	private static final long serialVersionUID = -6794764219858353804L;

	/*
	 * protected static ThreadLocal<HttpSession> sessionThreadLocal = new
	 * ThreadLocal<HttpSession>();
	 *
	 * public static void cleanSessionCache() { if(useThreadLocal) {
	 * sessionThreadLocal.remove(); } }
	 */

	private Class<? extends HttpSession> sessionClass = HttpSession.class;

	private String cookieKey = " geoair-user-session";

	private String tokenKey = "code";

	private boolean tokenInHeader = false;

	private long httpTimeout = 30 * 60;// http超时时间，秒

	private int cookieTimeout = 60 * 60 * 24;// 单位秒

	private long tokenTimeout = 1000 * 60 * 60 * 24 * 7; // 单位毫秒

	private boolean useCache = false;

	private String catalog = " gtc:session:sessions:";

	private String cacheName;

	public GiCache getSessionCache() {
		return GirCacheHelper.getCache(cacheName);
	}

	public GirSessionConfig(Class<? extends HttpSession> sessionClass) {
		this.sessionClass = sessionClass;
		GirSessionAn an = sessionClass.getAnnotation(GirSessionAn.class);
		if (an != null && !GirSessionAn.NULL.equals(an.catalog())) {
			catalog = an.catalog();
			cacheName = an.cacheName();
		}

	}

	@Override
	public boolean equals(Object obj) {
		if (obj != null && obj instanceof GirSessionConfig) {
			GirSessionConfig cfg = (GirSessionConfig) obj;
			if (Objects.equals(cfg.sessionClass, this.sessionClass) && Objects.equals(cfg.cookieKey, this.cookieKey)
					&& Objects.equals(cfg.tokenKey, this.tokenKey)) {
				return true;
			}
		}
		return false;// super.equals(obj);
	}

	public String getRequestSessionCode(boolean autoCreate) {
		String code = null;
		if (sessionClass == GirSpringSession.class || sessionClass == HttpSession.class) {
			HttpSession hs = GirHttpServletHelper.getRequest().getSession(false);
			if (hs != null) {
				code = hs.getId();
			}
			else if (autoCreate) {
				code = GirHttpServletHelper.getRequest().getSession().getId();
			}
		}
		else if (sessionClass == GirCookieSession.class) {
			Cookie cookie = GutilCookie.getCookie(cookieKey);
			if (cookie != null) {
				code = cookie.getValue();
			}
			else if (autoCreate) {
				code = UUID.randomUUID().toString();
				GutilCookie.addCookie(cookieKey, code, cookieTimeout);
			}
		}
		else if (sessionClass == GirTokenSession.class) {
			if (!useCache) {
				Object oldToken = GirHttpServletHelper.getRequest().getAttribute(" geoair-tokenKey-random");
				if (oldToken != null) {
					return (String) oldToken;
				}
			}
			String token = null;
			if (tokenInHeader) {
				token = GirHttpServletHelper.getRequest().getHeader(tokenKey);
			}
			else {
				token = GirHttpServletHelper.getRequest().getParameter(tokenKey);
			}
			if (GutilStr.hasText(token)) {
				code = token;
			}
			else if (autoCreate) {
				code = UUID.randomUUID().toString();
				if (!useCache) {
					GirHttpServletHelper.getRequest().setAttribute(" geoair-tokenKey-random", code);
				}
			}
		}
		return code;
	}

	public String getCookieKey() {
		return cookieKey;
	}

	public GirSessionConfig setCookieKey(String cookieKey) {
		this.cookieKey = cookieKey;
		return this;
	}

	public String getTokenKey() {
		return tokenKey;
	}

	public GirSessionConfig setTokenKey(String tokenKey) {
		this.tokenKey = tokenKey;
		return this;
	}

	public long getHttpTimeout() {
		return httpTimeout;
	}

	public GirSessionConfig setHttpTimeout(long httpTimeout) {
		this.httpTimeout = httpTimeout;
		return this;
	}

	public int getCookieTimeout() {
		return cookieTimeout;
	}

	public GirSessionConfig setCookieTimeout(int cookieTimeout) {
		this.cookieTimeout = cookieTimeout;
		return this;
	}

	public long getTokenTimeout() {
		return tokenTimeout;
	}

	public GirSessionConfig setTokenTimeout(long tokenTimeout) {
		this.tokenTimeout = tokenTimeout;
		return this;
	}

	public Class<? extends HttpSession> getSessionClass() {
		return sessionClass;
	}

	public String getCatalog() {
		return catalog;
	}

	public boolean isUseCache() {
		return useCache;
	}

	public void setUseCache(boolean useCache) {
		this.useCache = useCache;
	}

	public boolean isTokenInHeader() {
		return tokenInHeader;
	}

	public GirSessionConfig setTokenInHeader(boolean tokenInHeader) {
		this.tokenInHeader = tokenInHeader;
		return this;
	}

}
