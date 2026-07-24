package cn.geoair.map.tile.forge.core.bygwc.io.codec;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLogger;
import cn.geoair.map.tile.forge.core.bygwc.io.FileResource;
import cn.geoair.map.tile.forge.core.bygwc.io.Resource;
import it.geosolutions.imageio.stream.input.FileImageInputStreamExtImpl;
import it.geosolutions.imageio.stream.input.ImageInputStreamAdapter;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageReader;
import javax.imageio.spi.IIORegistry;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;

/**
 * Class implementing the ImageDecoder interface, the user should only create a new bean for
 * instantiating a new decoder object.
 */
public class ImageDecoderImpl implements ImageDecoder {
    /** Logger used */
    private static final GiLogger LOGGER = GirLogger.getLoger(ImageEncoderImpl.class);

    /** Default string used for exceptions */
    public static final String OPERATION_NOT_SUPPORTED = "Operation not supported";

    /** Boolean indicating is aggressive inputstream is supported */
    private final boolean isAggressiveInputStreamSupported;
    /** Supported Mimetypes */
    private final List<String> supportedMimeTypes;
    /** ImageReaderSpi object used */
    private ImageReaderSpi spi;

    /**
     * Creates a new Instance of ImageEncoder supporting or not OutputStream optimization, with the
     * defined MimeTypes and Spi classes.
     */
    public ImageDecoderImpl(
            boolean aggressiveInputStreamOptimization,
            List<String> supportedMimeTypes,
            List<String> readerSpi,
            ImageIOInitializer initializer) {

        this.isAggressiveInputStreamSupported = aggressiveInputStreamOptimization;
        this.supportedMimeTypes = new ArrayList<>(supportedMimeTypes);
        // Get the IIORegistry if needed
        IIORegistry theRegistry = initializer.getRegistry();
        // Checks for each Spi class if it is present and then it is added to the list.
        for (String spi : readerSpi) {
            try {
                Class<?> clazz = Class.forName(spi);
                ImageReaderSpi reader =
                        (ImageReaderSpi) theRegistry.getServiceProviderByClass(clazz);
                if (reader != null) {
                    this.spi = reader;
                    break;
                }
            } catch (ClassNotFoundException e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
    }

    /**
     * Decodes the selected image with the defined output object. The user can set the aggressive
     * outputStream if supported.
     *
     * @param source Source object to read
     * @param aggressiveInputStreamOptimization Parameter used if aggressive outputStream
     *     optimization must be used.
     */
    public BufferedImage decode(
            Object source, boolean aggressiveInputStreamOptimization, Map<String, Object> map)
            throws Exception {

        if (!isAggressiveInputStreamSupported() && aggressiveInputStreamOptimization) {
            throw new UnsupportedOperationException(OPERATION_NOT_SUPPORTED);
        }

        // Selection of the first priority writerSpi
        ImageReaderSpi newSpi = getReaderSpi();

        if (newSpi != null) {
            // Creation of the associated Writer
            ImageReader reader = null;
            ImageInputStream stream = null;
            try { // NOPMD (handling of stream is complicated)
                reader = newSpi.createReaderInstance();
                if (source instanceof Resource) {
                    if (source instanceof FileResource) {
                        // file
                        stream = new FileImageInputStreamExtImpl(((FileResource) source).getFile());
                        // Image reading
                        reader.setInput(stream);
                        return reader.read(0);
                    } else {
                        // create a stream and move on
                        source = ((Resource) source).getInputStream();
                    }
                }
                // Check if the input object is an InputStream
                if (source instanceof InputStream) {
                    // Use of the ImageInputStreamAdapter
                    if (isAggressiveInputStreamSupported()) {
                        stream = new ImageInputStreamAdapter((InputStream) source);
                    } else {
                        stream = new MemoryCacheImageInputStream((InputStream) source);
                    }

                    // Image reading
                    reader.setInput(stream);
                    return reader.read(0);
                } else {
                    throw new IllegalArgumentException("Wrong input object");
                }
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
                throw e;
            } finally {
                // reader disposal
                if (reader != null) {
                    reader.dispose();
                }
                // Stream closure
                if (stream != null) {
                    try {
                        stream.close();
                    } catch (IOException e) {
                        LOGGER.error(e.getMessage(), e);
                    }
                    stream = null;
                }
            }
        }

        return null;
    }

    /** Returns the ImageSpiReader associated to */
    ImageReaderSpi getReaderSpi() {
        return spi;
    }

    /**
     * Returns all the supported MimeTypes
     *
     * @return supportedMimeTypes List of all the supported Mime Types
     */
    public List<String> getSupportedMimeTypes() {
        return supportedMimeTypes;
    }

    /**
     * Indicates if optimization on InputStream can be used
     *
     * @return isAggressiveInputStreamSupported Boolean indicating if the selected decoder supports
     *     an aggressive input stream optimization
     */
    public boolean isAggressiveInputStreamSupported() {
        return isAggressiveInputStreamSupported;
    }
}
