package com.example.lightmanager.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.lightmanager.entity.InspectionExceptionRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface InspectionExceptionRecordMapper extends BaseMapper<InspectionExceptionRecord> {

    List<InspectionExceptionRecord> selectByExceptionId(@Param("exceptionId") Long exceptionId);
}
