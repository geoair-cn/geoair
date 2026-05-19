// package cn.geoair.comp.knife4j.ext.springdoc.controller;
//
// import io.swagger.v3.oas.annotations.Operation;
// import org.springframework.http.HttpStatus;
// import org.springframework.http.ResponseEntity;
// import org.springframework.web.bind.annotation.GetMapping;
// import org.springframework.web.bind.annotation.PathVariable;
// import org.springframework.web.bind.annotation.RequestMapping;
// import org.springframework.web.bind.annotation.RestController;
// import org.springframework.web.client.RestTemplate;
// import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
//
/// **
// * GirGroupedApiDocsController class.
// *
// * @author Administrator
// * @version $Id: $Id
// */
// @RestController
// @RequestMapping("/v3/api-docs")
// public class GirGroupedApiDocsController {
//
//    private final RestTemplate restTemplate;
//
//    /** Constructor for GirGroupedApiDocsController. */
//    public GirGroupedApiDocsController() {
//        this.restTemplate = new RestTemplate();
//    }
//
//    /**
//     * 映射 /v3/api-docs/{group} → /v3/api-docs?group={group}
//     *
//     * @param group a {@link java.lang.String} object
//     * @return a {@link org.springframework.http.ResponseEntity} object
//     */
//    @GetMapping("/{group}")
//    @Operation(hidden = true) // 隐藏该接口，避免出现在Swagger文档中
//    public ResponseEntity<String> getGroupedApiDocs(@PathVariable String group) {
//        // 1. 构建目标URL（查询参数形式）
//        String baseUrl =
//                ServletUriComponentsBuilder.fromCurrentContextPath()
//                        .path("/v3/api-docs")
//                        .queryParam("group", group)
//                        .toUriString();
//
//        try {
//            // 2. 转发请求到查询参数形式的端点
//            String apiDocs = restTemplate.getForObject(baseUrl, String.class);
//            // 3. 返回分组文档内容
//            return ResponseEntity.ok(apiDocs);
//        } catch (Exception e) {
//            // 4. 分组不存在时返回404
//            return ResponseEntity.status(HttpStatus.NOT_FOUND)
//                    .body("Group " + group + " not found");
//        }
//    }
// }
