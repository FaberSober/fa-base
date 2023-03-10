package com.faber.api.base;

import com.faber.core.config.dbinit.DbInit;
import com.faber.core.config.dbinit.vo.FaDdl;
import com.faber.core.config.dbinit.vo.FaDdlAddColumn;
import com.faber.core.config.dbinit.vo.FaDdlSql;
import com.faber.core.config.dbinit.vo.FaDdlTableCreate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Farando
 * @date 2023/2/18 20:12
 * @description
 */
@Slf4j
@Service
public class FaBaseDbInit implements DbInit {

    @Override
    public Integer getOrder() {
        return 0;
    }

    @Override
    public String getNo() {
        return "fa-base";
    }

    @Override
    public String getName() {
        return "基础模块";
    }

    @Override
    public List<FaDdl> getDdlList() {
        List<FaDdl> list = new ArrayList<>();

        list.add(new FaDdl(100_000_000L, "V1.0.0", "初始化")
                .addSql(new FaDdlSql("初始化表结构", "sql/1.0.0_base_full.sql"))
                .addSql(new FaDdlSql("初始化menu表数据", "sql/1.0.0_base_rbac_menu.sql"))
                .addSql(new FaDdlSql("初始化表数据", "sql/1.0.0_base_data.sql")));

        list.add(new FaDdl(100_000_001L, "V1.0.1", "增加BaseFileBiz通用业务附件表")
                .addSql(new FaDdlSql("初始化表结构", "sql/1.0.1_base_ddl.sql")));

        list.add(new FaDdl(100_000_002L, "V1.0.2", "增加代码生成模块")
                .addSql(new FaDdlSql("初始化表结构", "sql/1.0.2_base_ddl.sql")));

        return list;
    }
}
