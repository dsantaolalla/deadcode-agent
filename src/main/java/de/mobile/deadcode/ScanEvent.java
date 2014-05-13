package de.mobile.deadcode;

import com.google.common.base.Objects;

public final class ScanEvent {
    private final ClassLoader classLoader;
    private final String className;

    public ScanEvent(ClassLoader classLoader, String className) {
        this.classLoader = classLoader;
        this.className = className;
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    public String getClassName() {
        return className;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (object == null || getClass() != object.getClass()) {
            return false;
        }

        ScanEvent that = (ScanEvent) object;
        return Objects.equal(this.className, that.className);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(className);
    }
}
