package cn.geoair.map.tile.forge.core.zip;

/**
 * @author ：张俊
 * @date ：Created in 2026/4/23 14:10
 * @description： TODO
 */
public class LogProgressConsumer  implements ProgressConsumer{
    @Override
    public void accept(Long allCount, Long currentCount) {
        int progressBarLength = 50;
        // 计算进度百分比
        double progress = (double) currentCount / allCount;
        int percent = (int) (progress * 100);
        // 计算进度条已完成长度
        int completedLength = (int) (progress * progressBarLength);

        // 构建进度条字符串
        StringBuilder progressBar = new StringBuilder();
        progressBar.append("[");
        for (int i = 0; i < progressBarLength; i++) {
            if (i < completedLength) {
                progressBar.append("="); // 已完成部分
            } else if (i == completedLength && currentCount < allCount) {
                progressBar.append(">"); // 当前进度位置
            } else {
                progressBar.append(" "); // 未完成部分
            }
        }
        progressBar.append("] ");
        progressBar.append(percent).append("% ");
        progressBar.append("(").append(currentCount).append("/").append(allCount).append(")");
        System.out.print("\r" + progressBar);
        // 进度完成后换行
        if (currentCount >= allCount) {
            System.out.println();
        }
    }
}
