package com.xir.admin.Control;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xir.admin.Common.Result;
import com.xir.admin.Entity.Notice;
import com.xir.admin.Mapper.NoticeMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/notice")
public class NoticeController {

    @Autowired private NoticeMapper noticeMapper;
    @Autowired private JdbcTemplate jdbcTemplate;

    // ==================== 列表 ====================
    @GetMapping("/list")
    public Result list(@RequestParam(defaultValue = "1") int page,
                       @RequestParam(defaultValue = "10") int pageSize,
                       @RequestParam(required = false) String keyword,
                       @RequestParam(required = false) String status) {
        QueryWrapper<Notice> wrapper = new QueryWrapper<>();
        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.like("TITLE", keyword.trim());
        }
        if (status != null && !status.trim().isEmpty()) {
            wrapper.eq("STATUS", status.trim());
        }
        wrapper.orderByDesc("CREATE_TIME");

        long total = noticeMapper.selectCount(wrapper);
        int offset = (page - 1) * pageSize;
        wrapper.last("OFFSET " + offset + " ROWS FETCH NEXT " + pageSize + " ROWS ONLY");
        List<Notice> list = noticeMapper.selectList(wrapper);

        Map<String, Object> data = new HashMap<>();
        data.put("list", list);
        data.put("total", total);
        data.put("page", page);
        data.put("pageSize", pageSize);

        return Result.success(data);
    }

    // ==================== 发布/保存草稿 ====================
    @PostMapping("/add")
    public Result add(@RequestBody Notice notice) {
        Long id = jdbcTemplate.queryForObject("SELECT SEQ_NOTICE_ID.NEXTVAL FROM DUAL", Long.class);
        notice.setId(id);
        notice.setCreateTime(new Date());
        noticeMapper.insert(notice);
        return Result.success(notice.getStatus().equals("PUBLISHED") ? "发布成功" : "草稿已保存");
    }

    // ==================== 编辑 ====================
    @PutMapping("/update")
    public Result update(@RequestBody Notice notice) {
        notice.setUpdateTime(new Date());
        noticeMapper.updateById(notice);
        return Result.success("更新成功");
    }

    // ==================== 发布草稿 ====================
    @PutMapping("/publish/{id}")
    public Result publish(@PathVariable Long id) {
        Notice notice = noticeMapper.selectById(id);
        if (notice == null) return Result.error("公告不存在");
        notice.setStatus("PUBLISHED");
        notice.setUpdateTime(new Date());
        noticeMapper.updateById(notice);
        return Result.success("发布成功");
    }

    // ==================== 删除 ====================
    @DeleteMapping("/delete/{id}")
    public Result delete(@PathVariable Long id) {
        noticeMapper.deleteById(id);
        return Result.success("删除成功");
    }

    // NoticeController.java
    @GetMapping("/detail/{id}")
    public Result detail(@PathVariable Long id) {
        Notice notice = noticeMapper.selectById(id);
        return Result.success(notice);
    }
}