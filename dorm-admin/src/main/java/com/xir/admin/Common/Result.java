package com.xir.admin.Common;



import lombok.Data;

@Data
public class Result<T> {
    private Integer code;    // 200 成功，500 失败
    private String msg;      // 提示消息
    private T data;          // 返回的数据

    public static <T> Result<T> success(T data) {
        Result<T> r = new Result<>();
        r.setCode(200);
        r.setMsg("操作成功");
        r.setData(data);
        return r;
    }

    public static <T> Result<T> error(String msg) {
        Result<T> r = new Result<>();
        r.setCode(500);
        r.setMsg(msg);
        return r;
    }
}
