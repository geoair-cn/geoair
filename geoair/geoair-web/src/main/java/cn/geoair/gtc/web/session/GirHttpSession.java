package cn.geoair.gtc.web.session;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionContext;
import cn.geoair.gtc.web.util.GirHttpServletHelper;

@SuppressWarnings("deprecation")

public abstract class GirHttpSession implements HttpSession,Serializable{

	private static final long serialVersionUID = 7733215043083700732L;


	public static HttpSession getSession(GirSessionConfig sessionConfig, boolean autoCreate){

		Class<? extends HttpSession> sessionClass = sessionConfig.getSessionClass();
		if(sessionClass == HttpSession.class) {
			return  GirHttpServletHelper.getRequest().getSession(autoCreate);
		}

		if(sessionConfig.isUseCache()) {
			Object se =  GirHttpServletHelper.getRequest().getAttribute(" geoair-session-request-cache");
			if(se != null && se instanceof GirHttpSession) {
				if(sessionConfig.equals(((GirHttpSession)se).getSessionCfg())){
					return (GirHttpSession)se;
				}
			}
		}
		String code = sessionConfig.getRequestSessionCode(autoCreate);
		if(code == null) {
			return null;
		}
		String sessionKey = sessionConfig.getCatalog() + code;
		Object obj = sessionConfig.getSessionCache().getObject(sessionKey);
		if(obj != null && obj instanceof HttpSession){
			if(sessionConfig.isUseCache()) {
				 GirHttpServletHelper.getRequest().setAttribute(" geoair-session-request-cache", obj);
			}
			return (HttpSession) obj;
		}else {
			HttpSession session = null;
			try {
				Constructor<? extends HttpSession> cont = sessionClass.getDeclaredConstructor(String.class, GirSessionConfig.class);
				session = (GirHttpSession)cont.newInstance(code,sessionConfig);
			}catch (Exception e) {
				e.printStackTrace();
			}
			sessionConfig.getSessionCache().put(sessionKey,session,sessionConfig.getTokenTimeout());//一次登录最长登录时间
			if(sessionConfig.isUseCache()) {
				 GirHttpServletHelper.getRequest().setAttribute(" geoair-session-request-cache", session);
				// gtcSessionConfig.sessionThreadLocal.set(session);
			}
			return session;
		}
	}


	public static HttpSession getSession(String sessionCode, GirSessionConfig sessionConfig){

		Class<? extends HttpSession> sessionClass = sessionConfig.getSessionClass();
		if(sessionClass == HttpSession.class) {
			return null;
		}
		String sessionKey = sessionConfig.getCatalog() + sessionCode;
		Object obj = sessionConfig.getSessionCache().getObject(sessionKey);
		if(obj != null) {
			return (HttpSession)obj;
		}
		return null;
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
		getSessionCfg().getSessionCache().put(sessionCfg.getCatalog() + id,this,sessionCfg.getTokenTimeout());
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
	public void setMaxInactiveInterval(int interval) {
	}


	@Override
	public int getMaxInactiveInterval() {
		return 0;
	}


	@Override
	public HttpSessionContext getSessionContext() {
		return null;
	}


	@Override
	public Enumeration<String> getAttributeNames() {
		return null;
	}


	@Override
	public String[] getValueNames() {
		return (String[])attributes.keySet().toArray();
	}


	private Map<String,Object> attributes = new HashMap<String,Object>();

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
	public void putValue(String name, Object value) {
		attributes.put(name, value);
		this.freshCache();
	}

	@Override
	public Object getValue(String name) {
		return attributes.get(name);
	}

	@Override
	public void removeAttribute(String name) {
		attributes.remove(name);
		this.freshCache();
	}


	@Override
	public void removeValue(String name){
		attributes.remove(name);
		this.freshCache();
	}

	@Override
	public void invalidate() {
		getSessionCfg().getSessionCache().evict(sessionCfg.getCatalog() + this.getId());
	}

	@Override
	public boolean isNew() {
		return false;
	}
}
