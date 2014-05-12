package de.mobile.deadcode;

import java.lang.instrument.Instrumentation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeadCodeAgent {
    private final static Logger logger = LoggerFactory.getLogger(DeadCodeAgent.class);
    
    private static Instrumentation inst;
 
    public static Instrumentation getInstrumentation() { return inst; }
 
    public static void premain(String agentArgs, Instrumentation inst) {
//        RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();
//        logger.info("Runtime: {}: {}", runtimeMxBean.getName(), runtimeMxBean.getInputArguments());
        logger.info("Starting agent with arguments " + agentArgs);
        
        System.out.println(DeadCodeAgent.class.getName() + ":test ");
        DeadCodeAgent.inst = inst;
    }
}
