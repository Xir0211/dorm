package com.xir.admin.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import java.util.Date;

@Data
@TableName("SYS_ROOM")
public class Room {
    @JsonSerialize(using = ToStringSerializer.class)
    @TableId(value = "ROOM_ID", type = IdType.INPUT)
    private Long roomId;
    private Long buildingId;
    private String buildingName;
    private Integer floorNum;
    private String roomNo;
    private Integer bedCount;
    private Integer occupied;
    private String status;       // AVAILABLE / FULL / EMPTY / MAINTENANCE
    private Date createTime;
}