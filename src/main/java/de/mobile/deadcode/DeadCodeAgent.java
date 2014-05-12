package de.mobile.deadcode;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeadCodeAgent {
    private static Logger logger;
    
    private static Instrumentation inst;
 
    public static Instrumentation getInstrumentation() { return inst; }
 
    public static void premain(String agentArgs, Instrumentation inst) {
        PropertyConfigurator.configure(DeadCodeAgent.class.getResource("/log4j-deadcode-agent.properties"));
        DeadCodeAgent.logger = LoggerFactory.getLogger(DeadCodeAgent.class);
        
        logger.info("Starting agent with arguments " + agentArgs);
        
        System.out.println(DeadCodeAgent.class.getName() + ":test ");
        DeadCodeAgent.inst = inst;
        
        final Map<ClassLoader, Boolean> classLoaders = new HashMap<ClassLoader, Boolean>();

//        final java.io.PrintStream out = System.out;
        inst.addTransformer(new ClassFileTransformer() {
            public byte[] transform(ClassLoader loader, String className,
                         Class classBeingRedefined,
                         ProtectionDomain protectionDomain,
                         byte[] classfileBuffer) throws IllegalClassFormatException {
                
                if (!classLoaders.containsKey(loader)) {
                    classLoaders.put(loader, true);
                    for (String c : ClassPathScanner.getClassNames(loader, "de.mobile")) {            
                        System.out.println("Existing class: " + c
                            + ", class loader: " + loader.getClass().getName()
                            + ", parent: " + loader.getParent());
                    }
                }

                if (className.startsWith("de/mobile/")) {
                    
                    
                    logger.info(className + " loaded by " + loader + " at " +
                            new java.util.Date());
//                    out.print(className + " loaded by " + loader + " at " + new java.util.Date());
//                    out.println(" in " + protectionDomain);
                    
//                    Thread.dumpStack();
                }

                return null;
            }
        });
    }
}
