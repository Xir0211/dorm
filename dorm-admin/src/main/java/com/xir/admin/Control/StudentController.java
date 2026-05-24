package com.xir.admin.Control;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xir.admin.Common.Result;
import com.xir.admin.Entity.Room;
import com.xir.admin.Entity.SysUser;
import com.xir.admin.Mapper.RoomMapper;
import com.xir.admin.Mapper.UserMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/student")
public class StudentController {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RoomMapper roomMapper;

    // ==================== 学生列表（分页 + 搜索 + 筛选） ====================
    @GetMapping("/list")
    public Result list(@RequestParam(defaultValue = "1") int page,
                       @RequestParam(defaultValue = "10") int pageSize,
                       @RequestParam(required = false) String keyword,
                       @RequestParam(required = false) String department,
                       @RequestParam(required = false) String buildingName,
                       @RequestParam(required = false) String status,
                       @RequestParam(required = false) String gender) {

        QueryWrapper<SysUser> wrapper = new QueryWrapper<>();
        wrapper.eq("ROLE", "STUDENT");

        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.and(w -> w
                    .like("USERNAME", keyword.trim())
                    .or()
                    .like("REAL_NAME", keyword.trim())
                    .or()
                    .like("PHONE", keyword.trim())
            );
        }
        if (department != null && !department.trim().isEmpty()) {
            wrapper.eq("DEPARTMENT", department.trim());
        }
        if (gender != null && !gender.trim().isEmpty()) {
            wrapper.eq("GENDER", gender.trim());
        }
        if ("CHECKED_IN".equals(status)) {
            wrapper.isNotNull("ROOM_ID");
        } else if ("NOT_CHECKED_IN".equals(status)) {
            wrapper.isNull("ROOM_ID");
        }
        if (buildingName != null && !buildingName.trim().isEmpty()) {
            wrapper.inSql("ROOM_ID",
                    "SELECT ROOM_ID FROM SYS_ROOM WHERE BUILDING_NAME = '" + buildingName.trim() + "'");
        }

        wrapper.orderByDesc("CREATE_TIME");

        long total = userMapper.selectCount(wrapper);
        int offset = (page - 1) * pageSize;
        wrapper.last("OFFSET " + offset + " ROWS FETCH NEXT " + pageSize + " ROWS ONLY");
        List<SysUser> list = userMapper.selectList(wrapper);

        // 组装返回数据，带上宿舍信息
        List<Map<String, Object>> resultList = new ArrayList<>();
        for (SysUser user : list) {
            Map<String, Object> item = new HashMap<>();
            item.put("userId", user.getUserId());
            item.put("username", user.getUsername());
            item.put("realName", user.getRealName());
            item.put("gender", user.getGender());
            item.put("phone", user.getPhone());
            item.put("department", user.getDepartment());
            item.put("className", user.getClassName());
            item.put("major", user.getMajor());
            item.put("email", user.getEmail());
            item.put("createTime", user.getCreateTime());
            item.put("avatar", user.getAvatar());

            // 住宿信息
            if (user.getRoomId() != null) {
                Room room = roomMapper.selectById(user.getRoomId());
                if (room != null) {
                    item.put("status", "CHECKED_IN");
                    item.put("buildingName", room.getBuildingName());
                    item.put("roomNo", room.getRoomNo());
                    item.put("bedNo", user.getBedNo());
                    item.put("checkinTime", user.getCheckinTime());
                } else {
                    item.put("status", "NOT_CHECKED_IN");
                }
            } else {
                item.put("status", "NOT_CHECKED_IN");
            }

            resultList.add(item);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("list", resultList);
        data.put("total", total);
        data.put("page", page);
        data.put("pageSize", pageSize);

        return Result.success(data);
    }

    // ==================== 学生详情 ====================
    @GetMapping("/detail/{id}")
    public Result detail(@PathVariable Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            return Result.error("学生不存在");
        }

        Map<String, Object> data = new HashMap<>();
        data.put("userId", user.getUserId());
        data.put("username", user.getUsername());
        data.put("realName", user.getRealName());
        data.put("gender", user.getGender());
        data.put("phone", user.getPhone());
        data.put("idCard", user.getIdCard());
        data.put("email", user.getEmail());
        data.put("birthday", user.getBirthday());
        data.put("nativePlace", user.getNativePlace());
        data.put("nation", user.getNation());
        data.put("department", user.getDepartment());
        data.put("major", user.getMajor());
        data.put("className", user.getClassName());
        data.put("advisor", user.getAdvisor());
        data.put("createTime", user.getCreateTime());
        data.put("avatar", user.getAvatar());
        data.put("feeStatus", user.getFeeStatus());
        data.put("emergencyName", user.getEmergencyName());
        data.put("emergencyRelation", user.getEmergencyRelation());
        data.put("emergencyPhone", user.getEmergencyPhone());
        data.put("emergencyAddress", user.getEmergencyAddress());

        // 住宿信息
        if (user.getRoomId() != null) {
            Room room = roomMapper.selectById(user.getRoomId());
            if (room != null) {
                data.put("status", "CHECKED_IN");
                data.put("buildingName", room.getBuildingName());
                data.put("roomNo", room.getRoomNo());
                data.put("bedNo", user.getBedNo());
                data.put("checkinTime", user.getCheckinTime());
                // 计算已住天数
                if (user.getCheckinTime() != null) {
                    long days = (System.currentTimeMillis() - user.getCheckinTime().getTime()) / (1000 * 60 * 60 * 24);
                    data.put("stayDays", days);
                }
            }
        } else {
            data.put("status", "NOT_CHECKED_IN");
        }

        return Result.success(data);
    }

    // ==================== 更新学生信息 ====================
    @PutMapping("/update")
    public Result update(@RequestBody SysUser user) {
        SysUser exist = userMapper.selectById(user.getUserId());
        if (exist == null) {
            return Result.error("学生不存在");
        }
        // 只更新允许修改的字段
        exist.setRealName(user.getRealName());
        exist.setGender(user.getGender());
        exist.setPhone(user.getPhone());
        exist.setEmail(user.getEmail());
        exist.setDepartment(user.getDepartment());
        exist.setMajor(user.getMajor());
        exist.setClassName(user.getClassName());
        exist.setAdvisor(user.getAdvisor());
        exist.setBirthday(user.getBirthday());
        exist.setNativePlace(user.getNativePlace());
        exist.setNation(user.getNation());
        exist.setIdCard(user.getIdCard());
        exist.setEmergencyName(user.getEmergencyName());
        exist.setEmergencyRelation(user.getEmergencyRelation());
        exist.setEmergencyPhone(user.getEmergencyPhone());
        exist.setEmergencyAddress(user.getEmergencyAddress());
        exist.setFeeStatus(user.getFeeStatus());
        exist.setAvatar(user.getAvatar());

        userMapper.updateById(exist);
        return Result.success("更新成功");
    }

    // ==================== 院系列表（下拉用） ====================
    @GetMapping("/departments")
    public Result departments() {
        List<Map<String, Object>> list = userMapper.selectMaps(
                new QueryWrapper<SysUser>()
                        .select("DISTINCT DEPARTMENT as department")
                        .eq("ROLE", "STUDENT")
                        .isNotNull("DEPARTMENT")
                        .orderByAsc("DEPARTMENT")
        );
        return Result.success(list);
    }

    // ==================== 楼栋列表（下拉用） ====================
    @GetMapping("/buildings")
    public Result buildings() {
        List<Map<String, Object>> list = roomMapper.selectMaps(
                new QueryWrapper<Room>()
                        .select("DISTINCT BUILDING_NAME as buildingName")
                        .isNotNull("BUILDING_NAME")
                        .orderByAsc("BUILDING_NAME")
        );
        return Result.success(list);
    }

    // ==================== 新增学生 ====================
    @PostMapping("/add")
    public Result add(@RequestBody SysUser user) {
        // 检查学号是否已存在
        Long count = userMapper.selectCount(
                new QueryWrapper<SysUser>().eq("USERNAME", user.getUsername())
        );
        if (count > 0) {
            return Result.error("学号已存在");
        }

        user.setRole("STUDENT");
        user.setPassword(new BCryptPasswordEncoder().encode("123456")); // 默认密码
        user.setCreateTime(LocalDateTime.now());
        userMapper.insert(user);
        return Result.success("添加成功");
    }

    // ==================== 批量导入 Excel ====================
    @PostMapping("/import")
    public Result importExcel(@RequestBody List<Map<String, Object>> list) {
        int total = list.size();
        int success = 0;
        int skip = 0;
        int fail = 0;
        List<String> errors = new ArrayList<>();

        for (Map<String, Object> row : list) {
//            String username = (String) row.get("username");
//            String realName = (String) row.get("realName");
            String username = String.valueOf(row.get("username"));  // ✅
            String realName = String.valueOf(row.get("realName"));

            if (username == null || username.trim().isEmpty()) {
                fail++;
                errors.add("第" + (success + skip + fail) + "行：学号为空");
                continue;
            }
            if (realName == null || realName.trim().isEmpty()) {
                fail++;
                errors.add("学号" + username + "：姓名为空");
                continue;
            }

            // 检查学号是否已存在
            Long count = userMapper.selectCount(
                    new QueryWrapper<SysUser>().eq("USERNAME", username.trim())
            );
            if (count > 0) {
                skip++;
                continue;
            }

            try {
                SysUser user = new SysUser();
                user.setRealName(String.valueOf(row.get("realName")));
                user.setGender(String.valueOf(row.get("gender")));
                user.setPhone(String.valueOf(row.get("phone")));
                user.setUsername(username.trim());
                user.setRole("STUDENT");
                user.setPassword(new BCryptPasswordEncoder().encode("123456"));
                user.setDepartment(String.valueOf(row.get("department")));
                user.setMajor(String.valueOf(row.get("major")));
                user.setClassName(String.valueOf(row.get("className")));
                user.setAdvisor(String.valueOf(row.get("advisor")));
                user.setIdCard(String.valueOf(row.get("idCard")));
                user.setEmail(String.valueOf(row.get("email")));
                user.setNativePlace(String.valueOf(row.get("nativePlace")));
                user.setNation(String.valueOf(row.get("nation")));
                user.setEmergencyName(String.valueOf(row.get("emergencyName")));
                user.setEmergencyRelation(String.valueOf(row.get("emergencyRelation")));
                user.setEmergencyAddress(String.valueOf(row.get("emergencyAddress")));

                user.setEmergencyPhone(String.valueOf(row.get("emergencyPhone")));

                user.setCreateTime(LocalDateTime.now());

                // 出生日期
                Object birthday = row.get("birthday");
                if (birthday instanceof String && !((String) birthday).trim().isEmpty()) {
                    try {
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                        user.setBirthday(sdf.parse((String) birthday));
                    } catch (Exception e) {}
                }

                userMapper.insert(user);
                success++;
            } catch (Exception e) {
                fail++;
                errors.add("学号" + username + "：" + e.getMessage());
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("total", total);
        result.put("success", success);
        result.put("skip", skip);
        result.put("fail", fail);
        result.put("errors", errors);

        return Result.success(result);
    }

    // ==================== 下载导入模板 ====================
    @GetMapping("/template")
    public void downloadTemplate(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=student.xlsx");

        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheet = workbook.createSheet("学生信息");

        // 表头
        String[] headers = {"学号", "姓名", "性别", "手机", "院系", "专业", "班级", "辅导员",
                "身份证", "邮箱", "出生日期", "籍贯", "民族",
                "紧急联系人", "关系", "联系人手机", "联系人地址"};
        XSSFRow headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            XSSFCell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
        }

        // 示例数据
        XSSFRow row = sheet.createRow(1);
        String[] example = {"2021001", "张三", "男", "13800001001", "计算机学院", "计算机科学与技术",
                "计科2101", "刘老师", "440101200203150001", "zhangsan@xx.com",
                "2002-03-15", "广东省广州市", "汉族",
                "张父", "父亲", "13900001001", "广东省广州市天河区"};
        for (int i = 0; i < example.length; i++) {
            XSSFCell cell = row.createCell(i);
            cell.setCellValue(example[i]);
        }

        workbook.write(response.getOutputStream());
        workbook.close();
    }
}