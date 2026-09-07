package cn.geoair.map.dynamic.file.jdbc;

import cn.geoair.base.util.GutilObject;
import cn.geoair.comp.dynamic.ds.utils.DataSourceDruidFastCreate;
import cn.geoair.map.dynamic.adv.GirAdvQuery;
import cn.geoair.map.dynamic.adv.query.IAdvExecutor;
import cn.geoair.map.dynamic.adv.query.enums.AdvEnumsTypeGeom;
import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;
import cn.geoair.map.dynamic.file.core.exception.ExceptionConsumer;
import cn.geoair.map.dynamic.file.core.exception.GeoFileWriteException;
import cn.geoair.map.dynamic.file.core.link.LinkInfo;
import cn.geoair.map.dynamic.file.core.write.GeoFileWriter;
import cn.geoair.map.dynamic.file.core.write.config.WriteConfig;
import cn.geoair.map.dynamic.file.jdbc.link.JdbcGeoWriterLinkInfo;
import cn.geoair.map.dynamic.tools.GirGeoTools;
import org.geotools.data.DataStore;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.GeometryDescriptor;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 基于 JDBC 方言和 GirAdvQuery 的通用空间写入器。
 *
 * @author 张逢吉
 */
public abstract class AbstractJdbcGeoFileWriter implements GeoFileWriter {

    private final JdbcGeoDialect dialect;
    protected JdbcGeoWriterLinkInfo linkInfo;
    protected WriteConfig writeConfig = new WriteConfig();
    protected DataStore dataStore;
    protected DataSource dataSource;
    protected IAdvExecutor executor;

    protected AbstractJdbcGeoFileWriter(JdbcGeoDialect dialect) {
        this.dialect = dialect;
    }

    @Override
    public final void setLinkInfo(LinkInfo linkInfo) {
        if (!(linkInfo instanceof JdbcGeoWriterLinkInfo)) {
            throw new IllegalArgumentException("链接信息必须继承 JdbcGeoWriterLinkInfo");
        }
        closeQuietly();
        this.linkInfo = (JdbcGeoWriterLinkInfo) linkInfo;
        this.linkInfo.checkLinkInfo();
        try {
            DataSourceDruidFastCreate factory = new DataSourceDruidFastCreate();
            factory.setUrl(this.linkInfo.getJdbcUrl());
            factory.setUsername(this.linkInfo.getUsername());
            factory.setPassword(this.linkInfo.getPassword());
            factory.setInitialSize(1);
            factory.setMinIdle(1);
            factory.setConfigurator(dataSource -> dataSource.setConnectTimeout(this.linkInfo.getConnectTimeoutMillis()));
            this.dataSource = factory.toDataSource();
            this.dataStore = dialect.createDataStore(this.linkInfo);
            this.executor = GirAdvQuery.getIAdvExecutor(dataSource);
            if (GutilObject.isNotEmpty(this.linkInfo.getSchema())) {
                this.executor.setSchemaNameGetterFunction(() -> this.linkInfo.getSchema());
            }
        } catch (Exception e) {
            closeQuietly();
            throw new GeoFileWriteException("初始化 " + dialect.getName() + " 写入器失败", e);
        }
    }

    @Override
    public void setWriteConfig(WriteConfig writeConfig) {
        this.writeConfig = writeConfig == null ? new WriteConfig() : writeConfig;
    }

    @Override
    public GeoFileWriter writeHeader(SimpleFeatureType featureType, ExceptionConsumer exceptionConsumer) {
        try {
            if (featureType == null) {
                throw new IllegalArgumentException("featureType 不能为空");
            }
            if (dataStore.getSchema(linkInfo.getTableName()) != null) {
                if (!writeConfig.isOverwrite()) {
                    throw new GeoFileWriteException("目标表已存在且未开启覆盖：" + linkInfo.getTableName());
                }
                dataStore.removeSchema(linkInfo.getTableName());
            }
            org.geotools.feature.simple.SimpleFeatureTypeBuilder builder = new org.geotools.feature.simple.SimpleFeatureTypeBuilder();
            builder.init(featureType);
            builder.setName(linkInfo.getTableName());
            builder.setCRS(GirGeoTools.defaultInstance().getSridOpt().getCRS(writeConfig.getOutPutSrid()));
            SimpleFeatureType outputType = builder.buildFeatureType();
            dataStore.createSchema(outputType);

            GeometryDescriptor descriptor = outputType.getGeometryDescriptor();
            if (descriptor != null) {
                AdvEnumsTypeGeom type = AdvEnumsTypeGeom.findByGeoToolsClassValue(descriptor.getType().getBinding());
                executor.eDropGeomColumn(linkInfo.getTableName(), descriptor.getName().getLocalPart());
                executor.eAddGeomColumn(
                        linkInfo.getTableName(), descriptor.getName().getLocalPart(), type, writeConfig.getOutPutSrid());
            }
            return this;
        } catch (Exception e) {
            notifyException(exceptionConsumer, e);
            throw new GeoFileWriteException("创建 " + dialect.getName() + " 目标表失败", e);
        }
    }

    @Override
    public GeoFileWriter writeOneRow(GirAdvOneRow row, ExceptionConsumer exceptionConsumer) {
        if (row == null || row.isEmpty()) {
            return this;
        }
        try {
            GirAdvOneRow converted = convertRow(row);
            executor.bInsertIgnore(converted, option -> option
                    .setTableName(linkInfo.getTableName())
                    .setConflictKeys(linkInfo.getConflictKeys())
                    .setToUnderlineCase(false));
            return this;
        } catch (Exception e) {
            notifyException(exceptionConsumer, e);
            throw new GeoFileWriteException("写入 " + dialect.getName() + " 单条数据失败", e);
        }
    }

    @Override
    public GeoFileWriter writeRows(List<GirAdvOneRow> rows, ExceptionConsumer exceptionConsumer) {
        if (rows == null || rows.isEmpty()) {
            return this;
        }
        try {
            List<GirAdvOneRow> converted = new ArrayList<>(rows.size());
            for (GirAdvOneRow row : rows) {
                converted.add(convertRow(row));
            }
            executor.bInsertIgnoreBatch(converted, option -> option
                    .setTableName(linkInfo.getTableName())
                    .setBatchSize(linkInfo.getBatchSize())
                    .setConflictKeys(linkInfo.getConflictKeys())
                    .setToUnderlineCase(false));
            return this;
        } catch (Exception e) {
            notifyException(exceptionConsumer, e);
            throw new GeoFileWriteException("批量写入 " + dialect.getName() + " 数据失败", e);
        }
    }

    @Override
    public void close() {
        closeQuietly();
    }

    private GirAdvOneRow convertRow(GirAdvOneRow source) {
        GirAdvOneRow copy = GirAdvOneRow.ofByMap(source);
        for (Map.Entry<String, Object> entry : copy.entrySet()) {
            if (entry.getValue() instanceof Geometry) {
                Geometry geometry = (Geometry) entry.getValue();
                int sourceSrid = geometry.getSRID() > 0 ? geometry.getSRID() : linkInfo.getSrid();
                entry.setValue(GirGeoTools.defaultInstance().getSridOpt()
                        .convert(geometry, sourceSrid, writeConfig.getOutPutSrid()));
            }
        }
        return copy;
    }

    private void closeQuietly() {
        if (dataStore != null) {
            dataStore.dispose();
            dataStore = null;
        }
        if (dataSource instanceof AutoCloseable) {
            try {
                ((AutoCloseable) dataSource).close();
            } catch (Exception ignored) {
                // 关闭阶段不覆盖原始写入异常。
            }
        }
        dataSource = null;
        executor = null;
    }

    private void notifyException(ExceptionConsumer consumer, Exception e) {
        if (consumer != null) {
            consumer.accept(e);
        }
    }
}
