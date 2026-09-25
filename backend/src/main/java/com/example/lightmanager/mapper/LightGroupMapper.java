package com.example.lightmanager.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.lightmanager.entity.LightGroup;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface LightGroupMapper extends BaseMapper<LightGroup> {
    
    IPage<LightGroup> selectWithZoneNamePage(IPage<LightGroup> page,
                                              @Param("zoneId") Long zoneId, 
                                              @Param("groupCode") String groupCode, 
                                              @Param("status") Integer status);
    
    List<LightGroup> selectByIds(@Param("ids") List<Long> ids);
    
    int batchUpdateZoneId(@Param("ids") List<Long> ids, @Param("zoneId") Long zoneId);
    
    List<LightGroup> selectByZoneId(@Param("zoneId") Long zoneId);

    List<LightGroup> selectByZoneIds(@Param("zoneIds") List<Long> zoneIds);
}