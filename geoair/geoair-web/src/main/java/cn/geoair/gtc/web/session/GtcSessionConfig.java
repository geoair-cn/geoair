package cn.geoair.gtc.web.session;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpSession;

import cn.geoair.gtc.base.cache.GiCache;
import cn.geoair.gtc.base.cache.GtcCacheHelper;
import cn.geoair.gtc.base.util.GutilStr;
import cn.geoair.gtc.web.util.GtcHttpServletHelper;
import cn.geoair.gtc.web.util.GutilCookie;

public class GtcSessionConfig implements Serializable{

	private static final long serialVersionUID = -6794764219858353804L;

	/*
	protected static ThreadLocal<HttpSession> sessionThreadLocal = new ThreadLocal<HttpSession>();

	public static void cleanSessionCache() {
		if(useThreadLocal) {
			sessionThreadLocal.remove();
		}
	}
	*/

	private Class<? extends HttpSession> sessionClass = HttpSession.class;

	private String cookieKey = " geoair-user-session";

	private String tokenKey = "code";


	private boolean tokenInHeader = false;

	private long httpTimeout = 30 * 60;//http超时时间，秒

	private int cookieTimeout = 60 * 60 * 24;//单位秒

	private long tokenTimeout = 1000 * 60 * 60 * 24 * 7; // 单位毫秒

	private boolean useCache = false;

	private String catalog = " gtc:session:sessions:";


	private String cacheName;


	public GiCache getSessionCache() {
		return  GtcCacheHelper.getCache(cacheName);
	}

	public GtcSessionConfig(Class<? extends HttpSession> sessionClass) {
		this.sessionClass = sessionClass;
		 GtcSessionAn an = sessionClass.getAnnotation( GtcSessionAn.class);
		if(an != null && ! GtcSessionAn.NULL.equals(an.catalog())) {
			catalog = an.catalog();
			cacheName = an.cacheName();
		}

	}

	@Override
	public boolean equals(Object obj) {
		if(obj != null && obj instanceof GtcSessionConfig) {
			 GtcSessionConfig cfg = (GtcSessionConfig)obj;
			if(Objects.equals(cfg.sessionClass,this.sessionClass) && Objects.equals(cfg.cookieKey,this.cookieKey) && Objects.equals(cfg.tokenKey,this.tokenKey)) {
				return true;
			}
		}
		return false;//super.equals(obj);
	}


	public String getRequestSessionCode(boolean autoCreate) {
		String code = null;
		if(sessionClass ==  GtcSpringSession.class || sessionClass == HttpSession.class) {
			HttpSession hs =  GtcHttpServletHelper.getRequest().getSession(false);
			if(hs != null) {
				code = hs.getId();
			}else if(autoCreate) {
				code =  GtcHttpServletHelper.getRequest().getSession().getId();
			}
		}else if(sessionClass ==  GtcCookieSession.class) {
			Cookie cookie = GutilCookie.getCookie(cookieKey);
			if(cookie != null) {
				code = cookie.getValue();
			}else if(autoCreate){
				code = UUID.randomUUID().toString();
				GutilCookie.addCookie(cookieKey, code, cookieTimeout);
			}
		}else if(sessionClass ==  GtcTokenSession.class) {
			if(!useCache) {
				Object oldToken =  GtcHttpServletHelper.getRequest().getAttribute(" geoair-tokenKey-random");
				if(oldToken != null) {
					return (String)oldToken;
				}
			}
			String token = null;
			if(tokenInHeader) {
				token =  GtcHttpServletHelper.getRequest().getHeader(tokenKey);
			}else{
				token =  GtcHttpServletHelper.getRequest().getParameter(tokenKey);
			}
			if(GutilStr.hasText(token)) {
				code = token;
			}else if(autoCreate) {
				code = UUID.randomUUID().toString();
				if(!useCache) {
					 GtcHttpServletHelper.getRequest().setAttribute(" geoair-tokenKey-random", code);
				}
			}
		}
		return code;
	}

	public String getCookieKey() {
		return cookieKey;
	}

	public GtcSessionConfig setCookieKey(String cookieKey) {
		this.cookieKey = cookieKey;
		return this;
	}

	public String getTokenKey() {
		return tokenKey;
	}

	public GtcSessionConfig setTokenKey(String tokenKey) {
		this.tokenKey = tokenKey;
		return this;
	}

	public long getHttpTimeout() {
		return httpTimeout;
	}

	public GtcSessionConfig setHttpTimeout(long httpTimeout) {
		this.httpTimeout = httpTimeout;
		return this;
	}

	public int getCookieTimeout() {
		return cookieTimeout;
	}

	public GtcSessionConfig setCookieTimeout(int cookieTimeout) {
		this.cookieTimeout = cookieTimeout;
		return this;
	}

	public long getTokenTimeout() {
		return tokenTimeout;
	}

	public GtcSessionConfig setTokenTimeout(long tokenTimeout) {
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

	public GtcSessionConfig setTokenInHeader(boolean tokenInHeader) {
		this.tokenInHeader = tokenInHeader;
		return this;
	}
}
