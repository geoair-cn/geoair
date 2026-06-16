
package cn.geoair.map.tile.forge.core.bygwc.io;


import cn.geoair.map.tile.forge.core.bygwc.core.mime.MimeType;
import cn.geoair.map.tile.forge.core.bygwc.io.codec.ImageEncoder;


import java.awt.image.RenderedImage;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class used for containing all the ImageEncoder implementations in a map. The user should only
 * call the encode() method and internally it uses the writer associated to the input mimetype.
 */
public class GtcImageEncoderContainer {
    /**
     * Collection of all the ImageEncoder interface implementation
     */
    private Collection<ImageEncoder> encoders;
    /**
     * Map of all the encoders for mimetype
     */
    private Map<String, ImageEncoder> mapEncoders;

    public GtcImageEncoderContainer(List<ImageEncoder> encoders) {
        init(encoders);
    }

    public void encode(
            RenderedImage image,
            MimeType mimeType,
            Object destination,
            boolean aggressiveOutputStreamOptimization,
            Map<String, Object> map)
            throws Exception {
        if (mapEncoders == null) {
            throw new IllegalArgumentException("ApplicationContext must be set before encoding");
        }
        mapEncoders
                .get(mimeType.getMimeType())
                .encode(image, destination, aggressiveOutputStreamOptimization, mimeType, map);
    }

    public boolean isAggressiveOutputStreamSupported(String mimeType) {
        if (mapEncoders == null) {
            throw new IllegalArgumentException(
                    "ApplicationContext must be set before checking the AggressiveOutputStrean support");
        }
        return mapEncoders.get(mimeType).isAggressiveOutputStreamSupported();
    }


    public void init(List<ImageEncoder> encoders) {
        this.encoders = encoders;
        mapEncoders = new HashMap<>();
        for (ImageEncoder encoder : encoders) {
            List<String> supportedMimeTypes = encoder.getSupportedMimeTypes();
            for (String mimeType : supportedMimeTypes) {
                if (!mapEncoders.containsKey(mimeType)) {
                    mapEncoders.put(mimeType, encoder);
                }
            }
        }
    }
}
