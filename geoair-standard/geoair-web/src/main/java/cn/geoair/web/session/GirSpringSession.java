package cn.geoair.web.session;

import java.util.Enumeration;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionContext;
import cn.geoair.web.util.GirHttpServletHelper;

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
	public HttpSessionContext getSessionContext() {
		return getHttpSession(true).getSessionContext();
	}

	@Override
	public Enumeration<String> getAttributeNames() {
		return getHttpSession(true).getAttributeNames();
	}

	@Override
	public String[] getValueNames() {
		return getHttpSession(true).getValueNames();
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
	public void putValue(String name, Object value) {
		getHttpSession(true).setAttribute(name, value);
	}

	@Override
	public Object getValue(String name) {
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
	public void removeValue(String name) {
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
