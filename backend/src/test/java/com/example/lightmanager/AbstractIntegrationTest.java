package com.example.lightmanager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.junit.jupiter.api.BeforeEach;

import java.util.Arrays;

/**
 * 集成测试基类:
 * - MockBean 替换 RedisTemplate, 不依赖 Redis;
 * - 统一 @AutoConfigureMockMvc 复用同一 Spring 上下文;
 * - 每个用例前清空巡检业务数据并复位灯组分区, 保证用例间互不影响。
 */
@AutoConfigureMockMvc
public abstract class AbstractIntegrationTest {

    @MockBean
    protected RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void resetDatabase() {
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");
        for (String table : Arrays.asList(
                "inspection_exception_record",
                "inspection_exception",
                "inspection_item",
                "inspection_batch")) {
            jdbcTemplate.update("DELETE FROM " + table);
        }
        // 复位灯组到初始分区
        jdbcTemplate.update("UPDATE light_group SET zone_id = 1 WHERE id IN (1,2,3)");
        jdbcTemplate.update("UPDATE light_group SET zone_id = 2 WHERE id = 4");
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");
    }
}
