package cn.geoair.map.tile.forge.core.servlet;


import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.geoair.map.dynamic.tools.simple.GirImageUtil;
import cn.geoair.map.tile.forge.core.TileRequest;
import cn.geoair.map.tile.forge.core.base.enums.TileParamEnums;
import cn.geoair.web.GirWeb;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;
import org.springframework.http.HttpStatus;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;

/**
 * @author ：张俊
 * @date ：Created in 2026/7/13
 * @description： 瓦片响应构建工具
 */
public class TileResponseUtils {
    public static GiLogger log = GirLoggerFactory.getLogger();

    /**
     * 构建瓦片响应
     */
    public static void buildTileResponse(TileRequest tileRequest, HttpServletResponse response
    ) throws Exception {
        // 1. 校验瓦片是否存在
        if (!tileRequest.isExists()) {
            response.setStatus(HttpStatus.NO_CONTENT.value());
            return;
        }


        byte[] tileData = tileRequest.getBytes();
        String mimeType = tileRequest.getMimeType().getFormat();


        if (needEnhance()) {
            HttpServletRequest request = GirWeb.getRequest();
            String sharpenAmount = request.getParameter(TileParamEnums.SHARPEN_AMOUNT.getValue());
            String sharpenRadius = request.getParameter(TileParamEnums.SHARPEN_RADIUS.getValue());
            String sharpenThreshold = request.getParameter(TileParamEnums.SHARPEN_THRESHOLD.getValue());
            try {

                float amount = parseFloat(sharpenAmount, 1.2f);
                float radius = parseFloat(sharpenRadius, 1.5f);
                int threshold = parseInt(sharpenThreshold, 5);


                tileData = enhanceTile(tileData, radius, amount, threshold);

                // 更新响应长度
                response.setContentLengthLong(tileData.length);
            } catch (Exception e) {


                // 增强失败时使用原始数据
                log.error("图像增强失败，使用原始数据", e);
            }
        }

        // 4. 设置响应头
        response.setHeader("Cache-Control", "public, max-age=86400");
        response.setHeader("Last-Modified", String.valueOf(tileRequest.getLastModified()));
        response.setContentType(mimeType);
        response.setStatus(HttpStatus.OK.value());

        // 5. 输出数据
        ServletOutputStream outputStream = response.getOutputStream();
        IoUtil.copy(new ByteArrayInputStream(tileData), outputStream);
        IoUtil.close(outputStream);
    }


    private static boolean needEnhance() {
        HttpServletRequest request = GirWeb.getRequest();
        String sharpenAmount = request.getParameter(TileParamEnums.SHARPEN_AMOUNT.getValue());
        String sharpenRadius = request.getParameter(TileParamEnums.SHARPEN_RADIUS.getValue());
        String sharpenThreshold = request.getParameter(TileParamEnums.SHARPEN_THRESHOLD.getValue());
        return StrUtil.isNotBlank(sharpenAmount) || StrUtil.isNotBlank(sharpenRadius) || StrUtil.isNotBlank(sharpenThreshold);
    }

    /**
     * 执行瓦片增强
     */
    private static byte[] enhanceTile(byte[] tileData, float radius, float amount, int threshold) throws Exception {

        BufferedImage image = GirImageUtil.bytesToImage(tileData);
        if (image == null) {
            return tileData;
        }

        BufferedImage enhanced = GirImageUtil.unSharpMask(image, radius, amount, threshold);

        String format = GirImageUtil.detectImageFormat(tileData);

        return GirImageUtil.imageToBytes(enhanced, format);
    }


    /**
     * 解析浮点数
     */
    private static float parseFloat(String value, float defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        try {
            float parsed = Float.parseFloat(value);
            // 限制范围
            return Math.max(0.1f, Math.min(5.0f, parsed));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * 解析整数
     */
    private static int parseInt(String value, int defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        try {
            int parsed = Integer.parseInt(value);
            // 限制范围
            return Math.max(0, Math.min(50, parsed));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
