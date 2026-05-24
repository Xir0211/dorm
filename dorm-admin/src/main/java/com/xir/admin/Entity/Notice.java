package com.xir.admin.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.util.Date;

@Data
@TableName("SYS_NOTICE")
public class Notice {
    @TableId(value = "ID", type = IdType.INPUT)
    private Long id;
    private String title;
    private String content;
    private String status;       // PUBLISHED / DRAFT
    private Date createTime;
    private Date updateTime;
}