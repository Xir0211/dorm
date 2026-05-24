package com.xir.admin.Control;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xir.admin.Common.Result;
import com.xir.admin.Entity.*;
import com.xir.admin.Mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/checkout")
public class CheckoutController {

    @Autowired private UserMapper userMapper;
    @Autowired private RoomMapper roomMapper;
    @Autowired private CheckoutMapper checkoutMapper;
    @Autowired private JdbcTemplate jdbcTemplate;

    // ==================== 已入住学生列表（管理员直接退宿用） ====================
    @GetMapping("/students")
    public Result getStudents(@RequestParam(required = false) String keyword) {
        QueryWrapper<SysUser> wrapper = new QueryWrapper<>();
        wrapper.eq("ROLE", "STUDENT").isNotNull("ROOM_ID");
        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.and(w -> w.like("USERNAME", keyword).or().like("REAL_NAME", keyword));
        }
        wrapper.orderByAsc("ROOM_ID", "BED_NO");
        List<SysUser> list = userMapper.selectList(wrapper);

        List<Map<String, Object>> result = new ArrayList<>();
        for (SysUser u : list) {
            Map<String, Object> item = new HashMap<>();
            item.put("userId", u.getUserId());
            item.put("username", u.getUsername());
            item.put("realName", u.getRealName());
            item.put("gender", u.getGender());
            item.put("department", u.getDepartment());
            item.put("className", u.getClassName());
            item.put("phone", u.getPhone());
            item.put("roomId", u.getRoomId());
            item.put("bedNo", u.getBedNo());
            item.put("checkinTime", u.getCheckinTime());

            // 查房间信息
            if (u.getRoomId() != null) {
                Room room = roomMapper.selectById(u.getRoomId());
                if (room != null) {
                    item.put("buildingName", room.getBuildingName());
                    item.put("roomNo", room.getRoomNo());
                }
            }

            // 已住天数
            if (u.getCheckinTime() != null) {
                long days = (System.currentTimeMillis() - u.getCheckinTime().getTime()) / (1000 * 60 * 60 * 24);
                item.put("stayDays", days);
            }
            result.add(item);
        }
        return Result.success(result);
    }

    // ==================== 管理员直接退宿 ====================
    @PostMapping("/direct")
    public Result directCheckout(@RequestBody Map<String, Object> body) {
        Long userId = Long.valueOf(body.get("userId").toString());

        // 查学生当前房间
        SysUser student = userMapper.selectById(userId);
        if (student == null) return Result.error("学生不存在");
        if (student.getRoomId() == null) return Result.error("该学生未入住");

        Long oldRoomId = student.getRoomId();

        // ✅ 用 JdbcTemplate 直接更新，绕过实体类
        jdbcTemplate.update(
                "UPDATE SYS_USER SET ROOM_ID = NULL, BED_NO = NULL, CHECKIN_TIME = NULL, FEE_STATUS = 'UNPAID' WHERE USER_ID = ?",
                userId
        );

        // 同步房间状态
        syncRoomStatus(oldRoomId);

        return Result.success("退宿成功");
    }

    // ==================== 退宿申请列表（学生端提交的） ====================
    @GetMapping("/applications")
    public Result applications(@RequestParam(defaultValue = "1") int page,
                               @RequestParam(defaultValue = "10") int pageSize,
                               @RequestParam(required = false) String status) {
        QueryWrapper<Checkout> wrapper = new QueryWrapper<>();
        if (status != null && !status.trim().isEmpty()) wrapper.eq("STATUS", status);
        wrapper.orderByDesc("CREATE_TIME");

        long total = checkoutMapper.selectCount(wrapper);
        int offset = (page - 1) * pageSize;
        wrapper.last("OFFSET " + offset + " ROWS FETCH NEXT " + pageSize + " ROWS ONLY");
        List<Checkout> list = checkoutMapper.selectList(wrapper);

        Map<String, Object> data = new HashMap<>();
        data.put("list", list);
        data.put("total", total);
        return Result.success(data);
    }

    // ==================== 审批通过 ====================
    @PostMapping("/approve/{id}")
    public Result approve(@PathVariable Long id) {
        Checkout application = checkoutMapper.selectById(id);
        if (application == null) return Result.error("申请不存在");
        if (!"PENDING".equals(application.getStatus())) return Result.error("该申请已处理");

        // ✅ 直接 SQL 更新
        jdbcTemplate.update(
                "UPDATE SYS_USER SET ROOM_ID = NULL, BED_NO = NULL, CHECKIN_TIME = NULL, FEE_STATUS = 'UNPAID' WHERE USER_ID = ?",
                application.getStudentId()
        );

        // 同步房间状态
        SysUser student = userMapper.selectById(application.getStudentId());
        // 由于 student 已被更新，用 application 里的信息
        // 需要查房间...这里简化，传 null 查不到旧房间，改查所有房间
        syncAllRooms();

        application.setStatus("APPROVED");
        application.setApproveTime(new Date());
        application.setApprover("管理员");
        checkoutMapper.updateById(application);

        return Result.success("审批通过，已退宿");
    }

    private void syncAllRooms() {
        List<Room> rooms = roomMapper.selectList(null);
        for (Room room : rooms) {
            syncRoomStatus(room.getRoomId());
        }
    }

    // ==================== 审批拒绝 ====================
    @PostMapping("/reject/{id}")
    public Result reject(@PathVariable Long id) {
        Checkout application = checkoutMapper.selectById(id);
        if (application == null) return Result.error("申请不存在");

        application.setStatus("REJECTED");
        application.setApproveTime(new Date());
        application.setApprover("管理员");
        checkoutMapper.updateById(application);

        return Result.success("已拒绝");
    }

    // ==================== 同步房间状态 ====================
    private void syncRoomStatus(Long roomId) {
        Room room = roomMapper.selectById(roomId);
        if (room == null) return;
        Long occupiedLong = userMapper.selectCount(new QueryWrapper<SysUser>().eq("ROOM_ID", roomId));
        int occupied = occupiedLong != null ? occupiedLong.intValue() : 0;
        room.setOccupied(occupied);
        if (occupied == 0) room.setStatus("EMPTY");
        else if (occupied >= room.getBedCount()) room.setStatus("FULL");
        else room.setStatus("AVAILABLE");
        roomMapper.updateById(room);
    }

    // ==================== 学生提交退宿申请 ====================
    @PostMapping("/submit")
    public Result submit(@RequestBody Map<String, Object> body) {
        Long studentId = Long.valueOf(body.get("studentId").toString());
        SysUser student = userMapper.selectById(studentId);
        if (student == null) return Result.error("学生不存在");
        if (student.getRoomId() == null) return Result.error("您未入住");

        Checkout checkout = new Checkout();
        checkout.setId(jdbcTemplate.queryForObject("SELECT SEQ_CHECKOUT_ID.NEXTVAL FROM DUAL", Long.class));
        checkout.setCheckoutNo("CO" + System.currentTimeMillis());
        checkout.setStudentId(studentId);
        checkout.setStudentName(student.getRealName());
        checkout.setStudentNo(student.getUsername());
        checkout.setBedNo(student.getBedNo());
        checkout.setReason((String) body.get("reason"));
        checkout.setDestination((String) body.get("destination"));
        checkout.setStatus("PENDING");
        checkout.setApplyTime(new Date());
        checkout.setCreateTime(new Date());

        Room room = roomMapper.selectById(student.getRoomId());
        System.out.println("student.getRoomId(): " + student.getRoomId());  // ✅ 加
        System.out.println("room5555: " + room);  // ✅ 加
        if (room != null) {
            checkout.setBuildingName(room.getBuildingName());
            checkout.setRoomNo(room.getRoomNo());
        }

        checkoutMapper.insert(checkout);
        return Result.success("提交成功");
    }
}