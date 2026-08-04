package com.project.platform.service.impl;

import com.project.platform.entity.DigitalHuman;
import com.project.platform.exception.CustomException;
import com.project.platform.mapper.DigitalHumanMapper;
import com.project.platform.service.DigitalHumanService;
import com.project.platform.vo.PageVO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 数字人信息表 服务实现类
 * </p>
 */
@Service
public class DigitalHumanServiceImpl implements DigitalHumanService {
    @Resource
    private DigitalHumanMapper digitalHumanMapper;

    @Override
    public PageVO<DigitalHuman> page(Map<String, Object> query, Integer pageNum, Integer pageSize) {
        PageVO<DigitalHuman> page = new PageVO();
        List<DigitalHuman> list = digitalHumanMapper.queryPage((pageNum - 1) * pageSize, pageSize, query);
        page.setList(list);
        page.setTotal(digitalHumanMapper.queryCount(query));
        return page;
    }

    @Override
    public DigitalHuman selectById(Integer id) {
        DigitalHuman digitalHuman = digitalHumanMapper.selectById(id);
        return digitalHuman;
    }

    @Override
    public List<DigitalHuman> list() {
        return digitalHumanMapper.list();
    }

    @Override
    public void insert(DigitalHuman entity) {
        check(entity);
        if (entity.getStatus() == null) {
            entity.setStatus("启用");
        }
        entity.setCreateTime(LocalDateTime.now());
        digitalHumanMapper.insert(entity);
    }

    @Override
    public void updateById(DigitalHuman entity) {
        check(entity);
        digitalHumanMapper.updateById(entity);
    }

    private void check(DigitalHuman entity) {
        DigitalHuman digitalHuman = digitalHumanMapper.selectByDigitalHumanId(entity.getDigitalHumanId());
        if (digitalHuman != null && digitalHuman.getId() != entity.getId()) {
            throw new CustomException("数字人ID已存在");
        }
    }

    @Override
    public void removeByIds(List<Integer> ids) {
        digitalHumanMapper.removeByIds(ids);
    }

}