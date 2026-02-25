package com.jt.plugins.service.login;

import com.alibaba.fastjson.JSONObject;
import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.jt.plugins.bean.User;
import com.jt.plugins.utils.JWTUtils;
import com.jt.plugins.utils.Result;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @ClassName LoginService
 * @Author ：BLWY-1124
 * @Date ：2024-08-20 15:30
 * @Description：
 * @Version: 1.0
 */
@Service
public class LoginService {
    @Autowired
    private UserService userService;

    public Result<JSONObject> login(User user) {
        String username = user.getUsername();
        String password = user.getPassword();
        if (StringUtils.isBlank(username) || StringUtils.isBlank(password)){
            return Result.failure(500, "用户名和密码不能为空！");
        }
        User userDB = userService.getUser(user);
        if (userDB == null) {
            return Result.failure(500, "用户名或者密码输入错误！");
        }
        JSONObject userVo = new JSONObject();
        userVo.put("username", userDB.getUsername());
        String token = JWTUtils.getToken(userDB.getUsername(), userDB.getPassword());
        userVo.put("token", token);
        //查询密码验证
        return Result.success(userVo);
    }

    public Result<JSONObject> currentUserName(JSONObject request) {
        String token = request.getString("token");
        DecodedJWT decodedJWT = JWT.decode(token);
        List<String> audience = decodedJWT.getAudience();
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("username", audience.get(0));
        return Result.success(jsonObject);
    }

    public Result<JSONObject> alert(JSONObject request) {

        System.out.println(request.toJSONString());
        return null;
    }
}
