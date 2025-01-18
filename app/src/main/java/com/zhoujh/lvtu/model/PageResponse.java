package com.zhoujh.lvtu.model;

import java.util.List;

public class PageResponse<T> {
    private List<T> records; // 当前页的数据列表
    private int total;       // 总记录数
    private int size;        // 每页大小
    private int current;     // 当前页码
    private int pages;       // 总页数

    // Getter 和 Setter
    public List<T> getRecords() {
        return records;
    }

    public void setRecords(List<T> records) {
        this.records = records;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getCurrent() {
        return current;
    }

    public void setCurrent(int current) {
        this.current = current;
    }

    public int getPages() {
        return pages;
    }

    public void setPages(int pages) {
        this.pages = pages;
    }
}
