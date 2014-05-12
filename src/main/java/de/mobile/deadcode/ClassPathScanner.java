package de.mobile.deadcode;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Throwables;
import com.google.common.reflect.ClassPath;

import java.io.IOException;

import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Iterables.transform;

public class ClassPathScanner {
    public static Iterable<String> getClassNames(ClassLoader classLoader, final String basePackage) {
        try {
            ClassPath classPath = ClassPath.from(classLoader);
            final String agentPackage = ClassPathScanner.class.getPackage().getName();
            return transform(
                    filter(classPath.getAllClasses(), new Predicate<ClassPath.ClassInfo>() {
                        @Override
                        public boolean apply(ClassPath.ClassInfo classInfo) {
                            String className = classInfo.getName();
                            return className.startsWith(basePackage) && !className.startsWith(agentPackage);
                        }
                    }),
                    new Function<ClassPath.ClassInfo, String>() {
                        @Override
                        public String apply(ClassPath.ClassInfo classInfo) {
                            return classInfo.getName();
                        }
                    }
            );
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }
}
