package com.xir.admin.Control;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xir.admin.Common.Result;
import com.xir.admin.Entity.Worker;
import com.xir.admin.Mapper.WorkerMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/worker")
public class WorkerController {

    @Autowired private WorkerMapper workerMapper;
    @Autowired private JdbcTemplate jdbcTemplate;

    @GetMapping("/all")
    public Result all() {
        List<Worker> list = workerMapper.selectList(new QueryWrapper<Worker>().eq("STATUS", "IDLE"));
        return Result.success(list);
    }

    @GetMapping("/list")
    public Result list() {
        List<Worker> list = workerMapper.selectList(new QueryWrapper<Worker>().orderByAsc("ID"));
        return Result.success(list);
    }

    @PostMapping("/add")
    public Result add(@RequestBody Worker worker) {
        worker.setId(jdbcTemplate.queryForObject("SELECT SEQ_WORKER_ID.NEXTVAL FROM DUAL", Long.class));
        worker.setCreateTime(new Date());
        workerMapper.insert(worker);
        return Result.success("添加成功");
    }

    @PutMapping("/update")
    public Result update(@RequestBody Worker worker) {
        workerMapper.updateById(worker);
        return Result.success("更新成功");
    }

    @DeleteMapping("/delete/{id}")
    public Result delete(@PathVariable Long id) {
        workerMapper.deleteById(id);
        return Result.success("删除成功");
    }
}