package cn.geoair.comp.knife4j.ext.core.model;

import java.util.Arrays;
import java.util.List;

/**
 * DocketInfo class.
 *
 * @author ：张俊
 * @date ：Created in 2022/8/23 16:14 @description： Docket 收集模型
 * @version $Id: $Id
 */
public class DocketInfo {

    /** Constant <code>DEFAULT</code> */
    public static DocketInfo DEFAULT;

    private String groupName;

    private String basePackage;

    private List<String> basePackages;

    // .paths(PathSelectors.ant(“/sys/**”));
    private String specifyScan;

    private String modelscan;

    private List<Class> modelClassList;

    /**
     * Setter for the field <code>basePackages</code>.
     *
     * @param basePackages a {@link java.util.List} object
     */
    public void setBasePackages(List<String> basePackages) {
        this.basePackages = basePackages;
    }

    /**
     * Getter for the field <code>basePackages</code>.
     *
     * @return a {@link java.util.List} object
     */
    public List<String> getBasePackages() {
        if (basePackages == null) {
            basePackages = Arrays.asList(basePackage);
        }
        return basePackages;
    }

    /**
     * 基础实现
     *
     * @param groupName a {@link java.lang.String} object
     * @param basePackage a {@link java.lang.String} object
     */
    public DocketInfo(String groupName, String basePackage) {
        this.groupName = groupName;
        this.basePackage = basePackage;
        this.specifyScan = "";
    }

    /**
     * 基础实现
     *
     * @param groupName a {@link java.lang.String} object
     * @param basePackage a {@link java.lang.String} object
     * @param specifyScan 指定扫描某些controller 过滤 多个用逗号分隔 例如：api/task,api/layer
     */
    public DocketInfo(String groupName, String basePackage, String specifyScan) {
        this.groupName = groupName;
        this.basePackage = basePackage;
        this.specifyScan = specifyScan;
    }

    /**
     * 增强实现
     *
     * @param groupName a {@link java.lang.String} object
     * @param basePackage a {@link java.lang.String} object
     * @param specifyScan 指定的扫描controller 过滤 多个用逗号分隔 例如：/task,/layer
     * @param modelscan 扫描bean model 例如：com.ktw.api.model
     */
    public DocketInfo(String groupName, String basePackage, String specifyScan, String modelscan) {
        this.groupName = groupName;
        this.basePackage = basePackage;
        this.specifyScan = specifyScan;
        this.modelscan = modelscan;
    }

    /**
     * 增强实现
     *
     * @param groupName a {@link java.lang.String} object
     * @param basePackage a {@link java.lang.String} object
     * @param specifyScan 指定的扫描controller 过滤 多个用逗号分隔 例如：/task,/layer
     * @param modelClassList model Class集合
     */
    public DocketInfo(
            String groupName, String basePackage, String specifyScan, List<Class> modelClassList) {
        this.groupName = groupName;
        this.basePackage = basePackage;
        this.specifyScan = specifyScan;
        this.modelClassList = modelClassList;
    }

    /**
     * 增强实现
     *
     * @param groupName a {@link java.lang.String} object
     * @param basePackage a {@link java.lang.String} object
     * @param specifyScan 指定的扫描controller 过滤 多个用逗号分隔 例如：/task,/layer
     * @param modelscan 扫描bean model 例如：com.ktw.api.model
     * @param modelClassList model Class集合
     */
    public DocketInfo(
            String groupName,
            String basePackage,
            String specifyScan,
            String modelscan,
            List<Class> modelClassList) {
        this.groupName = groupName;
        this.basePackage = basePackage;
        this.specifyScan = specifyScan;
        this.modelscan = modelscan;
        this.modelClassList = modelClassList;
    }

    /**
     * Getter for the field <code>groupName</code>.
     *
     * @return a {@link java.lang.String} object
     */
    public String getGroupName() {
        return this.groupName;
    }

    /**
     * Getter for the field <code>basePackage</code>.
     *
     * @return a {@link java.lang.String} object
     */
    public String getBasePackage() {
        return this.basePackage;
    }

    /**
     * Getter for the field <code>modelscan</code>.
     *
     * @return a {@link java.lang.String} object
     */
    public String getModelscan() {
        return this.modelscan;
    }

    /**
     * Getter for the field <code>specifyScan</code>.
     *
     * @return a {@link java.lang.String} object
     */
    public String getSpecifyScan() {
        return this.specifyScan;
    }

    /**
     * Getter for the field <code>modelClassList</code>.
     *
     * @return a {@link java.util.List} object
     */
    public List<Class> getModelClassList() {
        return this.modelClassList;
    }

    static {
        DEFAULT = new DocketInfo("geoair", "cn.geoair.apidoc.controller", "");
    }
}
