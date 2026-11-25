package cn.geoair.map.tile.forge.core.bygwc.io.codec;

import cn.geoair.map.tile.forge.core.bygwc.core.mime.MimeType;
import java.awt.image.RenderedImage;
import java.util.List;
import java.util.Map;

/**
 * Interface for each encoder object. Each class implementing this interface can be added to the
 * spring application context as a bean and then will be automatically included in the class {@link
 * ImageEncoderContainer}.
 */
public interface ImageEncoder {

    /** Encodes the selected image */
    public void encode(
            RenderedImage image,
            Object destination,
            boolean aggressiveOutputStreamOptimization,
            MimeType type,
            Map<String, ?> option)
            throws Exception;

    /** Returns the list of the supported mimetypes */
    public List<String> getSupportedMimeTypes();

    /** Indicates if Aggressive outputStream is supported */
    public boolean isAggressiveOutputStreamSupported();
}
