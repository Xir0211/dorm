package com.xir.admin.Control;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xir.admin.Common.Result;
import com.xir.admin.Entity.Building;
import com.xir.admin.Mapper.BuildingMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/building")
public class BuildingController {

    @Autowired
    private BuildingMapper buildingMapper;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    // ==================== 楼栋列表（分页 + 搜索 + 筛选） ====================
    @GetMapping("/list")
    public Result list(@RequestParam(defaultValue = "1") int page,
                       @RequestParam(defaultValue = "10") int pageSize,
                       @RequestParam(required = false) String keyword,
                       @RequestParam(required = false) String status) {
        QueryWrapper<Building> wrapper = new QueryWrapper<>();

        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.like("BUILDING_NAME", keyword.trim());
        }
        if (status != null && !status.trim().isEmpty()) {
            wrapper.eq("STATUS", status.trim());
        }
        wrapper.orderByAsc("BUILDING_ID");

        // 分页
        long total = buildingMapper.selectCount(wrapper);
        int offset = (page - 1) * pageSize;
        wrapper.last("OFFSET " + offset + " ROWS FETCH NEXT " + pageSize + " ROWS ONLY");
        List<Building> list = buildingMapper.selectList(wrapper);

        Map<String, Object> data = new HashMap<>();
        data.put("list", list);
        data.put("total", total);
        data.put("page", page);
        data.put("pageSize", pageSize);

        return Result.success(data);
    }

    // ==================== 添加楼栋 ====================
    @PostMapping("/add")
    public Result add(@RequestBody Building building) {
        Long id = jdbcTemplate.queryForObject("SELECT SEQ_BUILDING_ID.NEXTVAL FROM DUAL", Long.class);
        building.setBuildingId(id);
        building.setCreateTime(new Date());
        buildingMapper.insert(building);
        return Result.success("添加成功");
    }

    // ==================== 编辑楼栋 ====================
    @PutMapping("/update")
    public Result update(@RequestBody Building building) {
        buildingMapper.updateById(building);
        return Result.success("修改成功");
    }

    // ==================== 删除楼栋 ====================
    @DeleteMapping("/delete/{id}")
    public Result delete(@PathVariable Long id) {
        buildingMapper.deleteById(id);
        return Result.success("删除成功");
    }

    // ==================== 楼栋详情 ====================
    @GetMapping("/detail/{id}")
    public Result detail(@PathVariable Long id) {
        Building building = buildingMapper.selectById(id);
        return Result.success(building);
    }

    // ==================== 全部楼栋（下拉选择用） ====================
    @GetMapping("/all")
    public Result all() {
        List<Building> list = buildingMapper.selectList(
                new QueryWrapper<Building>().eq("STATUS", "ACTIVE").orderByAsc("BUILDING_ID")
        );
        return Result.success(list);
    }
}