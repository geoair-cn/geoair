package cn.geoair.base.log.processor;

import org.junit.Test;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

/**
 * {@link GirLog4jProcessor} 编译期验证测试。
 * <p>
 * 通过 javax.tools 编程式编译带 {@code @GirLog4j} 的临时源码，验证：
 * 字段注入、已有字段跳过、topic 模式、非法标注报错。
 */
public class GirLog4jProcessorTest {

    private static final String DEMO_LOG = "package com.geoair.test;\n"
            + "import cn.geoair.base.log.GirLog4j;\n"
            + "@GirLog4j\n"
            + "public class DemoLog {\n"
            + "    public void say() {\n"
            + "        log.info(\"hello {}\", 1);\n"
            + "        log.error(\"boom\");\n"
            + "    }\n"
            + "}\n";

    private static final String DEMO_SKIP = "package com.geoair.test;\n"
            + "import cn.geoair.base.log.GirLog4j;\n"
            + "import cn.geoair.base.log.GiLogger;\n"
            + "import cn.geoair.base.log.GirLoggerFactory;\n"
            + "@GirLog4j\n"
            + "public class DemoSkip {\n"
            + "    private static GiLogger log = GirLoggerFactory.getLogger(DemoSkip.class);\n"
            + "}\n";

    private static final String DEMO_TOPIC = "package com.geoair.test;\n"
            + "import cn.geoair.base.log.GirLog4j;\n"
            + "@GirLog4j(topic = \"myTopic\", useFinal = true, fieldName = \"logger\")\n"
            + "public class DemoTopic {\n"
            + "    public void say() {\n"
            + "        logger.warn(\"topic log\");\n"
            + "    }\n"
            + "}\n";

    private static final String DEMO_INTERFACE = "package com.geoair.test;\n"
            + "import cn.geoair.base.log.GirLog4j;\n"
            + "@GirLog4j\n"
            + "public interface DemoInterface {\n"
            + "}\n";

    @Test
    public void testInjectLogField() throws Exception {
        File classesDir = compile("com.geoair.test.DemoLog", DEMO_LOG);
        URLClassLoader loader = new URLClassLoader(new URL[] {classesDir.toURI().toURL()},
                GirLog4jProcessorTest.class.getClassLoader());
        Class<?> clazz = Class.forName("com.geoair.test.DemoLog", false, loader);
        Field field = clazz.getDeclaredField("log");
        assertEquals("字段必须为静态", true, Modifier.isStatic(field.getModifiers()));
        assertEquals("字段类型应为 GiLogger", "cn.geoair.base.log.GiLogger", field.getType().getName());
        field.setAccessible(true);
        Object logger = field.get(null);
        assertNotNull("日志实例不应为 null", logger);
        loader.close();
    }

    @Test
    public void testExistingFieldSkipped() throws Exception {
        File classesDir = compile("com.geoair.test.DemoSkip", DEMO_SKIP);
        URLClassLoader loader = new URLClassLoader(new URL[] {classesDir.toURI().toURL()},
                GirLog4jProcessorTest.class.getClassLoader());
        Class<?> clazz = Class.forName("com.geoair.test.DemoSkip", false, loader);
        Field[] fields = clazz.getDeclaredFields();
        int logCount = 0;
        for (Field f : fields) {
            if ("log".equals(f.getName())) {
                logCount++;
            }
        }
        assertEquals("已存在 log 字段时不应重复注入", 1, logCount);
        loader.close();
    }

    @Test
    public void testTopicAndCustomField() throws Exception {
        File classesDir = compile("com.geoair.test.DemoTopic", DEMO_TOPIC);
        URLClassLoader loader = new URLClassLoader(new URL[] {classesDir.toURI().toURL()},
                GirLog4jProcessorTest.class.getClassLoader());
        Class<?> clazz = Class.forName("com.geoair.test.DemoTopic", false, loader);
        Field field = clazz.getDeclaredField("logger");
        assertEquals("自定义字段名生效", "logger", field.getName());
        assertEquals("useFinal=true 时字段应为 final", true, Modifier.isFinal(field.getModifiers()));
        loader.close();
    }

    @Test
    public void testInterfaceRejected() throws Exception {
        CompileResult result = tryCompile("com.geoair.test.DemoInterface", DEMO_INTERFACE);
        assertFalse("接口上标注 @GirLog4j 应编译失败", result.success);
        assertTrue("应输出中文错误提示: " + result.messages,
                result.messages.contains("只能标注在类上"));
    }

    /**
     * 编译结果。
     */
    private static class CompileResult {
        final boolean success;
        final String messages;
        final File outDir;

        CompileResult(boolean success, String messages, File outDir) {
            this.success = success;
            this.messages = messages;
            this.outDir = outDir;
        }
    }

    /**
     * 编程式编译指定源码，断言编译成功并返回输出目录。
     */
    private static File compile(String className, String source) throws Exception {
        CompileResult result = tryCompile(className, source);
        assertTrue("编译失败:\n" + result.messages, result.success);
        return result.outDir;
    }

    private static CompileResult tryCompile(String className, String source) throws Exception {
        File dir = Files.createTempDirectory("girlog4j-test-").toFile();
        File srcFile = new File(dir, className.replace('.', '/') + ".java");
        srcFile.getParentFile().mkdirs();
        Files.write(srcFile.toPath(), source.getBytes(StandardCharsets.UTF_8));

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null,
                StandardCharsets.UTF_8);
        File classesDir = new File(dir, "classes");
        classesDir.mkdirs();
        List<String> options = Arrays.asList(
                "-classpath", System.getProperty("java.class.path"),
                "-d", classesDir.getAbsolutePath());
        boolean success = compiler.getTask(null, fileManager, diagnostics, options, null,
                fileManager.getJavaFileObjects(srcFile)).call();
        StringBuilder sb = new StringBuilder();
        for (Diagnostic<? extends JavaFileObject> d : diagnostics.getDiagnostics()) {
            sb.append(d.getKind()).append(": ").append(d.getMessage(null)).append("\n");
        }
        fileManager.close();
        return new CompileResult(success, sb.toString(), classesDir);
    }
}
