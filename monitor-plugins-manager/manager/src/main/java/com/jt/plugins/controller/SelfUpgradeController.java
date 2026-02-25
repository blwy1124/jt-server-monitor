package com.jt.plugins.controller;

import com.alibaba.fastjson.JSONObject;
import com.jt.plugins.common.http.ExtensionRequestParam;
import com.jt.plugins.common.result.ResultMsg;
import com.jt.plugins.service.SelfUpgradeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
/**
 * @BelongsProject: jt-server-monitor
 * @BelongsPackage: com.jt.plugins.controller
 * @Author: 别来无恙qb
 * @CreateTime: 2025-09-04  16:32
 * @Description: 应用自升级控制器
 * @Version: 1.0
 */

@RestController
@RequestMapping("self-upgrade")
public class SelfUpgradeController {

    @Autowired
    private SelfUpgradeService selfUpgradeService;

    /**
     * 手动触发自升级
     */
    @PostMapping("perform")
    public ResultMsg<String> performSelfUpgrade(@RequestBody ExtensionRequestParam request) {
        return selfUpgradeService.manualSelfUpgrade(request);
    }

    /**
     * 获取升级状态
     */
    @GetMapping("status")
    public ResultMsg<JSONObject> getUpgradeStatus() {
        return selfUpgradeService.getUpgradeStatus();
    }

    /**
     * 获取备份列表
     */
    @GetMapping("backups")
    public ResultMsg<JSONObject> getBackupList() {
        return selfUpgradeService.getBackupList();
    }

    /**
     * 立即检查更新（测试用）
     */
    @PostMapping("check-now")
    public ResultMsg<String> checkNow(@RequestBody ExtensionRequestParam request) {
        return selfUpgradeService.manualSelfUpgrade(request);
    }
}
