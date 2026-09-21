package com.faber.api.portal.contacts.vo;

import com.faber.api.base.admin.enums.UserWorkStatusEnum;
import lombok.Data;

import java.io.Serializable;

@Data
public class PortalContactSummaryVo implements Serializable {

    private String id;

    private String name;

    private String avatar;

    private String departmentId;

    private String departmentName;

    private String roleNames;

    private UserWorkStatusEnum workStatus;
}
