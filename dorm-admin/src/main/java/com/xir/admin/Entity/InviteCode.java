package com.xir.admin.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.util.Date;

@Data
@TableName("SYS_INVITE_CODE")
public class InviteCode {
    @TableId(value = "ID", type = IdType.INPUT)
    private Long id;
    private String code;
    private String status;       // 1=可用 0=已使用 2=已过期
    private String creator;
    private String usedBy;
    private Date usedTime;
    private Date expireTime;
    private Date createTime;
}