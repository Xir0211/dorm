package com.xir.admin.Control;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xir.admin.Common.Result;
import com.xir.admin.DTO.LoginDTO;
import com.xir.admin.Entity.SysUser;
import com.xir.admin.Mapper.InviteCodeMapper;
import com.xir.admin.Mapper.UserMapper;
import com.xir.admin.Security.JwtUtils;
import com.xir.admin.Entity.InviteCode;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private UserMapper userMapper;
    @Autowired private RedisTemplate<String, String> redisTemplate;
    @Autowired private BCryptPasswordEncoder encoder;
    @Autowired private JwtUtils jwtUtils;

    @Autowired
    private InviteCodeMapper inviteCodeMapper;

    // ========== 0. 获取验证码（新增） ==========
    @GetMapping("/captcha")
    public Result getCaptcha() {
        String uuid = UUID.randomUUID().toString().replace("-", "");  // 生成 key
        String code = generateCode(4);                                // 生成验证码

        redisTemplate.opsForValue().set("captcha:" + uuid, code, 3, TimeUnit.MINUTES);

        Map<String, String> data = new HashMap<>();
        data.put("uuid", uuid);   // → 对应前端的 captchaKey
        data.put("code", code);   // → 对应前端的 captchaCode（用户照着输）
        return Result.success(data);
    }

    // 1. 管理员注册
    @PostMapping("/register")
    public Result register(@RequestBody SysUser user) {
        // ① 校验邀请码
        if (user.getInviteCode() == null || user.getInviteCode().trim().isEmpty()) {
            return Result.error("邀请码不能为空");
        }

        // 查数据库，邀请码是否有效
        InviteCode inviteCode = inviteCodeMapper.selectOne(
                new QueryWrapper<InviteCode>()
                        .eq("CODE", user.getInviteCode().trim().toUpperCase())
                        .eq("STATUS", "1")
        );

        if (inviteCode == null) {
            return Result.error("邀请码无效或已被使用");
        }

        // ② 检查用户名是否存在
        Long count = userMapper.selectCount(
                new QueryWrapper<SysUser>().eq("username", user.getUsername())
        );
        if (count > 0) {
            return Result.error("用户名已存在");
        }

        // ③ 注册
        user.setPassword(encoder.encode(user.getPassword()));
        user.setRole("ADMIN");
        userMapper.insert(user);

        // ④ 邀请码使用后失效（可选：一码只能用一次）
        inviteCodeMapper.updateById(inviteCode);
        // ④ 邀请码使用后失效
        inviteCode.setStatus("0");
        inviteCode.setUsedBy(user.getRealName() != null ? user.getRealName() : user.getUsername());
        inviteCode.setUsedTime(new Date());
        inviteCodeMapper.updateById(inviteCode);

        return Result.success("注册成功");
    }

    // 2. 登录 (支持 Admin 和 Student)
    @PostMapping("/login")
    public Result login(@RequestBody LoginDTO loginDto) {
        // A. 校验验证码 (从 Redis 取)
        String redisKey = "captcha:" + loginDto.getCaptchaKey();
        String code = redisTemplate.opsForValue().get(redisKey);
        if (code == null || !code.equalsIgnoreCase(loginDto.getCaptchaCode())) {
            return Result.error("验证码错误或已过期");
        }
        redisTemplate.delete(redisKey);

        // B. 查询用户
        SysUser user = userMapper.selectOne(new QueryWrapper<SysUser>().eq("username", loginDto.getUsername()));
        if (user == null || !encoder.matches(loginDto.getPassword(), user.getPassword())) {
            return Result.error("用户名或密码错误");
        }

        // C. 生成 Token
        String token = jwtUtils.createToken(user.getUserId(), user.getUsername(), user.getRole());

        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("user", user);
        return Result.success(data);
    }

    // 随机验证码生成
    private String generateCode(int len) {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < len; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    @GetMapping("/test")
    public Result test() {
        // 查学生
        SysUser user = userMapper.selectOne(
                new QueryWrapper<SysUser>().eq("username", "2510131240")
        );
        System.out.println("=== 数据库密文: " + user.getPassword());
        System.out.println("=== matches 结果: " + encoder.matches("123456", user.getPassword()));
        return Result.success("ok");
    }
}