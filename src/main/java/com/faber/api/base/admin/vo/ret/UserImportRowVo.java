package com.faber.api.base.admin.vo.ret;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;
import com.faber.core.annotation.FaModalName;
import lombok.Data;

/** 用户专用导入模板与读取模型，全部按文本读取后由业务层校验。 */
@Data
@FaModalName(name = "用户导入模板")
public class UserImportRowVo {

    @ExcelProperty(value = "用户ID", index = 0)
    private String id;

    @ExcelProperty(value = "用户名", index = 1)
    private String username;

    @ExcelProperty(value = "姓名", index = 2)
    private String name;

    @ExcelProperty(value = "部门", index = 3)
    private String department;

    @ExcelProperty(value = "角色名称", index = 4)
    private String roleNames;

    @ExcelProperty(value = "手机号", index = 5)
    private String tel;

    @ExcelProperty(value = "邮箱", index = 6)
    private String email;

    @ExcelProperty(value = "性别", index = 7)
    private String sex;

    @ExcelProperty(value = "工作状态", index = 8)
    private String workStatus;

    @ExcelProperty(value = "账户有效", index = 9)
    private String status;

    @ExcelProperty(value = "允许访问后台", index = 10)
    private String adminEnabled;

    @ExcelProperty(value = "生日", index = 11)
    private String birthday;

    @ExcelProperty(value = "联系地址", index = 12)
    private String address;

    @ExcelProperty(value = "备注", index = 13)
    private String description;

    @ExcelProperty(value = "开放平台的唯一标识符", index = 14)
    private String wxUnionId;

    @ExcelProperty(value = "微信小程序用户唯一标识", index = 15)
    private String wxMaOpenid;

    @ExcelProperty(value = "初始密码", index = 16)
    private String password;

    @ExcelIgnore
    private int rowNumber;
}
