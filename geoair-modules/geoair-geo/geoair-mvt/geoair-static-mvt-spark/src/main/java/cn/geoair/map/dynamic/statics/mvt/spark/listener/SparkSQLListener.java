package cn.geoair.map.dynamic.statics.mvt.spark.listener;

import cn.hutool.core.util.StrUtil;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.spark.scheduler.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.collection.JavaConverters;
import scala.collection.Seq;

/**
 * @author ：张逢吉
 * @date ：Created in 2025/12/26 15:16 @description： TODO
 */
public class SparkSQLListener extends SparkListener {

    private static final Logger log = LoggerFactory.getLogger(SparkSQLListener.class);

    @Override
    public void onTaskStart(SparkListenerTaskStart taskStart) {
        TaskInfo taskInfo = taskStart.taskInfo();
        log.info(
                "任务开始:stageId： {},taskId：{}, 分片Id：{}  执行器ID：{}节点信息{}",
                taskStart.stageId(),
                taskInfo.taskId(),
                taskInfo.partitionId(),
                taskInfo.executorId(),
                taskInfo.host());
    }

    @Override
    public void onTaskEnd(SparkListenerTaskEnd taskEnd) {
        TaskInfo taskInfo = taskEnd.taskInfo();
        boolean failed = taskInfo.failed();
        if (failed) {
            log.info(
                    "任务失败taskID: {},  分片ID：{}  失败原因：{},类型：{},节点信息{}",
                    taskInfo.taskId(),
                    taskInfo.partitionId(),
                    taskEnd.reason(),
                    taskEnd.taskType(),
                    taskEnd.taskInfo().host());
        } else {
            log.info(
                    "任务完成taskID: {},  分片ID：{} 结束原因：{},类型：{},节点信息{}",
                    taskInfo.taskId(),
                    taskInfo.partitionId(),
                    taskEnd.reason(),
                    taskEnd.taskType(),
                    taskEnd.taskInfo().host());
        }
    }

    // 监听作业启动
    @Override
    public void onJobStart(SparkListenerJobStart jobStart) {
        super.onJobStart(jobStart);
        Seq<StageInfo> stageInfoSeq = jobStart.stageInfos();
        List<StageInfo> java = JavaConverters.seqAsJavaListConverter(stageInfoSeq).asJava();
        List<String> names = java.stream().map(StageInfo::name).collect(Collectors.toList());
        String join = StrUtil.join(",", names);
        log.info("Spark作业[{}]启动 作业名称 {}，提交时间：{}", jobStart.jobId(), join, jobStart.time());
    }

    // 监听作业结束
    @Override
    public void onJobEnd(SparkListenerJobEnd jobEnd) {
        super.onJobEnd(jobEnd);

        log.info("Spark作业[{}]结束，状态：{} ", jobEnd.jobId(), jobEnd.jobResult().toString());
    }

    @Override
    public void onApplicationStart(SparkListenerApplicationStart applicationStart) {
        log.info("应用开始:[" + applicationStart.appId() + ":" + applicationStart.appName() + "]");
    }

    @Override
    public void onApplicationEnd(SparkListenerApplicationEnd applicationEnd) {
        log.info("应用结束:[时间:" + applicationEnd.time() + "]");
    }
}
