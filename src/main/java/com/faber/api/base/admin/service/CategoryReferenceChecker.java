package com.faber.api.base.admin.service;

import java.util.Collection;

/**
 * 由业务模块实现，用于阻止删除仍被业务数据引用的分类。
 */
public interface CategoryReferenceChecker {
    boolean hasReference(Collection<Integer> categoryIds);
}
