package com.jt.plugins.service.login;

import com.jt.plugins.bean.User;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.crypto.digests.SM3Digest;
import org.bouncycastle.util.encoders.Hex;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;


/**
 * @BelongsProject: JTBrowserUpdater
 * @BelongsPackage: com.jtzx.service
 * @Author: 别来无恙qb
 * @CreateTime: 2024-09-05  15:34
 * @Description: TODO
 * @Version: 1.0
 */
@Service
public class UserService {
    private static final String SALT = "MIGfMA@04G#CSd8%qG0S)I=3rDQ%E2B4Af#Q5U6A@A82jg4#0GER4b%nb#Nd=A5D$C2xB$5i$Q5j5%KB5g&Q1D1.&94W*h9(41t14_Pj%%Mfq3iM*WCW$SjS$#754%$215x22S35#78Hh56d64da$mz,n78vcb3dfg9x7@673&!^5@v).#m!jdsks/jg!@z@7sud%j7sk#o$5@h7k(*ahddd#$567@#%7735@#$$%77k";
    private  static final String PASSWORD = "a5ece1d04b170ef0d46a4807f1bdcd2590f7b5f4cbc251af4a1d9d854272b1cb";
    public User getById(Integer value) {
        User userDB = new User();
        //通过ID获取用户
        return userDB;
    }

    /**
     * 获取用户，验证密码
     * @param user
     * @return
     */
    public User getUser(User user) {
        //查文件账户密码id
        if (user == null){
            return null;
        }

        User userDB = new User();

        userDB.setId(Integer.parseInt("1"));
        userDB.setUsername("admin");
        userDB.setPassword(PASSWORD);

        //验证密码
        String username = user.getUsername();
        String password = user.getPassword();
        String userDBPwd = getPassword(username, password);
        if (username.equals(userDB.getUsername()) && userDBPwd.equals(userDB.getPassword())) {
            return userDB;
        }
        return null;
    }

    public static String getPassword(String username, String password) {
        if (StringUtils.isBlank(password) || StringUtils.isBlank(username)) {
            return "";
        }
        byte[] data = (password + SALT + username).getBytes();
        SM3Digest digest = new SM3Digest();
        digest.update(data, 0, data.length);
        byte[] result = new byte[digest.getDigestSize()];
        digest.doFinal(result, 0);
        return Hex.toHexString(result);
    }
}
