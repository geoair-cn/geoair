package cn.geoair.gtc.orm.spi.support;

import java.util.ArrayList;
import java.util.List;

/**
 * @author ：张俊
 * @date ：Created in 2022/6/30 16:11
 * @description： 简单sql拼接器
 */
public class GtcSqls {

    private Criteria criteria;

    private GtcSqls() {
        this.criteria = new Criteria();
    }

    public static GtcSqls custom() {
        return new GtcSqls();
    }

    public Criteria getCriteria() {
        return criteria;
    }

    public GtcSqls andIsNull(String property) {
        this.criteria.criterions.add(new Criterion(property, "is null", "and"));
        return this;
    }

    public GtcSqls andIsNotNull(String property) {
        this.criteria.criterions.add(new Criterion(property, "is not null", "and"));
        return this;
    }

    public GtcSqls andEqualTo(String property, Object value) {
        this.criteria.criterions.add(new Criterion(property, value, "=", "and"));
        return this;
    }

    public GtcSqls andNotEqualTo(String property, Object value) {
        this.criteria.criterions.add(new Criterion(property, value, "<>", "and"));
        return this;
    }

    public GtcSqls andGreaterThan(String property, Object value) {
        this.criteria.criterions.add(new Criterion(property, value, ">", "and"));
        return this;
    }

    public GtcSqls andGreaterThanOrEqualTo(String property, Object value) {
        this.criteria.criterions.add(new Criterion(property, value, ">=", "and"));
        return this;
    }


    public GtcSqls andLessThan(String property, Object value) {
        this.criteria.criterions.add(new Criterion(property, value, "<", "and"));
        return this;
    }

    public GtcSqls andLessThanOrEqualTo(String property, Object value) {
        this.criteria.criterions.add(new Criterion(property, value, "<=", "and"));
        return this;
    }

    public GtcSqls andIn(String property, Iterable values) {
        this.criteria.criterions.add(new Criterion(property, values, "in", "and"));
        return this;
    }

    public GtcSqls andNotIn(String property, Iterable values) {
        this.criteria.criterions.add(new Criterion(property, values, "not in", "and"));
        return this;
    }

    public GtcSqls andBetween(String property, Object value1, Object value2) {
        this.criteria.criterions.add(new Criterion(property, value1, value2, "between", "and"));
        return this;
    }

    public GtcSqls andNotBetween(String property, Object value1, Object value2) {
        this.criteria.criterions.add(new Criterion(property, value1, value2, "not between", "and"));
        return this;
    }

    public GtcSqls andLike(String property, String value) {
        this.criteria.criterions.add(new Criterion(property, value, "like", "and"));
        return this;
    }

    public GtcSqls andNotLike(String property, String value) {
        this.criteria.criterions.add(new Criterion(property, value, "not like", "and"));
        return this;
    }


    public GtcSqls orIsNull(String property) {
        this.criteria.criterions.add(new Criterion(property, "is null", "or"));
        return this;
    }

    public GtcSqls orIsNotNull(String property) {
        this.criteria.criterions.add(new Criterion(property, "is not null", "or"));
        return this;
    }


    public GtcSqls orEqualTo(String property, Object value) {
        this.criteria.criterions.add(new Criterion(property, value, "=", "or"));
        return this;
    }

    public GtcSqls orNotEqualTo(String property, Object value) {
        this.criteria.criterions.add(new Criterion(property, value, "<>", "or"));
        return this;
    }

    public GtcSqls orGreaterThan(String property, Object value) {
        this.criteria.criterions.add(new Criterion(property, value, ">", "or"));
        return this;
    }

    public GtcSqls orGreaterThanOrEqualTo(String property, Object value) {
        this.criteria.criterions.add(new Criterion(property, value, ">=", "or"));
        return this;
    }

    public GtcSqls orLessThan(String property, Object value) {
        this.criteria.criterions.add(new Criterion(property, value, "<", "or"));
        return this;
    }

    public GtcSqls orLessThanOrEqualTo(String property, Object value) {
        this.criteria.criterions.add(new Criterion(property, value, "<=", "or"));
        return this;
    }

    public GtcSqls orIn(String property, Iterable values) {
        this.criteria.criterions.add(new Criterion(property, values, "in", "or"));
        return this;
    }

    public GtcSqls orNotIn(String property, Iterable values) {
        this.criteria.criterions.add(new Criterion(property, values, "not in", "or"));
        return this;
    }

    public GtcSqls orBetween(String property, Object value1, Object value2) {
        this.criteria.criterions.add(new Criterion(property, value1, value2, "between", "or"));
        return this;
    }

    public GtcSqls orNotBetween(String property, Object value1, Object value2) {
        this.criteria.criterions.add(new Criterion(property, value1, value2, "not between", "or"));
        return this;
    }

    public GtcSqls orLike(String property, String value) {
        this.criteria.criterions.add(new Criterion(property, value, "like", "or"));
        return this;
    }

    public GtcSqls orNotLike(String property, String value) {
        this.criteria.criterions.add(new Criterion(property, value, "not like", "or"));
        return this;
    }

    public static class Criteria {
        private String andOr;
        private List<Criterion> criterions;

        public Criteria() {
            this.criterions = new ArrayList<Criterion>(2);
        }

        public List<Criterion> getCriterions() {
            return criterions;
        }

        public String getAndOr() {
            return andOr;
        }

        public void setAndOr(String andOr) {
            this.andOr = andOr;
        }
    }

    public static class Criterion {
        private String property;
        private Object value;
        private Object secondValue;
        private String condition;
        private String andOr;

        public Criterion(String property, String condition, String andOr) {
            this.property = property;
            this.condition = condition;
            this.andOr = andOr;
        }


        public Criterion(String property, Object value, String condition, String andOr) {
            this.property = property;
            this.value = value;
            this.condition = condition;
            this.andOr = andOr;
        }

        public Criterion(String property, Object value1, Object value2, String condition, String andOr) {
            this.property = property;
            this.value = value1;
            this.secondValue = value2;
            this.condition = condition;
            this.andOr = andOr;
        }

        public String getProperty() {
            return property;
        }

        public Object getValue() {
            return value;
        }

        public Object getSecondValue() {
            return secondValue;
        }

        public Object[] getValues() {
            if (value != null) {
                if (secondValue != null) {
                    return new Object[]{value, secondValue};
                } else {
                    return new Object[]{value};
                }
            } else {
                return new Object[]{};
            }
        }

        public String getCondition() {
            return condition;
        }

        public String getAndOr() {
            return andOr;
        }
    }
}
