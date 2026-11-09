package com.faber.api.base.admin.vo.ret;

import java.util.List;

/** 注册任务目录元数据，不保存到流程定义。 */
public record RegisteredJobVo(String clazzPath, String name, String group, String description,
                              List<String> tags, int sort, boolean deprecated) {}
