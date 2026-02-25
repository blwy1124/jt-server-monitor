package com.jt.plugins.controller;

import com.alibaba.fastjson.JSONObject;
import com.jt.plugins.service.MainService;
import com.jt.plugins.utils.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @ClassName MainController
 * @Author ：别来无恙qb
 * @Date ：2024-08-22 8:36
 * @Description：
 * @Version: 1.0
 */
@RestController
@Slf4j
@RequestMapping("/main")
public class MainController {
    @Autowired
    public MainService mainService;

    @RequestMapping(value = "/getSystemInfo", method = RequestMethod.POST)
    public Result<JSONObject> getSystemInfo() throws Exception {
        return mainService.getSystemInfo();
    }
}
