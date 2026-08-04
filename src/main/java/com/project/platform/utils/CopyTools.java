package com.project.platform.utils;

import org.springframework.beans.BeanUtils;

import java.util.ArrayList;
import java.util.List;

public class CopyTools {
    public static <T, S> List<T> copyList(List<S> sList, Class<T> classz) {
        List<T> list = new ArrayList<T>();
        for (S s : sList) {
            T t = null;
            try {
                t = classz.newInstance();
            } catch (Exception e) {
            }
            BeanUtils.copyProperties(s, t);
            list.add(t);
        }
        return list;
    }
    public static <T, S> T copy(S s, Class < T > classz)
    {
        T t = null;
        try
        {
            //通过Class.newInstance()反射创建对象
            t = classz.newInstance();
        }
        catch (Exception e)
        {
        }
        // 复制属性
        BeanUtils.copyProperties(s,t);
        return t;
    }
}
