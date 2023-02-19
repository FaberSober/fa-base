package com.faber.api.base;

import com.faber.core.config.dbinit.DbInit;
import com.faber.core.config.dbinit.vo.FaDdl;
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
                .addTableCreate(new FaDdlTableCreate("base_area", "BASE-中国行政地区表", "sql/1.0.1_base_area.sql"))
                .addTableCreate(new FaDdlTableCreate("base_notice", "BASE-通知与公告", "sql/1.0.0_base_notice.sql"))
                .addTableCreate(new FaDdlTableCreate("base_system_update_log", "BASE-系统版本更新日志表", "sql/1.0.0_base_system_update_log.sql")));

        return list;
    }
}
