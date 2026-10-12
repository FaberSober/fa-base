package com.faber.api.base.admin.entity;

import com.alibaba.excel.annotation.ExcelProperty;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.faber.core.annotation.FaModalName;
import com.faber.core.annotation.SqlEquals;
import com.faber.core.bean.BaseTnDelEntity;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;


/**
 * BASE-通知与公告
 * 
 * @author Farando
 * @email faberxu@gmail.com
 * @date 2021-01-07 09:37:36
 */
@FaModalName(name = "BASE-通知与公告")
@TableName("base_notice")
@Data
public class Notice extends BaseTnDelEntity {
	private static final long serialVersionUID = 1L;
	
    /** 所属租户仅由服务端上下文赋值，禁止通过更新接口转移公告。 */
    @Override
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public String getTenantId() {
        return super.getTenantId();
    }

    @ExcelProperty("ID")
    @TableId(type = IdType.AUTO)
    private Integer id;

    @ExcelProperty("标题")
    private String title;

    @ExcelProperty("内容")
    private String content;

    @SqlEquals
    @ExcelProperty("是否有效")
    private Boolean status;

    @SqlEquals
    @ExcelProperty("是否强提醒")
    private Boolean strongNotice;

}
