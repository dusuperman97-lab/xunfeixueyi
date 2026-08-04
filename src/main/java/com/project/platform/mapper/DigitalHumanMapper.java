package com.project.platform.mapper;

import com.project.platform.entity.DigitalHuman;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;


public interface DigitalHumanMapper {
    List<DigitalHuman> queryPage(Integer offset, Integer pageSize, @Param("query") Map<String, Object> query);

    int queryCount(@Param("query") Map<String, Object> query);

    @Select("SELECT * FROM digital_human WHERE id = #{id}")
    DigitalHuman selectById(Integer id);

    @Select("SELECT * FROM digital_human")
    List<DigitalHuman> list();

    int insert(DigitalHuman entity);

    int updateById(DigitalHuman entity);

    boolean removeByIds(List<Integer> ids);

    @Select("SELECT * FROM digital_human WHERE digital_human_id = #{digitalHumanId}")
    DigitalHuman selectByDigitalHumanId(String digitalHumanId);
}