package cn.geoair.spi.util;

import cn.geoair.base.bean.GirBeanException;
import cn.geoair.base.util.GutilBean;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

@Component
public class SpringBeanCopyProvider4Gir implements GutilBean.BeanCopyProvider {

    static {
        GutilBean.setBeanCopyProvider(new SpringBeanCopyProvider4Gir());
    }

    @Override
    public void copyProperties(
            Object source, Object target, Class<?> editable, String... ignoreProperties)
            throws GirBeanException {
        if (editable == null) {
            BeanUtils.copyProperties(source, target, ignoreProperties);
        } else {
            BeanUtils.copyProperties(source, target, editable);
        }
    }
}
