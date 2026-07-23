package com.faber.api.portal.contact.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.faber.core.bean.BaseDelEntity;
import lombok.Data;

@Data
@TableName("portal_contact_inquiry")
public class PortalContactInquiry extends BaseDelEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;
    private String company;
    private String tel;
    private String email;
    private String subject;
    private String message;
    private Integer status;
    private String source;
}
