package cn.geoair.map.dynamic.statics.mvt.spark.listener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;

import org.apache.spark.streaming.scheduler.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Spark Streaming 监听器：完整记录流任务生命周期事件，便于监控和问题定位
 *
 * @author ：张逢吉
 * @date ：Created in 2025/12/26 15:17
 */
public class SparkStreamListener implements StreamingListener {

	private static final Logger log = LoggerFactory.getLogger(SparkStreamListener.class);

	// 时间格式化：统一日志中的时间展示
	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

	/**
	 * 流任务启动事件
	 */
	@Override
	public void onStreamingStarted(StreamingListenerStreamingStarted event) {
		long startTime = event.time();
		String startTimeStr = DATE_FORMAT.format(new Date(startTime));
		log.info("==================== Spark Streaming 任务启动 ====================");
		log.info("启动时间戳: {} | 启动时间: {}", startTime, startTimeStr);
		log.info("================================================================\n");
	}

	/**
	 * Receiver 启动事件（数据接收者启动）
	 */
	@Override
	public void onReceiverStarted(StreamingListenerReceiverStarted event) {
		ReceiverInfo receiverInfo = event.receiverInfo();
		log.info("---------------------- Receiver 启动 ----------------------");
		log.info("Receiver ID: {}", receiverInfo.streamId());
		log.info("Receiver 名称: {}", receiverInfo.name());
		log.info("Receiver 状态: {}", receiverInfo.active());
		log.info("Receiver 位置: {}", receiverInfo.location());

		log.info("-----------------------------------------------------------\n");
	}

	/**
	 * Receiver 停止事件（数据接收者停止）
	 */
	@Override
	public void onReceiverStopped(StreamingListenerReceiverStopped event) {
		ReceiverInfo receiverInfo = event.receiverInfo();
		log.info("---------------------- Receiver 停止 ----------------------");
		log.info("Receiver ID: {}", receiverInfo.streamId());
		log.info("Receiver 名称: {}", receiverInfo.name());
		log.info("Receiver 状态: {}", receiverInfo.active());

	}

	/**
	 * Receiver 错误事件（数据接收者异常）
	 */
	@Override
	public void onReceiverError(StreamingListenerReceiverError event) {
		ReceiverInfo receiverInfo = event.receiverInfo();
		log.error("---------------------- Receiver 异常 ----------------------");
		log.error("Receiver ID: {}", receiverInfo.streamId());
		log.error("Receiver 名称: {}", receiverInfo.name());
		log.error("Receiver 位置: {}", receiverInfo.location());
		log.error("异常时间戳: {} | 异常时间: {}", receiverInfo.lastErrorTime(),
				DATE_FORMAT.format(new Date(receiverInfo.lastErrorTime())));

	}

	/**
	 * 批次提交事件（批次进入待处理队列）
	 */
	@Override
	public void onBatchSubmitted(StreamingListenerBatchSubmitted event) {
		BatchInfo batchInfo = event.batchInfo();
		log.info("---------------------- 批次提交 ----------------------");
		log.info("提交时间戳: {} | 提交时间: {}", batchInfo.submissionTime(),
				DATE_FORMAT.format(new Date(batchInfo.submissionTime())));
		log.info("批次处理延迟: {} ms", batchInfo.schedulingDelay());
		log.info("输入数据量: {} records", batchInfo.numRecords());
		log.info("-------------------------------------------------------\n");
	}

	/**
	 * 批次启动事件（批次开始处理）
	 */
	@Override
	public void onBatchStarted(StreamingListenerBatchStarted event) {
		BatchInfo batchInfo = event.batchInfo();
		log.info("---------------------- 批次启动处理 ----------------------");
		log.info("-----------------------------------------------------------\n");
	}

	/**
	 * 批次完成事件（批次处理结束，含成功/失败状态）
	 */
	@Override
	public void onBatchCompleted(StreamingListenerBatchCompleted event) {
		BatchInfo batchInfo = event.batchInfo();
		log.info("---------------------- 批次处理完成 ----------------------");

		// 打印处理延迟（若有）
		Optional.ofNullable(batchInfo.processingDelay()).ifPresent(delay -> log.info("任务处理延迟: {} ms", delay));
		Optional.ofNullable(batchInfo.totalDelay()).ifPresent(delay -> log.info("批次总延迟（提交→完成）: {} ms", delay));

		log.info("-----------------------------------------------------------\n");
	}

	/**
	 * 输出操作完成事件（批次内单个输出操作完成）
	 */
	@Override
	public void onOutputOperationCompleted(StreamingListenerOutputOperationCompleted event) {
		OutputOperationInfo opInfo = event.outputOperationInfo();
		log.info("---------------------- 输出操作完成 ----------------------");
		log.info("批次ID: {}", opInfo.id());
		log.info("输出操作名称: {}", opInfo.name());
		log.info("输出操作耗时: {} ms", opInfo.duration());
		log.info("-----------------------------------------------------------\n");
	}

	/**
	 * 输出操作启动事件（批次内单个输出操作开始）
	 */
	@Override
	public void onOutputOperationStarted(StreamingListenerOutputOperationStarted event) {
		OutputOperationInfo opInfo = event.outputOperationInfo();
		log.info("---------------------- 输出操作启动 ----------------------");
		log.info("输出操作名称: {}", opInfo.name());
		log.info("启动时间戳: {} ", opInfo.startTime());
		log.info("-----------------------------------------------------------\n");

	}

}
