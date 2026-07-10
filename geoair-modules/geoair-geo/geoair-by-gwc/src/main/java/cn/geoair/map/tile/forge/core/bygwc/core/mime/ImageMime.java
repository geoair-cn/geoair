package cn.geoair.map.tile.forge.core.bygwc.core.mime;

import java.awt.RenderingHints;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Iterator;
import javax.imageio.ImageWriter;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.geoair.map.tile.forge.core.bygwc.io.Resource;
import org.eclipse.imagen.ImageLayout;
import org.eclipse.imagen.ImageN;
import org.eclipse.imagen.media.colorindexer.ColorIndexer;
import org.eclipse.imagen.media.colorindexer.Quantizer;

public class ImageMime extends MimeType {

    public static final String NATIVE_PNG_WRITER_CLASS_NAME =
            "com.sun.media.imageioimpl.plugins.png.CLibPNGImageWriter";

    private static GiLogger log = GirLoggerFactory.getLogger(ImageMime.class);

    boolean supportsAlphaChannel;

    boolean supportsAlphaBit;

    public static final ImageMime png = new ImageMime(GirImageMime.png , true, true, true) {

        /** Any response mime starting with image/png will do */
        @Override
        public boolean isCompatible(String otherMimeType) {
            return super.isCompatible(otherMimeType) || otherMimeType.startsWith("image/png");
        }
    };

    public static final ImageMime jpeg =
            new ImageMime(GirImageMime.jpeg, true, false, false) {

        /** Shave off the alpha band, JPEG cannot write it out */
        @Override
        public RenderedImage preprocess(RenderedImage ri) {
            if (ri.getColorModel().hasAlpha()) {
                final int numBands = ri.getSampleModel().getNumBands();
                // handle both gray-alpha and RGBA (same code as in GeoTools ImageWorker)
                final int[] bands = new int[numBands - 1];
                for (int i = 0; i < bands.length; i++) {
                    bands[i] = i;
                }
                // ParameterBlock creation
                ParameterBlock pb = new ParameterBlock();
                pb.setSource(ri, 0);
                pb.set(bands, 0);
                final RenderingHints hints = new RenderingHints(ImageN.KEY_IMAGE_LAYOUT, new ImageLayout(ri));
                ri = ImageN.create("BandSelect", pb, hints);
            }
            return ri;
        }
    };

     public static final ImageMime gif =
            new ImageMime(GirImageMime.gif, true, false, true);

    public static final ImageMime tiff =
            new ImageMime(GirImageMime.tiff, true, true, true);

    public static final ImageMime png8 =
            new ImageMime(GirImageMime.png8, true, false, true) {

        /** Quantize if the source did not do so already */
        @Override
        public RenderedImage preprocess(RenderedImage canvas) {
            if (!(canvas.getColorModel() instanceof IndexColorModel)) {
                if (canvas.getColorModel() instanceof ComponentColorModel
                        && canvas.getSampleModel().getDataType() == DataBuffer.TYPE_BYTE) {
                    ColorIndexer indexer = new Quantizer(256).subsample().buildColorIndexer(canvas);
                    if (indexer != null) {
                        ParameterBlock pb = new ParameterBlock();
                        pb.setSource(canvas, 0); // The source image.
                        pb.set(indexer, 0);
                        canvas = ImageN.create(
                                "ColorIndexer", pb, ImageN.getDefaultInstance().getRenderingHints());
                    }
                }
            }
            return canvas;
        }
    };

    public static final ImageMime png24 =
            new ImageMime(GirImageMime.png24, true, true, true);
    public static final ImageMime png_24 =
            new ImageMime(GirImageMime.png_24, true, true, true);


    public static final ImageMime dds =
            new ImageMime(GirImageMime.dds, false, false, false);

    public static final ImageMime jpegPng =
            new JpegPngMime(
                    GirImageMime.jpegPng, jpeg, png);

    public static final ImageMime jpegPng8 =
            new JpegPngMime(
                    GirImageMime.jpegPng8,
                    jpeg,
                    png8);


    private ImageMime(
            GirImageMime girImageMime,
            boolean tiled,
            boolean alphaChannel,
            boolean alphaBit) {
        super(girImageMime.getMimeType(), girImageMime.getFileExtension(), girImageMime.getInternalName(), girImageMime.getFormat(), tiled);
        this.supportsAlphaChannel = alphaChannel;
        this.supportsAlphaBit = alphaBit;
    }

    protected static ImageMime checkForFormat(String formatStr) throws MimeException {
        if (!formatStr.startsWith("image/")) {
            return null;
        }

        // TODO Making a special exception, generalize later
        if (!formatStr.equals("image/png; mode=24bit") && formatStr.contains(";")) {
            if (log.isFatalEnabled( )) {
                log.fatal("Slicing off " + formatStr.split(";")[1]);
            }
            formatStr = formatStr.split(";")[0];
        }

        final String tmpStr = formatStr.substring(6);
        if (tmpStr.equalsIgnoreCase("png")) {
            return png;
        } else if (tmpStr.equalsIgnoreCase("jpeg")) {
            return jpeg;
        } else if (tmpStr.equalsIgnoreCase("gif")) {
            return gif;
        } else if (tmpStr.equalsIgnoreCase("tiff")) {
            return tiff;
        } else if (tmpStr.equalsIgnoreCase("png8")) {
            return png8;
        } else if (tmpStr.equalsIgnoreCase("png24")) {
            return png24;
        } else if (tmpStr.equalsIgnoreCase("png; mode=24bit")) {
            return png_24;
        } else if (tmpStr.equalsIgnoreCase("png;%20mode=24bit")) {
            return png_24;
        } else if (tmpStr.equalsIgnoreCase("vnd.jpeg-png")) {
            return jpegPng;
        } else if (tmpStr.equalsIgnoreCase("vnd.jpeg-png8")) {
            return jpegPng8;
        }
        return null;
    }

    protected static ImageMime checkForExtension(String fileExtension) throws MimeException {
        if (fileExtension.equalsIgnoreCase("png")) {
            return png;
        } else if (fileExtension.equalsIgnoreCase("jpeg") || fileExtension.equalsIgnoreCase("jpg")) {
            return jpeg;
        } else if (fileExtension.equalsIgnoreCase("gif")) {
            return gif;
        } else if (fileExtension.equalsIgnoreCase("tiff")) {
            return tiff;
        } else if (fileExtension.equalsIgnoreCase("png8")) {
            return png8;
        } else if (fileExtension.equalsIgnoreCase("png24")) {
            return png24;
        } else if (fileExtension.equalsIgnoreCase("png_24")) {
            return png_24;
        } else if (fileExtension.equalsIgnoreCase("jpeg-png")) {
            return jpegPng;
        } else if (fileExtension.equalsIgnoreCase("jpeg-png8")) {
            return jpegPng8;
        }
        return null;
    }

    public boolean supportsAlphaBit() {
        return supportsAlphaBit;
    }

    public boolean supportsAlphaChannel() {
        return supportsAlphaChannel;
    }


    public ImageWriter getImageWriter(RenderedImage image) {
        Iterator<ImageWriter> it = javax.imageio.ImageIO.getImageWritersByFormatName(internalName);
        ImageWriter writer = it.next();

        // Native PNG Writer can't handle 2-4 bit PNG, so if our sample depth isn't 1/8 and the
        // returned writer is the native version, let's skip it and move on to the next
        // which will presumably be the pure Java version. A bit hacky, but it's roughly what
        // GeoServer does to make sure it doesn't encode incompatible PNGs with the native writer
        if (this.internalName.equals(ImageMime.png.internalName)
                || this.internalName.equals(ImageMime.png8.internalName)) {

            int bitDepth = image.getSampleModel().getSampleSize(0);
            if (bitDepth > 1 && bitDepth < 8 && writer.getClass().getName().equals(NATIVE_PNG_WRITER_CLASS_NAME)) {

                writer = it.next();
            }
        }
        return writer;
    }

    /** Preprocesses the image to optimize it for the write about to happen */
    public RenderedImage preprocess(RenderedImage tile) {
        return tile;
    }

    private static class JpegPngMime extends ImageMime {

        private static final int JPEG_MAGIC_MASK = 0xffd80000;
        private final ImageMime jpegDelegate;
        private final ImageMime pngDelegate;

        public JpegPngMime(
                GirImageMime girImageMime,
                ImageMime jpegDelegate,
                ImageMime pngDelegate) {
            super(girImageMime, true, true, true);
            this.jpegDelegate = jpegDelegate;
            this.pngDelegate = pngDelegate;
        }

        /**
         * Returns true if the best format to encode the image is jpeg (the image is rgb, or rgba
         * without any actual transparency use). This code is duplicated in GeoServer
         * JpegPngRenderedImageMapOutputFormat. Unfortunately gwc-core does not depend on GeoTools,
         * so we don't have an easy place to share it. On the bright side, it's small.
         */
        boolean isBestFormatJpeg(RenderedImage renderedImage) {
            int numBands = renderedImage.getSampleModel().getNumBands();
            if (numBands == 4 || numBands == 2) {
                ImageWorker iw = new ImageWorker(renderedImage);
                iw.setRenderingHints(ImageN.getDefaultInstance().getRenderingHints());
                double[] mins = iw.getMinimums();

                return mins[mins.length - 1] == 255; // fully opaque
            } else if (renderedImage.getColorModel() instanceof IndexColorModel) {
                // JPEG would still compress a bit better, but in order to figure out
                // if the image has transparency we'd have to expand to RGB or roll
                // a new ImageN image op that looks for the transparent pixels. Out of scope
                // for the moment
                return false;
            } else {
                // otherwise support RGB or gray
                return (numBands == 3) || (numBands == 1);
            }
        }

        @Override
        public ImageWriter getImageWriter(RenderedImage image) {
            if (isBestFormatJpeg(image)) {
                return jpegDelegate.getImageWriter(image);
            } else {
                return pngDelegate.getImageWriter(image);
            }
        }

        @Override
        public String getMimeType( Resource resource) throws IOException {
            try (DataInputStream dis = new DataInputStream(resource.getInputStream())) {
                final int head = dis.readInt();
                if ((head & 0xFFFF0000) == JPEG_MAGIC_MASK) {
                    return jpegDelegate.getMimeType();
                } else {
                    return pngDelegate.getMimeType();
                }
            }
        }

        @Override
        public boolean isCompatible(String otherMimeType) {
            return jpegDelegate.isCompatible(otherMimeType) || pngDelegate.isCompatible(otherMimeType);
        }

        @Override
        public RenderedImage preprocess(RenderedImage tile) {
            if (isBestFormatJpeg(tile)) {
                return jpegDelegate.preprocess(tile);
            } else {
                return pngDelegate.preprocess(tile);
            }
        }
    }
}
