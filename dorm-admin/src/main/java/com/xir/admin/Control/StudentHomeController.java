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
@RequestMapping("/student-api")
public class StudentHomeController {

    @Autowired private UserMapper userMapper;
    @Autowired private RoomMapper roomMapper;
    @Autowired private NoticeMapper noticeMapper;
    @Autowired private RepairMapper repairMapper;

    @Autowired private DormChangeMapper dormChangeMapper;
    @Autowired private CheckoutMapper checkoutMapper;
    @Autowired private LeaveMapper leaveMapper;

    // ==================== 首页数据 ====================
    @GetMapping("/home")
    public Result home(@RequestParam Long userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) return Result.error("学生不存在");

        Map<String, Object> data = new HashMap<>();
        data.put("userId", user.getUserId());
        data.put("username", user.getUsername());
        data.put("realName", user.getRealName());
        data.put("gender", user.getGender());
        data.put("department", user.getDepartment());
        data.put("className", user.getClassName());
        data.put("phone", user.getPhone());
        data.put("avatar", user.getAvatar());

        // 宿舍信息 + 室友
        if (user.getRoomId() != null) {
            Room room = roomMapper.selectById(user.getRoomId());
            if (room != null) {
                data.put("buildingName", room.getBuildingName());
                data.put("roomNo", room.getRoomNo());
                data.put("bedNo", user.getBedNo());
                data.put("checkinTime", user.getCheckinTime());
                if (user.getCheckinTime() != null) {
                    long days = (System.currentTimeMillis() - user.getCheckinTime().getTime()) / (1000 * 60 * 60 * 24);
                    data.put("stayDays", days);
                }

                // 室友
                List<SysUser> roommates = userMapper.selectList(
                        new QueryWrapper<SysUser>().eq("ROOM_ID", user.getRoomId()).ne("USER_ID", userId)
                );
                List<Map<String, Object>> mateList = new ArrayList<>();
                for (SysUser mate : roommates) {
                    Map<String, Object> m = new HashMap<>();
                    m.put("userId", mate.getUserId());
                    m.put("realName", mate.getRealName());
                    m.put("bedNo", mate.getBedNo());
                    m.put("avatar", mate.getAvatar());
                    m.put("phone", mate.getPhone());
                    mateList.add(m);
                }
                // 补空床位
                int totalBeds = room.getBedCount();
                int occupied = roommates.size() + 1;
                String[] allBeds = {"A", "B", "C", "D", "E", "F", "G", "H"};
                for (int i = 0; i < totalBeds; i++) {
                    boolean taken = allBeds[i].equals(user.getBedNo());
                    for (SysUser mate : roommates) {
                        if (allBeds[i].equals(mate.getBedNo())) taken = true;
                    }
                    if (!taken) {
                        Map<String, Object> m = new HashMap<>();
                        m.put("bedNo", allBeds[i]);
                        m.put("realName", null);
                        m.put("avatar", null);
                        m.put("empty", true);
                        mateList.add(m);
                    }
                }
                data.put("roommates", mateList);
            }
        }



        // 最新公告（3条）
        List<Notice> notices = noticeMapper.selectList(
                new QueryWrapper<Notice>().eq("STATUS", "PUBLISHED").orderByDesc("CREATE_TIME").last("FETCH FIRST 3 ROWS ONLY")
        );
        data.put("notices", notices);

        return Result.success(data);
    }

    @GetMapping("/messages")
    public Result messages(@RequestParam Long userId) {
        List<Map<String, Object>> messages = new ArrayList<>();

        // 1. 互换请求（别人想和我换）
        List<DormChange> swapRequests = dormChangeMapper.selectList(
                new QueryWrapper<DormChange>()
                        .eq("SWAP_STUDENT_ID", userId)
                        .eq("SWAP_STATUS", "WAITING")
                        .eq("STATUS", "WAITING_SWAP")
        );
        for (DormChange d : swapRequests) {
            Map<String, Object> msg = new HashMap<>();
            msg.put("type", "SWAP_REQUEST");
            msg.put("title", d.getStudentName() + "想和你互换宿舍");
            msg.put("desc", d.getOldBuildingName() + " " + d.getOldRoomNo() + "室 " + d.getOldBedNo() + "床");
            msg.put("time", d.getCreateTime());
            msg.put("id", d.getId());
            msg.put("canAgree", true);
            messages.add(msg);
        }

        // 2. 互换结果（B同意/拒绝）
        List<DormChange> swapResults = dormChangeMapper.selectList(
                new QueryWrapper<DormChange>()
                        .eq("STUDENT_ID", userId)
                        .eq("CHANGE_TYPE", "SWAP")
                        .in("SWAP_STATUS", Arrays.asList("AGREED", "REJECTED"))
        );
        for (DormChange d : swapResults) {
            Map<String, Object> msg = new HashMap<>();
            msg.put("type", "SWAP_RESULT");
            if ("AGREED".equals(d.getSwapStatus())) {
                msg.put("title", d.getSwapStudentName() + "同意了你的互换申请");
                msg.put("desc", "等待管理员审批");
            } else {
                msg.put("title", d.getSwapStudentName() + "拒绝了你的互换申请");
                msg.put("desc", "你可以重新发起申请");
            }
            msg.put("time", d.getCreateTime());
            msg.put("id", d.getId());
            messages.add(msg);
        }

        // 3. Admin审批结果（互换）
        List<DormChange> adminResults = dormChangeMapper.selectList(
                new QueryWrapper<DormChange>()
                        .eq("STUDENT_ID", userId)
                        .eq("CHANGE_TYPE", "SWAP")
                        .in("STATUS", Arrays.asList("APPROVED", "REJECTED"))
        );
        for (DormChange d : adminResults) {
            Map<String, Object> msg = new HashMap<>();
            msg.put("type", "SWAP_RESULT");
            msg.put("title", "APPROVED".equals(d.getStatus()) ? "调宿申请已通过，宿舍已互换" : "调宿申请被管理员拒绝");
            msg.put("desc", (d.getSwapBuildingName() != null ? d.getSwapBuildingName() : d.getNewBuildingName()) + " " +
                    (d.getSwapRoomNo() != null ? d.getSwapRoomNo() : d.getNewRoomNo()));
            msg.put("time", d.getApproveTime());
            msg.put("id", d.getId());
            messages.add(msg);
        }

        // 4. 搬空床位结果
        List<DormChange> emptyBedResults = dormChangeMapper.selectList(
                new QueryWrapper<DormChange>()
                        .eq("STUDENT_ID", userId)
                        .eq("CHANGE_TYPE", "EMPTY_BED")
                        .in("STATUS", Arrays.asList("APPROVED", "REJECTED"))
        );
        for (DormChange d : emptyBedResults) {
            Map<String, Object> msg = new HashMap<>();
            msg.put("type", "SWAP_RESULT");
            msg.put("title", "APPROVED".equals(d.getStatus()) ? "调宿申请已通过" : "调宿申请被拒绝");
            msg.put("desc", d.getNewBuildingName() + " " + d.getNewRoomNo() + "室 " + d.getNewBedNo() + "床");
            msg.put("time", d.getApproveTime());
            msg.put("id", d.getId());
            messages.add(msg);
        }

        // 5. 退宿结果
        List<Checkout> checkouts = checkoutMapper.selectList(
                new QueryWrapper<Checkout>()
                        .eq("STUDENT_ID", userId)
                        .in("STATUS", Arrays.asList("APPROVED", "REJECTED"))
        );
        for (Checkout c : checkouts) {
            Map<String, Object> msg = new HashMap<>();
            msg.put("type", "CHECKOUT_RESULT");
            if ("APPROVED".equals(c.getStatus())) {
                msg.put("title", "退宿申请已通过");
                msg.put("desc", "你的床位已释放");
            } else {
                msg.put("title", "退宿申请被拒绝");
                msg.put("desc", "如有疑问请联系管理员");
            }
            msg.put("time", c.getApproveTime());
            msg.put("id", c.getId());
            messages.add(msg);
        }

        // 6. 报修进度
        List<Repair> repairs = repairMapper.selectList(
                new QueryWrapper<Repair>()
                        .eq("STUDENT_ID", userId)
                        .in("STATUS", Arrays.asList("PROCESSING", "COMPLETED"))
        );
        for (Repair r : repairs) {
            Map<String, Object> msg = new HashMap<>();
            msg.put("type", "REPAIR_PROGRESS");
            if ("PROCESSING".equals(r.getStatus())) {
                msg.put("title", "报修已派单");
                msg.put("desc", r.getWorkerName() + "将处理你的报修，电话：" + r.getWorkerPhone());
            } else {
                msg.put("title", "报修已完成");
                msg.put("desc", r.getHandleResult());
            }
            msg.put("time", r.getHandleTime() != null ? r.getHandleTime() : r.getCreateTime());
            msg.put("id", r.getId());
            messages.add(msg);
        }

        // 7. 请假结果
        List<Leave> leaves = leaveMapper.selectList(
                new QueryWrapper<Leave>()
                        .eq("STUDENT_ID", userId)
                        .in("STATUS", Arrays.asList("APPROVED", "REJECTED"))
        );
        for (Leave l : leaves) {
            Map<String, Object> msg = new HashMap<>();
            msg.put("type", "LEAVE_RESULT");
            if ("APPROVED".equals(l.getStatus())) {
                msg.put("title", "请假申请已通过");
            } else {
                msg.put("title", "请假申请被拒绝");
                msg.put("desc", l.getApproveOpinion());
            }
            msg.put("time", l.getApproveTime());
            msg.put("id", l.getId());
            messages.add(msg);
        }

        // 按时间倒序
        messages.sort((a, b) -> {
            Date t1 = (Date) a.get("time");
            Date t2 = (Date) b.get("time");
            if (t1 == null) return 1;
            if (t2 == null) return -1;
            return t2.compareTo(t1);
        });

        Map<String, Object> data = new HashMap<>();
        data.put("list", messages);
        data.put("total", messages.size());
        return Result.success(data);
    }

}