package com.jt.plugins.controller;

import com.jt.plugins.service.PluginUpdateManagerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @BelongsProject: jt-server-monitor
 * @BelongsPackage: com.jt.plugins.controller
 * @Author: 别来无恙qb
 * @CreateTime: 2025-09-03  09:59
 * @Description: TODO
 * @Version: 1.0
 */

@RestController
@RequestMapping("update")
public class PluginUpdateManagerController {
    @Autowired
    private PluginUpdateManagerService pluginUpdateManagerService;

}
