package com.example.lightmanager.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.lightmanager.entity.InspectionItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface InspectionItemMapper extends BaseMapper<InspectionItem> {

    /** 草稿登记/补录: 仅当批次仍为暂存、且该灯组结果尚未提交为异常时才允许写入, 保证原始结果不被覆盖 */
    int upsertDraftItem(@Param("item") InspectionItem item);

    List<InspectionItem> selectByBatchId(@Param("batchId") Long batchId);

    int countRecordedByBatchId(@Param("batchId") Long batchId);
}
