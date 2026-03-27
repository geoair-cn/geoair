package cn.geoair.orm.tkmapper.impls;

import cn.geoair.base.data.model.annotation.GaModelField;
import cn.geoair.base.data.model.support.GirVisualModelKid;
import cn.geoair.base.data.model.support.GirVisualTreeModelKid;
import cn.geoair.base.data.page.GfunPageExcute;
import cn.geoair.base.data.page.GiPageParam;
import cn.geoair.base.data.page.GiPager;
import cn.geoair.base.exception.GirException;
import cn.geoair.base.gpa.dao.GiPagerDao;
import cn.geoair.base.gpa.dao.GiVisualSelectDao;
import cn.geoair.base.gpa.entity.GiEntityVisuable;
import cn.geoair.base.util.GutilReflection;
import cn.geoair.orm.mybatis.impls.MyBatisMapper;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.persistence.Id;
import tk.mybatis.mapper.common.example.SelectByExampleMapper;
import tk.mybatis.mapper.entity.Example;

public interface TkVisualSelectMapper<T extends GiEntityVisuable<PK>, PK extends Serializable>
        extends MyBatisMapper<T, PK>,
                SelectByExampleMapper<T>,
                GiVisualSelectDao<T, PK> /* ,MySqlMapper<T>,IdsMapper<T> */ {

    @SuppressWarnings("rawtypes")
    @Override
    default List<GirVisualModelKid> gtcSearchVisualModel(
            String displayQuery, String[] containKeys) {

        Class<?> modelClass = getModelClass();

        Example example2 = new Example(modelClass);

        Example.Criteria criteria = example2.createCriteria();
        Field[] fields = modelClass.getDeclaredFields();

        Field[] matchField = new Field[3];

        for (Field field : fields) {
            Id idAno = field.getAnnotation(Id.class);
            if (idAno != null) {
                if (matchField[0] == null) {
                    matchField[0] = field;
                }
                if (matchField[1] == null) {
                    matchField[1] = field;
                }
            }
            GaModelField rxModelField = field.getAnnotation(GaModelField.class);
            if (rxModelField != null) {
                if (rxModelField.isID()) {
                    matchField[0] = field;
                    if (matchField[1] == null) {
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
        for (Field eachField : matchField) {
            if (eachField != null) {
                strings.add(eachField.getName());
                GutilReflection.makeAccessible(eachField);
            }
        }
        if (matchField[0] == null) {
            throw new GirException("配置有误，模型缺少主键标识");
        }

        example2.selectProperties(strings.toArray(new String[] {}));
        List<GirVisualModelKid> listVoList = new ArrayList<GirVisualModelKid>();

        if (containKeys != null && containKeys.length > 0) {
            criteria.orIn(matchField[0].getName(), Arrays.asList(containKeys));
        }
        if (displayQuery != null) {
            criteria.andLike(matchField[1].getName(), "%" + displayQuery + "%");
        }

        List<T> poList = this.selectByExample(example2);
        for (T po : poList) {
            GirVisualModelKid listVo;
            if (matchField[2] != null) {
                listVo = new GirVisualTreeModelKid();
                try {
                    ((GirVisualTreeModelKid) listVo).setPid(String.valueOf(matchField[2].get(po)));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            } else {
                listVo = new GirVisualModelKid();
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

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    default GiPager<GirVisualModelKid> gtcSearchVisualPage(
            String displayQuery, String[] containKeys, GiPageParam pageParam) {
        TkVisualSelectMapper me = this;
        TkPagerMapper<T, PK> pageDao =
                (TkPagerMapper<T, PK>) GiPagerDao.getDao(this.getModelClass());

        return pageDao.pageExcuter()
                .excutePage(
                        new GfunPageExcute<GirVisualModelKid>() {
                            @Override
                            public List<GirVisualModelKid> excute() {
                                return me.gtcSearchVisualModel(displayQuery, containKeys);
                            }
                        },
                        pageParam);
    }
}
