package com.xir.admin.Control;

import com.xir.admin.Common.Result;
import com.xir.admin.Entity.SysUser;
import com.xir.admin.Mapper.UserMapper;
import com.xir.admin.Security.JwtUtils;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/settings")
public class ProfileController {

    @Autowired private UserMapper userMapper;
    @Autowired private BCryptPasswordEncoder encoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // ==================== 获取当前管理员信息 ====================
    @GetMapping("/profile")
    public Result getProfile(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        // 从 token 解析用户名（需要 JwtUtils，或者前端传 userId）
        // 简化：前端传 userId
        return Result.success(null); // 前端自己从 localStorage 取
    }

    // ==================== 更新个人资料 ====================
    @PutMapping("/profile")
    public Result updateProfile(@RequestBody Map<String, Object> body) {
        Long userId = Long.valueOf(body.get("userId").toString());

        // 只更新传过来的字段，不用 MyBatis-Plus updateById（会把所有字段都更新）
        if (body.get("realName") != null) {
            jdbcTemplate.update("UPDATE SYS_USER SET REAL_NAME = ? WHERE USER_ID = ?",
                    body.get("realName"), userId);
        }
        if (body.get("phone") != null) {
            jdbcTemplate.update("UPDATE SYS_USER SET PHONE = ? WHERE USER_ID = ?",
                    body.get("phone"), userId);
        }
        if (body.get("email") != null) {
            jdbcTemplate.update("UPDATE SYS_USER SET EMAIL = ? WHERE USER_ID = ?",
                    body.get("email"), userId);
        }
        if (body.get("avatar") != null) {
            jdbcTemplate.update("UPDATE SYS_USER SET AVATAR = ? WHERE USER_ID = ?",
                    body.get("avatar"), userId);
        }

        return Result.success(body);
    }
    // ProfileController.java
    @GetMapping("/avatar/{userId}")
    public void getAvatar(@PathVariable Long userId, HttpServletResponse response) throws Exception {
        SysUser user = userMapper.selectById(userId);
        if (user == null || user.getAvatar() == null) {
            response.sendRedirect("https://api.dicebear.com/7.x/avataaars/svg?seed=default");
            return;
        }
        // avatar 存的是 base64，直接输出
        String base64 = user.getAvatar();
        if (base64.startsWith("data:image")) {
            String[] parts = base64.split(",");
            response.setContentType("image/png");
            byte[] bytes = Base64.getDecoder().decode(parts[1]);
            response.getOutputStream().write(bytes);
        }
    }

    // ==================== 修改密码 ====================
    @PutMapping("/password")
    public Result changePassword(@RequestBody Map<String, String> body) {
        Long userId = Long.valueOf(body.get("userId"));
        String oldPassword = body.get("oldPassword");
        String newPassword = body.get("newPassword");

        SysUser user = userMapper.selectById(userId);
        if (user == null) return Result.error("用户不存在");

        // 验证原密码
        if (!encoder.matches(oldPassword, user.getPassword())) {
            return Result.error("原密码错误");
        }

        // 更新密码
        user.setPassword(encoder.encode(newPassword));
        userMapper.updateById(user);

        return Result.success("密码修改成功，请重新登录");
    }
}