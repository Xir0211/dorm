package com.xir.admin.Control;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xir.admin.Common.Result;
import com.xir.admin.Entity.Checkout;
import com.xir.admin.Entity.DormChange;
import com.xir.admin.Entity.Leave;
import com.xir.admin.Entity.Repair;
import com.xir.admin.Mapper.CheckoutMapper;
import com.xir.admin.Mapper.DormChangeMapper;
import com.xir.admin.Mapper.LeaveMapper;
import com.xir.admin.Mapper.RepairMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@RestController
@RequestMapping("/dashboard")
public class DashboardController {

    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private RepairMapper repairMapper;
    @Autowired
    private CheckoutMapper checkoutMapper;
    @Autowired
    private DormChangeMapper dormChangeMapper;
    @Autowired
    private LeaveMapper leaveMapper;

    // ==================== 1. 统计数据 ====================
    @GetMapping("/stats")
    public Result stats() {
        Map<String, Object> data = new HashMap<>();

        // 楼栋总数
        Integer buildingCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM SYS_BUILDING", Integer.class
        );
        data.put("buildingCount", buildingCount != null ? buildingCount : 0);
        data.put("buildingTrend", 0); // 可查上月数据对比

        // 房间总数 + 总床位
        Map<String, Object> roomStats = jdbcTemplate.queryForMap(
                "SELECT COUNT(*) AS ROOM_COUNT, NVL(SUM(BED_COUNT), 0) AS TOTAL_BEDS FROM SYS_ROOM"
        );
        data.put("roomCount", roomStats.get("ROOM_COUNT"));
        data.put("totalBeds", roomStats.get("TOTAL_BEDS"));

        // 入住学生数
        Integer studentCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM SYS_USER WHERE ROLE = 'STUDENT'", Integer.class
        );
        data.put("studentCount", studentCount != null ? studentCount : 0);

        // 入住率
        Long totalBeds = ((Number) roomStats.get("TOTAL_BEDS")).longValue();
        if (totalBeds > 0) {
            double rate = (double) studentCount / totalBeds * 100;
            data.put("occupancyRate", Math.round(rate * 10) / 10.0);  // 保留1位小数
        } else {
            data.put("occupancyRate", 0);
        }

        // 待处理报修
        Integer pendingRepair = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM SYS_REPAIR WHERE STATUS = 'PENDING'", Integer.class
        );
        data.put("pendingRepairCount", pendingRepair != null ? pendingRepair : 0);
        data.put("repairTrend", 0);

        data.put("totalBeds", totalBeds);

        return Result.success(data);
    }

    // ==================== 2. 入住率趋势（近7天） ====================
    @GetMapping("/occupancy-trend")
    public Result occupancyTrend() {
        List<Map<String, Object>> result = new ArrayList<>();
        String[] weekDays = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};

        for (int i = 6; i >= 0; i--) {
            Map<String, Object> item = new HashMap<>();
            item.put("day", weekDays[6 - i]);

            // 查当天入住数（简化：用总入住数代替，实际应按日期段查）
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM SYS_USER WHERE ROLE = 'STUDENT'", Integer.class
            );
            item.put("rate", count != null ? count * 100 / 320 : 0); // 假设 320 总床位
            result.add(item);
        }

        return Result.success(result);
    }

    // ==================== 3. 最近入住 ====================
    @GetMapping("/recent-checkins")
    public Result recentCheckins(@RequestParam(defaultValue = "3") int limit) {
        String sql = """
            SELECT u.USER_ID, u.USERNAME, u.REAL_NAME, u.CREATE_TIME,
                   r.ROOM_NO, b.BUILDING_NAME
            FROM SYS_USER u
            LEFT JOIN SYS_ROOM r ON u.ROOM_ID = r.ROOM_ID
            LEFT JOIN SYS_BUILDING b ON r.BUILDING_ID = b.BUILDING_ID
            WHERE u.ROLE = 'STUDENT'
            ORDER BY u.CREATE_TIME DESC
            FETCH FIRST ? ROWS ONLY
        """;

        List<Map<String, Object>> list = jdbcTemplate.queryForList(sql, limit);
        List<Map<String, Object>> result = new ArrayList<>();

        for (Map<String, Object> row : list) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", row.get("USER_ID"));
            item.put("studentName", row.get("REAL_NAME") != null ? row.get("REAL_NAME") : row.get("USERNAME"));
            item.put("buildingName", row.get("BUILDING_NAME") != null ? row.get("BUILDING_NAME") : "未分配");
            item.put("roomNo", row.get("ROOM_NO") != null ? row.get("ROOM_NO").toString() : "未分配");
            item.put("checkinTime", row.get("CREATE_TIME") != null ? row.get("CREATE_TIME").toString().substring(0, 10) : "");
            item.put("avatar", "https://api.dicebear.com/7.x/avataaars/svg?seed=" + row.get("USERNAME"));
            result.add(item);
        }

        return Result.success(result);
    }

    // ==================== 4. 最新公告 ====================
    @GetMapping("/recent-notices")
    public Result recentNotices(@RequestParam(defaultValue = "3") int limit) {
        String sql = """
            SELECT ID, TITLE, CONTENT, CREATE_TIME
            FROM SYS_NOTICE
            ORDER BY CREATE_TIME DESC
            FETCH FIRST ? ROWS ONLY
        """;

        List<Map<String, Object>> list = jdbcTemplate.queryForList(sql, limit);
        List<Map<String, Object>> result = new ArrayList<>();

        for (Map<String, Object> row : list) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", row.get("ID"));
            item.put("title", row.get("TITLE"));
            item.put("createTime", row.get("CREATE_TIME") != null ? row.get("CREATE_TIME").toString().substring(0, 10) : "");
            result.add(item);
        }

        return Result.success(result);
    }

    // ==================== 5. 最近报修 ====================
    @GetMapping("/recent-repairs")
    public Result recentRepairs(@RequestParam(defaultValue = "5") int limit) {
        String sql = """
            SELECT r.ID, r.REPAIR_NO, r.DESCRIPTION, r.STATUS, rm.ROOM_NO, b.BUILDING_NAME
            FROM SYS_REPAIR r
            LEFT JOIN SYS_ROOM rm ON r.ROOM_ID = rm.ROOM_ID
            LEFT JOIN SYS_BUILDING b ON rm.BUILDING_ID = b.BUILDING_ID
            ORDER BY r.CREATE_TIME DESC
            FETCH FIRST ? ROWS ONLY
        """;

        List<Map<String, Object>> list = jdbcTemplate.queryForList(sql, limit);
        List<Map<String, Object>> result = new ArrayList<>();

        for (Map<String, Object> row : list) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", row.get("ID"));
            item.put("repairNo", row.get("REPAIR_NO"));
            item.put("roomNo", (row.get("BUILDING_NAME") != null ? row.get("BUILDING_NAME") + "#" : "") + row.get("ROOM_NO"));
            item.put("description", row.get("DESCRIPTION"));
            item.put("status", row.get("STATUS"));
            result.add(item);
        }

        return Result.success(result);
    }



    @GetMapping("/todos")
    public Result todos() {
        Map<String, Object> data = new HashMap<>();
        data.put("pendingRepair", repairMapper.selectCount(new QueryWrapper<Repair>().eq("STATUS", "PENDING")));
        data.put("pendingCheckout", checkoutMapper.selectCount(new QueryWrapper<Checkout>().eq("STATUS", "PENDING")));
        data.put("pendingChange", dormChangeMapper.selectCount(new QueryWrapper<DormChange>().eq("STATUS", "PENDING")));
        data.put("pendingLeave", leaveMapper.selectCount(new QueryWrapper<Leave>().eq("STATUS", "PENDING")));
        return Result.success(data);
    }

    @GetMapping("/activities")
    public Result activities() {
        List<Map<String, Object>> list = new ArrayList<>();

        List<Repair> repairs = repairMapper.selectList(
                new QueryWrapper<Repair>().orderByDesc("CREATE_TIME").last("FETCH FIRST 3 ROWS ONLY"));
        for (Repair r : repairs) {
            Map<String, Object> item = new HashMap<>();
            item.put("type", "repair");
            item.put("title", r.getStudentName() + " 提交了报修申请");
            item.put("desc", r.getDescription());
            item.put("time", r.getCreateTime());
            list.add(item);
        }

        List<DormChange> changes = dormChangeMapper.selectList(
                new QueryWrapper<DormChange>().orderByDesc("CREATE_TIME").last("FETCH FIRST 3 ROWS ONLY"));
        for (DormChange c : changes) {
            Map<String, Object> item = new HashMap<>();
            item.put("type", "change");
            item.put("title", c.getStudentName() + " 申请了调宿");

            // ✅ 互换和搬空床位取不同字段
            String newLocation;
            if ("SWAP".equals(c.getChangeType())) {
                newLocation = (c.getSwapBuildingName() != null ? c.getSwapBuildingName() : "") + " " +
                        (c.getSwapRoomNo() != null ? c.getSwapRoomNo() : "");
            } else {
                newLocation = (c.getNewBuildingName() != null ? c.getNewBuildingName() : "") + " " +
                        (c.getNewRoomNo() != null ? c.getNewRoomNo() : "");
            }
            item.put("desc", (c.getOldBuildingName() != null ? c.getOldBuildingName() : "") + " " +
                    (c.getOldRoomNo() != null ? c.getOldRoomNo() : "") + " → " + newLocation);

            item.put("time", c.getCreateTime());
            list.add(item);
        }
        list.sort((a, b) -> ((Date) b.get("time")).compareTo((Date) a.get("time")));
        if (list.size() > 5) list = list.subList(0, 5);

        return Result.success(list);
    }
}
