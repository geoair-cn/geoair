package cn.geoair.comp.dynamic.ds.datasource.web;


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.geoair.comp.dynamic.ds.datasource.GirDynamicStackDataSource;
 
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Stack;

/**
 * @author ：张俊
 * @date ：Created in 2025/6/19 10:44
 * @description： 动态数据源切换相关配置，这里可以接入相关业务库查询获取到数据库信息 注意：
 *     如果涉及到异步线程，还需要手动注入数据源配置
 */
 
public class GirDataSourceWebContextInterceptor implements HandlerInterceptor {
    public static GiLogger log = GirLoggerFactory.getLogger();
    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception ex) {

        // 记录请求完成开始清理数据源上下文
        // log.info("请求处理完成，开始清理数据源上下文");

        // 获取当前数据源栈状态
        Stack<String> dataSourceStack = GirDynamicStackDataSource.getDataSourceStack();
        int stackSize = dataSourceStack.size();

        if (stackSize > 0) {
            // 构建当前数据源栈的字符串表示
            StringBuilder stackBuilder = new StringBuilder();
            for (int i = 0; i < stackSize; i++) {
                stackBuilder.append(dataSourceStack.get(i));
                if (i < stackSize - 1) {
                    stackBuilder.append(" -> ");
                }
            }
            log.warn("  检测到未清理的数据源栈: {}", stackBuilder);
            //
            // 清空数据源栈
            int popCount = 0;
            while (GirDynamicStackDataSource.getCurrentDataSource() != null) {
                GirDynamicStackDataSource.popDataSource();
                popCount++;
            }

            log.info("  已成功清理数据源栈，共弹出 {} 个数据源", popCount);
        } else {
            // log.debug(" 数据源栈已为空，无需清理");
        }

        // 记录请求处理完成
        // log.info(" 请求处理全流程完成");
    }
}
