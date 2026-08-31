package cn.geoair.map.tile.forge.core.bygwc.io;

import cn.geoair.map.tile.forge.core.bygwc.io.codec.ImageDecoder;

import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class used for containing all the ImageDecoder implementations in a map. The user should only
 * call the decode() method and internally it uses the reader associated to the input mimetype.
 */
public class GirImageDecoderContainer {
    /** Collection of all the ImageDecoder interface implementation */
    private Collection<ImageDecoder> decoders;

    /** Map of all the decoders for mimetype */
    private Map<String, ImageDecoder> mapDecoders;

    public GirImageDecoderContainer(List<ImageDecoder> decoders) {
        init(decoders);
    }

    public BufferedImage decode(
            String mimeType,
            Object input,
            boolean aggressiveInputStreamOptimization,
            Map<String, Object> map)
            throws Exception {
        if (mapDecoders == null) {
            throw new IllegalArgumentException("ApplicationContext must be set before decoding");
        }
        return mapDecoders.get(mimeType).decode(input, aggressiveInputStreamOptimization, map);
    }

    public boolean isAggressiveInputStreamSupported(String mimeType) {
        if (mapDecoders == null) {
            throw new IllegalArgumentException(
                    "ApplicationContext must be set before checking the AggressiveInputStrean support");
        }
        return mapDecoders.get(mimeType).isAggressiveInputStreamSupported();
    }

    public void init(List<ImageDecoder> decoders) {
        this.decoders = decoders;
        mapDecoders = new HashMap<>();
        for (ImageDecoder encoder : decoders) {
            List<String> supportedMimeTypes = encoder.getSupportedMimeTypes();
            for (String mimeType : supportedMimeTypes) {
                if (!mapDecoders.containsKey(mimeType)) {
                    mapDecoders.put(mimeType, encoder);
                }
            }
        }
    }
}
