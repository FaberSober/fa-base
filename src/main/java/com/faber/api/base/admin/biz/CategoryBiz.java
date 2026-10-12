package com.faber.api.base.admin.biz;

import com.faber.api.base.admin.entity.Category;
import com.faber.api.base.admin.enums.CategoryModuleEnum;
import com.faber.api.base.admin.mapper.CategoryMapper;
import com.faber.api.base.admin.service.CategoryReferenceChecker;
import com.faber.core.constant.CommonConstants;
import com.faber.core.exception.BuzzException;
import com.faber.core.vo.tree.TreeNode;
import com.faber.core.vo.tree.TreePosChangeVo;
import com.faber.core.web.biz.BaseTreeBiz;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

@Service
public class CategoryBiz extends BaseTreeBiz<CategoryMapper, Category> {

    /**
     * 延迟解析业务模块的检查器，避免分类服务与资源服务在启动阶段形成依赖环。
     */
    private final ObjectProvider<CategoryReferenceChecker> referenceCheckerProvider;

    public CategoryBiz(ObjectProvider<CategoryReferenceChecker> referenceCheckerProvider) {
        this.referenceCheckerProvider = referenceCheckerProvider;
    }

    @Override
    protected void saveBefore(Category entity) {
        Category old = entity.getId() == null ? null : getById(entity.getId());
        if (entity.getId() != null && old == null) {
            throw new BuzzException("分类不存在");
        }
        if (old != null) {
            entity.setModule(old.getModule());
        }
        if (entity.getModule() == null) {
            throw new BuzzException("所属模块不能为空");
        }
        if (entity.getParentId() == null) {
            entity.setParentId(CommonConstants.ROOT);
        }
        validateParentModule(entity.getParentId(), entity.getModule());
    }

    @Override
    public List<TreeNode<Category>> allTree() {
        throw new UnsupportedOperationException("请按所属模块查询分类树");
    }

    public List<TreeNode<Category>> allTree(CategoryModuleEnum module) {
        return listToTree(lambdaQuery()
                .eq(Category::getModule, module)
                .orderByAsc(Category::getSort)
                .list(), getRootId());
    }

    public Category requireById(Integer id, CategoryModuleEnum module) {
        if (id == null) return null;
        Category category = getById(id);
        if (category == null || category.getModule() != module) {
            throw new BuzzException("所属分类不存在或不匹配当前模块");
        }
        return category;
    }

    public List<Category> findAllChildren(Integer id, CategoryModuleEnum module) {
        requireById(id, module);
        return super.findAllChildren(id);
    }

    @Override
    protected void enhanceTreeQueryForMaxSort(QueryWrapper<Category> wrapper, Category entity) {
        wrapper.eq("module", entity.getModule());
    }

    @Override
    public void changePos(List<TreePosChangeVo> list) {
        if (list == null || list.isEmpty()) return;
        for (TreePosChangeVo item : list) {
            Integer id = toInteger(item.getKey());
            Integer parentId = toInteger(item.getPid());
            Category category = getById(id);
            if (category == null) throw new BuzzException("分类不存在");
            validateParentModule(parentId, category.getModule());
        }
        super.changePos(list);
    }

    @Override
    public boolean removeById(Serializable id) {
        Integer categoryId = toInteger(id);
        List<Category> categories = super.findAllChildren(categoryId);
        List<Integer> categoryIds = categories.stream().filter(Objects::nonNull).map(Category::getId).toList();
        if (referenceCheckerProvider.orderedStream().anyMatch(checker -> checker.hasReference(categoryIds))) {
            throw new BuzzException("分类或其子分类下仍有关联数据，无法删除");
        }
        return super.removeById(categoryId);
    }

    @Override
    public void removeBatchByIds(List<Serializable> ids) {
        if (ids == null || ids.isEmpty()) return;
        for (Serializable id : ids) {
            removeById(id);
        }
    }

    private void validateParentModule(Integer parentId, CategoryModuleEnum module) {
        if (parentId == null || parentId.equals(CommonConstants.ROOT)) return;
        Category parent = getById(parentId);
        if (parent == null || parent.getModule() != module) {
            throw new BuzzException("上级分类不存在或不属于当前模块");
        }
    }

    private Integer toInteger(Object value) {
        if (value == null) return null;
        try {
            return Integer.valueOf(value.toString());
        } catch (NumberFormatException e) {
            throw new BuzzException("分类ID格式错误");
        }
    }
}
