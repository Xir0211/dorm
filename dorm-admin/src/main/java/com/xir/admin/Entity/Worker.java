package com.xir.admin.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.util.Date;

@Data
@TableName("SYS_WORKER")
public class Worker {
    @TableId(value = "ID", type = IdType.INPUT)
    private Long id;
    private String name;
    private String phone;
    private String type;
    private String status;
    private Date createTime;
}