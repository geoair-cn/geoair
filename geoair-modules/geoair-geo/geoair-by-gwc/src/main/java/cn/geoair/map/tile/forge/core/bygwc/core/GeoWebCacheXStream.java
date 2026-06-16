
package cn.geoair.map.tile.forge.core.bygwc.core;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.ConverterLookup;
import com.thoughtworks.xstream.converters.ConverterRegistry;
import com.thoughtworks.xstream.converters.*;
import com.thoughtworks.xstream.converters.basic.*;
import com.thoughtworks.xstream.converters.basic.FloatConverter;
import com.thoughtworks.xstream.converters.basic.IntConverter;
import com.thoughtworks.xstream.converters.collections.ArrayConverter;
import com.thoughtworks.xstream.converters.collections.*;
import com.thoughtworks.xstream.converters.extended.*;
import com.thoughtworks.xstream.converters.reflection.ReflectionConverter;
import com.thoughtworks.xstream.converters.reflection.ReflectionProvider;
import com.thoughtworks.xstream.core.ClassLoaderReference;
import com.thoughtworks.xstream.core.JVM;
import com.thoughtworks.xstream.core.util.SelfStreamingInstanceChecker;
import com.thoughtworks.xstream.io.HierarchicalStreamDriver;
import com.thoughtworks.xstream.mapper.Mapper;
import com.thoughtworks.xstream.security.NoTypePermission;
import com.thoughtworks.xstream.security.PrimitiveTypePermission;

import java.lang.reflect.Constructor;

/**
 * XStream 子类，用于 GeoWebCache 的 XML 序列化和反序列化
 *
 * @author Kevin Smith, Boundless
 */
public class GeoWebCacheXStream extends XStream {

    /**
     * 默认构造函数
     */
    public GeoWebCacheXStream() {
        super();
        init();
    }

    /**
     * 使用指定的分层流驱动构造 XStream 实例
     *
     * @param hierarchicalStreamDriver 分层流驱动
     */
    public GeoWebCacheXStream(HierarchicalStreamDriver hierarchicalStreamDriver) {
        super(hierarchicalStreamDriver);
        init();
    }

    /**
     * 使用指定的反射提供者、驱动、类加载器引用、映射器、转换器查找和注册表构造 XStream 实例
     *
     * @param reflectionProvider 反射提供者
     * @param driver 驱动程序
     * @param classLoaderReference 类加载器引用
     * @param mapper 映射器
     * @param converterLookup 转换器查找
     * @param converterRegistry 转换器注册表
     */
    public GeoWebCacheXStream(
            ReflectionProvider reflectionProvider,
            HierarchicalStreamDriver driver,
            ClassLoaderReference classLoaderReference,
            Mapper mapper,
            ConverterLookup converterLookup,
            ConverterRegistry converterRegistry) {
        super(
                reflectionProvider,
                driver,
                classLoaderReference,
                mapper,
                converterLookup,
                converterRegistry);
        init();
    }

    /**
     * 使用指定的反射提供者、驱动、类加载器引用和映射器构造 XStream 实例
     *
     * @param reflectionProvider 反射提供者
     * @param driver 驱动程序
     * @param classLoaderReference 类加载器引用
     * @param mapper 映射器
     */
    public GeoWebCacheXStream(
            ReflectionProvider reflectionProvider,
            HierarchicalStreamDriver driver,
            ClassLoaderReference classLoaderReference,
            Mapper mapper) {
        super(reflectionProvider, driver, classLoaderReference, mapper);
        init();
    }

    /**
     * 使用指定的反射提供者、驱动和类加载器引用构造 XStream 实例
     *
     * @param reflectionProvider 反射提供者
     * @param driver 驱动程序
     * @param classLoaderReference 类加载器引用
     */
    public GeoWebCacheXStream(
            ReflectionProvider reflectionProvider,
            HierarchicalStreamDriver driver,
            ClassLoaderReference classLoaderReference) {
        super(reflectionProvider, driver, classLoaderReference);
        init();
    }

    /**
     * 使用指定的反射提供者和驱动构造 XStream 实例
     *
     * @param reflectionProvider 反射提供者
     * @param hierarchicalStreamDriver 分层流驱动
     */
    public GeoWebCacheXStream(
            ReflectionProvider reflectionProvider,
            HierarchicalStreamDriver hierarchicalStreamDriver) {
        super(reflectionProvider, hierarchicalStreamDriver);
        init();
    }

    /**
     * 使用指定的反射提供者构造 XStream 实例
     *
     * @param reflectionProvider 反射提供者
     */
    public GeoWebCacheXStream(ReflectionProvider reflectionProvider) {
        super(reflectionProvider);
        init();
    }

    /**
     * 使用指定的反射提供者、驱动、类加载器、映射器、转换器查找和注册表构造 XStream 实例（已弃用）
     *
     * @param reflectionProvider 反射提供者
     * @param driver 驱动程序
     * @param classLoader 类加载器
     * @param mapper 映射器
     * @param converterLookup 转换器查找
     * @param converterRegistry 转换器注册表
     * @deprecated 使用 {@link #GeoWebCacheXStream(ReflectionProvider, HierarchicalStreamDriver, ClassLoaderReference, Mapper, ConverterLookup, ConverterRegistry)} 替代
     */
    @Deprecated
    public GeoWebCacheXStream(
            ReflectionProvider reflectionProvider,
            HierarchicalStreamDriver driver,
            ClassLoader classLoader,
            Mapper mapper,
            ConverterLookup converterLookup,
            ConverterRegistry converterRegistry) {
        super(reflectionProvider, driver, classLoader, mapper, converterLookup, converterRegistry);
        init();
    }

    /**
     * 使用指定的反射提供者、驱动、类加载器和映射器构造 XStream 实例（已弃用）
     *
     * @param reflectionProvider 反射提供者
     * @param driver 驱动程序
     * @param classLoader 类加载器
     * @param mapper 映射器
     * @deprecated 使用 {@link #GeoWebCacheXStream(ReflectionProvider, HierarchicalStreamDriver, ClassLoaderReference, Mapper)} 替代
     */
    @Deprecated
    public GeoWebCacheXStream(
            ReflectionProvider reflectionProvider,
            HierarchicalStreamDriver driver,
            ClassLoader classLoader,
            Mapper mapper) {
        super(reflectionProvider, driver, classLoader, mapper);
        init();
    }

    /**
     * 使用指定的反射提供者、驱动和类加载器构造 XStream 实例（已弃用）
     *
     * @param reflectionProvider 反射提供者
     * @param driver 驱动程序
     * @param classLoader 类加载器
     * @deprecated 使用 {@link #GeoWebCacheXStream(ReflectionProvider, HierarchicalStreamDriver, ClassLoaderReference)} 替代
     */
    @Deprecated
    public GeoWebCacheXStream(
            ReflectionProvider reflectionProvider,
            HierarchicalStreamDriver driver,
            ClassLoader classLoader) {
        super(reflectionProvider, driver, classLoader);
        init();
    }

    /**
     * 使用指定的反射提供者、映射器和驱动构造 XStream 实例（已弃用）
     *
     * @param reflectionProvider 反射提供者
     * @param mapper 映射器
     * @param driver 驱动程序
     * @deprecated 使用 {@link #GeoWebCacheXStream(ReflectionProvider, HierarchicalStreamDriver, ClassLoaderReference, Mapper)} 替代
     */
    @Deprecated
    public GeoWebCacheXStream(
            ReflectionProvider reflectionProvider, Mapper mapper, HierarchicalStreamDriver driver) {
        super(reflectionProvider, mapper, driver);
        init();
    }

    /**
     * 为类型层次结构添加安全权限
     *
     * @param types 允许的基础类型
     * @since 1.4.7
     */
    public void allowTypeHierarchies(Class<?>... types) {
        for (Class<?> type : types) {
            this.allowTypeHierarchy(type);
        }
    }

    /**
     * 初始化方法，设置安全策略和忽略未知元素
     */
    private void init() {
        // 忽略未知字段，这允许加载具有已弃用和现在已删除元素的旧配置
        ignoreUnknownElements();

        // 要求类在白名单上
        addPermission(NoTypePermission.NONE);

        // 允许基本类型
        addPermission(new PrimitiveTypePermission());

        // 常见的非基本类型
        allowTypes(
                new Class[] {
                    String.class,
                    java.util.Date.class,
                    java.sql.Date.class,
                    java.sql.Timestamp.class,
                    java.sql.Time.class,
                });

        // 常见集合类型
        allowTypes(
                new Class[] {
                    java.util.TreeSet.class,
                    java.util.SortedSet.class,
                    java.util.Set.class,
                    java.util.HashSet.class,
                    java.util.List.class,
                    java.util.ArrayList.class,
                    java.util.Map.class,
                    java.util.HashMap.class,
                    java.util.concurrent.CopyOnWriteArrayList.class,
                    java.util.concurrent.ConcurrentHashMap.class,
                });

//        String whitelistProp = GeoWebCacheExtensions.getProperty("GEOWEBCACHE_XSTREAM_WHITELIST");
//        if (whitelistProp != null) {
//            String[] wildcards = whitelistProp.split("\\s+|(\\s*;\\s*)");
//            this.allowTypesByWildcard(wildcards);
//        }
    }

    /**
     * 此方法是基类方法的克隆，在可能的情况下保持转换器的相同顺序，
     * 但修改了一些对Java核心类执行非法反射访问的转换器，
     * 用不执行此类访问的替代品替换它们，或者如果我们不使用它们就简单地移除
     */
    @Override
    protected void setupConverters() {
        Mapper mapper = getMapper();
        ReflectionProvider reflectionProvider = getReflectionProvider();
        ClassLoaderReference classLoaderReference = getClassLoaderReference();
        ConverterLookup converterLookup = getConverterLookup();

        registerConverter(new ReflectionConverter(mapper, reflectionProvider), PRIORITY_VERY_LOW);

        registerConverter(new NullConverter(), PRIORITY_VERY_HIGH);
        registerConverter(new IntConverter(), PRIORITY_NORMAL);
        registerConverter(new FloatConverter(), PRIORITY_NORMAL);
        registerConverter(new DoubleConverter(), PRIORITY_NORMAL);
        registerConverter(new LongConverter(), PRIORITY_NORMAL);
        registerConverter(new ShortConverter(), PRIORITY_NORMAL);
        registerConverter((Converter) new CharConverter(), PRIORITY_NORMAL);
        registerConverter(new BooleanConverter(), PRIORITY_NORMAL);
        registerConverter(new ByteConverter(), PRIORITY_NORMAL);

        registerConverter(new StringConverter(), PRIORITY_NORMAL);
        registerConverter(new StringBufferConverter(), PRIORITY_NORMAL);
        registerConverter(new DateConverter(), PRIORITY_NORMAL);
        registerConverter(new BitSetConverter(), PRIORITY_NORMAL);
        registerConverter(new URIConverter(), PRIORITY_NORMAL);
        registerConverter(new URLConverter(), PRIORITY_NORMAL);
        registerConverter(new BigIntegerConverter(), PRIORITY_NORMAL);
        registerConverter(new BigDecimalConverter(), PRIORITY_NORMAL);

        registerConverter(new ArrayConverter(mapper), PRIORITY_NORMAL);
        registerConverter(new CharArrayConverter(), PRIORITY_NORMAL);
        registerConverter(new CollectionConverter(mapper), PRIORITY_NORMAL);
        registerConverter(new MapConverter(mapper), PRIORITY_NORMAL);
        registerConverter(new TreeMapConverter(mapper), PRIORITY_NORMAL);
        registerConverter(new TreeSetConverter(mapper), PRIORITY_NORMAL);
        registerConverter(new SingletonCollectionConverter(mapper), PRIORITY_NORMAL);
        registerConverter(new SingletonMapConverter(mapper), PRIORITY_NORMAL);
        registerConverter((Converter) new EncodedByteArrayConverter(), PRIORITY_NORMAL);

        registerConverter(new FileConverter(), PRIORITY_NORMAL);
        if (JVM.isSQLAvailable()) {
            registerConverter(new SqlTimestampConverter(), PRIORITY_NORMAL);
            registerConverter(new SqlTimeConverter(), PRIORITY_NORMAL);
            registerConverter(new SqlDateConverter(), PRIORITY_NORMAL);
        }
        registerConverter(new JavaClassConverter(classLoaderReference), PRIORITY_NORMAL);
        registerConverter(new JavaMethodConverter(classLoaderReference), PRIORITY_NORMAL);
        registerConverter(new JavaFieldConverter(classLoaderReference), PRIORITY_NORMAL);

        if (JVM.isAWTAvailable()) {
            registerConverter(new ColorConverter(), PRIORITY_NORMAL);
        }
        if (JVM.isSwingAvailable()) {
            registerConverter(
                    new LookAndFeelConverter(mapper, reflectionProvider), PRIORITY_NORMAL);
        }
        registerConverter(new LocaleConverter(), PRIORITY_NORMAL);
        registerConverter(new GregorianCalendarConverter(), PRIORITY_NORMAL);

        // 动态绑定转换器 - 允许XStream在较早的JDK上编译
        registerConverterDynamically(
                "com.thoughtworks.xstream.converters.extended.SubjectConverter",
                PRIORITY_NORMAL,
                new Class[] {Mapper.class},
                new Object[] {mapper});
        registerConverterDynamically(
                "com.thoughtworks.xstream.converters.extended.ThrowableConverter",
                PRIORITY_NORMAL,
                new Class[] {ConverterLookup.class},
                new Object[] {converterLookup});
        registerConverterDynamically(
                "com.thoughtworks.xstream.converters.extended.StackTraceElementConverter",
                PRIORITY_NORMAL,
                null,
                null);
        registerConverterDynamically(
                "com.thoughtworks.xstream.converters.extended.CurrencyConverter",
                PRIORITY_NORMAL,
                null,
                null);
        registerConverterDynamically(
                "com.thoughtworks.xstream.converters.extended.RegexPatternConverter",
                PRIORITY_NORMAL,
                null,
                null);
        registerConverterDynamically(
                "com.thoughtworks.xstream.converters.extended.CharsetConverter",
                PRIORITY_NORMAL,
                null,
                null);

        // 动态绑定转换器 - 允许XStream在较早的JDK上编译
        if (JVM.loadClassForName("javax.xml.datatype.Duration") != null) {
            registerConverterDynamically(
                    "com.thoughtworks.xstream.converters.extended.DurationConverter",
                    PRIORITY_NORMAL,
                    null,
                    null);
        }
        registerConverterDynamically(
                "com.thoughtworks.xstream.converters.enums.EnumConverter",
                PRIORITY_NORMAL,
                null,
                null);
        registerConverterDynamically(
                "com.thoughtworks.xstream.converters.basic.StringBuilderConverter",
                PRIORITY_NORMAL,
                null,
                null);
        registerConverterDynamically(
                "com.thoughtworks.xstream.converters.basic.UUIDConverter",
                PRIORITY_NORMAL,
                null,
                null);
        if (JVM.loadClassForName("javax.activation.ActivationDataFlavor") != null) {
            registerConverterDynamically(
                    "com.thoughtworks.xstream.converters.extended.ActivationDataFlavorConverter",
                    PRIORITY_NORMAL,
                    null,
                    null);
        }
        registerConverterDynamically(
                "com.thoughtworks.xstream.converters.extended.PathConverter",
                PRIORITY_NORMAL,
                null,
                null);

        registerConverterDynamically(
                "com.thoughtworks.xstream.converters.time.ChronologyConverter",
                PRIORITY_NORMAL,
                null,
                null);
        registerConverterDynamically(
                "com.thoughtworks.xstream.converters.time.DurationConverter",
                PRIORITY_NORMAL,
                null,
                null);
        registerConverterDynamically(
                "com.thoughtworks.xstream.converters.time.HijrahDateConverter",
                PRIORITY_NORMAL,
                null,
                null);
        registerConverterDynamically(
                "com.thoughtworks.xstream.converters.time.JapaneseDateConverter",
                PRIORITY_NORMAL,
                null,
                null);
        registerConverterDynamically(
                "com.thoughtworks.xstream.converters.time.JapaneseEraConverter",
                PRIORITY_NORMAL,
                null,
                null);
        registerConverterDynamically(
                "com.thoughtworks.xstream.converters.time.InstantConverter",
                PRIORITY_NORMAL,
                null,
                null);
        registerConverterDynamically(
                "com.thoughtworks.xstream.converters.time.LocalDateConverter",
                PRIORITY_NORMAL,
                null,
                null);
        registerConverterDynamically(
                "com.thoughtworks.xstream.converters.time.LocalDateTimeConverter",
                PRIORITY_NORMAL,
                null,
                null);
        registerConverterDynamically(
                "com.thoughtworks.xstream.converters.time.LocalTimeConverter",
                PRIORITY_NORMAL,
                null,
                null);
        registerConverterDynamically(
                "com.thoughtworks.xstream.converters.time.MinguoDateConverter",
                PRIORITY_NORMAL,
                null,
                null);
        registerConverterDynamically(
                "com.thoughtworks.xstream.converters.time.MonthDayConverter",
                PRIORITY_NORMAL,
                null,
                null);
        registerConverterDynamically(
                "com.thoughtworks.xstream.converters.time.OffsetDateTimeConverter",
                PRIORITY_NORMAL,
                null,
                null);
        registerConverterDynamically(
                "com.thoughtworks.xstream.converters.time.OffsetTimeConverter",
                PRIORITY_NORMAL,
                null,
                null);
        registerConverterDynamically(
                "com.thoughtworks.xstream.converters.time.PeriodConverter",
                PRIORITY_NORMAL,
                null,
                null);
        registerConverterDynamically(
                "com.thoughtworks.xstream.converters.time.SystemClockConverter",
                PRIORITY_NORMAL,
                new Class[] {Mapper.class},
                new Object[] {mapper});
        registerConverterDynamically(
                "com.thoughtworks.xstream.converters.time.ThaiBuddhistDateConverter",
                PRIORITY_NORMAL,
                null,
                null);
        registerConverterDynamically(
                "com.thoughtworks.xstream.converters.time.ValueRangeConverter",
                PRIORITY_NORMAL,
                new Class[] {Mapper.class},
                new Object[] {mapper});
        registerConverterDynamically(
                "com.thoughtworks.xstream.converters.time.WeekFieldsConverter",
                PRIORITY_NORMAL,
                new Class[] {Mapper.class},
                new Object[] {mapper});
        registerConverterDynamically(
                "com.thoughtworks.xstream.converters.time.YearConverter",
                PRIORITY_NORMAL,
                null,
                null);
        registerConverterDynamically(
                "com.thoughtworks.xstream.converters.time.YearMonthConverter",
                PRIORITY_NORMAL,
                null,
                null);
        registerConverterDynamically(
                "com.thoughtworks.xstream.converters.time.ZonedDateTimeConverter",
                PRIORITY_NORMAL,
                null,
                null);
        registerConverterDynamically(
                "com.thoughtworks.xstream.converters.time.ZoneIdConverter",
                PRIORITY_NORMAL,
                null,
                null);
        registerConverterDynamically(
                "com.thoughtworks.xstream.converters.reflection.LambdaConverter",
                PRIORITY_NORMAL,
                new Class[] {Mapper.class, ReflectionProvider.class, ClassLoaderReference.class},
                new Object[] {mapper, reflectionProvider, classLoaderReference});

        registerConverter(new SelfStreamingInstanceChecker(converterLookup, this), PRIORITY_NORMAL);
    }

    /**
     * 直接复制自XStream的私有方法registerConverterDynamically，希望如果XStream放松访问控制则可以移除
     *
     * @param className 类名
     * @param priority 优先级
     * @param constructorParamTypes 构造函数参数类型
     * @param constructorParamValues 构造函数参数值
     */
    private void registerConverterDynamically(
            String className,
            int priority,
            Class[] constructorParamTypes,
            Object[] constructorParamValues) {
        try {
            Class<?> type =
                    Class.forName(className, false, getClassLoaderReference().getReference());
            Constructor constructor = type.getConstructor(constructorParamTypes);
            Object instance = constructor.newInstance(constructorParamValues);
            if (instance instanceof Converter) {
                registerConverter((Converter) instance, priority);
            } else if (instance instanceof SingleValueConverter) {
                registerConverter((SingleValueConverter) instance, priority);
            }
        } catch (Exception | LinkageError e) {
            throw new com.thoughtworks.xstream.InitializationException(
                    "Could not instantiate converter : " + className, e);
        }
    }
}
