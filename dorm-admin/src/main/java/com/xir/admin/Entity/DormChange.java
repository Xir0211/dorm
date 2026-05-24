package com.xir.admin.Entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.util.Date;

@Data
@TableName("SYS_DORM_CHANGE")
public class DormChange {
    @TableId
    private Long id;
    private String changeNo;
    private Long studentId;
    private String studentName;
    private String studentNo;
    private Long oldBuildingId;
    private String oldBuildingName;
    private Long oldRoomId;
    private String oldRoomNo;
    private String oldBedNo;
    private Long newBuildingId;
    private String newBuildingName;
    private Long newRoomId;
    private String newRoomNo;
    private String newBedNo;
    private String reason;
    private String status;       // PENDING / APPROVED / REJECTED
    private Date applyTime;
    private Date approveTime;
    private String approver;
    private String approveOpinion;
    private Date createTime;

    private String changeType;      // EMPTY_BED / SWAP
    private String swapStatus;      // WAITING / AGREED / REJECTED
    private Long swapStudentId;
    private String swapStudentName;
    private String swapStudentNo;
    private String swapBuildingName;
    private String swapRoomNo;
    private String swapBedNo;
}