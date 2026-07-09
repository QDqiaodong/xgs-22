package com.example.lightmanager.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.lightmanager.entity.GarageZone;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface GarageZoneMapper extends BaseMapper<GarageZone> {
    
    List<GarageZone> selectTreeByParentId(Long parentId);
    
    List<GarageZone> selectAllZones();
}