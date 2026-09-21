package com.faber.api.portal.contacts.vo;

import lombok.Data;

import java.io.Serializable;

@Data
public class PortalContactPageQueryVo implements Serializable {

    private String keyword;

    private String departmentId;
}
