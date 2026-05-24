package com.xir.admin.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.util.Date;

@Data
@TableName("SYS_REPAIR")
public class Repair {
    @TableId(value = "ID", type = IdType.INPUT)
    private Long id;
    private String repairNo;
    private Long studentId;
    private String studentName;
    private String studentNo;
    private Long buildingId;
    private String buildingName;
    private Long roomId;
    private String roomNo;
    private String description;
    private String status;       // PENDING / PROCESSING / COMPLETED
    private String handler;
    private Date handleTime;
    private String handleResult;
    private Date createTime;

    private Long workerId;
    private String workerName;
    private String workerPhone;
    private String repairType;
    private Date assignTime;
    private Double cost;
    private Integer rating;
    private String remark;
    private String imageUrl;
}