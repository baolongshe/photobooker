package com.xhxi.photobooker.result;

import com.xhxi.photobooker.entity.Order;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 封装分页查询结果
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PageResult<T> {

    private long total; //总记录数

    private List<T> records; //当前页数据集合

    public static <T> PageResult<T> success(long total, List<T> records) {
        PageResult<T> result = new PageResult<T>();
        result.records = records;
        result.total = total;
        return result;
    }

}
