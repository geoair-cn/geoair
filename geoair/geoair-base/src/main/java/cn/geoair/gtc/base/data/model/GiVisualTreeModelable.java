//package  com.gtc.base.data.model;
//
//import java.io.Serializable;
//import  com.gtc.base.data.GiVisuable;
//import com.gtc.base.data.model.support.GirVisualTreeModelKid;
//
//public interface GiVisualTreeModelable<ID extends Serializable> extends GiTreeModelable<ID>,GiVisuable {
//
//	default GirVisualTreeModelKid<ID> toVisualTreeModelKid() {
//		return  GirVisualTreeModelKid.valueWith(this.id(), this.display(), this.parentId());
//	}
//}
