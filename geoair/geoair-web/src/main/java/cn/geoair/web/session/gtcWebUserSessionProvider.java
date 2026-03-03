// package com.gtc.web.session;
//
//
// import cn.geoair..base.user.session.GiUserSession;
// import cn.geoair..base.user.session.GiUserSessionProvider;
//
//
//
/// *
// public class gtcWebUserSessionProvider implements GiUserSessionProvider<?> {
//
// @Override
// public GiUserSession userSession(boolean autoCreate) {
//
// HttpSession session = gtcHttpServletHelper.getRequest().getSession(autoCreate);
//
// if(session != null) {
// return new gtcWebUserSession(session);
// }
//
// return null;
// }
// }
//
// */
