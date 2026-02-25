package com.jt.plugins.service;

import com.alibaba.fastjson.JSONObject;
import com.jt.plugins.utils.Result;
import com.jt.plugins.utils.SystemUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.dom4j.*;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @ClassName MainService
 * @Author ：别来无恙qb
 * @Date ：2024-08-22 8:37
 * @Description：
 * @Version: 1.0
 */
@Service
@Slf4j
public class MainService {

    public Result<JSONObject> getSystemInfo() throws DocumentException, IOException {
        JSONObject systemInfo = new JSONObject();
        systemInfo.put("system", SystemUtils.getOperatingSystem());
        double cpuUsagePercentage = SystemUtils.getCpuUsagePercentage(1000);
        if(cpuUsagePercentage == -100 ){
            log.error("无法获取本系统的cpu使用状态，不支持获取或者系统负载过高获取失败");
        }
        systemInfo.put("cpu", cpuUsagePercentage > 0 ? cpuUsagePercentage : 0 );
        systemInfo.put("memory", Math.round(SystemUtils.getMemoryUsagePercentage() * 100.0) / 100.0);
        systemInfo.put("disk",Math.round(SystemUtils.getDiskUsagePercentage() * 100.0) / 100.0);
//        systemInfo.put("runtime",SystemUtils.formatDuration(SystemUtils.getRuntimeDuration()));
        systemInfo.put("runtime",SystemUtils.getRuntimeDuration());
        //TODO 当前版本号需要修改
        ArrayList<JSONObject> arrayList = new ArrayList<>();
        systemInfo.put("versionForEnvironment",arrayList);
        return Result.success(systemInfo);
    }
}
