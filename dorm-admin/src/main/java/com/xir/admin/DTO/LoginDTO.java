package com.xir.admin.DTO;



import lombok.Data;

/**
 * 登录数据传输对象
 */
@Data
public class LoginDTO {

    // 用户名/学号
    private String username;

    // 密码
    private String password;

    // 前端传回来的验证码文本
    private String captchaCode;

    // 验证码在 Redis 中对应的唯一 Key
    private String captchaKey;

    //注册验证码
    private String inviteCode;
}
