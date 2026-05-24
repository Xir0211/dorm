package com.xir.admin.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.util.Date;

@Data
@TableName("SYS_LEAVE")
public class Leave {
    @TableId(value = "ID", type = IdType.INPUT)
    private Long id;
    private Long studentId;
    private String studentName;
    private String studentNo;
    private String type;
    private Date startDate;
    private Date endDate;
    private String reason;
    private String destination;
    private String emergencyPhone;
    private String imageUrl;
    private String status;
    private String buildingName;
    private String roomNo;
    private String approver;
    private Date approveTime;
    private String approveOpinion;
    private Date createTime;
}