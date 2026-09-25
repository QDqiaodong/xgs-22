package com.example.lightmanager;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 通过 MockMvc 走完整 HTTP 层, 模拟验收中的"两个并发请求复核同一条异常"。
 */
@SpringBootTest
class InspectionHttpIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper om = new ObjectMapper();

    @SuppressWarnings("unchecked")
    private JsonNode postJson(String url, Map<String, Object> body) throws Exception {
        MvcResult result = mockMvc.perform(post(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding("UTF-8")
                        .content(om.writeValueAsString(body)))
                .andReturn();
        result.getResponse().setCharacterEncoding("UTF-8");
        return om.readTree(result.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
    }

    @Test
    void http_fullFlowAndConcurrentConfirm() throws Exception {
        // 创建批次
        Map<String, Object> createBody = new HashMap<>();
        createBody.put("inspector", "HTTP巡检员");
        createBody.put("zoneIds", List.of(1));
        JsonNode created = postJson("/api/inspection/batch", createBody);
        assertEquals(200, created.get("code").asInt());
        long batchId = created.get("data").get("id").asLong();

        // 登记结果
        Map<String, Object> saveBody = new HashMap<>();
        saveBody.put("batchId", batchId);
        List<Map<String, Object>> items = new ArrayList<>();
        items.add(Map.of("lightGroupId", 1, "inspectResult", "OFF", "siteDescription", "http-1熄灭"));
        items.add(Map.of("lightGroupId", 2, "inspectResult", "NORMAL"));
        items.add(Map.of("lightGroupId", 3, "inspectResult", "NORMAL"));
        saveBody.put("items", items);
        JsonNode saved = postJson("/api/inspection/items/save", saveBody);
        assertEquals(200, saved.get("code").asInt());

        // 异常缺说明 -> 400
        Map<String, Object> badSave = new HashMap<>();
        badSave.put("batchId", batchId);
        badSave.put("items", List.of(Map.of("lightGroupId", 3, "inspectResult", "FLICKER")));
        JsonNode badResp = postJson("/api/inspection/items/save", badSave);
        assertEquals(400, badResp.get("code").asInt());
        assertTrue(badResp.get("message").asText().contains("现场说明"));

        // 提交
        JsonNode submitted = postJson("/api/inspection/batch/submit", Map.of("batchId", batchId));
        assertEquals(200, submitted.get("code").asInt());
        assertEquals(1, submitted.get("data").get("abnormalCount").asInt());

        // 异常清单
        MvcResult pageResult = mockMvc.perform(get("/api/inspection/exception/page")
                        .param("batchNo", submitted.get("data").get("batchNo").asText()))
                .andExpect(status().isOk()).andReturn();
        pageResult.getResponse().setCharacterEncoding("UTF-8");
        JsonNode page = om.readTree(pageResult.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
        JsonNode ex = page.get("data").get(0);
        long exId = ex.get("id").asLong();
        assertEquals("PENDING", ex.get("status").asText());
        assertEquals(0, ex.get("version").asInt());

        // 两个并发确认请求(都基于 version=0)
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        List<JsonNode> results = java.util.Collections.synchronizedList(new ArrayList<>());
        for (int i = 0; i < 2; i++) {
            final String reviewer = i == 0 ? "HTTP复核甲" : "HTTP复核乙";
            pool.submit(() -> {
                try {
                    start.await();
                    Map<String, Object> body = new HashMap<>();
                    body.put("exceptionId", exId);
                    body.put("version", 0);
                    body.put("operator", reviewer);
                    body.put("reviewOpinion", reviewer + "意见");
                    body.put("handleDeadline", "2099-01-01 12:00:00");
                    results.add(postJson("/api/inspection/exception/confirm", body));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }
        start.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(30, TimeUnit.SECONDS));

        long ok = results.stream().filter(r -> r.get("code").asInt() == 200).count();
        long conflict = results.stream().filter(r -> r.get("code").asInt() == 409).count();
        assertEquals(1, ok, "只有一个确认成功");
        assertEquals(1, conflict, "另一个必须是 409 冲突");

        JsonNode loser = results.stream().filter(r -> r.get("code").asInt() == 409).findFirst().orElseThrow();
        assertTrue(loser.get("message").asText().contains("已被其他人"), loser.get("message").asText());

        // 详情: CONFIRMED + version=1 + 仅一条 CONFIRM 记录
        MvcResult detailResult = mockMvc.perform(get("/api/inspection/exception/" + exId))
                .andExpect(status().isOk()).andReturn();
        detailResult.getResponse().setCharacterEncoding("UTF-8");
        JsonNode detail = om.readTree(detailResult.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
        JsonNode exception = detail.get("data").get("exception");
        assertEquals("CONFIRMED", exception.get("status").asText());
        assertEquals(1, exception.get("version").asInt());
        JsonNode records = detail.get("data").get("records");
        int confirmCount = 0;
        for (JsonNode r : records) {
            if ("CONFIRM".equals(r.get("action").asText())) confirmCount++;
        }
        assertEquals(1, confirmCount, "确认记录不能重复生成");
    }
}
