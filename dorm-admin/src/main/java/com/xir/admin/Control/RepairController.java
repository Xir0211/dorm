package com.xir.admin.Control;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xir.admin.Common.Result;
import com.xir.admin.Entity.Repair;
import com.xir.admin.Entity.Room;
import com.xir.admin.Entity.SysUser;
import com.xir.admin.Entity.Worker;
import com.xir.admin.Mapper.RepairMapper;
import com.xir.admin.Mapper.RoomMapper;
import com.xir.admin.Mapper.UserMapper;
import com.xir.admin.Mapper.WorkerMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/repair")
public class RepairController {

    @Autowired private RepairMapper repairMapper;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Autowired private WorkerMapper workerMapper;

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private RoomMapper roomMapper;

    // ==================== 报修列表 ====================
    @GetMapping("/list")
    public Result list(@RequestParam(defaultValue = "1") int page,
                       @RequestParam(defaultValue = "10") int pageSize,
                       @RequestParam(required = false) String keyword,
                       @RequestParam(required = false) String status) {
        QueryWrapper<Repair> wrapper = new QueryWrapper<>();
        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.and(w -> w.like("STUDENT_NAME", keyword).or().like("REPAIR_NO", keyword).or().like("ROOM_NO", keyword));
        }
        if (status != null && !status.trim().isEmpty()) {
            wrapper.eq("STATUS", status);
        }
        wrapper.orderByDesc("CREATE_TIME");

        long total = repairMapper.selectCount(wrapper);
        int offset = (page - 1) * pageSize;
        wrapper.last("OFFSET " + offset + " ROWS FETCH NEXT " + pageSize + " ROWS ONLY");
        List<Repair> list = repairMapper.selectList(wrapper);

        Map<String, Object> data = new HashMap<>();
        data.put("list", list);
        data.put("total", total);
        return Result.success(data);
    }

    // ==================== 详情 ====================
    @GetMapping("/detail/{id}")
    public Result detail(@PathVariable Long id) {
        Repair repair = repairMapper.selectById(id);
        return Result.success(repair);
    }

    // ==================== 接单（待处理 → 处理中） ====================
    @PutMapping("/process/{id}")
    public Result process(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Repair repair = repairMapper.selectById(id);
        if (repair == null) return Result.error("报修不存在");
        if (!"PENDING".equals(repair.getStatus())) return Result.error("该报修已处理");

        repair.setStatus("PROCESSING");
        repair.setHandler(body.get("handler"));
        repairMapper.updateById(repair);
        return Result.success("接单成功");
    }

    // RepairController.java 加
    @PostMapping("/submit")
    public Result submit(@RequestBody Map<String, Object> body) {
        Long studentId = Long.valueOf(body.get("studentId").toString());
        SysUser student = userMapper.selectById(studentId);
        if (student == null) return Result.error("学生不存在");

        String repairNo = "BX" + System.currentTimeMillis();
        Repair repair = new Repair();
        repair.setId(jdbcTemplate.queryForObject("SELECT SEQ_REPAIR_ID.NEXTVAL FROM DUAL", Long.class));
        repair.setRepairNo(repairNo);
        repair.setStudentId(studentId);
        repair.setStudentName(student.getRealName());
        repair.setStudentNo(student.getUsername());
        repair.setDescription((String) body.get("description"));
        repair.setRepairType((String) body.get("repairType"));
        repair.setStatus("PENDING");
        repair.setCreateTime(new Date());

        // 宿舍信息
        if (student.getRoomId() != null) {
            Room room = roomMapper.selectById(student.getRoomId());
            if (room != null) {
                repair.setBuildingId(room.getBuildingId());
                repair.setBuildingName(room.getBuildingName());
                repair.setRoomId(room.getRoomId());
                repair.setRoomNo(room.getRoomNo());
            }
        }

        // 图片
        List<String> images = (List<String>) body.get("images");
        if (images != null && !images.isEmpty()) {
            repair.setImageUrl(images.get(0)); // 存第一张
        }

        repairMapper.insert(repair);
        return Result.success(repairNo);
    }

    // 派单
    @PutMapping("/assign/{id}")
    public Result assign(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Repair repair = repairMapper.selectById(id);
        if (repair == null) return Result.error("报修不存在");
        if (!"PENDING".equals(repair.getStatus())) return Result.error("该报修已派单");

        Long workerId = Long.valueOf(body.get("workerId").toString());
        Worker worker = workerMapper.selectById(workerId);
        if (worker == null) return Result.error("师傅不存在");

        repair.setStatus("PROCESSING");
        repair.setWorkerId(workerId);
        repair.setWorkerName(worker.getName());
        repair.setWorkerPhone(worker.getPhone());
        repair.setRepairType((String) body.get("repairType"));
        repair.setRemark((String) body.get("remark"));
        repair.setAssignTime(new Date());
        repairMapper.updateById(repair);

        // 师傅状态改为忙碌
        worker.setStatus("BUSY");
        workerMapper.updateById(worker);

        return Result.success("派单成功");
    }

    // 完成（改）
    @PutMapping("/complete/{id}")
    public Result complete(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Repair repair = repairMapper.selectById(id);
        if (repair == null) return Result.error("报修不存在");
        if (!"PROCESSING".equals(repair.getStatus())) return Result.error("状态不正确");

        repair.setStatus("COMPLETED");
        repair.setHandleTime(new Date());
        repair.setHandleResult((String) body.get("result"));
        if (body.get("cost") != null) repair.setCost(Double.valueOf(body.get("cost").toString()));
        if (body.get("rating") != null) repair.setRating(Integer.valueOf(body.get("rating").toString()));
        repairMapper.updateById(repair);

        // 师傅恢复空闲
        if (repair.getWorkerId() != null) {
            Worker worker = workerMapper.selectById(repair.getWorkerId());
            if (worker != null) {
                worker.setStatus("IDLE");
                workerMapper.updateById(worker);
            }
        }

        return Result.success("处理完成");
    }
}