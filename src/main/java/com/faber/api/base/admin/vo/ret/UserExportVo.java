package com.faber.api.base.admin.vo.ret;

import com.alibaba.excel.annotation.ExcelProperty;
import com.faber.api.base.admin.entity.User;
import com.faber.api.base.admin.enums.UserWorkStatusEnum;
import com.faber.core.annotation.FaModalName;
import com.faber.core.enums.SexEnum;
import lombok.Data;

import java.util.Date;

/**
 * 用户导出数据模型。
 *
 * @author Codex
 */
@Data
@FaModalName(name = "用户")
public class UserExportVo {

    @ExcelProperty("用户ID")
    private String id;

    @ExcelProperty("用户名")
    private String username;

    @ExcelProperty("姓名")
    private String name;

    @ExcelProperty("部门")
    private String departmentName;

    @ExcelProperty("角色名称")
    private String roleNames;

    @ExcelProperty("手机号")
    private String tel;

    @ExcelProperty("邮箱")
    private String email;

    @ExcelProperty("性别")
    private SexEnum sex;

    @ExcelProperty("工作状态")
    private UserWorkStatusEnum workStatus;

    @ExcelProperty("账户有效")
    private Boolean status;

    @ExcelProperty("允许访问后台")
    private Boolean adminEnabled;

    @ExcelProperty("生日")
    private Date birthday;

    @ExcelProperty("联系地址")
    private String address;

    @ExcelProperty("备注")
    private String description;

    @ExcelProperty("开放平台的唯一标识符")
    private String wxUnionId;

    @ExcelProperty("微信小程序用户唯一标识")
    private String wxMaOpenid;

    public static UserExportVo from(User user) {
        UserExportVo vo = new UserExportVo();
        vo.id = user.getId();
        vo.username = user.getUsername();
        vo.name = user.getName();
        vo.departmentName = user.getDepartmentName();
        vo.roleNames = user.getRoleNames();
        vo.tel = user.getTel();
        vo.email = user.getEmail();
        vo.sex = user.getSex();
        vo.workStatus = user.getWorkStatus();
        vo.status = user.getStatus();
        vo.adminEnabled = user.getAdminEnabled();
        vo.birthday = user.getBirthday();
        vo.address = user.getAddress();
        vo.description = user.getDescription();
        vo.wxUnionId = user.getWxUnionId();
        vo.wxMaOpenid = user.getWxMaOpenid();
        return vo;
    }
}
