package cn.geoair.map.tile.forge.fuser.encoding;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

/** 任务编码的只读绑定；跨线程须显式 capture/wrap，不能依赖线程池继承 ThreadLocal。 */
public final class TileEncodingContext {
    private static final ThreadLocal<TileEncodingContext> CURRENT = new ThreadLocal<>();
    private static final TileEncodingContext EMPTY =
            new TileEncodingContext(Collections.emptyMap());
    private final Map<String, TileEncodingPolicy> policies;

    private TileEncodingContext(Map<String, TileEncodingPolicy> policies) {
        Map<String, TileEncodingPolicy> copied = new HashMap<>();
        policies.forEach((name, value) -> copied.put(name, TileEncodingSnapshot.copy(value)));
        this.policies = Collections.unmodifiableMap(copied);
    }

    public static TileEncodingContext empty() {
        return EMPTY;
    }

    public static TileEncodingContext capture() {
        TileEncodingContext value = CURRENT.get();
        return value == null ? EMPTY : value;
    }

    public TileEncodingContext with(String cache, TileEncodingPolicy policy) {
        if (cache == null || cache.trim().isEmpty() || policy == null)
            throw new IllegalArgumentException("逻辑缓存名和编码策略不能为空");
        Map<String, TileEncodingPolicy> next = new HashMap<>(policies);
        next.put(cache, TileEncodingSnapshot.copy(policy));
        return new TileEncodingContext(next);
    }

    public TileEncodingPolicy policy(String cache) {
        TileEncodingPolicy value = policies.get(cache);
        return value == null ? null : TileEncodingSnapshot.copy(value);
    }

    /** 嵌套绑定只覆盖显式指定的缓存，退出时完整恢复外层绑定。 */
    public TileEncodingScope bind() {
        TileEncodingContext previous = CURRENT.get();
        Map<String, TileEncodingPolicy> merged = new HashMap<>();
        if (previous != null) merged.putAll(previous.policies);
        merged.putAll(policies);
        TileEncodingContext installed = new TileEncodingContext(merged);
        CURRENT.set(installed);
        return new TileEncodingScope(previous, installed);
    }

    public <T> Callable<T> wrap(Callable<T> work) {
        if (work == null) throw new IllegalArgumentException("工作回调不能为空");
        return () -> {
            try (TileEncodingScope scope = bind()) {
                return work.call();
            }
        };
    }

    public Runnable wrap(Runnable work) {
        if (work == null) throw new IllegalArgumentException("工作回调不能为空");
        return () -> {
            try (TileEncodingScope scope = bind()) {
                work.run();
            }
        };
    }

    static void restore(TileEncodingContext previous, TileEncodingContext installed) {
        if (CURRENT.get() != installed) throw new IllegalStateException("编码作用域须按嵌套顺序关闭");
        if (previous == null) CURRENT.remove();
        else CURRENT.set(previous);
    }
}
