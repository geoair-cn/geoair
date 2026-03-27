package cn.geoair.map.dynamic.adv.dbmeta;

public interface DataType {

    public static DataType ILLEGAL =
            new DataType() {

                @Override
                public int ignoreLength() {
                    return -1;
                }

                @Override
                public int ignorePrecision() {
                    return -1;
                }

                @Override
                public int ignoreScale() {
                    return -1;
                }

                @Override
                public boolean support() {
                    return false;
                }

                @Override
                public Class supportClass() {
                    return null;
                }
            };

    /**
     * 定义列时 数据类型格式
     *
     * @return boolean
     */
    public abstract int ignoreLength();

    public abstract int ignorePrecision();

    public abstract int ignoreScale();

    public abstract boolean support();

    public abstract Class supportClass();
}
