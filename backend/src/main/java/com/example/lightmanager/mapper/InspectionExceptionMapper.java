package com.example.lightmanager.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.lightmanager.entity.InspectionException;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

@Mapper
public interface InspectionExceptionMapper extends BaseMapper<InspectionException> {

    /** 异常清单分页: 关联灯组实时分区, 计算当前分区是否已相对巡检快照变化 */
    IPage<InspectionException> selectExceptionPage(IPage<InspectionException> page,
                                                   @Param("batchNo") String batchNo,
                                                   @Param("groupCode") String groupCode,
                                                   @Param("exceptionType") String exceptionType,
                                                   @Param("status") String status,
                                                   @Param("reporter") String reporter,
                                                   @Param("zoneChanged") Integer zoneChanged);

    /** 详情: 同样带实时分区比对字段 */
    InspectionException selectDetailById(@Param("id") Long id);

    /** 退回: PENDING -> RETURNED */
    int returnException(@Param("id") Long id,
                        @Param("expectedVersion") Integer expectedVersion,
                        @Param("now") LocalDateTime now);

    /** 补充: RETURNED -> PENDING, 更新现场说明(原始说明保留不动) */
    int supplementException(@Param("id") Long id,
                            @Param("expectedVersion") Integer expectedVersion,
                            @Param("siteDescription") String siteDescription,
                            @Param("now") LocalDateTime now);

    /** 确认: PENDING -> CONFIRMED, 写入处理意见、处理时限、确认人 */
    int confirmException(@Param("id") Long id,
                         @Param("expectedVersion") Integer expectedVersion,
                         @Param("reviewOpinion") String reviewOpinion,
                         @Param("handleDeadline") LocalDateTime handleDeadline,
                         @Param("reviewer") String reviewer,
                         @Param("now") LocalDateTime now);

    /** 登记处理结果: CONFIRMED 状态不变, 写入处理人/结果/时间; 仍按版本号防并发重复提交 */
    int registerHandleResult(@Param("id") Long id,
                             @Param("expectedVersion") Integer expectedVersion,
                             @Param("handler") String handler,
                             @Param("handleResult") String handleResult,
                             @Param("now") LocalDateTime now);

    /** 关闭: CONFIRMED -> CLOSED, 终态不可逆 */
    int closeException(@Param("id") Long id,
                       @Param("expectedVersion") Integer expectedVersion,
                       @Param("closer") String closer,
                       @Param("now") LocalDateTime now);
}
