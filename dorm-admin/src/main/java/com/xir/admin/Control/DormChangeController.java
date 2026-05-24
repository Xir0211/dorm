package com.xir.admin.Control;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xir.admin.Common.Result;
import com.xir.admin.Entity.DormChange;
import com.xir.admin.Entity.Room;
import com.xir.admin.Entity.SysUser;
import com.xir.admin.Mapper.DormChangeMapper;
import com.xir.admin.Mapper.RoomMapper;
import com.xir.admin.Mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/dorm-change")
public class DormChangeController {

    @Autowired
    private DormChangeMapper dormChangeMapper;
    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RoomMapper roomMapper;


    // ==================== 分页列表 ====================

    @GetMapping("/list")
    public Result list(@RequestParam(defaultValue = "1") int page,
                       @RequestParam(defaultValue = "10") int pageSize) {
        long total = dormChangeMapper.countWithFilter();
        int offset = (page - 1) * pageSize;
        List<DormChange> list = dormChangeMapper.selectWithFilter(offset, pageSize);

        Map<String, Object> data = new HashMap<>();
        data.put("list", list);
        data.put("total", total);
        data.put("page", page);
        data.put("pageSize", pageSize);
        return Result.success(data);
    }
    // ==================== 详情 ====================
    @GetMapping("/detail/{id}")
    public Result detail(@PathVariable Long id) {
        DormChange record = dormChangeMapper.selectById(id);
        return Result.success(record);
    }

    // 审批通过
    // 审批通过
    @PutMapping("/approve/{id}")
    public Result approve(@PathVariable Long id) {
        DormChange record = dormChangeMapper.selectById(id);
        if (record == null) return Result.error("申请不存在");

        if ("EMPTY_BED".equals(record.getChangeType())) {
            SysUser student = userMapper.selectById(record.getStudentId());
            if (student != null) {
                Long oldRoomId = student.getRoomId();
                student.setRoomId(record.getNewRoomId());
                student.setBedNo(record.getNewBedNo());
                student.setCheckinTime(new Date());
                userMapper.updateById(student);

                syncRoomStatus(oldRoomId);
                syncRoomStatus(record.getNewRoomId());
            }
        } else if ("SWAP".equals(record.getChangeType())) {
            SysUser studentA = userMapper.selectById(record.getStudentId());
            SysUser studentB = userMapper.selectById(record.getSwapStudentId());
            if (studentA != null && studentB != null) {
                Long roomA = studentA.getRoomId();
                String bedA = studentA.getBedNo();
                studentA.setRoomId(studentB.getRoomId());
                studentA.setBedNo(studentB.getBedNo());
                studentB.setRoomId(roomA);
                studentB.setBedNo(bedA);
                userMapper.updateById(studentA);
                userMapper.updateById(studentB);
                syncRoomStatus(studentA.getRoomId());
                syncRoomStatus(studentB.getRoomId());
            }
        }

        record.setStatus("APPROVED");
        record.setSwapStatus("AGREED");


        record.setApproveTime(new Date());
        record.setApprover("管理员");
        dormChangeMapper.updateById(record);
        return Result.success("审批通过");
    }

    //拒绝
    @PutMapping("/reject/{id}")
    public Result reject(@PathVariable Long id) {
        DormChange record = dormChangeMapper.selectById(id);
        if (record == null) return Result.error("申请不存在");
        record.setStatus("REJECTED");
        dormChangeMapper.updateById(record);
        return Result.success("已拒绝");
    }

    // ✅ 加这个私有方法
    private void syncRoomStatus(Long roomId) {
        if (roomId == null) return;
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

    // B同意 → 等Admin审批
    @PutMapping("/agree-swap/{id}")
    public Result agreeSwap(@PathVariable Long id) {
        DormChange record = dormChangeMapper.selectById(id);
        if (record == null) return Result.error("申请不存在");
        record.setSwapStatus("AGREED");
        // STATUS 保持 WAITING_SWAP
        dormChangeMapper.updateById(record);
        return Result.success("已同意，等待管理员审批");
    }

    // B拒绝 → 直接结束，不到Admin
    @PutMapping("/reject-swap/{id}")
    public Result rejectSwap(@PathVariable Long id) {
        DormChange record = dormChangeMapper.selectById(id);
        if (record == null) return Result.error("申请不存在");
        record.setStatus("REJECTED");
        record.setSwapStatus("REJECTED");
        dormChangeMapper.updateById(record);
        return Result.success("已拒绝");
    }

}