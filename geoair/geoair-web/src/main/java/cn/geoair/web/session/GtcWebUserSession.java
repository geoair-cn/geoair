// package com.gtc.web.session;
//
// import java.util.Enumeration;
// import javax.servlet.http.HttpSession;
// import cn.geoair..base.user.session.GiUserSession;
//
// public class GirWebUserSession implements GiUserSession{
//
// /**
// *
// */
// private static final long serialVersionUID = -4203220949466177098L;
//
// private HttpSession httpSession;
//
// public GirWebUserSession(HttpSession httpSession){
// this.httpSession = httpSession;
// }
//
//
// @Override
// public long getCreationTime() {
// // TODO Auto-generated method stub
// return httpSession.getCreationTime();
// }
//
// @Override
// public String getId() {
// // TODO Auto-generated method stub
// return httpSession.getId();
// }
//
// @Override
// public long getLastAccessedTime() {
// // TODO Auto-generated method stub
// return httpSession.getLastAccessedTime();
// }
//
// @Override
// public void setMaxInactiveInterval(int interval) {
// // TODO Auto-generated method stub
// httpSession.setMaxInactiveInterval(interval);
// }
//
// @Override
// public int getMaxInactiveInterval() {
// // TODO Auto-generated method stub
// return httpSession.getMaxInactiveInterval();
// }
//
// @Override
// public Object getAttribute(String name) {
// // TODO Auto-generated method stub
// return httpSession.getAttribute(name);
// }
//
// @Override
// public Enumeration<String> getAttributeNames() {
// // TODO Auto-generated method stub
// return httpSession.getAttributeNames();
// }
//
// @Override
// public void setAttribute(String name, Object value) {
// // TODO Auto-generated method stub
// httpSession.setAttribute(name, value);
// }
//
// @Override
// public void removeAttribute(String name) {
// // TODO Auto-generated method stub
// httpSession.removeAttribute(name);
// }
//
// @Override
// public void invalidate() {
// // TODO Auto-generated method stub
// httpSession.invalidate();
// }
//
// @Override
// public boolean isNew() {
// // TODO Auto-generated method stub
// return false;
// }
//
// }
