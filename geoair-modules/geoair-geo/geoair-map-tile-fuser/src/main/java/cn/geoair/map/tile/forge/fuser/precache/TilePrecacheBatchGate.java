package cn.geoair.map.tile.forge.fuser.precache;

import cn.geoair.map.tile.forge.fuser.execution.TileCancellationToken;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;

/** 多个协调器共享一份批次容量，有多个任务等待时轮流提交。 */
public final class TilePrecacheBatchGate {
    private final Map<String, Deque<Object>> waiting = new LinkedHashMap<>();
    private final int capacity;
    private int active;
    private String lastTask;

    public TilePrecacheBatchGate(int capacity) {
        if (capacity <= 0) throw new IllegalArgumentException("批次并发数必须大于零");
        this.capacity = capacity;
    }

    synchronized void acquire(String task, TileCancellationToken cancellation) throws InterruptedException {
        Object request = new Object();
        Deque<Object> queue = waiting.computeIfAbsent(task, ignored -> new ArrayDeque<>());
        queue.addLast(request);
        try {
            while (active >= capacity || !task.equals(nextTask()) || queue.peekFirst() != request) {
                cancellation.throwIfCancelled();
                wait(100);
            }
            cancellation.throwIfCancelled();
            if (Thread.interrupted()) throw new InterruptedException();
            active++;
            lastTask = task;
            queue.removeFirst();
            waiting.remove(task);
            if (!queue.isEmpty()) waiting.put(task, queue);
        } finally {
            if (queue.remove(request) && queue.isEmpty()) waiting.remove(task);
            notifyAll();
        }
    }

    private String nextTask() {
        for (String task : waiting.keySet())
            if (waiting.size() == 1 || !task.equals(lastTask)) return task;
        return null;
    }

    synchronized void release() {
        active--;
        notifyAll();
    }
}
