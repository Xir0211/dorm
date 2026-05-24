package com.xir.admin.Control;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xir.admin.Common.Result;
import com.xir.admin.Entity.Room;
import com.xir.admin.Entity.SysUser;
import com.xir.admin.Mapper.RoomMapper;
import com.xir.admin.Mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/checkin")
public class CheckinController {

    @Autowired private UserMapper userMapper;
    @Autowired private RoomMapper roomMapper;

    // ==================== 未入住学生列表 ====================
    @GetMapping("/students")
    public Result getStudents(@RequestParam(required = false) String keyword) {
        QueryWrapper<SysUser> wrapper = new QueryWrapper<>();
        wrapper.eq("ROLE", "STUDENT").isNull("ROOM_ID");
        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.and(w -> w.like("USERNAME", keyword).or().like("REAL_NAME", keyword));
        }
        wrapper.orderByDesc("CREATE_TIME");
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
            result.add(item);
        }
        return Result.success(result);
    }

    // ==================== 可选楼栋列表 ====================
    @GetMapping("/buildings")
    public Result getBuildings() {
        QueryWrapper<Room> wrapper = new QueryWrapper<>();
        wrapper.select("DISTINCT BUILDING_ID, BUILDING_NAME")
                .and(w -> w.eq("STATUS", "AVAILABLE").or().eq("STATUS", "EMPTY"))
                .inSql("BUILDING_ID", "SELECT BUILDING_ID FROM SYS_BUILDING")  // ✅ 加这行
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

    // ==================== 可选房间列表（有空位的） ====================
    @GetMapping("/rooms")
    public Result getRooms(@RequestParam Long buildingId) {
        QueryWrapper<Room> wrapper = new QueryWrapper<>();
        wrapper.eq("BUILDING_ID", buildingId);
        // 只查有空位的房间
        wrapper.and(w -> w.eq("STATUS", "AVAILABLE").or().eq("STATUS", "EMPTY"));
        wrapper.orderByAsc("FLOOR_NUM", "ROOM_NO");
        List<Room> list = roomMapper.selectList(wrapper);
        return Result.success(list);
    }

    // ==================== 确认入住 ====================
    @PostMapping("/confirm")
    public Result confirm(@RequestBody Map<String, Object> body) {
        Long userId = Long.valueOf(body.get("userId").toString());
        Long roomId = Long.valueOf(body.get("roomId").toString());
        String bedNo = (String) body.get("bedNo");

        // 查房间
        Room room = roomMapper.selectById(roomId);
        if (room == null) return Result.error("房间不存在");

        // 检查床位是否已被占用
        Long bedOccupied = userMapper.selectCount(
                new QueryWrapper<SysUser>().eq("ROOM_ID", roomId).eq("BED_NO", bedNo)
        );
        if (bedOccupied > 0) return Result.error("该床位已被占用");

        // 更新学生
        SysUser student = userMapper.selectById(userId);
        if (student == null) return Result.error("学生不存在");
        if (student.getRoomId() != null) return Result.error("该学生已入住");

        student.setRoomId(roomId);
        student.setBedNo(bedNo);
        student.setCheckinTime(new Date());
        student.setFeeStatus("PAID");
        userMapper.updateById(student);

        // 同步房间状态
        syncRoomStatus(roomId);

        return Result.success("入住成功");
    }

    // ==================== 同步房间状态 ====================
    private void syncRoomStatus(Long roomId) {
        Room room = roomMapper.selectById(roomId);
        if (room == null) return;
        Long occupiedLong = userMapper.selectCount(new QueryWrapper<SysUser>().eq("ROOM_ID", roomId));
        int occupied = occupiedLong != null ? occupiedLong.intValue() : 0;
        room.setOccupied(occupied);
        if (occupied == 0) {
            room.setStatus("EMPTY");
        } else if (occupied >= room.getBedCount()) {
            room.setStatus("FULL");
        } else {
            room.setStatus("AVAILABLE");
        }
        roomMapper.updateById(room);
    }

}