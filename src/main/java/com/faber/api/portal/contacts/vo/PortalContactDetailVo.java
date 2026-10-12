package com.faber.api.portal.contacts.vo;

import lombok.Data;

@Data
public class PortalContactDetailVo extends PortalContactSummaryVo {
    private String tel;

    private String email;
}
