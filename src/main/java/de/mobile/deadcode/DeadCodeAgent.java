package de.mobile.deadcode;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.log4j.MDC;
import org.apache.log4j.xml.DOMConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.lang.Runtime.getRuntime;
import static java.util.Collections.synchronizedSet;

public class DeadCodeAgent {
    private static final Logger logger;
    private static final ExecutorService executor;

    private static final int MAX_THREADS = 4;

    static {
        DOMConfigurator.configure(DeadCodeAgent.class.getResource("/log4j-deadcode-agent.xml"));
        logger = LoggerFactory.getLogger(DeadCodeAgent.class);

        executor = Executors.newFixedThreadPool(
                MAX_THREADS, 
                new ThreadFactoryBuilder()
                        .setDaemon(true)
                        .setNameFormat("DeadCodeDetection-%d")
                        .setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                            @Override
                            public void uncaughtException(Thread t, Throwable e) {
                                logger.error(e.getMessage(), e);
                            }
                        })
                        .build()
        );

        getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                executor.shutdownNow();
            }
        });
    }

    private static final Set<ScanEvent> SCAN_EVENTS = synchronizedSet(new HashSet<ScanEvent>());
    private static final Set<LoadEvent> LOAD_EVENTS = synchronizedSet(new HashSet<LoadEvent>());

    public static void premain(String agentArgs, Instrumentation instrumentation) {
        final String basePackage = agentArgs;
        if (isNullOrEmpty(basePackage)) {
            throw new IllegalArgumentException("No base package specified");
        }

        final Map<ClassLoader, Boolean> classLoaders = new HashMap<ClassLoader, Boolean>();
        instrumentation.addTransformer(new ClassFileTransformer() {
            public byte[] transform(final ClassLoader classLoader, String className,
                                    Class classBeingRedefined,
                                    ProtectionDomain protectionDomain,
                                    byte[] classfileBuffer) throws IllegalClassFormatException {

                if (!classLoaders.containsKey(classLoader)) {
                    classLoaders.put(classLoader, true);

                    executor.execute(new Runnable() {
                        @Override
                        public void run() {
                            for (String scannedClass : ClassPathScanner.getClassNames(classLoader, basePackage)) {
                                SCAN_EVENTS.add(new ScanEvent(classLoader, scannedClass));
                            }
                        }
                    });
                }

                final String loadedClass = className.replaceAll("/", ".");
                if (loadedClass.startsWith(basePackage)) {
                    executor.execute(new Runnable() {
                        @Override
                        public void run() {
                            LOAD_EVENTS.add(new LoadEvent(classLoader, loadedClass, getReferrers()));
                        }
                    });
                }

                return null;
            }
        });
    }

    private static void printScannedClasses(ClassLoader classLoader, String basePackage) {
        for (String className : ClassPathScanner.getClassNames(classLoader, basePackage)) {
            Map<String, Object> message = new HashMap<String, Object>();
            message.put("eventType", "deadCodeDetection");
            message.put("eventValue", "classScanned");
            message.put("className", className);
            message.put("classLoader", classLoader.getClass().getName());
            message.put("parentClassLoader", classLoader.getParent() != null ? classLoader.getParent().getClass().getName() : "null");
            log(message);
        }
    }

    private static final int MAX_REFERRERS = 20;

    private static void printLoadedClass(ClassLoader classLoader, String className) {
        Map<String, Object> message = new HashMap<String, Object>();
        message.put("eventType", "deadCodeDetection");
        message.put("eventValue", "classLoaded");
        message.put("className", className.replaceAll("/", "."));
        message.put("classLoader", classLoader.getClass().getName());
        message.put("parentClassLoader", classLoader.getParent() != null ? classLoader.getParent().getClass().getName() : "null");
        message.put("referrers", getReferrers());
        log(message);
    }

    private static List<String> getReferrers() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        List<String> referrers = new ArrayList<String>(MAX_REFERRERS);

        // gather class names referring to the loaded class; skip following entries:
        // - first entry which is Thread.getStackTrace()
        // - agent classes
        // - jvm classes
        // - container classes (Tomcat)
        for (int i = 1; i < stackTrace.length && referrers.size() < MAX_REFERRERS; ) {
            String referrer = stackTrace[i].getClassName();
            if (canBeSkipped(referrer)) {
                i++;
                continue;
            }
            if (referrer.startsWith("org.springframework")) {
                i = processSpring(stackTrace, referrers, i);
            } else {
                referrers.add(referrer);
                i++;
            }
        }
        return referrers;
    }

    private static boolean canBeSkipped(String referrer) {
        return referrer.startsWith(DeadCodeAgent.class.getPackage().getName())
                || referrer.startsWith("java")
                || referrer.startsWith("sun")
                || referrer.startsWith("org.apache.catalina");
    }

    private static int processSpring(StackTraceElement[] stackTrace, List<String> referrers, int i) {
        // compress consecutive Spring referrers into the pseudo-referrer "Spring"
        referrers.add("Spring");

        int j = i + 1;
        while (j < stackTrace.length) {
            String referrer = stackTrace[j].getClassName();
            if (!referrer.startsWith("org.springframework") && !canBeSkipped(referrer)) {
                break;
            }
            j++;
        }
        return j;
    }

    private static void log(Map<String, Object> message) {
        for (String key : message.keySet()) {
            MDC.put(key, message.get(key));
        }
        logger.info("{}", message);
    }
}
