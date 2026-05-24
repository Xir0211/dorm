package com.xir.admin.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xir.admin.Entity.SysUser;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<SysUser> {
    // MyBatis Plus 会自动实现基础 CRUD
}
