package com.faber.api.base.admin.vo.query;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class OnlineUserKickoutVo {
    /** 页面会话 ID，不能传入 Token。 */
    @NotBlank
    @Size(max = 64)
    @Pattern(regexp = "[a-f0-9]{64}", message = "会话ID格式错误")
    private String id;
    private boolean allSessions;
}
