package com.faber.api.base.admin.vo.query;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class OnlineUserKickoutUserVo {
    @NotBlank
    @Size(max = 64)
    private String userId;
}
