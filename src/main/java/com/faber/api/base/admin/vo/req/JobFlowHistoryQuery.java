package com.faber.api.base.admin.vo.req;

import com.faber.core.vo.query.BasePageQuery;
import lombok.Data;

/** 历史运行只读查询，排序及任务归属由服务端固定。 */
public class JobFlowHistoryQuery extends BasePageQuery<JobFlowHistoryQuery.Filters> {
    public JobFlowHistoryQuery() { setPageSize(20); }
    @Data
    public static class Filters {
        private Integer logId;
        private Integer status;
        private Integer triggerSource;
        private String beginTimeFrom;
        private String beginTimeTo;
    }
}
