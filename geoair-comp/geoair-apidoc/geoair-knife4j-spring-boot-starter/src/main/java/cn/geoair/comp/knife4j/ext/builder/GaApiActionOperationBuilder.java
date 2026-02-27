package cn.geoair.comp.knife4j.ext.builder;

import cn.geoair.gtc.base.api.annotation.GaApiAction;
import com.google.common.collect.Sets;

import io.swagger.annotations.ApiOperation;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.OperationBuilderPlugin;
import springfox.documentation.spi.service.contexts.OperationContext;

import java.util.List;
import java.util.Set;

import static com.google.common.collect.Sets.union;

/**
 * @author ：张俊
 * @date ：Created in 2023/3/1 10:16
 * @description： 使用  GaApi 替换 ApiOperation
 */
public class GaApiActionOperationBuilder implements OperationBuilderPlugin {

    @Override
    public void apply(OperationContext context) {
        List<ApiOperation> list = context.findAllAnnotations(ApiOperation.class);
        if (list.isEmpty()) {
            List<GaApiAction> explainList = context.findAllAnnotations(GaApiAction.class);
            if (!explainList.isEmpty()) {
                GaApiAction explain = explainList.get(0);
                context.operationBuilder().summary(explain.text());//替换默认值
            }
        }




    }

    @Override
    public boolean supports(DocumentationType delimiter) {
        return true;
    }


}
