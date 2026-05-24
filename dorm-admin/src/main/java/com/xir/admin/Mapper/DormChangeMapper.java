package com.xir.admin.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xir.admin.Entity.DormChange;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface DormChangeMapper extends BaseMapper<DormChange> {

    @Select("SELECT * FROM SYS_DORM_CHANGE WHERE " +
            "(CHANGE_TYPE IS NULL OR CHANGE_TYPE = 'EMPTY_BED' OR (CHANGE_TYPE = 'SWAP' AND SWAP_STATUS = 'AGREED')) " +
            "ORDER BY CREATE_TIME DESC " +
            "OFFSET #{offset} ROWS FETCH NEXT #{pageSize} ROWS ONLY")
    List<DormChange> selectWithFilter(@Param("offset") int offset, @Param("pageSize") int pageSize);

    @Select("SELECT COUNT(*) FROM SYS_DORM_CHANGE WHERE " +
            "(CHANGE_TYPE IS NULL OR CHANGE_TYPE = 'EMPTY_BED' OR (CHANGE_TYPE = 'SWAP' AND SWAP_STATUS = 'AGREED'))")
    long countWithFilter();
}