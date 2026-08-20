package com.faber.api.base.admin.rest;

import com.faber.api.base.admin.biz.CategoryBiz;
import com.faber.api.base.admin.entity.Category;
import com.faber.api.base.admin.enums.CategoryModuleEnum;
import com.faber.core.annotation.FaLogBiz;
import com.faber.core.vo.msg.Ret;
import com.faber.core.vo.tree.TreeNode;
import com.faber.core.web.rest.BaseTreeController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@FaLogBiz("通用分类")
@RestController
@RequestMapping("/api/base/admin/category")
public class CategoryController extends BaseTreeController<CategoryBiz, Category, Integer> {

    @GetMapping("/allTree/{module}")
    public Ret<List<TreeNode<Category>>> allTreeByModule(@PathVariable CategoryModuleEnum module) {
        return ok(baseBiz.allTree(module));
    }
}
