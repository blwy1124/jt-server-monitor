package com.jt.plugins.extension;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.jt.plugins.common.annotation.ActionHandler;
import com.jt.plugins.common.file.PluginFileStorage;
import com.jt.plugins.common.http.ExtensionRequestParam;
import com.jt.plugins.common.log.PluginLogger;
import com.jt.plugins.common.result.ResultMsg;
import com.jt.plugins.utils.attach.AttachApiUtil;
import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;

import javax.management.remote.JMXConnector;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @BelongsProject: jt-server-monitor
 * @BelongsPackage: com.jt.plugins.extension
 * @Author: 别来无恙qb
 * @CreateTime: 2025-08-28  17:41
 * @Description: Jvm监控工具扩展实现
 * @Version: 1.0
 */

/**
 * JVM监控工具扩展实现
 * 使用统一的文件存储路径管理
 */
public class JvmToolsProviderImpl implements JvmToolsProvider {

    // 插件日志记录器
    private static final PluginLogger logger = PluginLogger.getLogger("jvm-monitor-plugin");

    // 插件文件存储管理器
    private static final PluginFileStorage fileStorage = PluginFileStorage.getStorage("jvm-monitor-plugin");

    // 缓存方法映射，避免每次反射查找
    private static final Map<String, Method> ACTION_HANDLERS = new HashMap<>();

    // JMX连接缓存
    private static final Map<String, JMXConnector> JMX_CONNECTORS = new ConcurrentHashMap<>();

    // 在类加载时初始化方法映射
    static {
        Method[] methods = JvmToolsProviderImpl.class.getDeclaredMethods();
        for (Method method : methods) {
            ActionHandler annotation = method.getAnnotation(ActionHandler.class);
            if (annotation != null) {
                ACTION_HANDLERS.put(annotation.value(), method);
            }
        }
    }

    @Override
    public ResultMsg<JSONObject> execute(ExtensionRequestParam extensionRequestParam) {
        String targetAction = extensionRequestParam.getTargetAction();
        logger.info("开始执行JVM监控操作: {}", targetAction);

        // 参数校验
        if (targetAction == null || targetAction.isEmpty()) {
            logger.warn("操作类型不能为空");
            return ResultMsg.fail("操作类型不能为空");
        }

        // 根据targetAction查找对应的处理方法
        Method handlerMethod = ACTION_HANDLERS.get(targetAction);

        if (handlerMethod != null) {
            try {
                // 调用对应的方法处理请求
                logger.info("调用处理方法: {}", handlerMethod.getName());
                ResultMsg<JSONObject> result = (ResultMsg<JSONObject>) handlerMethod.invoke(this, extensionRequestParam);
                logger.info("JVM监控操作执行完成: {}", targetAction);
                return result;
            } catch (Exception e) {
                logger.error("执行JVM监控操作失败: {}", targetAction, e);
                // 区分不同类型的异常并抛出相应的错误信息
                Throwable cause = e.getCause();
                if (cause != null) {
                    if (cause instanceof SecurityException) {
                        return ResultMsg.fail("权限不足，无法访问目标JVM进程：" + cause.getMessage());
                    } else if (cause instanceof IllegalArgumentException) {
                        return ResultMsg.fail("参数错误：" + cause.getMessage());
                    } else if (cause instanceof IllegalStateException) {
                        return ResultMsg.fail("状态错误：" + cause.getMessage());
                    } else {
                        return ResultMsg.fail("执行操作失败：" + cause.getMessage());
                    }
                }
                return ResultMsg.fail("执行操作失败：" + e.getMessage());
            }
        }

        logger.warn("不支持的操作类型: {}", targetAction);
        return ResultMsg.fail("不支持的操作类型：" + targetAction);
    }

    @ActionHandler("ping")
    private ResultMsg<JSONObject> handlePing(ExtensionRequestParam request) {
        logger.debug("处理ping请求");
        try {
            return ResultMsg.success(new JSONObject(), "Pong");
        } catch (Exception e) {
            logger.error("处理ping请求失败", e);
            return ResultMsg.fail("处理ping请求失败：" + e.getMessage());
        }
    }

    /**
     * 获取当前服务器所有Java进程信息（PID、进程名称等）
     */
    @ActionHandler("getJavaProcesses")
    private ResultMsg<JSONObject> handleGetJavaProcesses(ExtensionRequestParam request) {
        logger.info("开始获取Java进程信息");
        try {
            // 使用Attach API获取所有可连接的Java进程
            List<VirtualMachineDescriptor> vms = VirtualMachine.list();
            JSONArray processes = new JSONArray();

            for (com.sun.tools.attach.VirtualMachineDescriptor vm : vms) {
                JSONObject processInfo = new JSONObject();
                processInfo.put("pid", vm.id()); // 进程ID
                processInfo.put("displayName", vm.displayName()); // 进程名称（通常是主类名或JAR路径）
                processes.add(processInfo);
            }

            JSONObject result = new JSONObject();
            result.put("processes", processes);
            result.put("total", processes.size()); // 进程总数

            logger.info("成功获取到 {} 个Java进程信息", processes.size());
            return ResultMsg.success(result, "Java进程信息获取成功");
        } catch (Exception e) {
            logger.error("获取Java进程信息失败", e);
            // 根据异常类型返回更具体的错误信息
            if (e instanceof SecurityException) {
                return ResultMsg.fail("权限不足，无法获取Java进程列表：" + e.getMessage());
            } else if (e instanceof RuntimeException) {
                return ResultMsg.fail("获取Java进程列表时发生运行时错误：" + e.getMessage());
            } else {
                return ResultMsg.fail("获取Java进程信息失败：" + e.getMessage());
            }
        }
    }

    /**
     * 获取目标JVM进程详细信息（堆内存/线程/CPU等）
     * 参数说明：
     * - pid: 目标进程ID（特殊值"self"表示当前进程）
     */
    @ActionHandler("getJvmInfo")
    private ResultMsg<JSONObject> handleGetJvmInfo(ExtensionRequestParam request) {
        String pid = request.getParameter("pid");
        logger.info("开始获取JVM信息，PID: {}", pid);

        if (pid == null || pid.isEmpty()) {
            logger.warn("参数错误：PID不能为空");
            return ResultMsg.fail("参数错误：PID不能为空");
        }

        try {
            // 处理当前进程特殊标识
            if ("self".equals(pid)) {
                pid = getCurrentProcessId();
                logger.debug("转换self为实际PID: {}", pid);
            }

            // 调用AttachApiUtil获取JVM信息（包含堆内存分代、线程、CPU等）
            JSONObject jvmInfo = AttachApiUtil.getJvmInfo(pid);
            logger.info("成功获取JVM信息，PID: {}", pid);
            return ResultMsg.success(jvmInfo, "JVM信息获取成功");
        } catch (Exception e) {
            logger.error("获取JVM信息失败，PID: {}", pid, e);
            // 根据异常类型返回更具体的错误信息
            if (e instanceof SecurityException) {
                return ResultMsg.fail("权限不足，无法连接到目标JVM进程：" + e.getMessage());
            } else if (e instanceof IllegalArgumentException) {
                return ResultMsg.fail("无效的PID参数：" + e.getMessage());
            } else if (e instanceof IllegalStateException) {
                return ResultMsg.fail("无法连接到目标JVM进程，可能进程已终止：" + e.getMessage());
            } else {
                return ResultMsg.fail("获取JVM信息失败：" + e.getMessage());
            }
        }
    }

    /**
     * 触发目标JVM进程GC
     * 参数说明：
     * - pid: 目标进程ID（特殊值"self"表示当前进程）
     */
    @ActionHandler("triggerGC")
    private ResultMsg<JSONObject> handleTriggerGC(ExtensionRequestParam request) {
        String pid = request.getParameter("pid");
        logger.info("开始触发GC，PID: {}", pid);

        if (pid == null || pid.isEmpty()) {
            logger.warn("参数错误：PID不能为空");
            return ResultMsg.fail("参数错误：PID不能为空");
        }

        try {
            // 处理当前进程特殊标识
            if ("self".equals(pid)) {
                pid = getCurrentProcessId();
                logger.debug("转换self为实际PID: {}", pid);
            }

            // 调用AttachApiUtil触发GC
            AttachApiUtil.triggerGC(pid);
            logger.info("成功触发GC，PID: {}", pid);
            return ResultMsg.success(new JSONObject(), "GC触发成功");
        } catch (Exception e) {
            logger.error("触发GC失败，PID: {}", pid, e);
            // 根据异常类型返回更具体的错误信息
            if (e instanceof SecurityException) {
                return ResultMsg.fail("权限不足，无法触发目标JVM进程的GC：" + e.getMessage());
            } else if (e instanceof IllegalArgumentException) {
                return ResultMsg.fail("无效的PID参数：" + e.getMessage());
            } else if (e instanceof IllegalStateException) {
                return ResultMsg.fail("无法连接到目标JVM进程，可能进程已终止：" + e.getMessage());
            } else {
                return ResultMsg.fail("触发GC失败：" + e.getMessage());
            }
        }
    }

    /**
     * Dump目标JVM进程堆内存
     * 参数说明：
     * - pid: 目标进程ID（特殊值"self"表示当前进程）
     * - filePath: 堆文件保存路径（如"D:/dump/heap.bin"，未传入则使用默认路径）
     * - live: 是否只Dump存活对象（true/false，默认true）
     */
    @ActionHandler("dumpHeap")
    private ResultMsg<JSONObject> handleDumpHeap(ExtensionRequestParam request) {
        String pid = request.getParameter("pid");
        String filePath = request.getParameter("filePath");
        String liveStr = request.getParameter("live", "true"); // 默认只Dump存活对象
        boolean live = Boolean.parseBoolean(liveStr);

        logger.info("开始执行堆转储，PID: {}, Live: {}", pid, live);

        // 参数校验：PID不能为空
        if (pid == null || pid.isEmpty()) {
            logger.warn("参数错误：PID不能为空");
            return ResultMsg.fail("参数错误：PID不能为空");
        }

        try {
            // 处理当前进程特殊标识，获取真实PID
            String actualPid = "self".equals(pid) ? getCurrentProcessId() : pid;
            logger.debug("实际PID: {}", actualPid);

            // 若未传入filePath，生成默认路径（插件存储目录 + heap.hprof）
            if (filePath == null || filePath.isEmpty()) {
                String timestamp = String.valueOf(System.currentTimeMillis());
                String defaultFileName = "heap_dump_" + actualPid + "_" + timestamp + ".hprof";
                // 使用插件文件存储管理器创建文件
                File dumpFile = fileStorage.createFile(defaultFileName);
                filePath = dumpFile.getAbsolutePath();
                logger.debug("使用默认文件路径: {}", filePath);
            }

            // 确保目标目录存在（若目录不存在则创建）
            File dumpFile = new File(filePath);
            File parentDir = dumpFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs(); // 创建多级目录
                logger.debug("创建目录: {}", parentDir.getAbsolutePath());
            }

            // 调用AttachApiUtil执行堆Dump
            String resultPath = AttachApiUtil.dumpHeap(actualPid, filePath, live);
            JSONObject data = new JSONObject();
            data.put("dumpPath", resultPath);
            logger.info("堆转储完成，文件路径: {}", resultPath);
            return ResultMsg.success(data, "堆内存Dump成功，文件路径：" + resultPath);
        } catch (Exception e) {
            logger.error("堆内存Dump失败", e);
            // 根据异常类型返回更具体的错误信息
            if (e instanceof SecurityException) {
                return ResultMsg.fail("权限不足，无法执行堆转储：" + e.getMessage());
            } else if (e instanceof IllegalArgumentException) {
                return ResultMsg.fail("参数错误：" + e.getMessage());
            } else if (e instanceof IllegalStateException) {
                return ResultMsg.fail("无法连接到目标JVM进程，可能进程已终止：" + e.getMessage());
            } else if (e instanceof java.io.IOException) {
                return ResultMsg.fail("文件操作失败：" + e.getMessage());
            } else {
                return ResultMsg.fail("堆内存Dump失败：" + e.getMessage());
            }
        }
    }

    /**
     * 获取目标JVM进程活跃线程数量
     * 参数说明：
     * - pid: 目标进程ID（特殊值"self"表示当前进程）
     */
    @ActionHandler("getActiveThreadCount")
    private ResultMsg<JSONObject> handleGetActiveThreadCount(ExtensionRequestParam request) {
        String pid = request.getParameter("pid");
        logger.info("开始获取活跃线程数，PID: {}", pid);

        if (pid == null || pid.isEmpty()) {
            logger.warn("参数错误：PID不能为空");
            return ResultMsg.fail("参数错误：PID不能为空");
        }

        try {
            // 处理当前进程特殊标识
            if ("self".equals(pid)) {
                pid = getCurrentProcessId();
                logger.debug("转换self为实际PID: {}", pid);
            }

            // 调用AttachApiUtil获取JVM信息，提取线程数量
            JSONObject jvmInfo = AttachApiUtil.getJvmInfo(pid);
            int activeThreadCount = jvmInfo.getIntValue("threadCount"); // 从JVM信息中提取线程数

            JSONObject result = new JSONObject();
            result.put("activeThreadCount", activeThreadCount);
            logger.info("成功获取活跃线程数: {}，PID: {}", activeThreadCount, pid);
            return ResultMsg.success(result, "活跃线程数量获取成功");
        } catch (Exception e) {
            logger.error("获取活跃线程数量失败，PID: {}", pid, e);
            // 根据异常类型返回更具体的错误信息
            if (e instanceof SecurityException) {
                return ResultMsg.fail("权限不足，无法获取线程信息：" + e.getMessage());
            } else if (e instanceof IllegalArgumentException) {
                return ResultMsg.fail("无效的PID参数：" + e.getMessage());
            } else if (e instanceof IllegalStateException) {
                return ResultMsg.fail("无法连接到目标JVM进程，可能进程已终止：" + e.getMessage());
            } else {
                return ResultMsg.fail("获取活跃线程数量失败：" + e.getMessage());
            }
        }
    }

    /**
     * 获取某个时间段的GC信息日志
     * 支持两种时间格式：
     * 1. 相对时间：hoursAgo（多少小时以前）
     * 2. 绝对时间：startTime和endTime（具体时间戳）
     * 
     * 参数说明：
     * - pid: 目标进程ID（特殊值"self"表示当前进程）
     * - hoursAgo: 相对时间，多少小时以前（优先级高于startTime/endTime）
     * - startTime: 开始时间戳（毫秒）
     * - endTime: 结束时间戳（毫秒，默认为当前时间）
     */
    @ActionHandler("getGcLogInfo")
    private ResultMsg<JSONObject> handleGetGcLogInfo(ExtensionRequestParam request) {
        String pid = request.getParameter("pid");
        String hoursAgoStr = request.getParameter("hoursAgo");
        String startTimeStr = request.getParameter("startTime");
        String endTimeStr = request.getParameter("endTime");
        
        long currentTime = System.currentTimeMillis();
        long startTime, endTime;
        
        // 优先处理相对时间格式
        if (hoursAgoStr != null && !hoursAgoStr.isEmpty()) {
            try {
                int hoursAgo = Integer.parseInt(hoursAgoStr);
                if (hoursAgo <= 0) {
                    return ResultMsg.fail("参数错误：hoursAgo必须大于0");
                }
                endTime = currentTime;
                startTime = currentTime - (hoursAgo * 3600000L); // 转换为毫秒
                logger.info("使用相对时间格式：过去{}小时", hoursAgo);
            } catch (NumberFormatException e) {
                return ResultMsg.fail("参数错误：hoursAgo必须是有效的数字");
            }
        } else {
            // 处理绝对时间格式
            long defaultStartTime = currentTime - 3600000; // 默认1小时前
            long defaultEndTime = currentTime;
            
            startTime = startTimeStr != null ? Long.parseLong(startTimeStr) : defaultStartTime;
            endTime = endTimeStr != null ? Long.parseLong(endTimeStr) : defaultEndTime;
            
            logger.info("使用绝对时间格式：{} - {}", startTime, endTime);
        }
        
        // 验证时间范围合理性
        if (startTime >= endTime) {
            return ResultMsg.fail("参数错误：开始时间必须小于结束时间");
        }
        
        if (endTime > currentTime + 60000) { // 允许1分钟的误差
            return ResultMsg.fail("参数错误：结束时间不能超过当前时间");
        }
        
        logger.info("开始获取GC日志信息，PID: {}, 时间范围: {} - {}", pid, startTime, endTime);
        
        if (pid == null || pid.isEmpty()) {
            logger.warn("参数错误：PID不能为空");
            return ResultMsg.fail("参数错误：PID不能为空");
        }
        
        try {
            // 处理当前进程特殊标识
            if ("self".equals(pid)) {
                pid = getCurrentProcessId();
                logger.debug("转换self为实际PID: {}", pid);
            }
            
            // 调用AttachApiUtil获取GC信息
            JSONObject gcInfo = AttachApiUtil.getGcLogInfo(pid, startTime, endTime);
            
            // 添加时间范围描述
            JSONObject timeRange = gcInfo.getJSONObject("timeRange");
            timeRange.put("description", generateTimeRangeDescription(startTime, endTime));
            
            logger.info("成功获取GC日志信息，PID: {}", pid);
            return ResultMsg.success(gcInfo, "GC日志信息获取成功");
            
        } catch (Exception e) {
            logger.error("获取GC日志信息失败，PID: {}", pid, e);
            if (e instanceof SecurityException) {
                return ResultMsg.fail("权限不足，无法获取GC信息：" + e.getMessage());
            } else if (e instanceof IllegalArgumentException) {
                return ResultMsg.fail("无效的PID参数：" + e.getMessage());
            } else if (e instanceof IllegalStateException) {
                return ResultMsg.fail("无法连接到目标JVM进程，可能进程已终止：" + e.getMessage());
            } else {
                return ResultMsg.fail("获取GC日志信息失败：" + e.getMessage());
            }
        }
    }

    /**
     * 生成时间范围描述
     */
    private String generateTimeRangeDescription(long startTime, long endTime) {
        java.time.Instant startInstant = java.time.Instant.ofEpochMilli(startTime);
        java.time.Instant endInstant = java.time.Instant.ofEpochMilli(endTime);
        
        java.time.ZoneId zoneId = java.time.ZoneId.systemDefault();
        java.time.format.DateTimeFormatter formatter = 
            java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        
        String startStr = java.time.ZonedDateTime.ofInstant(startInstant, zoneId).format(formatter);
        String endStr = java.time.ZonedDateTime.ofInstant(endInstant, zoneId).format(formatter);
        
        long durationHours = (endTime - startTime) / 3600000;
        long durationMinutes = ((endTime - startTime) % 3600000) / 60000;
        
        return String.format("%s 到 %s (持续时间: %d小时%d分钟)", 
                        startStr, endStr, durationHours, durationMinutes);
    }

    /**
     * 获取当前进程ID
     */
    private String getCurrentProcessId() {
        try {
            // 从运行时MXBean中提取进程ID（格式：pid@hostname）
            String runtimeName = ManagementFactory.getRuntimeMXBean().getName();
            return runtimeName.split("@")[0];
        } catch (Exception e) {
            logger.error("获取当前进程ID失败", e);
            throw new RuntimeException("无法获取当前进程ID", e);
        }
    }
}
