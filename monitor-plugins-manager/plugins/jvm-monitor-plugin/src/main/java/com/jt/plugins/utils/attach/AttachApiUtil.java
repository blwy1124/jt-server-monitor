package com.jt.plugins.utils.attach;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.sun.management.HotSpotDiagnosticMXBean;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.File;
import java.lang.management.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class AttachApiUtil {

    /**
     * 建立 JMX 连接
     */
    private static MBeanServerConnection connectViaAttach(String pid) throws Exception {
        // 1. attach 到目标 JVM
        com.sun.tools.attach.VirtualMachine vm = com.sun.tools.attach.VirtualMachine.attach(pid);

        try {
            // 2. 确保 management agent 已加载
            Properties agentProps = vm.getAgentProperties();
            String address = agentProps.getProperty("com.sun.management.jmxremote.localConnectorAddress");
            if (address == null) {
                String javaHome = vm.getSystemProperties().getProperty("java.home");
                String agent = javaHome + File.separator + "lib" + File.separator + "management-agent.jar";
                vm.loadAgent(agent);
                agentProps = vm.getAgentProperties();
                address = agentProps.getProperty("com.sun.management.jmxremote.localConnectorAddress");
            }

            // 3. 建立 JMX 连接
            JMXServiceURL jmxUrl = new JMXServiceURL(address);
            JMXConnector connector = JMXConnectorFactory.connect(jmxUrl);
            return connector.getMBeanServerConnection();
        } finally {
            vm.detach();
        }
    }

    /**
     * 获取详细 JVM 信息（堆内存、线程、CPU、GC）
     */
    public static JSONObject getJvmInfo(String pid) throws Exception {
        MBeanServerConnection mbsc = connectViaAttach(pid);

        JSONObject result = new JSONObject();

        // 1. 堆 & 非堆
        MemoryMXBean memoryMXBean = ManagementFactory.newPlatformMXBeanProxy(
                mbsc, ManagementFactory.MEMORY_MXBEAN_NAME, MemoryMXBean.class);
        result.put("heapMemory", memoryMXBean.getHeapMemoryUsage());
        result.put("nonHeapMemory", memoryMXBean.getNonHeapMemoryUsage());

        // 2. 各内存池
        JSONArray pools = new JSONArray();
        for (MemoryPoolMXBean pool : ManagementFactory.getPlatformMXBeans(mbsc, MemoryPoolMXBean.class)) {
            JSONObject poolObj = new JSONObject();
            poolObj.put("name", pool.getName());
            poolObj.put("type", pool.getType().toString());
            poolObj.put("usage", pool.getUsage());
            pools.add(poolObj);
        }
        result.put("memoryPools", pools);

        // 3. GC 信息
        JSONArray gcs = new JSONArray();
        for (GarbageCollectorMXBean gc : ManagementFactory.getPlatformMXBeans(mbsc, GarbageCollectorMXBean.class)) {
            JSONObject gcObj = new JSONObject();
            gcObj.put("name", gc.getName());
            gcObj.put("count", gc.getCollectionCount());
            gcObj.put("time", gc.getCollectionTime());
            gcs.add(gcObj);
        }
        result.put("garbageCollectors", gcs);

        // 4. 线程信息
        ThreadMXBean threadMXBean = ManagementFactory.newPlatformMXBeanProxy(
                mbsc, ManagementFactory.THREAD_MXBEAN_NAME, ThreadMXBean.class);
        result.put("threadCount", threadMXBean.getThreadCount());
        result.put("peakThreadCount", threadMXBean.getPeakThreadCount());

        // 5. CPU 信息
        OperatingSystemMXBean osBean = ManagementFactory.newPlatformMXBeanProxy(
                mbsc, ManagementFactory.OPERATING_SYSTEM_MXBEAN_NAME, OperatingSystemMXBean.class);
        result.put("osName", osBean.getName());
        result.put("arch", osBean.getArch());
        result.put("availableProcessors", osBean.getAvailableProcessors());
        result.put("systemLoadAverage", osBean.getSystemLoadAverage());

        return result;
    }

    /**
     * 执行 GC
     */
    public static void triggerGC(String pid) throws Exception {
        MBeanServerConnection mbsc = connectViaAttach(pid);
        MemoryMXBean memoryMXBean = ManagementFactory.newPlatformMXBeanProxy(
                mbsc, ManagementFactory.MEMORY_MXBEAN_NAME, MemoryMXBean.class);
        memoryMXBean.gc();
    }

    /**
     * Dump 堆
     */
    public static String dumpHeap(String pid, String filePath, boolean live) throws Exception {
        MBeanServerConnection mbsc = connectViaAttach(pid);
        HotSpotDiagnosticMXBean hsDiag = ManagementFactory.newPlatformMXBeanProxy(
                mbsc, "com.sun.management:type=HotSpotDiagnostic", HotSpotDiagnosticMXBean.class);
        hsDiag.dumpHeap(filePath, live);
        return filePath;
    }

    /**
     * 获取线程转储
     */
    public static JSONArray getThreadDump(String pid) throws Exception {
        MBeanServerConnection mbsc = connectViaAttach(pid);
        ThreadMXBean threadMXBean = ManagementFactory.newPlatformMXBeanProxy(
                mbsc, ManagementFactory.THREAD_MXBEAN_NAME, ThreadMXBean.class);

        long[] ids = threadMXBean.getAllThreadIds();
        ThreadInfo[] infos = threadMXBean.getThreadInfo(ids, 100);

        JSONArray arr = new JSONArray();
        for (ThreadInfo info : infos) {
            if (info == null) continue;
            JSONObject obj = new JSONObject();
            obj.put("threadId", info.getThreadId());
            obj.put("threadName", info.getThreadName());
            obj.put("state", info.getThreadState().toString());
            obj.put("stackTrace", info.getStackTrace());
            arr.add(obj);
        }
        return arr;
    }

}
