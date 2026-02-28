package com.jt.plugins.utils.attach;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.sun.management.HotSpotDiagnosticMXBean;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.File;
import java.lang.management.*;
import java.util.List;
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
     * 获取指定时间段的GC日志信息
     * @param pid 进程ID
     * @param startTime 开始时间戳（毫秒）
     * @param endTime 结束时间戳（毫秒）
     * @return GC日志信息JSON对象
     */
    public static JSONObject getGcLogInfo(String pid, long startTime, long endTime) throws Exception {
        MBeanServerConnection mbsc = connectViaAttach(pid);
        
        JSONObject result = new JSONObject();
        JSONArray gcEvents = new JSONArray();
        
        // 获取所有GarbageCollectorMXBean
        List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getPlatformMXBeans(mbsc, GarbageCollectorMXBean.class);
        
        long totalGcCount = 0;
        long totalGcTime = 0;
        
        for (GarbageCollectorMXBean gcBean : gcBeans) {
            JSONObject gcStat = new JSONObject();
            gcStat.put("name", gcBean.getName());
            gcStat.put("collectionCount", gcBean.getCollectionCount());
            gcStat.put("collectionTime", gcBean.getCollectionTime());
            gcStat.put("avgCollectionTime", 
                  gcBean.getCollectionCount() > 0 ? 
                  (double) gcBean.getCollectionTime() / gcBean.getCollectionCount() : 0);
        
            gcEvents.add(gcStat);
        
            totalGcCount += gcBean.getCollectionCount();
            totalGcTime += gcBean.getCollectionTime();
        }
        
        // 获取内存池信息
        JSONArray memoryPools = new JSONArray();
        List<MemoryPoolMXBean> poolBeans = ManagementFactory.getPlatformMXBeans(mbsc, MemoryPoolMXBean.class);
        
        for (MemoryPoolMXBean poolBean : poolBeans) {
            JSONObject poolInfo = new JSONObject();
            poolInfo.put("name", poolBean.getName());
            poolInfo.put("type", poolBean.getType().toString());
        
            // 获取内存使用情况
            MemoryUsage usage = poolBean.getUsage();
            if (usage != null) {
                poolInfo.put("used", usage.getUsed());
                poolInfo.put("max", usage.getMax());
                poolInfo.put("committed", usage.getCommitted());
                poolInfo.put("init", usage.getInit());
                poolInfo.put("usagePercentage", 
                        usage.getMax() > 0 ? (double) usage.getUsed() / usage.getMax() * 100 : 0);
            }
        
            memoryPools.add(poolInfo);
        }
        
        // 获取操作系统信息
        OperatingSystemMXBean osBean = ManagementFactory.newPlatformMXBeanProxy(
            mbsc, ManagementFactory.OPERATING_SYSTEM_MXBEAN_NAME, OperatingSystemMXBean.class);
    
        // 组装结果
        result.put("pid", pid);
        result.put("timeRange", new JSONObject()
        .fluentPut("startTime", startTime)
        .fluentPut("endTime", endTime)
        .fluentPut("duration", endTime - startTime));
        result.put("gcStatistics", gcEvents);
        result.put("memoryPools", memoryPools);
        result.put("systemInfo", new JSONObject()
        .fluentPut("osName", osBean.getName())
        .fluentPut("arch", osBean.getArch())
        .fluentPut("availableProcessors", osBean.getAvailableProcessors())
        .fluentPut("systemLoadAverage", osBean.getSystemLoadAverage()));
        result.put("summary", new JSONObject()
        .fluentPut("totalGcCollections", totalGcCount)
        .fluentPut("totalGcTimeMs", totalGcTime)
        .fluentPut("avgGcIntervalMs", totalGcCount > 0 ? (endTime - startTime) / totalGcCount : 0));
    
        return result;
    }

    /**
     * 获取线程转储（保持原有方法）
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
