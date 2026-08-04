package com.project.platform.service;

import com.project.platform.entity.DigitalHuman;
import com.project.platform.vo.PageVO;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 数字人信息表 服务类
 * </p>
 */
public interface DigitalHumanService {

    PageVO<DigitalHuman> page(Map<String, Object> query, Integer pageNum, Integer pageSize);

    DigitalHuman selectById(Integer id);

    List<DigitalHuman> list();

    void insert(DigitalHuman entity);

    void updateById(DigitalHuman entity);

    void removeByIds(List<Integer> ids);

}