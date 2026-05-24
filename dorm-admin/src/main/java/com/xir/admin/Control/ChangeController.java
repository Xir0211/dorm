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
@RequestMapping("/change")
public class ChangeController {

    @Autowired private UserMapper userMapper;
    @Autowired private RoomMapper roomMapper;
    @Autowired private DormChangeMapper dormChangeMapper;
    @Autowired private JdbcTemplate jdbcTemplate;

    // ==================== 已入住学生列表 ====================
    @GetMapping("/students")
    public Result getStudents(@RequestParam(required = false) String keyword) {
        QueryWrapper<SysUser> wrapper = new QueryWrapper<>();
        wrapper.eq("ROLE", "STUDENT").isNotNull("ROOM_ID");
        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.and(w -> w.like("USERNAME", keyword).or().like("REAL_NAME", keyword));
        }
        List<SysUser> list = userMapper.selectList(wrapper);

        List<Map<String, Object>> result = new ArrayList<>();
        for (SysUser u : list) {
            Map<String, Object> item = new HashMap<>();
            item.put("userId", u.getUserId());
            item.put("username", u.getUsername());
            item.put("realName", u.getRealName());
            item.put("department", u.getDepartment());
            item.put("className", u.getClassName());
            if (u.getRoomId() != null) {
                Room room = roomMapper.selectById(u.getRoomId());
                if (room != null) {
                    item.put("buildingName", room.getBuildingName());
                    item.put("roomNo", room.getRoomNo());
                    item.put("buildingId", room.getBuildingId());
                    item.put("roomId", room.getRoomId());
                }
            }
            item.put("bedNo", u.getBedNo());
            result.add(item);
        }
        return Result.success(result);
    }

    // ==================== 可选楼栋（有空位的） ====================
    @GetMapping("/buildings")
    public Result getBuildings() {
        QueryWrapper<Room> wrapper = new QueryWrapper<>();
        wrapper.select("DISTINCT BUILDING_ID, BUILDING_NAME")
                .and(w -> w.eq("STATUS", "AVAILABLE").or().eq("STATUS", "EMPTY"))
                .orderByAsc("BUILDING_ID");
        List<Map<String, Object>> list = roomMapper.selectMaps(wrapper);

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> row : list) {
            Map<String, Object> item = new HashMap<>();
            item.put("buildingId", row.get("BUILDING_ID"));
            item.put("buildingName", row.get("BUILDING_NAME"));
            result.add(item);
        }
        return Result.success(result);
    }

    // ==================== 可选房间（有空位的） ====================
    @GetMapping("/rooms")
    public Result getRooms(@RequestParam Long buildingId) {
        QueryWrapper<Room> wrapper = new QueryWrapper<>();
        wrapper.eq("BUILDING_ID", buildingId)
                .and(w -> w.eq("STATUS", "AVAILABLE").or().eq("STATUS", "EMPTY"))
                .orderByAsc("FLOOR_NUM", "ROOM_NO");
        List<Room> list = roomMapper.selectList(wrapper);
        return Result.success(list);
    }

    // ==================== 确认调宿 ====================
    @PostMapping("/confirm")
    public Result confirm(@RequestBody Map<String, Object> body) {
        Long userId = Long.valueOf(body.get("userId").toString());
        Long newRoomId = Long.valueOf(body.get("newRoomId").toString());
        String newBedNo = (String) body.get("newBedNo");
        String reason = (String) body.get("reason");

        SysUser student = userMapper.selectById(userId);
        if (student == null) return Result.error("学生不存在");
        if (student.getRoomId() == null) return Result.error("该学生未入住");

        // 检查新床位是否被占
        Long occupied = userMapper.selectCount(
                new QueryWrapper<SysUser>().eq("ROOM_ID", newRoomId).eq("BED_NO", newBedNo)
        );
        if (occupied > 0) return Result.error("该床位已被占用");

        Long oldRoomId = student.getRoomId();
        String oldBedNo = student.getBedNo();

        // 查旧房间
        Room oldRoom = roomMapper.selectById(oldRoomId);
        Room newRoom = roomMapper.selectById(newRoomId);

        // 更新学生
        student.setRoomId(newRoomId);
        student.setBedNo(newBedNo);
        student.setCheckinTime(new Date());
        userMapper.updateById(student);

        // 同步新旧房间状态
        syncRoomStatus(oldRoomId);
        syncRoomStatus(newRoomId);

        // 记录调宿历史
        DormChange record = new DormChange();
        record.setId(jdbcTemplate.queryForObject("SELECT SEQ_DORM_CHANGE_ID.NEXTVAL FROM DUAL", Long.class));
        record.setChangeNo("DH" + System.currentTimeMillis());
        record.setStudentId(userId);
        record.setStudentName(student.getRealName());
        record.setStudentNo(student.getUsername());
        record.setOldBuildingId(oldRoom != null ? oldRoom.getBuildingId() : null);
        record.setOldBuildingName(oldRoom != null ? oldRoom.getBuildingName() : null);
        record.setOldRoomId(oldRoomId);
        record.setOldRoomNo(oldRoom != null ? oldRoom.getRoomNo() : null);
        record.setOldBedNo(oldBedNo);
        record.setNewBuildingId(newRoom.getBuildingId());
        record.setNewBuildingName(newRoom.getBuildingName());
        record.setNewRoomId(newRoomId);
        record.setNewRoomNo(newRoom.getRoomNo());
        record.setNewBedNo(newBedNo);
        record.setReason(reason);
        record.setStatus("APPROVED");
        record.setApplyTime(new Date());
        record.setApproveTime(new Date());
        record.setApprover("管理员");
        record.setCreateTime(new Date());
        dormChangeMapper.insert(record);

        return Result.success("调宿成功");
    }

    private void syncRoomStatus(Long roomId) {
        Room room = roomMapper.selectById(roomId);
        if (room == null) return;
        Long count = userMapper.selectCount(new QueryWrapper<SysUser>().eq("ROOM_ID", roomId));
        int occupied = count != null ? count.intValue() : 0;
        room.setOccupied(occupied);
        if (occupied == 0) room.setStatus("EMPTY");
        else if (occupied >= room.getBedCount()) room.setStatus("FULL");
        else room.setStatus("AVAILABLE");
        roomMapper.updateById(room);
    }

    @PostMapping("/submit")
    public Result submit(@RequestBody Map<String, Object> body) {
        Long studentId = Long.valueOf(body.get("studentId").toString());
        String changeType = (String) body.get("changeType");
        String reason = (String) body.get("reason");

        SysUser student = userMapper.selectById(studentId);
        if (student == null || student.getRoomId() == null) return Result.error("您未入住");

        DormChange record = new DormChange();
        record.setId(jdbcTemplate.queryForObject("SELECT SEQ_DORM_CHANGE_ID.NEXTVAL FROM DUAL", Long.class));
        record.setChangeNo("DH" + System.currentTimeMillis());
        record.setStudentId(studentId);
        record.setStudentName(student.getRealName());
        record.setStudentNo(student.getUsername());
        record.setReason(reason);
        record.setChangeType(changeType);
        record.setCreateTime(new Date());

        Room oldRoom = roomMapper.selectById(student.getRoomId());
        if (oldRoom != null) {
            record.setOldBuildingName(oldRoom.getBuildingName());
            record.setOldRoomNo(oldRoom.getRoomNo());
            record.setOldBedNo(student.getBedNo());
        }

        if ("EMPTY_BED".equals(changeType)) {
            record.setNewRoomId(Long.valueOf(body.get("newRoomId").toString()));
            record.setNewBedNo((String) body.get("newBedNo"));
            Room newRoom = roomMapper.selectById(record.getNewRoomId());
            if (newRoom != null) {
                record.setNewBuildingName(newRoom.getBuildingName());
                record.setNewRoomNo(newRoom.getRoomNo());
            }
            record.setStatus("PENDING");
        } else {
            Long swapId = Long.valueOf(body.get("swapStudentId").toString());
            SysUser swapStudent = userMapper.selectById(swapId);
            record.setSwapStudentId(swapId);
            record.setSwapStudentName(swapStudent.getRealName());
            record.setSwapStudentNo(swapStudent.getUsername());
            record.setSwapBedNo(swapStudent.getBedNo());
            if (swapStudent.getRoomId() != null) {
                Room swapRoom = roomMapper.selectById(swapStudent.getRoomId());
                if (swapRoom != null) {
                    record.setSwapBuildingName(swapRoom.getBuildingName());
                    record.setSwapRoomNo(swapRoom.getRoomNo());
                }
            }
            record.setStatus("WAITING_SWAP");
            record.setSwapStatus("WAITING");
        }

        dormChangeMapper.insert(record);
        return Result.success("提交成功");
    }

    // CheckinController 或 ChangeController 加
    @GetMapping("/occupied-beds")
    public Result occupiedBeds(@RequestParam Long roomId) {
        List<SysUser> list = userMapper.selectList(
                new QueryWrapper<SysUser>().eq("ROOM_ID", roomId).isNotNull("BED_NO")
        );
        List<String> beds = new ArrayList<>();
        for (SysUser u : list) beds.add(u.getBedNo());
        return Result.success(beds);
    }
}