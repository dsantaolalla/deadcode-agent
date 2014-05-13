package de.mobile.deadcode;

import com.google.common.base.Objects;

import java.util.List;

public final class LoadEvent {
    private final ClassLoader classLoader;
    private final String className;
    private final List<String> referrers;

    public LoadEvent(ClassLoader classLoader, String className, List<String> referrers) {
        this.classLoader = classLoader;
        this.className = className;
        this.referrers = referrers;
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    public String getClassName() {
        return className;
    }

    public List<String> getReferrers() {
        return referrers;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (object == null || getClass() != object.getClass()) {
            return false;
        }

        LoadEvent that = (LoadEvent) object;
        return Objects.equal(this.className, that.className);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(className);
    }
}
