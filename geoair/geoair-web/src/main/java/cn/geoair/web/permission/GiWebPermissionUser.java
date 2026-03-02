// package com.gtc.web.permission;
//
// import java.io.Serializable;
// import java.util.Collection;
//
// import cn.geoair.gtc.base.log.GiLoger;
// import cn.geoair.gtc.base.log.GirLoger;
// import cn.geoair.gtc.base.user.permission.GiPermission;
// import cn.geoair.gtc.base.user.permission.GiPermissionUser;
// import cn.geoair.gtc.base.user.session.GiSessionUser;
// import cn.geoair.gtc.base.user.session.GiUserSession;
// import cn.geoair.gtc.base.user.session.GiUserSessionConfig;
// import com.gtc.web.user.GiWebUser;
//
// public interface GiWebPermissionUser<ID extends Serializable> extends
// GiWebUser<ID>,GiPermissionUser<ID> {
//
//
//
// public static GiLoger logger = GirLoger.getLoger(GiWebPermissionUser.class);
//
//
// /**
// * 设定用户为登录状态
// * @return 返回会话id
// */
// @Override
// default public void sessionLogin() {
// GiWebUser.super.sessionLogin();
// this.permissions();
// }
//
// /**
// * 设定用户为登出状态
// * @return
// */
// @Override
// default public void sessionLogout() {
// GiUserSession session = userSession(false);
// if(session != null) {
// GiUserSessionConfig usc = GiUserSessionConfig.getUserConfig(this. gtcType().
// gtcTypeUserClass(GiSessionUser.class));
// session.removeAttribute(usc.keyPermissionInSession());
// }
// GiWebUser.super.sessionLogout();
// }
//
//
// @Override
// default public Collection<? extends GiPermission> permissions(){
//
// GiUserSession session = userSession(true);
// GiUserSessionConfig cfg = GiUserSessionConfig.getUserConfig(this. gtcType().
// gtcTypeUserClass(GiSessionUser.class));
//
// Object obj = session.getAttribute(cfg.keyPermissionInSession());
// if(obj == null) {
// Collection<? extends GiPermission> res = GiPermissionUser.super.permissions();
// session.setAttribute(cfg.keyPermissionInSession(), res);
// return res;
// }
// return (Collection<? extends GiPermission>)obj;
// }
//
// }
