package cn.geoair.comp.jdbc.url.beans;

import java.io.Serializable;
import lombok.Data;

/**
 * JDBC URL 参数。参数以列表而不是 Map 保存，避免重写 URL 时丢失参数顺序或重复参数。
 *
 * @author 张逢吉
 */
@Data
public final class JdbcUrlProperty implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 参数名，保留 URL 中的原始大小写。 */
    private final String name;
    /** 参数值；无等号的开关型参数为 null。 */
    private final String value;
    /** 是否在原 URL 中显式包含等号。 */
    private final boolean hasEquals;

    public JdbcUrlProperty(String name, String value, boolean hasEquals) {
        this.name = name;
        this.value = value;
        this.hasEquals = hasEquals;
    }

    public String render() {
        return hasEquals ? name + "=" + (value == null ? "" : value) : name;
    }
}
