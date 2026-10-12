package com.faber.api.portal.contacts.vo;

import lombok.Data;

import java.util.List;

@Data
public class PortalDepartmentNodeVo {

    private String id;
    private String parentId;
    private String name;
    private Integer sort;
    private Integer memberCount;
    private boolean hasChildren;
    private List<PortalDepartmentNodeVo> children;
}
