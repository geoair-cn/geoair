package cn.geoair.map.dynamic.adv.query;


/**
 * 基础操作的接口，模仿mybatis-plus的baseMapper
 * 约定：所有的方法接口以b开头，便于代码编译器识别
 */
public interface IAdvBaseOpt extends IAdvBaseSelectOpt, IAdvBaseDeleteOpt, IAdvBaseAccessOpt, IAdvBaseUpdateOpt {

}
