package cn.geoair.map.dynamic.tools.simple.response;

import cn.geoair.base.data.result.GiResult;
import cn.geoair.base.json.GirJSON;
import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.geoair.base.util.GutilObject;
import cn.geoair.map.dynamic.tools.grid.dto.TileZxyApo;
import cn.geoair.map.dynamic.tools.simple.GirImageUtil;
import cn.geoair.map.dynamic.tools.simple.GirServletUtil;
import cn.geoair.web.GirWeb;
import cn.geoair.web.mime.GirImageMime;
import cn.hutool.core.util.StrUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;


import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * 瓦片响应构建工具
 * 基于TileResponse对象进行响应构建
 *
 * @author 张俊
 * @date 2026/7/13
 */
public class GirTileResponseDefaultOpt implements GirTileResponseOpt {

    private static final GiLogger log = GirLoggerFactory.getLogger();


    /**
     * 从TileResponse对象构建响应
     */
    @Override
    public void buildFromException(Exception exception, HttpServletResponse response) {
        TileResponse error = TileResponse.error(exception.getMessage());
        buildFromTileResponse(error, response, -1);
    }

    @Override
    public void buildFromTileResponse(TileResponse tileResponse, HttpServletResponse response) {
        buildFromTileResponse(tileResponse, response, null);
    }

    /**
     * 从TileResponse对象构建响应（支持自定义缓存时间）
     */
    public static void buildFromTileResponse(TileResponse tileResponse, HttpServletResponse response, Integer cacheMaxAge) {
        if (tileResponse == null) {
            handleNotFound(response);
            return;
        }

        // 检查是否有效
        if (!tileResponse.isValid()) {
            handleError(422, response, tileResponse);
            return;
        }

        // 如果是图片类型，尝试应用锐化
        tileResponse.getBytes();
        byte[] finalBytes;
        SharpeningResult sharpeningResult = null;

        if (tileResponse.getMimeType() instanceof GirImageMime) {
            sharpeningResult = applySharpeningIfNeededWithResult(tileResponse.getBytes());
            if (sharpeningResult.isApplied()) {
                finalBytes = sharpeningResult.getData();
                // 更新tileResponse对象
                tileResponse.setBytesAndUpdateSize(finalBytes);
            }
        }
        setSysHeaders(response, tileResponse);
        // 设置缓存头
        setCacheHeaders(response, tileResponse, cacheMaxAge);

        // 设置锐化信息到响应头
        if (sharpeningResult != null && sharpeningResult.isApplied()) {
            setSharpeningHeaders(response, sharpeningResult);
        }


        response.setContentLengthLong(tileResponse.getContentLength());
        response.setStatus(HttpServletResponse.SC_OK);

        // 如果有自定义缓存头，额外设置
        if (GutilObject.isNotEmpty(tileResponse.getCacheHeaders())) {
            tileResponse.getCacheHeaders().forEach(response::setHeader);
        }

        if (GutilObject.isNotEmpty(tileResponse.getExtrasHeaders())) {
            tileResponse.getExtrasHeaders().forEach(response::setHeader);
        }

        InputStream inputStream = tileResponse.getInputStream();
        if (GutilObject.isNotEmpty(inputStream)) {
            GirServletUtil.toResponse(response, inputStream, tileResponse.getMimeType().getFormat());
        } else {
            handleNotFound(response);
        }


    }

    // ==================== 锐化处理相关 ====================

    /**
     * 锐化结果封装
     */
    public static class SharpeningResult {
        private final byte[] data;
        private final boolean applied;
        private final SharpeningParams params;
        private final long elapsedTime;

        public SharpeningResult(byte[] data, boolean applied, SharpeningParams params, long elapsedTime) {
            this.data = data;
            this.applied = applied;
            this.params = params;
            this.elapsedTime = elapsedTime;
        }

        public byte[] getData() {
            return data;
        }

        public boolean isApplied() {
            return applied;
        }

        public SharpeningParams getParams() {
            return params;
        }

        public long getElapsedTime() {
            return elapsedTime;
        }

        /**
         * 获取锐化信息用于写入Header
         */
        public String getInfoHeader() {
            if (!applied || params == null) {
                return "none";
            }
            return String.format("applied;amount=%.2f;radius=%.2f;threshold=%d;time=%dms",
                    params.getAmount(), params.getRadius(), params.getThreshold(), elapsedTime);
        }
    }

    /**
     * 判断是否需要应用锐化
     */
    private static boolean shouldApplySharpening() {
        HttpServletRequest request = GirWeb.getRequest();
        if (request == null) {
            return false;
        }
        // 检查是否有锐化参数
        String enhance = request.getParameter(TileParamEnums.ENHANCE.getValue());

        if (GutilObject.equal(enhance, "false")) {
            return false;
        }

        String amount = request.getParameter(TileParamEnums.SHARPEN_AMOUNT.getValue());
        String radius = request.getParameter(TileParamEnums.SHARPEN_RADIUS.getValue());
        String threshold = request.getParameter(TileParamEnums.SHARPEN_THRESHOLD.getValue());

        return StrUtil.isNotBlank(amount) || StrUtil.isNotBlank(radius) || StrUtil.isNotBlank(threshold);
    }

    /**
     * 解析锐化参数
     */
    private static SharpeningParams parseSharpeningParams() {
        HttpServletRequest request = GirWeb.getRequest();
        if (request == null) {
            return null;
        }

        String amountStr = request.getParameter(TileParamEnums.SHARPEN_AMOUNT.getValue());
        String radiusStr = request.getParameter(TileParamEnums.SHARPEN_RADIUS.getValue());
        String thresholdStr = request.getParameter(TileParamEnums.SHARPEN_THRESHOLD.getValue());

        float DEFAULT_AMOUNT = 1.2f;
        float DEFAULT_RADIUS = 1.5f;
        int DEFAULT_THRESHOLD = 5;

        float MIN_AMOUNT = 0.0f;
        float MAX_AMOUNT = 5.0f;
        float MIN_RADIUS = 0.0f;
        float MAX_RADIUS = 5.0f;
        int MIN_THRESHOLD = 0;
        int MAX_THRESHOLD = 50;

        // 解析参数
        float amount = parseFloat(amountStr, DEFAULT_AMOUNT, MIN_AMOUNT, MAX_AMOUNT);
        float radius = parseFloat(radiusStr, DEFAULT_RADIUS, MIN_RADIUS, MAX_RADIUS);
        int threshold = parseInt(thresholdStr, DEFAULT_THRESHOLD, MIN_THRESHOLD, MAX_THRESHOLD);

        return new SharpeningParams(amount, radius, threshold);
    }

    /**
     * 执行瓦片锐化
     */
    private static byte[] enhanceTile(byte[] tileData, float radius, float amount, int threshold) {
        // 参数校验
        if (tileData == null || tileData.length == 0) {
            return tileData;
        }

        if (amount <= 0 || radius <= 0) {
            log.debug("锐化参数无效: amount={}, radius={}, 跳过锐化", amount, radius);
            return tileData;
        }

        try {
            // 转换为BufferedImage
            BufferedImage image = GirImageUtil.bytesToImage(tileData);
            if (image == null) {
                log.warn("无法解析图像数据，跳过锐化");
                return tileData;
            }

            // 执行USM锐化
            BufferedImage enhanced = GirImageUtil.unSharpMask(image, radius, amount, threshold);

            // 检测原始图像格式
            String format = GirImageUtil.detectImageFormat(tileData);

            // 转换为字节数组
            byte[] result = GirImageUtil.imageToBytes(enhanced, format);
            if (result.length == 0) {
                log.warn("图像转换失败，使用原始数据");
                return tileData;
            }

            log.debug("锐化完成: 原始大小={} bytes, 锐化后大小={} bytes", tileData.length, result.length);
            return result;
        } catch (Exception e) {
            log.error("锐化处理失败，使用原始数据", e);
            return tileData;
        }
    }

    /**
     * 预锐化处理（带结果返回）
     */
    public static SharpeningResult applySharpeningIfNeededWithResult(byte[] tileData) {
        long startTime = System.currentTimeMillis();

        if (tileData == null || tileData.length == 0) {
            return new SharpeningResult(tileData, false, null, 0);
        }

        if (!shouldApplySharpening()) {
            return new SharpeningResult(tileData, false, null, 0);
        }

        SharpeningParams params = parseSharpeningParams();
        if (params == null || !params.isValid()) {
            return new SharpeningResult(tileData, false, null, 0);
        }

        try {
            byte[] enhancedData = enhanceTile(tileData, params.getRadius(), params.getAmount(), params.getThreshold());
            long elapsedTime = System.currentTimeMillis() - startTime;

            boolean applied = enhancedData != tileData && enhancedData.length > 0;
            return new SharpeningResult(enhancedData, applied, params, elapsedTime);
        } catch (Exception e) {
            log.error("预锐化处理失败", e);
            return new SharpeningResult(tileData, false, null, 0);
        }
    }


    /**
     * 设置锐化信息到响应头
     */
    private static void setSharpeningHeaders(HttpServletResponse response, SharpeningResult sharpeningResult) {
        if (sharpeningResult == null || !sharpeningResult.isApplied()) {
            return;
        }

        SharpeningParams params = sharpeningResult.getParams();

        response.setHeader("X-Sharpen-Applied", "true");
        response.setHeader("X-Sharpen-Amount", String.format("%.2f", params.getAmount()));
        response.setHeader("X-Sharpen-Radius", String.format("%.2f", params.getRadius()));
        response.setHeader("X-Sharpen-Threshold", String.valueOf(params.getThreshold()));
        response.setHeader("X-Sharpen-Elapsed-Time", String.format("%dms", sharpeningResult.getElapsedTime()));
        response.setHeader("X-Sharpen-Info", sharpeningResult.getInfoHeader());
    }

    // ==================== 缓存控制 ====================

    /**
     * 设置缓存头（基于TileResponse）
     *
     * @param response     HttpServletResponse
     * @param tileResponse TileResponse对象
     * @param cacheMaxAge  缓存时间（秒），-1表示不缓存，null表示使用默认值
     */
    private static void setCacheHeaders(HttpServletResponse response, TileResponse tileResponse, Integer cacheMaxAge) {
        // 判断是否需要禁用缓存
        if (cacheMaxAge != null && cacheMaxAge == -1) {
            // 不缓存响应头
            response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
            response.setHeader("Pragma", "no-cache");
            response.setHeader("Expires", "0");
            return;
        }

        // 默认缓存时间：1天
        int maxAge = cacheMaxAge != null ? cacheMaxAge : 86400;

        // Cache-Control
        response.setHeader("Cache-Control", String.format("public, max-age=%d", maxAge));

        // Last-Modified
        if (tileResponse.getLastModified() > 0) {
            response.setHeader("Last-Modified", formatHttpDate(tileResponse.getLastModified()));
        }

        // ETag
        String eTag = tileResponse.getETag();
        if (eTag != null) {
            response.setHeader("ETag", eTag);
        }

        // Expires
        long expires = System.currentTimeMillis() + (maxAge * 1000L);
        response.setHeader("Expires", formatHttpDate(expires));
    }


    private static void setSysHeaders(HttpServletResponse response, TileResponse tileResponse) {
        String dataSource = tileResponse.getDataSource();
        if (GutilObject.isNotEmpty(dataSource)) {
            response.setHeader("X-Tile-DataSource", dataSource);
        }
        String version = tileResponse.getVersion();
        if (GutilObject.isNotEmpty(version)) {
            response.setHeader("X-Tile-Version", version);
        }
        String gridEpsgStr = tileResponse.getGridEpsgStr();
        if (GutilObject.isNotEmpty(gridEpsgStr)) {
            response.setHeader("X-Tile-Grid-Epsg", gridEpsgStr);
        }


    }

    /**
     * 格式化HTTP日期
     */
    private static String formatHttpDate(long timestamp) {
        return DateTimeFormatter.RFC_1123_DATE_TIME
                .withZone(ZoneId.of("GMT"))
                .format(Instant.ofEpochMilli(timestamp));
    }

    // ==================== 错误处理 ====================

    /**
     * 处理瓦片不存在
     */
    private static void handleNotFound(HttpServletResponse response) {
        response.setStatus(HttpServletResponse.SC_NO_CONTENT);
        response.setHeader("Cache-Control", "no-cache");
    }


    /**
     * 处理错误（带错误码和消息）
     */
    public static void handleError(int httpCode, HttpServletResponse response, TileResponse tileResponse) {
        TileZxyApo coordinate = tileResponse.getCoordinate();
        setSysHeaders(response, tileResponse);
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        if (GutilObject.isNotEmpty(coordinate)) {
            try {
                response.setHeader("X-Tile-Bounding-Box", coordinate.toBox4326WktString());
            } catch (Exception e) {
            }
        }
        String errorMessage = tileResponse.getErrorMessage();
        if (GutilObject.isEmpty(errorMessage) && GutilObject.isNotEmpty(coordinate)) {
            errorMessage = StrUtil.format("无法找到瓦片  {}", coordinate.getZxyString());
        }
        GiResult result = GiResult.failureMsg(errorMessage).andCode(httpCode);
        String json = GirJSON.toJson(result).toJSONString();
        GirServletUtil.toResponse(response, json.getBytes(StandardCharsets.UTF_8), "application/json,charset=utf-8", httpCode);
    }


    /**
     * 解析浮点数（带范围限制）
     */
    private static float parseFloat(String value, float defaultValue, float min, float max) {
        if (StrUtil.isBlank(value)) {
            return defaultValue;
        }
        try {
            float parsed = Float.parseFloat(value.trim());
            return Math.max(min, Math.min(max, parsed));
        } catch (NumberFormatException e) {
            log.warn("解析浮点数失败: {}, 使用默认值: {}", value, defaultValue);
            return defaultValue;
        }
    }

    /**
     * 解析整数（带范围限制）
     */
    private static int parseInt(String value, int defaultValue, int min, int max) {
        if (StrUtil.isBlank(value)) {
            return defaultValue;
        }
        try {
            int parsed = Integer.parseInt(value.trim());
            return Math.max(min, Math.min(max, parsed));
        } catch (NumberFormatException e) {
            log.warn("解析整数失败: {}, 使用默认值: {}", value, defaultValue);
            return defaultValue;
        }
    }

    // ==================== 内部类 ====================

    public static class SharpeningParams {
        private final float amount;
        private final float radius;
        private final int threshold;

        public SharpeningParams(float amount, float radius, int threshold) {
            this.amount = amount;
            this.radius = radius;
            this.threshold = threshold;
        }

        public float getAmount() {
            return amount;
        }

        public float getRadius() {
            return radius;
        }

        public int getThreshold() {
            return threshold;
        }

        /**
         * 检查参数是否有效
         */
        public boolean isValid() {
            return amount > 0 && radius > 0 && threshold >= 0;
        }

        @Override
        public String toString() {
            return String.format("amount=%.2f, radius=%.2f, threshold=%d", amount, radius, threshold);
        }
    }


}
