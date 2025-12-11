//package  com.gtc.base.data.model;
//
//import java.io.Serializable;
//import  com.gtc.base.data.GiVisuable;
//import com.gtc.base.data.model.support.GtcVisualTreeModelKid;
//
//public interface GiVisualTreeModelable<ID extends Serializable> extends GiTreeModelable<ID>,GiVisuable {
//
//	default GtcVisualTreeModelKid<ID> toVisualTreeModelKid() {
//		return  GtcVisualTreeModelKid.valueWith(this.id(), this.display(), this.parentId());
//	}
//}
