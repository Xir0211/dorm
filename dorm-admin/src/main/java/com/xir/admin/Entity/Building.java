package com.xir.admin.Entity;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import java.util.Date;

@Data
@TableName("SYS_BUILDING")
public class Building {
    @JsonSerialize(using = ToStringSerializer.class)
    @TableId(value = "BUILDING_ID", type = IdType.INPUT)
    private Long buildingId;
    private String buildingName;
    private Integer floorCount;
    private Integer roomCount;
    private String status;        // ACTIVE / MAINTENANCE / DISABLED
    private Date createTime;
}