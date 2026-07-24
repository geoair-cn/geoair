package cn.geoair.web.mime;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import java.util.ArrayList;
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

    private static List<IMimeTypeGetter> iMimeGetters;

    /** 获取所有IMimeTypeGetter */
    public static List<IMimeTypeGetter> getMimeTypeGetters() {
        if (iMimeGetters == null) {
            loadMimes();
        }
        return iMimeGetters;
    }

    /** 加载IMimeTypeGetter */
    public static void loadMimes() {
        iMimeGetters = new ArrayList<>();

        List<IMimeTypeGetter> spiIMimeTypeGetters = loadFromSpi();
        for (IMimeTypeGetter handler : spiIMimeTypeGetters) {
            iMimeGetters.add(handler);
            log.trace("通过Java SPI加载IMimeTypeGetter: {}", handler.getClass().getSimpleName());
        }

        log.trace("IMimeTypeGetter加载完成，共加载 {} 个IMimeTypeGetter", iMimeGetters.size());
    }

    /** 从Java SPI加载IMimeTypeGetter */
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
    }
}
