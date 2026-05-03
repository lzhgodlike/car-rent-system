package com.sdjzu.carrental.common;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class PageResult<T> {

    private List<T> records;
    private long total;
    private long pageNum;
    private long pageSize;
    private Map<String, Object> summary = new HashMap<>();

    public PageResult() {
    }

    public PageResult(List<T> records, long total, long pageNum, long pageSize) {
        this.records = records;
        this.total = total;
        this.pageNum = pageNum;
        this.pageSize = pageSize;
    }

    public static <T> PageResult<T> of(IPage<T> page) {
        return new PageResult<>(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    public PageResult<T> summary(String key, Object value) {
        this.summary.put(key, value);
        return this;
    }
}
