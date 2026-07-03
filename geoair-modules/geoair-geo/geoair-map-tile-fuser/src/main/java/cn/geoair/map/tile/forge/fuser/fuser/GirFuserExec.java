package cn.geoair.map.tile.forge.fuser.fuser;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.geoair.map.dynamic.tools.grid.dto.RangeApo;
import cn.geoair.map.tile.forge.core.bygwc.core.mime.ImageMime;
import cn.geoair.map.tile.forge.core.bygwc.grid.BoundingBox;
import cn.geoair.map.tile.forge.core.bygwc.grid.GridSubset;
import cn.geoair.map.tile.forge.fuser.enums.HintsLevel;
import cn.geoair.map.tile.forge.core.bygwc.io.GirImageDecoderContainer;
import cn.geoair.map.tile.forge.core.bygwc.io.GirImageEncoderContainer;
import cn.geoair.map.tile.forge.core.bygwc.io.ImageCodecInitializer;
import cn.geoair.map.tile.forge.core.bygwc.io.Resource;
import cn.geoair.map.tile.forge.fuser.provider.LayerTileGetter;
import cn.hutool.core.date.StopWatch;
import lombok.Getter;
import org.apache.commons.io.IOUtils;
import org.eclipse.imagen.PlanarImage;
import org.geotools.image.util.ImageUtilities;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * 瓦片融合器
 * 将多个瓦片拼接成一张完整的大图
 */

public class GirFuserExec implements FuserExec {
    private static GiLogger log = GirLoggerFactory.getLogger( );
    final GridSubset gridSubset;
    @Getter
    final ImageMime outputFormat;

    final LayerTileGetter layerTileGetter;
    @Getter
    ImageMime srcFormat;

    int reqHeight;

    int reqWidth;

    // 请求的范围边界
    final BoundingBox reqBounds;

    // 用于调整最终栅格
    double xResolution;

    double yResolution;

    // 源分辨率（遵循GIS而非图形惯例，表示为像素的物理大小而非密度）
    double srcResolution;

    int srcIdx;

    // 使用的瓦片在瓦片坐标中的区域
    long[] srcRectangle;

    // 用于满足请求的瓦片空间范围
    BoundingBox srcBounds;

    /**
     * 画布尺寸
     */
    int[] canvasSize = new int[2];

    /**
     * 全局日志开关（控制info和debug级别日志）
     */
    private static boolean globalLogEnabled = false;

    /**
     * 设置全局日志开关
     *
     * @param enabled true表示开启日志输出，false表示关闭所有日志
     */
    public static void setGlobalLogEnabled(boolean enabled) {
        globalLogEnabled = enabled;
        if (enabled) {
            log.info("瓦片融合器日志开关已开启");
        }
    }

    /**
     * 获取全局日志开关状态
     */
    public static boolean isGlobalLogEnabled() {
        return globalLogEnabled;
    }

    /**
     * 条件info日志输出
     */
    private void infoLog(String message) {
        if (globalLogEnabled) {
            log.info(message);
        }
    }

    /**
     * 条件info日志输出（带参数）
     */
    private void infoLog(String format, Object... arguments) {
        if (globalLogEnabled) {
            log.info(format, arguments);
        }
    }

    /**
     * 条件debug日志输出
     */
    private void debugLog(String message) {
        if (globalLogEnabled && log.isDebugEnabled()) {
            log.debug(message);
        }
    }

    /**
     * 条件debug日志输出（带参数）
     */
    private void debugLog(String format, Object... arguments) {
        if (globalLogEnabled && log.isDebugEnabled()) {
            log.debug(format, arguments);
        }
    }

    /**
     * 条件warn日志输出
     */
    private void warnLog(String message) {
        if (globalLogEnabled) {
            log.warn(message);
        }
    }

    /**
     * 条件warn日志输出（带参数）
     */
    private void warnLog(String format, Object... arguments) {
        if (globalLogEnabled) {
            log.warn(format, arguments);
        }
    }

    /**
     * 条件error日志输出
     */
    private void errorLog(String message) {
        if (globalLogEnabled) {
            log.error(message);
        }
    }

    /**
     * 条件error日志输出（带参数）
     */
    private void errorLog(String format, Object... arguments) {
        if (globalLogEnabled) {
            log.error(format, arguments);
        }
    }

    /**
     * 条件error日志输出（带异常）
     */
    private void errorLog(String message, Throwable t) {
        if (globalLogEnabled) {
            log.error(message, t);
        }
    }

    static class SpatialOffsets {
        double top;
        double bottom;
        double left;
        double right;
    }

    static class PixelOffsets {
        int top;
        int bottom;
        int left;
        int right;
    }

    /**
     * 缩放前的像素偏移值
     */
    PixelOffsets canvOfs = new PixelOffsets();

    SpatialOffsets boundOfs = new SpatialOffsets();

    /**
     * 马赛克图像
     */
    BufferedImage canvas;

    /**
     * 用于将瓦片绘制到马赛克中的图形对象
     */
    Graphics2D gfx;

    /**
     * 所有可用的解码器映射
     */
    private GirImageDecoderContainer decoderMap;

    /**
     * 所有可用的编码器映射
     */
    private GirImageEncoderContainer encoderMap;

    /**
     * 构造函数 - 使用默认输出格式(PNG)
     *
     * @param layerTileGetter 瓦片获取器
     * @param gridSubset      网格子集
     * @param bounds          请求范围
     * @param width           请求宽度
     * @param height          请求高度
     */
    public GirFuserExec(
            LayerTileGetter layerTileGetter, GridSubset gridSubset, BoundingBox bounds, int width, int height) {
        infoLog("初始化瓦片融合器 - 使用指定GridSubset, 范围: {}, 尺寸: {}x{}", bounds, width, height);
        init();
        this.outputFormat = ImageMime.png;
        this.layerTileGetter = layerTileGetter;
        this.gridSubset = gridSubset;
        this.reqBounds = bounds;
        this.reqWidth = width;
        this.reqHeight = height;
        this.srcFormat = layerTileGetter.getSrcFormat();
        infoLog("瓦片融合器初始化完成 - 源格式: {}, 输出格式: png", srcFormat.getMimeType());
    }

    /**
     * 构造函数 - 从LayerTileGetter中获取GridSubset，使用默认输出格式(PNG)
     *
     * @param layerTileGetter 瓦片获取器
     * @param bounds          请求范围
     * @param width           请求宽度
     * @param height          请求高度
     */
    public GirFuserExec(
            LayerTileGetter layerTileGetter, BoundingBox bounds, int width, int height) {
        infoLog("初始化瓦片融合器 - 从LayerTileGetter获取GridSubset, 范围: {}, 尺寸: {}x{}", bounds, width, height);
        init();
        this.outputFormat = ImageMime.png;
        this.layerTileGetter = layerTileGetter;
        this.gridSubset = layerTileGetter.getSrcGridSubset();
        this.reqBounds = bounds;
        this.reqWidth = width;
        this.reqHeight = height;
        this.srcFormat = layerTileGetter.getSrcFormat();
        infoLog("瓦片融合器初始化完成 - 源格式: {}, 输出格式: png, GridSubset分辨率数: {}",
                srcFormat.getMimeType(), gridSubset.getResolutions().length);
    }

    /**
     * 构造函数 - 从LayerTileGetter中获取GridSubset，指定输出格式
     *
     * @param layerTileGetter 瓦片获取器
     * @param outputFormat    输出格式
     * @param bounds          请求范围
     * @param width           请求宽度
     * @param height          请求高度
     */
    public GirFuserExec(
            LayerTileGetter layerTileGetter, ImageMime outputFormat, BoundingBox bounds, int width, int height) {
        infoLog("初始化瓦片融合器 - 从LayerTileGetter获取GridSubset, 输出格式: {}, 范围: {}, 尺寸: {}x{}",
                outputFormat.getMimeType(), bounds, width, height);
        if (layerTileGetter == null) {
            errorLog("LayerTileGetter不能为null");
            throw new IllegalArgumentException("LayerTileGetter不能为null");
        }
        if (bounds == null) {
            errorLog("请求范围不能为null");
            throw new IllegalArgumentException("请求范围不能为null");
        }
        if (width <= 0 || height <= 0) {
            errorLog("请求尺寸无效: {}x{}", width, height);
            throw new IllegalArgumentException("请求尺寸必须大于0");
        }
        init();
        this.outputFormat = outputFormat;
        this.layerTileGetter = layerTileGetter;
        this.gridSubset = layerTileGetter.getSrcGridSubset();
        if (this.gridSubset == null) {
            errorLog("从LayerTileGetter获取的GridSubset为null");
            throw new IllegalStateException("GridSubset不能为null");
        }
        this.reqBounds = bounds;
        this.reqWidth = width;
        this.reqHeight = height;
        this.srcFormat = layerTileGetter.getSrcFormat();
        infoLog("瓦片融合器初始化完成 - 源格式: {}, 输出格式: {}, GridSubset瓦片尺寸: {}x{}, 分辨率数: {}",
                srcFormat.getMimeType(), outputFormat.getMimeType(),
                gridSubset.getTileWidth(), gridSubset.getTileHeight(),
                gridSubset.getResolutions().length);
    }

    /**
     * 确定源分辨率
     */
    protected void determineSourceResolution() {
        infoLog("开始确定源分辨率");
        xResolution = reqBounds.getWidth() / reqWidth;
        yResolution = reqBounds.getHeight() / reqHeight;
        debugLog("计算分辨率 - X方向: {} 米/像素, Y方向: {} 米/像素", xResolution, yResolution);

        double tmpResolution;
        // 使用较小的分辨率
        if (yResolution < xResolution) {
            tmpResolution = yResolution;
            debugLog("使用Y方向分辨率作为基准: {}", tmpResolution);
        } else {
            tmpResolution = xResolution;
            debugLog("使用X方向分辨率作为基准: {}", tmpResolution);
        }

        // 预留0.5%的余量
        double compResolution = 1.005 * tmpResolution;
        debugLog("比较分辨率(含余量): {}", compResolution);

        double[] resArray = gridSubset.getResolutions();
        debugLog("网格子集分辨率数组长度: {}", resArray.length);

        for (srcIdx = 0; srcIdx < resArray.length; srcIdx++) {
            srcResolution = resArray[srcIdx];
            if (srcResolution < compResolution) {
                debugLog("找到合适的分辨率等级 - 索引: {}, 分辨率: {}", srcIdx, srcResolution);
                break;
            }
        }

        if (srcIdx >= resArray.length) {
            srcIdx = resArray.length - 1;
            srcResolution = resArray[srcIdx];
            infoLog("未找到足够精细的分辨率，使用最精细等级 - 索引: {}, 分辨率: {}", srcIdx, srcResolution);
        }

        infoLog("源分辨率确定完成 - 等级索引: {}, 分辨率: {} 米/像素 (目标基准: {} 米/像素)",
                srcIdx, srcResolution, tmpResolution);
    }

    /**
     * 确定画布布局
     */
    protected void determineCanvasLayout() {
        infoLog("开始确定画布布局");
        // 找出覆盖所需范围所需的图块的空间范围
        srcRectangle = gridSubset.getCoverageIntersection(srcIdx, reqBounds);
        debugLog("瓦片覆盖范围矩形: {}", Arrays.toString(srcRectangle));

        srcBounds = gridSubset.boundsFromRectangle(srcRectangle);
        debugLog("瓦片范围边界: {}", srcBounds);
        debugLog("请求范围边界: {}", reqBounds);

        // 计算偏移量（正数表示第一个图块有空白空间，负数表示不会使用整个图块）
        boundOfs.left = srcBounds.getMinX() - reqBounds.getMinX();
        boundOfs.bottom = srcBounds.getMinY() - reqBounds.getMinY();
        boundOfs.right = reqBounds.getMaxX() - srcBounds.getMaxX();
        boundOfs.top = reqBounds.getMaxY() - srcBounds.getMaxY();

        infoLog("地理偏移量(米): left={}, right={}, top={}, bottom={}",
                boundOfs.left, boundOfs.right, boundOfs.top, boundOfs.bottom);

        canvasSize[0] = (int) Math.round(reqBounds.getWidth() / this.srcResolution);
        canvasSize[1] = (int) Math.round(reqBounds.getHeight() / this.srcResolution);
        debugLog("计算画布尺寸(像素): {}x{} (基于源分辨率)", canvasSize[0], canvasSize[1]);

        PixelOffsets naiveOfs = new PixelOffsets();
        // 计算相应的像素偏移量
        naiveOfs.left = (int) Math.round(boundOfs.left / this.srcResolution);
        naiveOfs.bottom = (int) Math.round(boundOfs.bottom / this.srcResolution);
        naiveOfs.right = (int) Math.round(boundOfs.right / this.srcResolution);
        naiveOfs.top = (int) Math.round(boundOfs.top / this.srcResolution);
        debugLog("初步像素偏移量(像素): left={}, bottom={}, right={}, top={}",
                naiveOfs.left, naiveOfs.bottom, naiveOfs.right, naiveOfs.top);

        // 找到相对侧的偏移量
        int tileWidth = this.gridSubset.getTileWidth();
        int tileHeight = this.gridSubset.getTileHeight();
        debugLog("瓦片尺寸: {}x{} 像素", tileWidth, tileHeight);

        canvOfs.left = naiveOfs.left;
        canvOfs.bottom = naiveOfs.bottom;

        canvOfs.right = (canvasSize[0] - canvOfs.left) % tileWidth;
        canvOfs.right = (Integer.signum(naiveOfs.right) * tileWidth + canvOfs.right) % tileWidth;
        canvOfs.right = canvOfs.right - (naiveOfs.right % tileWidth) + naiveOfs.right;

        canvOfs.top = (canvasSize[1] - canvOfs.bottom) % tileHeight;
        canvOfs.top = (Integer.signum(naiveOfs.top) * tileHeight + canvOfs.top) % tileHeight;
        canvOfs.top = canvOfs.top - (naiveOfs.top % tileHeight) + naiveOfs.top;

        // 后置条件验证
        assert Math.abs(canvOfs.left - naiveOfs.left) <= 1;
        assert Math.abs(canvOfs.bottom - naiveOfs.bottom) <= 1;
        assert Math.abs(canvOfs.right - naiveOfs.right) <= 1;
        assert Math.abs(canvOfs.top - naiveOfs.top) <= 1;

        infoLog("画布布局确定完成 - 画布尺寸: {}x{}, 最终像素偏移: left={}, bottom={}, right={}, top={}",
                canvasSize[0], canvasSize[1], canvOfs.left, canvOfs.bottom, canvOfs.right, canvOfs.top);

        if (globalLogEnabled && log.isDebugEnabled()) {
            log.debug("瓦片覆盖矩形: " + Arrays.toString(srcRectangle));
            log.debug("瓦片覆盖边界: " + srcBounds + " (请求边界: " + reqBounds + ")");
            log.debug("边界偏移量: [{}, {}, {}, {}]", boundOfs.left, boundOfs.bottom, boundOfs.right, boundOfs.top);
            log.debug("画布尺寸: {}x{} (请求尺寸: {}x{})", canvasSize[0], canvasSize[1], reqWidth, reqHeight);
            log.debug("画布偏移量: [{}, {}, {}, {}]", canvOfs.left, canvOfs.bottom, canvOfs.right, canvOfs.top);
        }
    }

    /**
     * 创建画布
     */
    protected void createCanvas() {
        infoLog("开始创建画布");

        Color bgColor = null;
        int canvasType;
        if (outputFormat.supportsAlphaBit() || outputFormat.supportsAlphaChannel()) {
            canvasType = BufferedImage.TYPE_INT_ARGB;
            infoLog("画布类型: ARGB (支持透明通道)");
        } else {
            canvasType = BufferedImage.TYPE_INT_RGB;
            bgColor = Color.WHITE;
            infoLog("画布类型: RGB (不支持透明通道, 背景色: 白色)");
        }

        // 创建画布和图形对象
        canvas = new BufferedImage(canvasSize[0], canvasSize[1], canvasType);
        gfx = (Graphics2D) canvas.getGraphics();
        debugLog("画布创建完成 - 尺寸: {}x{}, 类型: {}", canvasSize[0], canvasSize[1], canvasType);

        if (bgColor != null) {
            gfx.setColor(bgColor);
            gfx.fillRect(0, 0, canvasSize[0], canvasSize[1]);
            debugLog("已填充背景色: {}", bgColor);
        }

        // 渲染提示设置
        RenderingHints hintsTemp = HintsLevel.QUALITY.getRenderingHints();
        gfx.addRenderingHints(hintsTemp);
        debugLog("渲染提示已应用: {}", HintsLevel.QUALITY.getModeName());

        infoLog("画布创建完成");
    }

    /**
     * 渲染画布 - 将所有瓦片绘制到画布上
     */
    protected void renderCanvas() throws Exception {
        infoLog("开始渲染画布");

        // 遍历所有相关瓦片并将其写入画布，从底部开始，向右和向上移动
        long starty = srcRectangle[1];
        long totalTiles = (srcRectangle[2] - srcRectangle[0] + 1) * (srcRectangle[3] - srcRectangle[1] + 1);
        long processedTiles = 0;
        infoLog("需要处理的瓦片总数: {}", totalTiles);
        StopWatch stopWatch = new StopWatch();
        stopWatch.start("获取瓦片");
        Map<String, Resource> resourceMap = new ConcurrentHashMap<>();
        IntStream.rangeClosed((int) starty, (int) srcRectangle[3])
                .parallel()
                .forEach(gridy -> {
                    IntStream.rangeClosed((int) srcRectangle[0], (int) srcRectangle[2])
                            .parallel()
                            .forEach(gridx -> {
                                String key = srcIdx + "_" + gridx + "_" + gridy;
                                Resource blob = layerTileGetter.getTileResource(srcIdx, gridx, gridy);    // tms 原点，找原始
                                if (blob != null) {
                                    resourceMap.put(key, blob);
                                }
                            });
                });
        stopWatch.stop();
        stopWatch.start("渲染瓦片renderCanvas");
        // gridy 是瓦片行索引
        for (long gridy = starty; gridy <= srcRectangle[3]; gridy++) {
            int tiley = 0;
            int canvasy = (int) (srcRectangle[3] - gridy) * gridSubset.getTileHeight();
            int tileHeight = gridSubset.getTileHeight();
            debugLog("处理行 gridy={}, 起始Y坐标={}", gridy, canvasy);

            if (canvOfs.top > 0) {
                canvasy += canvOfs.top;
                debugLog("添加顶部填充: {}", canvOfs.top);
            } else {
                if (gridy == srcRectangle[3]) {
                    tileHeight = tileHeight + canvOfs.top;
                    tiley = -canvOfs.top;
                    debugLog("顶部瓦片被裁剪 - 新高度: {}, 起始Y偏移: {}", tileHeight, tiley);
                } else {
                    canvasy += canvOfs.top;
                    debugLog("调整Y坐标: {}", canvasy);
                }
            }

            if (gridy == srcRectangle[1] && canvOfs.bottom < 0) {
                tileHeight += canvOfs.bottom;
                debugLog("底部瓦片被裁剪 - 新高度: {}", tileHeight);
            }

            long startx = srcRectangle[0];
            for (long gridx = startx; gridx <= srcRectangle[2]; gridx++) {
                long[] gridLoc = {gridx, gridy, srcIdx};
                processedTiles++;

                debugLog("处理瓦片 [{}, {}], 等级索引={}, 进度: {}/{}",
                        gridx, gridy, srcIdx, processedTiles, totalTiles);

                // 获取瓦片资源
//                Resource blob = layerTileGetter.getTileResource(srcIdx, (int) gridx, (int) gridy);
                String key = srcIdx + "_" + gridx + "_" + gridy;
                Resource blob = resourceMap.get(key);
                if (blob == null) {
                    warnLog("瓦片资源为空 - gridx={}, gridy={}, level={}", gridx, gridy, srcIdx);
                    continue;
                }
                resourceMap.remove(key);
                debugLog("成功获取瓦片资源 - gridx={}, gridy={}", gridx, gridy);
                String formatName = srcFormat.getMimeType();
                BufferedImage tileImg = decoderMap.decode(
                        formatName, blob,
                        decoderMap.isAggressiveInputStreamSupported(formatName),
                        null);

                if (tileImg == null) {
                    errorLog("瓦片解码失败 - formatName={}, gridx={}, gridy={}", formatName, gridx, gridy);
                    continue;
                }
                debugLog("瓦片解码成功 - 尺寸: {}x{}, 格式: {}", tileImg.getWidth(), tileImg.getHeight(), formatName);

                int tilex = 0;
                int canvasx = (int) (gridx - startx) * gridSubset.getTileWidth();
                int tileWidth = gridSubset.getTileWidth();

                if (canvOfs.left > 0) {
                    canvasx += canvOfs.left;
                    debugLog("添加左侧填充: {}", canvOfs.left);
                } else {
                    if (gridx == srcRectangle[0]) {
                        tileWidth = tileWidth + canvOfs.left;
                        tilex = -canvOfs.left;
                        debugLog("左侧瓦片被裁剪 - 新宽度: {}, 起始X偏移: {}", tileWidth, tilex);
                    } else {
                        canvasx += canvOfs.left;
                        debugLog("调整X坐标: {}", canvasx);
                    }
                }

                if (gridx == srcRectangle[2] && canvOfs.right < 0) {
                    tileWidth = tileWidth + canvOfs.right;
                    debugLog("右侧瓦片被裁剪 - 新宽度: {}", tileWidth);
                }

                if (tileWidth == 0 || tileHeight == 0) {
                    warnLog("瓦片尺寸无效 - tileWidth: {}, tileHeight: {}, 跳过", tileWidth, tileHeight);
                    continue;
                }

                // 裁剪瓦片到需要的部分
                if (tileWidth != gridSubset.getTileWidth() || tileHeight != gridSubset.getTileHeight()) {
                    debugLog("裁剪瓦片 - getSubimage({}, {}, {}, {})", tilex, tiley, tileWidth, tileHeight);
                    tileImg = tileImg.getSubimage(tilex, tiley, tileWidth, tileHeight);
                }

                // 将瓦片渲染到大画布上
                debugLog("绘制瓦片 - 画布位置: ({}, {}), 瓦片位置: [{}, {}, {}]",
                        canvasx, canvasy, gridx, gridy, srcIdx);
                gfx.drawImage(tileImg, canvasx, canvasy, null);
            }
        }
        stopWatch.stop();
        gfx.dispose();
        infoLog("画布渲染完成 - 共处理瓦片: {}/{},总耗时: {}", processedTiles, totalTiles, stopWatch.prettyPrint(TimeUnit.SECONDS));
        ;
    }

    /**
     * 缩放栅格图像到请求的尺寸
     */
    protected void scaleRaster() {
        if (canvasSize[0] != reqWidth || canvasSize[1] != reqHeight) {
            infoLog("开始缩放栅格 - 从 {}x{} 缩放到 {}x{}", canvasSize[0], canvasSize[1], reqWidth, reqHeight);

            BufferedImage preTransform = canvas;
            canvas = new BufferedImage(reqWidth, reqHeight, preTransform.getType());
            Graphics2D gfx = canvas.createGraphics();

            double scaleX = (double) reqWidth / preTransform.getWidth();
            double scaleY = (double) reqHeight / preTransform.getHeight();
            AffineTransform affineTrans = AffineTransform.getScaleInstance(scaleX, scaleY);

            debugLog("缩放比例 - X: {}, Y: {}", scaleX, scaleY);

            // 渲染提示设置
            RenderingHints hintsTemp = HintsLevel.DEFAULT.getRenderingHints();
            gfx.addRenderingHints(hintsTemp);
            gfx.drawRenderedImage(preTransform, affineTrans);
            gfx.dispose();

            infoLog("栅格缩放完成 - 最终尺寸: {}x{}", reqWidth, reqHeight);
        } else {
            debugLog("画布尺寸已匹配请求尺寸，无需缩放");
        }
    }

    /**
     * 将融合后的图像转换为字节数组
     *
     * @return 图像字节数组
     * @throws Exception 编码异常
     */
    public byte[] toImageBytes() throws Exception {
        long startTime = System.currentTimeMillis();
        infoLog("开始瓦片融合处理 - 请求范围: {}, 输出尺寸: {}x{}", reqBounds, reqWidth, reqHeight);

        try {
            determineSourceResolution();
            determineCanvasLayout();
            createCanvas();
            renderCanvas();
            scaleRaster();

            ByteArrayOutputStream aos = new ByteArrayOutputStream();
            RenderedImage finalImage = null;
            byte[] imageBytes = null;

            try {
                finalImage = canvas;
                debugLog("开始编码图像 - 格式: {}", outputFormat.getMimeType());
                encoderMap.encode(
                        finalImage,
                        outputFormat,
                        aos,
                        encoderMap.isAggressiveOutputStreamSupported(outputFormat.getMimeType()),
                        null);
                imageBytes = aos.toByteArray();
                debugLog("图像编码完成 - 字节大小: {} bytes", imageBytes.length);

            } catch (Exception e) {
                errorLog("编码图像时发生异常: {}", e.getMessage(), e);
                if (finalImage != null) {
                    ImageUtilities.disposePlanarImageChain(PlanarImage.wrapRenderedImage(finalImage));
                    debugLog("已释放图像资源");
                }
                throw e;
            } finally {
                IOUtils.closeQuietly(aos);
                debugLog("已关闭输出流");
            }

            long endTime = System.currentTimeMillis();
            infoLog("瓦片融合处理完成 - 耗时: {} ms, 输出大小: {} bytes", (endTime - startTime), imageBytes.length);
            return imageBytes;

        } catch (Exception e) {
            errorLog("瓦片融合处理失败: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public RangeApo getSrcRange() {
        if (srcRectangle == null) {
            determineSourceResolution();
            determineCanvasLayout();
        }
        return new RangeApo(srcRectangle[0], srcRectangle[2], srcRectangle[1], srcRectangle[3], (int) srcRectangle[4]);
    }

    /**
     * 初始化解码器和编码器
     */
    public void init() {
        infoLog("初始化解码器和编码器");
        decoderMap = new GirImageDecoderContainer(ImageCodecInitializer.getAllDecodersList());
        encoderMap = new GirImageEncoderContainer(ImageCodecInitializer.getAllEncodersList());
        infoLog("解码器和编码器初始化完成");
    }
}
