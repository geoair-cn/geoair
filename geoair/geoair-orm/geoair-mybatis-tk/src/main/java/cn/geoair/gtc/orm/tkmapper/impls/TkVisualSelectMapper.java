package cn.geoair.gtc.orm.tkmapper.impls;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.persistence.Id;
import cn.geoair.gtc.base.data.model.annotation.GaModelField;
import cn.geoair.gtc.base.data.model.support.GtcVisualModelKid;
import cn.geoair.gtc.base.data.model.support.GtcVisualTreeModelKid;
import cn.geoair.gtc.base.data.page.GfunPageExcute;
import cn.geoair.gtc.base.data.page.GiPageParam;
import cn.geoair.gtc.base.data.page.GiPager;
import cn.geoair.gtc.base.exception.GtcException;
import cn.geoair.gtc.base.gpa.dao.GiPagerDao;
import cn.geoair.gtc.base.gpa.dao.GiVisualSelectDao;
import cn.geoair.gtc.base.gpa.entity.GiEntityVisuable;
import cn.geoair.gtc.base.util.GutilReflection;
import cn.geoair.gtc.orm.mybatis.impls.MyBatisMapper;
import tk.mybatis.mapper.common.example.SelectByExampleMapper;
import tk.mybatis.mapper.entity.Example;


public interface TkVisualSelectMapper<T extends GiEntityVisuable<PK>,PK extends Serializable> extends MyBatisMapper<T,PK>,SelectByExampleMapper<T>,GiVisualSelectDao<T,PK>/*,MySqlMapper<T>,IdsMapper<T>*/ {



	@SuppressWarnings("rawtypes")
	@Override
	default List<GtcVisualModelKid>  gtcSearchVisualModel(String displayQuery, String[] containKeys){

		Class<?> modelClass = getModelClass();

		Example example2 = new Example(modelClass);

        Example.Criteria criteria = example2.createCriteria();
        Field[] fields = modelClass.getDeclaredFields();

        Field[] matchField = new Field[3];

        for (Field field : fields) {
            Id idAno = field.getAnnotation(Id.class);
            if (idAno != null) {
                if (matchField[0] == null){
                    matchField[0] = field;
                }
                if (matchField[1] == null){
                    matchField[1] = field;
                }
            }
            GaModelField rxModelField = field.getAnnotation(GaModelField.class);
            if (rxModelField != null) {
                if (rxModelField.isID()) {
                    matchField[0] = field;
                    if (matchField[1] == null){
                        matchField[1] = field;
                    }
                }
                if (rxModelField.isDisplay()) {
                    matchField[1] = field;
                }
                if (rxModelField.isParentId()) {
                    matchField[2] = field;
                }
            }
        }
        List<String> strings = new ArrayList<>();
        for (Field eachField : matchField){
            if (eachField != null) {
                strings.add(eachField.getName());
                GutilReflection.makeAccessible(eachField);
            }
        }
        if (matchField[0] == null) {
            throw new GtcException("配置有误，模型缺少主键标识");
        }

        example2.selectProperties(strings.toArray(new String[]{}));
        List<GtcVisualModelKid> listVoList = new ArrayList<GtcVisualModelKid>();

        if (containKeys != null && containKeys.length > 0) {
            criteria.orIn(matchField[0].getName(), Arrays.asList(containKeys));
        }
        if (displayQuery != null) {
            criteria.andLike(matchField[1].getName(), "%" + displayQuery + "%");
        }

        List<T> poList = this.selectByExample(example2);
        for (T po : poList) {
        	 GtcVisualModelKid listVo;
            if (matchField[2] != null) {
                listVo = new GtcVisualTreeModelKid();
                try {
                    ((GtcVisualTreeModelKid) listVo).setPid(String.valueOf(matchField[2].get(po)));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            } else {
                listVo = new GtcVisualModelKid();
            }
            listVoList.add(listVo);
            try {
                listVo.setId(String.valueOf(matchField[0].get(po)));
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
            try {
                listVo.setName(String.valueOf(matchField[1].get(po)));
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return listVoList;
	}


	@SuppressWarnings({ "rawtypes", "unchecked" })

	@Override
	default GiPager<GtcVisualModelKid>  gtcSearchVisualPage(String displayQuery, String[] containKeys, GiPageParam pageParam) {
		TkVisualSelectMapper me = this;
		TkPagerMapper<T, PK> pageDao = (TkPagerMapper<T, PK>)GiPagerDao.getDao(this.getModelClass());

    	return pageDao.pageExcuter().excutePage(new GfunPageExcute<GtcVisualModelKid>() {
			@Override
            public List<GtcVisualModelKid> excute() {
            	return me. gtcSearchVisualModel(displayQuery,containKeys);
            }
        },pageParam);
	}
}
