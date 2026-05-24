package com.xir.admin.Control;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xir.admin.Common.Result;
import com.xir.admin.Entity.InviteCode;
import com.xir.admin.Mapper.InviteCodeMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/invite")
public class InviteController {

    @Autowired private InviteCodeMapper inviteCodeMapper;
    @Autowired private JdbcTemplate jdbcTemplate;

    // ==================== 统计 ====================
    @GetMapping("/stats")
    public Result stats() {
        Map<String, Object> data = new HashMap<>();
        data.put("total", inviteCodeMapper.selectCount(null));
        data.put("available", inviteCodeMapper.selectCount(new QueryWrapper<InviteCode>().eq("STATUS", "1")));
        data.put("used", inviteCodeMapper.selectCount(new QueryWrapper<InviteCode>().eq("STATUS", "0")));
        data.put("expired", inviteCodeMapper.selectCount(new QueryWrapper<InviteCode>().eq("STATUS", "2")));
        return Result.success(data);
    }

    // ==================== 列表 ====================
    @GetMapping("/list")
    public Result list(@RequestParam(defaultValue = "1") int page,
                       @RequestParam(defaultValue = "10") int pageSize,
                       @RequestParam(required = false) String keyword,
                       @RequestParam(required = false) String status) {
        // 先更新过期状态
        jdbcTemplate.update("UPDATE SYS_INVITE_CODE SET STATUS = '2' WHERE STATUS = '1' AND EXPIRE_TIME < SYSTIMESTAMP");

        QueryWrapper<InviteCode> wrapper = new QueryWrapper<>();
        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.like("CODE", keyword.trim());
        }
        if (status != null && !status.trim().isEmpty()) {
            wrapper.eq("STATUS", status);
        }
        wrapper.orderByDesc("CREATE_TIME");

        long total = inviteCodeMapper.selectCount(wrapper);
        int offset = (page - 1) * pageSize;
        wrapper.last("OFFSET " + offset + " ROWS FETCH NEXT " + pageSize + " ROWS ONLY");
        List<InviteCode> list = inviteCodeMapper.selectList(wrapper);

        Map<String, Object> data = new HashMap<>();
        data.put("list", list);
        data.put("total", total);
        return Result.success(data);
    }

    // ==================== 生成邀请码 ====================
    @PostMapping("/generate")
    public Result generate(@RequestBody Map<String, Object> body) {
        int count = Integer.parseInt(body.get("count").toString());
        int days = Integer.parseInt(body.get("days").toString());
        String creator = (String) body.get("creator");

        if (count < 1 || count > 50) return Result.error("数量范围1-50");

        List<String> codes = new ArrayList<>();
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

        for (int i = 0; i < count; i++) {
            StringBuilder sb = new StringBuilder("DORM2026-");
            Random random = new Random();
            for (int j = 0; j < 4; j++) {
                sb.append(chars.charAt(random.nextInt(chars.length())));
            }

            String code = sb.toString();
            InviteCode inviteCode = new InviteCode();
            inviteCode.setId(jdbcTemplate.queryForObject("SELECT SEQ_INVITE_CODE_ID.NEXTVAL FROM DUAL", Long.class));
            inviteCode.setCode(code);
            inviteCode.setStatus("1");
            inviteCode.setCreator(creator);
            inviteCode.setCreateTime(new Date());

            Calendar cal = Calendar.getInstance();
            cal.setTime(new Date());
            cal.add(Calendar.DAY_OF_MONTH, days);
            inviteCode.setExpireTime(cal.getTime());

            inviteCodeMapper.insert(inviteCode);
            codes.add(code);
        }

        return Result.success(codes);
    }
}