package cn.geoair.map.dynamic.adv.query.wherequery.test;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * 用户实体类
 * 用于演示Lambda条件构建器的使用
 *
 * @author 张俊
 * @date Created in 2026/5/17
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 姓名
     */
    private String name;

    /**
     * 年龄
     */
    private Integer age;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 性别：0-女，1-男
     */
    private Integer gender;

    /**
     * 状态：0-禁用，1-启用
     */
    private Integer status;

    /**
     * 角色：admin-管理员，user-普通用户，guest-访客
     */
    private String role;

    /**
     * 部门ID
     */
    private Long deptId;

    /**
     * 薪资
     */
    private BigDecimal salary;

    /**
     * 入职日期
     */
    private LocalDateTime hireDate;

    /**
     * 生日
     */
    private Date birthday;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 删除时间（用于逻辑删除）
     */
    private LocalDateTime deletedAt;

    /**
     * 备注
     */
    private String remark;
}
