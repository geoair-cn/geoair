package cn.geoair.spi.env;

import java.lang.ref.WeakReference;

final class SpringEnvironmentProviderResolver {

    private static WeakReference<SpringEnvironment4Gir> weakReference =
            new WeakReference<>(null);

    private SpringEnvironmentProviderResolver() {}

    static SpringEnvironment4Gir getProvider() {
        return weakReference.get();
    }

    static void setProvider(SpringEnvironment4Gir provider) {
        weakReference = new WeakReference<>(provider);
    }
}
