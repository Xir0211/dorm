package com.xir.admin.Control;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xir.admin.Common.Result;
import com.xir.admin.Entity.*;
import com.xir.admin.Mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.text.SimpleDateFormat;
import java.util.*;

@RestController
@RequestMapping("/leave")
public class LeaveController {

    @Autowired private LeaveMapper leaveMapper;
    @Autowired private UserMapper userMapper;
    @Autowired private RoomMapper roomMapper;
    @Autowired private JdbcTemplate jdbcTemplate;

    // ==================== 学生提交请假 ====================
    @PostMapping("/submit")
    public Result submit(@RequestBody Map<String, Object> body) {
        Long studentId = Long.valueOf(body.get("studentId").toString());
        SysUser student = userMapper.selectById(studentId);
        if (student == null) return Result.error("学生不存在");

        Leave leave = new Leave();
        leave.setId(jdbcTemplate.queryForObject("SELECT SEQ_LEAVE_ID.NEXTVAL FROM DUAL", Long.class));
        leave.setStudentId(studentId);
        leave.setStudentName(student.getRealName());
        leave.setStudentNo(student.getUsername());
        leave.setType((String) body.get("type"));
        leave.setReason((String) body.get("reason"));
        leave.setDestination((String) body.get("destination"));
        leave.setEmergencyPhone((String) body.get("emergencyPhone"));
        leave.setImageUrl((String) body.get("imageUrl"));
        leave.setStatus("PENDING");
        leave.setCreateTime(new Date());

        // 日期
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            leave.setStartDate(sdf.parse((String) body.get("startDate")));
            leave.setEndDate(sdf.parse((String) body.get("endDate")));
        } catch (Exception e) {}

        // 宿舍信息
        if (student.getRoomId() != null) {
            Room room = roomMapper.selectById(student.getRoomId());
            if (room != null) {
                leave.setBuildingName(room.getBuildingName());
                leave.setRoomNo(room.getRoomNo());
            }
        }

        leaveMapper.insert(leave);
        return Result.success("提交成功");
    }

    // ==================== 请假列表（Admin端） ====================
    @GetMapping("/list")
    public Result list(@RequestParam(defaultValue = "1") int page,
                       @RequestParam(defaultValue = "10") int pageSize,
                       @RequestParam(required = false) String keyword,
                       @RequestParam(required = false) String status) {
        QueryWrapper<Leave> wrapper = new QueryWrapper<>();
        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.and(w -> w.like("STUDENT_NAME", keyword).or().like("STUDENT_NO", keyword));
        }
        if (status != null && !status.trim().isEmpty()) {
            wrapper.eq("STATUS", status);
        }
        wrapper.orderByDesc("CREATE_TIME");

        long total = leaveMapper.selectCount(wrapper);
        int offset = (page - 1) * pageSize;
        wrapper.last("OFFSET " + offset + " ROWS FETCH NEXT " + pageSize + " ROWS ONLY");
        List<Leave> list = leaveMapper.selectList(wrapper);

        Map<String, Object> data = new HashMap<>();
        data.put("list", list);
        data.put("total", total);
        return Result.success(data);
    }

    // ==================== 审批通过 ====================
    @PostMapping("/approve/{id}")
    public Result approve(@PathVariable Long id) {
        Leave leave = leaveMapper.selectById(id);
        if (leave == null) return Result.error("申请不存在");
        leave.setStatus("APPROVED");
        leave.setApproveTime(new Date());
        leave.setApprover("管理员");
        leaveMapper.updateById(leave);
        return Result.success("已通过");
    }

    // ==================== 审批拒绝 ====================
    @PostMapping("/reject/{id}")
    public Result reject(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Leave leave = leaveMapper.selectById(id);
        if (leave == null) return Result.error("申请不存在");
        leave.setStatus("REJECTED");
        leave.setApproveTime(new Date());
        leave.setApprover("管理员");
        leave.setApproveOpinion(body.get("opinion"));
        leaveMapper.updateById(leave);
        return Result.success("已拒绝");
    }
}