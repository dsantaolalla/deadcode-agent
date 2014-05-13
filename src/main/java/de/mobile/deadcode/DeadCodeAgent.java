package de.mobile.deadcode;

import org.apache.log4j.MDC;
import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Strings.isNullOrEmpty;

public class DeadCodeAgent {
    private static Logger logger;

    public static void premain(String agentArgs, Instrumentation instrumentation) {
        final String basePackage = agentArgs;
        if (isNullOrEmpty(basePackage)) {
            throw new IllegalArgumentException("No base package specified");
        }

        final String baseDirectory = basePackage.replaceAll("\\.", "/");

        PropertyConfigurator.configure(DeadCodeAgent.class.getResource("/log4j-deadcode-agent.properties"));
        logger = LoggerFactory.getLogger(DeadCodeAgent.class);

        final Map<ClassLoader, Boolean> classLoaders = new HashMap<ClassLoader, Boolean>();
        instrumentation.addTransformer(new ClassFileTransformer() {
            public byte[] transform(ClassLoader classLoader, String className,
                                    Class classBeingRedefined,
                                    ProtectionDomain protectionDomain,
                                    byte[] classfileBuffer) throws IllegalClassFormatException {

                if (!classLoaders.containsKey(classLoader)) {
                    classLoaders.put(classLoader, true);
                    printScannedClasses(classLoader, basePackage);
                }

                if (className.startsWith(baseDirectory)) {
                    printLoadedClass(classLoader, className);
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

        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        List<String> referrers = new ArrayList<String>(MAX_REFERRERS);
        for (int i = 0; i < stackTrace.length && i < MAX_REFERRERS; i++) {
            referrers.add(stackTrace[i].getClassName());
        }
        message.put("referrers", referrers);

        /*
        referrers=[
        java.lang.Thread,
        de.mobile.deadcode.DeadCodeAgent, 
        de.mobile.deadcode.DeadCodeAgent,
        de.mobile.deadcode.DeadCodeAgent$1,
        sun.instrument.TransformerManager]
         */

        log(message);
    }

    private static void log(Map<String, Object> message) {
        for (String key : message.keySet()) {
            MDC.put(key, message.get(key));
        }
        logger.info("{}", message);
    }
}
