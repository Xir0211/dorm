package com.xir.admin.Control;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xir.admin.Common.Result;
import com.xir.admin.Entity.SysUser;
import com.xir.admin.Mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private UserMapper userMapper;

    // ==================== 宿管列表 ====================
    @GetMapping("/list")
    public Result list(@RequestParam(defaultValue = "1") int page,
                       @RequestParam(defaultValue = "10") int pageSize,
                       @RequestParam(required = false) String keyword) {
        QueryWrapper<SysUser> wrapper = new QueryWrapper<>();
        wrapper.eq("ROLE", "ADMIN");
        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.and(w -> w.like("USERNAME", keyword).or().like("REAL_NAME", keyword));
        }
        wrapper.orderByDesc("CREATE_TIME");

        long total = userMapper.selectCount(wrapper);
        int offset = (page - 1) * pageSize;
        wrapper.last("OFFSET " + offset + " ROWS FETCH NEXT " + pageSize + " ROWS ONLY");
        List<SysUser> list = userMapper.selectList(wrapper);

        // 过滤密码
        List<Map<String, Object>> result = new ArrayList<>();
        for (SysUser u : list) {
            Map<String, Object> item = new HashMap<>();
            item.put("userId", u.getUserId());
            item.put("username", u.getUsername());
            item.put("realName", u.getRealName());
            item.put("phone", u.getPhone());
            item.put("email", u.getEmail());
            item.put("status", u.getStatus() != null ? u.getStatus() : "1");
            item.put("createTime", u.getCreateTime());
            result.add(item);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("list", result);
        data.put("total", total);
        return Result.success(data);
    }

    // ==================== 编辑宿管 ====================
    @PutMapping("/update")
    public Result update(@RequestBody Map<String, Object> body) {
        Long userId = Long.valueOf(body.get("userId").toString());
        SysUser user = userMapper.selectById(userId);
        if (user == null) return Result.error("用户不存在");

        if (body.get("realName") != null) user.setRealName((String) body.get("realName"));
        if (body.get("phone") != null) user.setPhone((String) body.get("phone"));
        if (body.get("email") != null) user.setEmail((String) body.get("email"));
        if (body.get("status") != null) user.setStatus((String) body.get("status"));

        userMapper.updateById(user);
        return Result.success("更新成功");
    }

    // ==================== 删除宿管 ====================
    @DeleteMapping("/delete/{id}")
    public Result delete(@PathVariable Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) return Result.error("用户不存在");
        if (!"ADMIN".equals(user.getRole())) return Result.error("只能删除宿管账号");
        userMapper.deleteById(id);
        return Result.success("删除成功");
    }
}