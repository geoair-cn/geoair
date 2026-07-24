package cn.geoair.map.tile.forge.core.bygwc.wmts;

import cn.geoair.map.tile.forge.core.bygwc.ProviderConfig;
import cn.geoair.map.tile.forge.core.bygwc.config.*;
import cn.geoair.map.tile.forge.core.bygwc.grid.*;
import cn.geoair.map.tile.forge.core.bygwc.layer.*;
import cn.hutool.core.util.StrUtil;
import java.io.StringWriter;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/** 生成GetCapabilities XML文档的工具类 用于描述ArcGIS瓦片缓存图层的能力信息 */
public class GetCapabilitiesGenerator {

    private static final String XMLNS = "http://www.opengis.net/wmts/1.0";
    private static final String XMLNS_XSI = "http://www.w3.org/2001/XMLSchema-instance";
    private static final String XSI_SCHEMA_LOCATION =
            "http://www.opengis.net/wmts/1.0 http://schemas.opengis.net/wmts/1.0/wmtsGetCapabilities_response.xsd";
    private static final String XMLNS_OWS = "http://www.opengis.net/ows/1.1";
    private static final String XMLNS_XLINK = "http://www.w3.org/1999/xlink";

    ArcGISCacheLayer layer;

    public static GetCapabilitiesGenerator getInstance() {
        return new GetCapabilitiesGenerator();
    }

    /**
     * 为ArcGIS缓存图层生成GetCapabilities XML字符串
     *
     * @param layer ArcGIS缓存图层对象
     * @return 生成的XML字符串
     * @throws Exception 生成过程中发生的异常
     */
    public String generate(ArcGISCacheLayer layer) throws Exception {
        if (layer == null || layer.getCacheInfo() == null) {
            throw new IllegalArgumentException("图层或缓存信息不能为空");
        }
        this.layer = layer;

        // 创建XML文档
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
        Document doc = docBuilder.newDocument();

        // 创建根元素
        Element rootElement = doc.createElementNS(XMLNS, "Capabilities");
        rootElement.setAttribute("xmlns", XMLNS);
        rootElement.setAttribute("xmlns:xsi", XMLNS_XSI);
        rootElement.setAttribute("xsi:schemaLocation", XSI_SCHEMA_LOCATION);
        rootElement.setAttribute("xmlns:ows", XMLNS_OWS);
        rootElement.setAttribute("xmlns:xlink", XMLNS_XLINK);
        rootElement.setAttribute("version", "1.0.0");
        doc.appendChild(rootElement);

        // 添加ServiceIdentification部分
        addServiceIdentification(doc, rootElement, layer);

        // 添加ServiceProvider部分
        addServiceProvider(doc, rootElement);

        // 添加OperationsMetadata部分
        addOperationsMetadata(doc, rootElement);

        // 添加Contents部分
        addContents(doc, rootElement, layer);

        // 转换为XML字符串
        return convertDocumentToString(doc);
    }

    /** 添加服务标识信息 */
    private void addServiceIdentification(Document doc, Element parent, ArcGISCacheLayer layer) {
        Element serviceIdentification = doc.createElementNS(XMLNS_OWS, "ows:ServiceIdentification");

        Element title = doc.createElementNS(XMLNS_OWS, "ows:Title");
        title.setTextContent("ArcGIS Tile Cache Service");
        serviceIdentification.appendChild(title);

        Element abstractT = doc.createElementNS(XMLNS_OWS, "ows:Abstract");
        abstractT.setTextContent("提供ArcGIS瓦片缓存服务");
        serviceIdentification.appendChild(abstractT);

        Element serviceType = doc.createElementNS(XMLNS_OWS, "ows:ServiceType");
        serviceType.setTextContent("WMTS");
        serviceIdentification.appendChild(serviceType);

        Element serviceTypeVersion = doc.createElementNS(XMLNS_OWS, "ows:ServiceTypeVersion");
        serviceTypeVersion.setTextContent("1.0.0");
        serviceIdentification.appendChild(serviceTypeVersion);

        Element fees = doc.createElementNS(XMLNS_OWS, "ows:Fees");
        fees.setTextContent("none");
        serviceIdentification.appendChild(fees);

        Element accessConstraints = doc.createElementNS(XMLNS_OWS, "ows:AccessConstraints");
        accessConstraints.setTextContent("none");
        serviceIdentification.appendChild(accessConstraints);

        parent.appendChild(serviceIdentification);
    }

    /** 添加服务提供者信息 */
    private void addServiceProvider(Document doc, Element parent) {
        Element serviceProvider = doc.createElementNS(XMLNS_OWS, "ows:ServiceProvider");

        Element providerName = doc.createElementNS(XMLNS_OWS, "ows:ProviderName");
        providerName.setTextContent(ProviderConfig.getInstance().getProviderName());
        serviceProvider.appendChild(providerName);

        Element providerSite = doc.createElementNS(XMLNS_OWS, "ows:ProviderSite");
        providerSite.setAttribute("xlink:href", ProviderConfig.getInstance().getProviderSite());
        serviceProvider.appendChild(providerSite);

        Element serviceContact = doc.createElementNS(XMLNS_OWS, "ows:ServiceContact");
        Element individualName = doc.createElementNS(XMLNS_OWS, "ows:IndividualName");
        individualName.setTextContent(ProviderConfig.getInstance().getProviderName());
        serviceContact.appendChild(individualName);
        serviceProvider.appendChild(serviceContact);

        parent.appendChild(serviceProvider);
    }

    /** 添加操作元数据信息 */
    private void addOperationsMetadata(Document doc, Element parent) {
        Element operationsMetadata = doc.createElementNS(XMLNS_OWS, "ows:OperationsMetadata");

        // 添加GetCapabilities操作
        addOperation(
                doc,
                operationsMetadata,
                "GetCapabilities",
                "http://localhost:8080/wmts?service=WMTS&request=GetCapabilities");

        // 添加GetTile操作
        addOperation(
                doc,
                operationsMetadata,
                "GetTile",
                "http://localhost:8080/wmts?service=WMTS&request=GetTile");

        // 添加GetFeatureInfo操作（如果支持）
        addOperation(
                doc,
                operationsMetadata,
                "GetFeatureInfo",
                "http://localhost:8080/wmts?service=WMTS&request=GetFeatureInfo");

        parent.appendChild(operationsMetadata);
    }

    /** 添加单个操作信息 */
    private void addOperation(Document doc, Element parent, String operationName, String href) {
        Element operation = doc.createElementNS(XMLNS_OWS, "ows:Operation");
        operation.setAttribute("name", operationName);

        Element dcp = doc.createElementNS(XMLNS_OWS, "ows:DCP");
        Element http = doc.createElementNS(XMLNS_OWS, "ows:HTTP");
        Element get = doc.createElementNS(XMLNS_OWS, "ows:Get");
        get.setAttribute("xlink:href", href);
        http.appendChild(get);
        dcp.appendChild(http);
        operation.appendChild(dcp);

        parent.appendChild(operation);
    }

    /** 添加内容信息（图层、样式等） */
    private void addContents(Document doc, Element parent, ArcGISCacheLayer layer) {
        Element contents = doc.createElement("Contents");
        parent.appendChild(contents);

        // 添加图层信息
        addLayer(doc, contents, layer);

        // 添加样式信息
        addStyle(doc, contents);
    }

    /** 添加图层信息 */
    private void addLayer(Document doc, Element parent, ArcGISCacheLayer layer) {
        CacheInfo cacheInfo = layer.getCacheInfo();
        TileCacheInfo tileCacheInfo = cacheInfo.getTileCacheInfo();
        BoundingBox layerBounds = layer.getLayerBounds();

        Element layerElement = doc.createElement("Layer");

        // 图层基本信息
        Element title = doc.createElement("Title");
        title.setTextContent(this.layer.getLayerName());
        layerElement.appendChild(title);

        Element abstractT = doc.createElement("Abstract");
        abstractT.setTextContent(ProviderConfig.getInstance().getAbstractInfo());
        layerElement.appendChild(abstractT);

        Element identifier = doc.createElement("Identifier");
        identifier.setTextContent(this.layer.getLayerName());
        layerElement.appendChild(identifier);

        // 边界范围
        if (layerBounds != null) {
            Element wgs84BoundingBox = doc.createElementNS(XMLNS_OWS, "ows:WGS84BoundingBox");
            addBoundingBoxCoordinates(doc, wgs84BoundingBox, layerBounds);
            layerElement.appendChild(wgs84BoundingBox);
        }

        // 瓦片矩阵集链接
        Element tileMatrixSetLink = doc.createElement("TileMatrixSetLink");
        Element tileMatrixSet = doc.createElement("TileMatrixSet");
        tileMatrixSet.setTextContent("EPSG:" + this.layer.getGridSet().getSrs().getNumber());
        tileMatrixSetLink.appendChild(tileMatrixSet);
        layerElement.appendChild(tileMatrixSetLink);

        // 格式信息
        TileImageInfo tileImageInfo = cacheInfo.getTileImageInfo();
        if (tileImageInfo != null && tileImageInfo.getCacheTileFormat() != null) {
            Element format = doc.createElement("Format");
            String mimeType = getMimeType(tileImageInfo.getCacheTileFormat());
            format.setTextContent(mimeType);
            layerElement.appendChild(format);
        }

        // 添加ResourceURL信息
        Element resourceURL = doc.createElement("ResourceURL");
        resourceURL.setAttribute("format", getMimeType(tileImageInfo.getCacheTileFormat()));

        resourceURL.setAttribute("resourceType", "tile");
        String template = "/wmts?layer={}&tilematrixset={}";
        String format =
                StrUtil.format(
                        template,
                        this.layer.getLayerName(),
                        "EPSG:" + this.layer.getGridSet().getSrs().getNumber());
        resourceURL.setAttribute(
                "template",
                format
                        + "&Service=WMTS&Request=GetTile&Version=1.0.0&Format=image/png&TileMatrix={TileMatrix}&TileCol={TileCol}&TileRow={TileRow}");
        layerElement.appendChild(resourceURL);

        // 添加所有层级信息
        addTileMatrixSet(doc, parent, tileCacheInfo);

        parent.appendChild(layerElement);
    }

    /** 添加瓦片矩阵集信息（包含所有层级） */
    private void addTileMatrixSet(Document doc, Element parent, TileCacheInfo tileCacheInfo) {
        Element tileMatrixSet = doc.createElement("TileMatrixSet");

        Element identifier = doc.createElement("Identifier");
        identifier.setTextContent("EPSG:" + this.layer.getGridSet().getSrs().getNumber());
        tileMatrixSet.appendChild(identifier);

        Element supportedCRS = doc.createElement("SupportedCRS");
        supportedCRS.setTextContent(
                "urn:ogc:def:crs:EPSG::" + this.layer.getGridSet().getSrs().getNumber());
        tileMatrixSet.appendChild(supportedCRS);

        // 瓦片原点
        Element tileMatrixSetLimits = doc.createElement("TileMatrixSetLimits");
        tileMatrixSet.appendChild(tileMatrixSetLimits);
        // 添加每个层级的瓦片矩阵信息
        List<LODInfo> lodInfos = tileCacheInfo.getLodInfos();
        if (lodInfos != null) {
            GridSet gridSet = this.layer.getGridSet();

            for (int i = 0; i < lodInfos.size(); i++) {
                LODInfo lodInfo = lodInfos.get(i);
                double[] tlCoordinates = gridSet.getOrderedTopLeftCorner(i);
                Grid grid = gridSet.getGrid(i);
                addTileMatrix(doc, tileMatrixSet, tileCacheInfo, lodInfo, grid, tlCoordinates);
            }
        }

        parent.appendChild(tileMatrixSet);
    }

    /** 添加单个层级的瓦片矩阵信息 */
    private void addTileMatrix(
            Document doc,
            Element tileMatrixSet,
            TileCacheInfo tileCacheInfo,
            LODInfo lod,
            Grid grid,
            double[] tlCoordinates) {
        Element tileMatrix = doc.createElement("TileMatrix");

        Element identifier = doc.createElement("Identifier");
        identifier.setTextContent(String.valueOf(lod.getLevelID()));
        tileMatrix.appendChild(identifier);

        // 比例尺分母
        Element scaleDenominator = doc.createElement("ScaleDenominator");
        scaleDenominator.setTextContent(String.valueOf(lod.getScale()));
        tileMatrix.appendChild(scaleDenominator);

        // 瓦片像素尺寸
        Element tileWidth = doc.createElement("TileWidth");
        tileWidth.setTextContent(String.valueOf(tileCacheInfo.getTileCols()));
        tileMatrix.appendChild(tileWidth);

        Element tileHeight = doc.createElement("TileHeight");
        tileHeight.setTextContent(String.valueOf(tileCacheInfo.getTileRows()));
        tileMatrix.appendChild(tileHeight);

        Element matrixWidth = doc.createElement("MatrixWidth");
        matrixWidth.setTextContent(grid.getNumTilesWide() + "");

        tileMatrix.appendChild(matrixWidth);

        Element matrixHeight = doc.createElement("MatrixHeight");
        matrixHeight.setTextContent(grid.getNumTilesHigh() + "");
        tileMatrix.appendChild(matrixHeight);

        // 左上角点坐标
        Element topLeftCorner = doc.createElement("TopLeftCorner");
        topLeftCorner.setTextContent(
                StrUtil.format(
                        "{} {}",
                        Double.toString(tlCoordinates[0]),
                        Double.toString(tlCoordinates[1])));
        tileMatrix.appendChild(topLeftCorner);

        tileMatrixSet.appendChild(tileMatrix);
    }

    /** 添加样式信息 */
    private void addStyle(Document doc, Element parent) {
        Element style = doc.createElement("Style");

        Element identifier = doc.createElement("Identifier");
        identifier.setTextContent("default");
        style.appendChild(identifier);

        Element title = doc.createElement("Title");
        title.setTextContent("Default Style");
        style.appendChild(title);

        Element isDefault = doc.createElement("IsDefault");
        isDefault.setTextContent("true");
        style.appendChild(isDefault);

        parent.appendChild(style);
    }

    /** 添加边界框坐标信息 */
    private void addBoundingBoxCoordinates(Document doc, Element boundingBox, BoundingBox bbox) {
        Element lowerCorner = doc.createElementNS(XMLNS_OWS, "ows:LowerCorner");
        lowerCorner.setTextContent(bbox.getMinX() + " " + bbox.getMinY());
        boundingBox.appendChild(lowerCorner);

        Element upperCorner = doc.createElementNS(XMLNS_OWS, "ows:UpperCorner");
        upperCorner.setTextContent(bbox.getMaxX() + " " + bbox.getMaxY());
        boundingBox.appendChild(upperCorner);
    }

    /** 将文档转换为XML字符串 */
    private String convertDocumentToString(Document doc) throws Exception {
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(doc), new StreamResult(writer));
        return writer.toString();
    }

    /** 根据瓦片格式获取对应的MIME类型 */
    private String getMimeType(String tileFormat) {
        switch (tileFormat.toUpperCase()) {
            case "JPEG":
                return "image/jpeg";
            case "PNG8":
            case "PNG24":
            case "PNG32":
                return "image/png";
            default:
                return "image/png";
        }
    }
}
