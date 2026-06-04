package cn.geoair.comp.dynamic.ds.tx;

import cn.geoair.comp.dynamic.ds.tx.enums.IsolationLevel;
import cn.geoair.comp.dynamic.ds.tx.enums.Propagation;

import java.util.function.Supplier;

public class GirDsJdbcTxBuilder {
    private final GirDefaultIDsTxTemplate template;
    private Propagation propagation = Propagation.REQUIRED;
    private IsolationLevel isolation = IsolationLevel.REPEATABLE_READ;
    private boolean readOnly = false;
    private Class<? extends Throwable>[] rollFor = new Class[]{RuntimeException.class};
    private Class<? extends Throwable>[] noRollFor = new Class[0];

    public GirDsJdbcTxBuilder(GirDefaultIDsTxTemplate template) {
        this.template = template;
    }

    public GirDsJdbcTxBuilder propagation(Propagation propagation) {
        this.propagation = propagation;
        return this;
    }

    public GirDsJdbcTxBuilder isolation(IsolationLevel level) {
        this.isolation = level;
        return this;
    }

    public GirDsJdbcTxBuilder readOnly(boolean readOnly) {
        this.readOnly = readOnly;
        return this;
    }

    @SafeVarargs
    public final GirDsJdbcTxBuilder rollFor(Class<? extends Throwable>... rollFor) {
        this.rollFor = rollFor;
        return this;
    }

    @SafeVarargs
    public final GirDsJdbcTxBuilder noRollFor(Class<? extends Throwable>... noRollFor) {
        this.noRollFor = noRollFor;
        return this;
    }

    // 无参无返回
    public void run(Runnable action) {
        template.doTx(propagation, isolation, readOnly, rollFor, noRollFor, action, null);
    }

    // 带参无返回
    public <P> void run(TxAction<P> action, P param) {
        template.doTx(propagation, isolation, readOnly, rollFor, noRollFor, action, param);
    }

    // 无参有返回
    public <T> T call(Supplier<T> supplier) {
        return template.doTx(propagation, isolation, readOnly, rollFor, noRollFor, supplier, null);
    }

    // 带参有返回
    public <P, R> R call(TxFunc<P, R> func, P param) {
        return template.doTx(propagation, isolation, readOnly, rollFor, noRollFor, func, param);
    }
}
