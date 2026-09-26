package com.faber.api.base.push.vo.req;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class PushTestSendReqVo {

    @NotEmpty
    @Size(max = 5)
    private List<Long> deviceIds;

    @NotBlank
    @Size(max = 50)
    private String title;

    @NotBlank
    @Size(max = 256)
    private String content;

    @Size(max = 1024)
    private String link;

    private JsonNode extra;
}
