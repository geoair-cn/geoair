package cn.geoair.map.dynamic.file.core.transfer;

import cn.geoair.map.dynamic.file.core.read.GeoFileReader;
import cn.geoair.map.dynamic.file.core.write.GeoFileWriter;
import cn.geoair.map.dynamic.file.core.write.config.WriteConfig;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 一次空间数据传输的完整输入。
 * <p>
 * 将旧 {@code GeoFileTran} 分散的 setter、上下文和回调收敛为单个请求对象，
 * 使 {@link GeoTransferEngine} 本身保持无状态、可复用。
 *
 * @author 张逢吉
 */
@Data
@Accessors(chain = true)
public class GeoTransferRequest {

    /** 已初始化的输入读取器。 */
    private GeoFileReader reader;

    /** 已初始化的输出写入器。 */
    private GeoFileWriter writer;

    /** 写入端建表、覆盖与输出 SRID 设置。 */
    private WriteConfig writeConfig = new WriteConfig();

    /** 传输执行选项。 */
    private GeoTransferOptions options = new GeoTransferOptions();

    /** 可选的进度回调。 */
    private GeoTransferProgressListener progressListener;
}
