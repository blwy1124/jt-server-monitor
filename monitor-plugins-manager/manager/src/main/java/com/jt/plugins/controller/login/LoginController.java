package com.jt.plugins.controller.login;

import com.alibaba.fastjson.JSONObject;
import com.jt.plugins.bean.User;
import com.jt.plugins.service.login.LoginService;
import com.jt.plugins.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;


/**
 * @ClassName LoginController
 * @Author ：BLWY-1124
 * @Date ：2024-08-20 14:56
 * @Description：
 * @Version: 1.0
 */
@RestController
public class LoginController {

    @Autowired
    public LoginService loginService;

    @RequestMapping("/login")
    public Result<JSONObject> reloadCurrentDirectory(@RequestBody User request) {
        return loginService.login(request);
    }

    @RequestMapping("/currentUserName")
    public Result<JSONObject> reloadCurrentDirectory(@RequestBody JSONObject request) {
        return loginService.currentUserName(request);
    }

    @RequestMapping(value = "/alert" ,method = RequestMethod.POST)
    public Result<JSONObject> alert(@RequestBody JSONObject request) {
        return loginService.alert(request);
    }


}

