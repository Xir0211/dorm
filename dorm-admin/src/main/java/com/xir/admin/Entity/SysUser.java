package com.xir.admin.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Date;

@Data
@TableName("SYS_USER") // 对应 Oracle 表名
public class SysUser {
    @TableId(type = IdType.AUTO) // Oracle 12c+ 的 Identity 方式
    private Long userId;
    private String username;
    private String password;
    private String realName;
    private String role; // ADMIN 或 STUDENT
    private String avatar;
    private LocalDateTime createTime;

    @TableField(exist = false)  // ✅ 这个必须有，数据库不存在
    private String inviteCode;

    private Long roomId;
    private String gender;
    private String department;
    private String phone;
    private String bedNo;
    private Date checkinTime;

    private String className;
    private String major;
    private String idCard;
    private String email;
    private Date birthday;
    private String nativePlace;
    private String nation;
    private String advisor;
    private String feeStatus;
    private String emergencyName;
    private String emergencyRelation;
    private String emergencyPhone;
    private String emergencyAddress;
    private String status; // 1=正常 0=禁用
}
