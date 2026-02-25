package com.jt.plugins.utils;


import com.sun.management.OperatingSystemMXBean;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.concurrent.TimeUnit;

/**
 * @ClassName SystemInfo
 * @Author ：BLWY-1124
 * @Date ：2024-08-22 9:23
 * @Description：系统信息工具类
 * @Version: 1.0
 */
public class SystemUtils {
    /**
     * 操作系统
     * @return
     */
    public static String getOperatingSystem() {
        return System.getProperty("os.name");
    }

    /**
     * 总内存 KB
     * @return
     */
    public static long getTotalMemory() {
        long totalMemoryMB = Runtime.getRuntime().totalMemory() / 1024;
        return totalMemoryMB;
    }

    // 获取程序运行时长（毫秒）
    public static long getRuntimeDuration() {
        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        return runtimeMXBean.getUptime();
    }

    // 格式化运行时长为HH:mm:ss格式
    public static String formatDuration(long durationMillis) {
        long days = TimeUnit.MILLISECONDS.toDays(durationMillis);
        long hours = TimeUnit.MILLISECONDS.toHours(durationMillis);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(durationMillis) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(durationMillis) % 60;
        return String.format("%d天 %02d:%02d:%02d", days, hours, minutes, seconds);
    }

    /**
     * 内存使用率 %
     * @return
     */
    public static double getMemoryUsagePercentage() {
        // 获取操作系统MXBean实例
        OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);

        // 获取系统总物理内存
        long totalMemory = osBean.getTotalPhysicalMemorySize();

        // 获取系统可用物理内存
        long freeMemory = osBean.getFreePhysicalMemorySize();

        // 计算已使用内存
        long usedMemory = totalMemory - freeMemory;

        // 计算内存使用率百分比
        double memoryUsagePercentage = ((double) usedMemory / totalMemory) * 100;

        // 返回值是一个百分比（0-100），表示内存使用率
        return memoryUsagePercentage;
    }

    /**
     * 一段时间cpu使用率
     * @param delay 一段时间 毫秒 不传默认一秒
     * @return
     */
    public static double getCpuUsagePercentage(long delay) {
        OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);

        // 获取进程的CPU使用率
        double cpuLoad = osBean.getProcessCpuLoad() * 100;

        // 返回值是一个百分比（0-100），表示进程的CPU使用率
        return Math.round(cpuLoad * 100.0) / 100.0;
    }

    /**
     * 硬盘 KB
     * @return
     */
    public static long getDiskSpace() {
        String currentPath = System.getProperty("user.dir");
        File file = new File(currentPath);

        // 获取磁盘的总空间、可用空间和已用空间
        long totalSpace = file.getTotalSpace(); // 总空间，单位为字节
        return totalSpace / 1024;
    }

    /**
     * 磁盘使用率
     * @return
     */
    public static double getDiskUsagePercentage() {
        String currentPath = System.getProperty("user.dir");
        File file = new File(currentPath);

        // 获取磁盘的总空间、可用空间和已用空间
        long totalSpace = file.getTotalSpace(); // 总空间，单位为字节
        long freeSpace = file.getFreeSpace();   // 可用空间，单位为字节
        // 计算磁盘使用率

        return ((double)(totalSpace - freeSpace) / totalSpace) * 100;
    }

    public static String getLocalIPv4Address() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                // 排除虚拟接口，如lo（回环接口）
                if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                    continue;
                }

                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    // 检查地址是否是IPv4且不是回环地址
                    if (!address.isLoopbackAddress() && address.getHostAddress().indexOf(':') == -1) {
                        return address.getHostAddress();
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return null; // 如果没有找到IPv4地址，则返回null
    }


}
