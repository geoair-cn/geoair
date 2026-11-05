package cn.geoair.web.mime;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.geoair.base.sp.GirSpHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ServiceLoader;

/**
 * SPI IMimeTypeGetter加载器
 *
 * @author 张俊
 * @date 2026/7/8
 */
public class SpiMimeLoader {
    public static GiLogger log = GirLoggerFactory.getLogger();

    private static volatile List<IMimeTypeGetter> iMimeGetters;

    /**
     * 获取完整的只读 MIME 解析器快照；初始化后的读取不需要加锁。
     */
    public static List<IMimeTypeGetter> getMimeTypeGetters() {
        List<IMimeTypeGetter> snapshot = iMimeGetters;
        if (snapshot == null) {
            synchronized (SpiMimeLoader.class) {
                snapshot = iMimeGetters;
                if (snapshot == null) {
                    loadMimes();
                    snapshot = iMimeGetters;
                }
            }
        }
        return snapshot;
    }

    /**
     * 串行重载 MIME 解析器；成功后原子替换快照，失败时保留上一次可用结果。
     */
    public static synchronized void loadMimes() {
        // 加载过程中只修改局部列表，读者不会看见半初始化状态，已有快照也不会被原地修改。
        List<IMimeTypeGetter> loaded = new ArrayList<>();
        List<IMimeTypeGetter> spiIMimeTypeGetters = GirSpHelper.loadAll(IMimeTypeGetter.class);
        for (IMimeTypeGetter handler : spiIMimeTypeGetters) {
            loaded.add(handler);
            log.trace("通过Java SPI加载IMimeTypeGetter: {}", handler.getClass().getSimpleName());
        }
        log.trace("IMimeTypeGetter加载完成，共加载 {} 个IMimeTypeGetter", loaded.size());
        // volatile 安全发布完整只读快照；重载期间已经取得旧快照的线程可以继续遍历。
        iMimeGetters = Collections.unmodifiableList(loaded);
    }

    /**
     * 从Java SPI加载IMimeTypeGetter
     */
    private static List<IMimeTypeGetter> loadFromSpi() {
        List<IMimeTypeGetter> result = new ArrayList<>();
        try {
            ServiceLoader<IMimeTypeGetter> serviceLoader =
                    ServiceLoader.load(IMimeTypeGetter.class);
            for (IMimeTypeGetter handler : serviceLoader) {
                result.add(handler);
            }
        } catch (Exception e) {
            log.warn("从Java SPI加载IMimeTypeGetter失败: {}", e.getMessage());
        }
        return result;
    }

    public static void main(String[] args) {
        List<IMimeTypeGetter> proxyIMimeTypeGetters = loadFromSpi();
        for (IMimeTypeGetter handler : proxyIMimeTypeGetters) {
            System.out.println(handler.getClass().getSimpleName());
        }
        System.out.println("=========");
        List<IMimeTypeGetter> iMimeTypeGetters = GirSpHelper.loadAll(IMimeTypeGetter.class);
        for (IMimeTypeGetter handler : iMimeTypeGetters) {
            System.out.println(handler.getClass().getSimpleName());
        }
    }
}
