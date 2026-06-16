package cn.geoair.map.tile.forge.core.bygwc.io.codec;



import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;

/**
 * Interface for each decoder object. Each class implementing this interface can be added to the
 * spring application context as a bean and then will be automatically included in the class {@link
 * ImageDecoderContainer}.
 */
public interface ImageDecoder {

    /** Returns the list of the supported mimetypes */
    public List<String> getSupportedMimeTypes();

    /** Decodes the selected input object. */
    public BufferedImage decode(
            Object input, boolean aggressiveInputStreamOptimization, Map<String, Object> map)
            throws Exception;

    /** Indicates if Aggressive inputStream is supported */
    public boolean isAggressiveInputStreamSupported();
}
