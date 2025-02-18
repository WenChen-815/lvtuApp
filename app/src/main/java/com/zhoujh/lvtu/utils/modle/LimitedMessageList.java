package com.zhoujh.lvtu.utils.modle;

import java.util.LinkedList;

/**
 *  选择继承LinkedList是因为LinkedList实现了 List 接口和 Deque 接口，基于双向链表实现，适合频繁的插入和删除操作，但随机访问性能较差
 *
 * @param <E>
 */
public class  LimitedMessageList<E> extends LinkedList<E> {
    private final int maxSize;

    public LimitedMessageList(int maxSize) {
        this.maxSize = maxSize;
    }

    /**
     * 向集合中添加一个元素，并确保集合的大小不超过指定的最大容量。
     * 如果添加元素后集合的大小超过了最大容量，则移除最早添加的元素。
     *
     * @param e 要添加到集合中的元素
     * @return 总是返回 true，表示元素已成功添加
     */
    @Override
    public boolean add(E e) {
        // 调用父类的 add 方法将元素添加到集合中
        super.add(e);

        // 如果集合的大小超过了最大容量，移除最早添加的元素
        while (size() > maxSize) {
            super.removeFirst();
        }

        return true;
    }
}
