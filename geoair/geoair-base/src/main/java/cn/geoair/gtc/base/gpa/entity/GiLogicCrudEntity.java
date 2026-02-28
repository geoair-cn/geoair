package cn.geoair.gtc.base.gpa.entity;

import java.io.Serializable;

/**
 * @author ：张俊
 * @date ：Created in 2023/12/13 13:36 @description：
 * 逻辑删除相关的基础接口实体类，继承自GiCrudEntity接口，提供逻辑删除标记的设置方法
 * @param <PK> 主键类型，必须实现Serializable接口
 */
public interface GiLogicCrudEntity<PK extends Serializable> extends GiCrudEntity<PK> {

	/**
	 * 设置实体为已删除状态，用于逻辑删除操作
	 */
	void setDel();

	/**
	 * 设置实体为未删除状态，用于恢复逻辑删除的实体
	 */
	void setNotDel();

}
