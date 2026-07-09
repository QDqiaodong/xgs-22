package com.example.lightmanager.service.impl;

import com.example.lightmanager.dto.PageRequestDTO;
import com.example.lightmanager.entity.LightGroup;
import com.example.lightmanager.mapper.LightGroupMapper;
import com.example.lightmanager.service.LightGroupService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LightGroupServiceImpl implements LightGroupService {
    
    private final LightGroupMapper lightGroupMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    
    public LightGroupServiceImpl(LightGroupMapper lightGroupMapper, RedisTemplate<String, Object> redisTemplate) {
        this.lightGroupMapper = lightGroupMapper;
        this.redisTemplate = redisTemplate;
    }
    
    private static final String LIGHT_GROUP_POWER_KEY = "light_group:power:";
    
    @Override
    public LightGroup createLightGroup(LightGroup lightGroup) {
        lightGroupMapper.insert(lightGroup);
        updateRedisPower(lightGroup);
        return lightGroup;
    }
    
    @Override
    public LightGroup updateLightGroup(LightGroup lightGroup) {
        LightGroup old = lightGroupMapper.selectById(lightGroup.getId());
        lightGroupMapper.updateById(lightGroup);
        if (old != null && !old.getZoneId().equals(lightGroup.getZoneId())) {
            removeRedisPower(old);
        }
        updateRedisPower(lightGroup);
        return lightGroup;
    }
    
    @Override
    public void deleteLightGroup(Long id) {
        LightGroup lightGroup = lightGroupMapper.selectById(id);
        if (lightGroup != null) {
            removeRedisPower(lightGroup);
        }
        lightGroupMapper.deleteById(id);
    }
    
    @Override
    public LightGroup getLightGroupById(Long id) {
        return lightGroupMapper.selectById(id);
    }
    
    @Override
    public IPage<LightGroup> getLightGroupPage(PageRequestDTO request) {
        Page<LightGroup> page = new Page<>(request.getPageNum(), request.getPageSize());
        return lightGroupMapper.selectWithZoneNamePage(page, request.getZoneId(), 
                request.getGroupCode(), request.getStatus());
    }
    
    @Override
    public List<LightGroup> getLightGroupsByIds(List<Long> ids) {
        return lightGroupMapper.selectByIds(ids);
    }
    
    @Override
    public List<LightGroup> getLightGroupsByZoneId(Long zoneId) {
        return lightGroupMapper.selectByZoneId(zoneId);
    }
    
    private void updateRedisPower(LightGroup lightGroup) {
        String key = LIGHT_GROUP_POWER_KEY + lightGroup.getZoneId();
        redisTemplate.opsForZSet().add(key, lightGroup.getGroupCode(), lightGroup.getPower());
    }
    
    private void removeRedisPower(LightGroup lightGroup) {
        String key = LIGHT_GROUP_POWER_KEY + lightGroup.getZoneId();
        redisTemplate.opsForZSet().remove(key, lightGroup.getGroupCode());
    }
}