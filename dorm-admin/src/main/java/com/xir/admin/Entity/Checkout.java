package com.xir.admin.Entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.util.Date;

@Data
@TableName("SYS_CHECKOUT")
public class Checkout {
    @TableId(value = "ID", type = com.baomidou.mybatisplus.annotation.IdType.INPUT)
    private Long id;
    private String checkoutNo;
    private Long studentId;
    private String studentName;
    private String studentNo;
    @TableField("BUILDING_NAME")
    private String buildingName;
    @TableField("ROOM_NO")
    private String roomNo;
    private String bedNo;
    private String reason;
    private String status;
    private Date applyTime;
    private Date approveTime;
    private String approver;
    private Date createTime;
    private String destination;




}