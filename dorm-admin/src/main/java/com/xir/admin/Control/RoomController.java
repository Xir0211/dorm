package com.xir.admin.Control;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xir.admin.Common.Result;
import com.xir.admin.Entity.Room;
import com.xir.admin.Entity.SysUser;
import com.xir.admin.Mapper.RoomMapper;
import com.xir.admin.Mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/room")
public class RoomController {

    @Autowired private RoomMapper roomMapper;
    @Autowired private UserMapper userMapper;
    @Autowired private JdbcTemplate jdbcTemplate;

    // ==================== 根据楼栋ID查房间列表 ====================
    @GetMapping("/list")
    public Result list(@RequestParam(required = false) Long buildingId,
                       @RequestParam(required = false) Integer floorNum,
                       @RequestParam(required = false) String status,
                       @RequestParam(required = false) String keyword) {
        QueryWrapper<Room> wrapper = new QueryWrapper<>();
        if (buildingId != null) wrapper.eq("BUILDING_ID", buildingId);
        if (floorNum != null) wrapper.eq("FLOOR_NUM", floorNum);
        if (status != null && !status.trim().isEmpty()) wrapper.eq("STATUS", status.trim());
        if (keyword != null && !keyword.trim().isEmpty()) wrapper.like("ROOM_NO", keyword.trim());
        wrapper.orderByAsc("FLOOR_NUM", "ROOM_NO");
        List<Room> list = roomMapper.selectList(wrapper);

        for (Room room : list) {
            Long count = userMapper.selectCount(new QueryWrapper<SysUser>().eq("ROOM_ID", room.getRoomId()));
            room.setOccupied(count != null ? count.intValue() : 0);
        }

        Map<Integer, List<Room>> grouped = new LinkedHashMap<>();
        for (Room room : list) {
            grouped.computeIfAbsent(room.getFloorNum(), k -> new ArrayList<>()).add(room);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("grouped", grouped);
        data.put("total", list.size());
        return Result.success(data);
    }

    // ==================== 房间详情（含入住学生） ====================
    @GetMapping("/detail/{id}")
    public Result detail(@PathVariable Long id) {
        Room room = roomMapper.selectById(id);

        // 查该房间所有学生
        List<SysUser> students = userMapper.selectList(
                new QueryWrapper<SysUser>()
                        .eq("ROOM_ID", id)
                        .orderByAsc("BED_NO")
        );

        Map<String, Object> data = new HashMap<>();
        data.put("room", room);

        // 构建床位列表（按ABCD排）
        List<Map<String, Object>> beds = new ArrayList<>();
        String[] bedLabels = {"A", "B", "C", "D", "E", "F", "G", "H"};
        for (int i = 0; i < room.getBedCount(); i++) {
            String bedNo = bedLabels[i];
            SysUser student = students.stream()
                    .filter(s -> bedNo.equals(s.getBedNo()))
                    .findFirst().orElse(null);

            Map<String, Object> bed = new HashMap<>();
            bed.put("bedNo", bedNo);
            bed.put("occupied", student != null);
            if (student != null) {
                Map<String, Object> info = new HashMap<>();
                info.put("userId", student.getUserId());
                info.put("username", student.getUsername());
                info.put("realName", student.getRealName());
                info.put("gender", student.getGender());
                info.put("department", student.getDepartment());
                info.put("phone", student.getPhone());
                info.put("checkinTime", student.getCheckinTime());
                bed.put("student", info);
            }
            beds.add(bed);
        }
        data.put("beds", beds);
        data.put("occupiedCount", students.size());
        data.put("emptyCount", room.getBedCount() - students.size());

        return Result.success(data);
    }

    // ==================== 添加房间 ====================
    @PostMapping("/add")
    public Result add(@RequestBody Room room) {
        // ✅ 从序列取 ID
        Long id = jdbcTemplate.queryForObject("SELECT SEQ_ROOM_ID.NEXTVAL FROM DUAL", Long.class);
        room.setRoomId(id);

        room.setCreateTime(new Date());
        if (room.getOccupied() == null) room.setOccupied(0);
        if (room.getStatus() == null) room.setStatus("AVAILABLE");
        roomMapper.insert(room);
        return Result.success("添加成功");
    }
    // ==================== 批量添加 ====================
    @PostMapping("/batch-add")
    public Result batchAdd(@RequestBody Map<String, Object> params) {
        Long buildingId = Long.valueOf(params.get("buildingId").toString());
        String buildingName = (String) params.get("buildingName");
        int startFloor = Integer.parseInt(params.get("startFloor").toString());
        int endFloor = Integer.parseInt(params.get("endFloor").toString());
        int startRoom = Integer.parseInt(params.get("startRoom").toString());
        int roomCount = Integer.parseInt(params.get("roomCount").toString());
        int bedCount = Integer.parseInt(params.get("bedCount").toString());

        for (int floor = startFloor; floor <= endFloor; floor++) {
            for (int r = 0; r < roomCount; r++) {
                Room room = new Room();
                room.setRoomId(jdbcTemplate.queryForObject("SELECT SEQ_ROOM_ID.NEXTVAL FROM DUAL", Long.class));
                room.setBuildingId(buildingId);
                room.setBuildingName(buildingName);
                room.setFloorNum(floor);
                room.setRoomNo(floor + String.format("%02d", startRoom + r));
                room.setBedCount(bedCount);
                room.setOccupied(0);
                room.setStatus("AVAILABLE");
                room.setCreateTime(new Date());
                roomMapper.insert(room);
            }
        }
        return Result.success("批量添加成功");
    }

    // ==================== 更新房间 ====================
    @PutMapping("/update")
    public Result update(@RequestBody Room room) {
        Room exist = roomMapper.selectById(room.getRoomId());
        if (exist != null) {
            // 如果床位数改了，更新已入住人数
            if (room.getBedCount() != null && room.getBedCount() < exist.getOccupied()) {
                return Result.error("床位数不能小于已入住人数");
            }
            room.setOccupied(exist.getOccupied());
        }
        roomMapper.updateById(room);
        return Result.success("修改成功");
    }

    // ==================== 删除房间 ====================
    @DeleteMapping("/delete/{id}")
    public Result delete(@PathVariable Long id) {
        Room room = roomMapper.selectById(id);
        if (room != null && room.getOccupied() > 0) {
            return Result.error("该房间有学生入住，无法删除");
        }
        roomMapper.deleteById(id);
        return Result.success("删除成功");
    }

    // ==================== 楼层列表 ====================
    @GetMapping("/floors")
    public Result floors(@RequestParam(required = false) Long buildingId) {
        QueryWrapper<Room> wrapper = new QueryWrapper<>();
        wrapper.select("DISTINCT FLOOR_NUM as floorNum");  // ✅ 加别名
        if (buildingId != null) wrapper.eq("BUILDING_ID", buildingId);
        wrapper.orderByAsc("FLOOR_NUM");
        List<Map<String, Object>> list = roomMapper.selectMaps(wrapper);
        return Result.success(list);
    }

}