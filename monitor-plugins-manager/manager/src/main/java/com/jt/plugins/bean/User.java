package com.jt.plugins.bean;

/**
 * @BelongsProject: JTBrowserUpdater
 * @BelongsPackage: com.jtzx.bean
 * @Author: 别来无恙qb
 * @CreateTime: 2024-09-05  11:57
 * @Description: TODO
 * @Version: 1.0
 */

public class User {
    private Integer id;
    private String username;
    private String password;

    // 构造函数
    public User() {
    }

    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }

    // Getter 和 Setter 方法
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }
}

