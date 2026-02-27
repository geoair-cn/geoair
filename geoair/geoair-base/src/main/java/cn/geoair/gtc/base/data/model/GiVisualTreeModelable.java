//package  cn.geoair.gtc.base.data.model;
//
//import java.io.Serializable;
//import  cn.geoair.gtc.base.data.GiVisuable;
//import cn.geoair.gtc.base.data.model.support.GirVisualTreeModelKid;
//
//public interface GiVisualTreeModelable<ID extends Serializable> extends GiTreeModelable<ID>,GiVisuable {
//
//	default GirVisualTreeModelKid<ID> toVisualTreeModelKid() {
//		return  GirVisualTreeModelKid.valueWith(this.id(), this.display(), this.parentId());
//	}
//}
