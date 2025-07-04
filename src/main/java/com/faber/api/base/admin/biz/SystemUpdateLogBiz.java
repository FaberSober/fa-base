package com.faber.api.base.admin.biz;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.util.ClassUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.faber.api.base.admin.entity.SystemUpdateLog;
import com.faber.api.base.admin.mapper.SystemUpdateLogMapper;
import com.faber.api.base.admin.vo.dto.FaSqlHeader;
import com.faber.api.base.rbac.biz.RbacRoleMenuBiz;
import com.faber.core.config.dbinit.DbInit;
import com.faber.core.config.dbinit.vo.FaDdl;
import com.faber.core.config.dbinit.vo.FaDdlAddColumn;
import com.faber.core.config.dbinit.vo.FaDdlSql;
import com.faber.core.config.dbinit.vo.FaDdlTableCreate;
import com.faber.core.context.BaseContextHandler;
import com.faber.core.utils.FaDateUtils;
import com.faber.core.utils.FaResourceUtils;
import com.faber.core.utils.SqlUtils;
import com.faber.core.web.biz.BaseBiz;
import lombok.SneakyThrows;
import org.apache.ibatis.jdbc.ScriptRunner;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * BASE-系统版本更新日志表
 *
 * @author Farando
 * @email faberxu@gmail.com
 * @date 2022-08-17 17:10:02
 */
@Service
public class SystemUpdateLogBiz extends BaseBiz<SystemUpdateLogMapper, SystemUpdateLog> {

    @Resource
    DataSource dataSource;

    @Resource
    RbacRoleMenuBiz rbacRoleMenuBiz;

    public static final String SQL_SPLITTER = "-- ------------------------- info -------------------------";

    public void initDb() {
        BaseContextHandler.useAdmin();

        // 1. 初始化数据
        ClassUtil.scanPackageBySuper("com.faber", DbInit.class)
                .stream().map(clazz -> (DbInit) SpringUtil.getBean(clazz))
                .sorted(Comparator.comparing(DbInit::getOrder))
                .forEach(i -> initOneBuzz(i));

        // 2. 给超级管理员角色赋权限
        rbacRoleMenuBiz.initAdminRoleMenu();
    }

    @SneakyThrows
    private void initOneBuzz(DbInit dbInit) {
        // 1. 获取数据库操作信息
        String no = dbInit.getNo();
        String name = dbInit.getName();

        // 2. 查询数据库当前记录最新的版本
        SystemUpdateLog latestLog = this.getLatestByNo(no);

        // 3. 获取sql文件列表
        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

        // 根据数据库类型选择不同的SQL文件
        String sqlPattern = "classpath*:sql/" + no + "/*.sql";
        try {
            Connection conn = dataSource.getConnection();
            String databaseProductName = conn.getMetaData().getDatabaseProductName();
            if ("DM DBMS".equalsIgnoreCase(databaseProductName) || databaseProductName.toLowerCase().contains("dm")) {
                // 达梦数据库优先使用_dm.sql文件
                sqlPattern = "classpath*:sql/" + no + "/*_dm.sql";
                org.springframework.core.io.Resource[] dmResources = resolver.getResources(sqlPattern);
                if (dmResources.length == 0) {
                    // 如果没有达梦专用文件，使用通用文件
                    sqlPattern = "classpath*:sql/" + no + "/*.sql";
                }
            }
            conn.close();
        } catch (Exception e) {
            _logger.warn("检测数据库类型失败，使用默认SQL文件: {}", e.getMessage());
        }

        org.springframework.core.io.Resource[] resources = resolver.getResources(sqlPattern);

        // 4. 解析sql文件
        Connection conn = dataSource.getConnection();
        try {
            ListUtil.of(resources).stream().map(resource -> {
                        try {
                            return getSqlFileHeader(resource);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }).filter(i -> i != null)
                    .sorted(Comparator.comparing(FaSqlHeader::getVer)) // 按照版本号升序排列
                    .filter(i -> {
                        if (latestLog == null) return true;
                        return i.getVer() > latestLog.getVer();
                    }) // 过滤需要升级的sql
//                .sorted(Comparator.comparing(FaSqlHeader::getVer)) // 按照版本号升序排列
                    .forEach(i -> {
                        // 执行升级sql
                        String errorMsg = "";
                        try {
                            _logger.info("执行升级sql: no: {} name: {} ver: {} verNo: {}", no, name, i.getVer(), i.getVerNo());
                            // 处理达梦数据库兼容性
                            String sql = processSqlForDameng(conn, i.getSql());
                            SqlUtils.executeSql(conn, sql);
                            Thread.sleep(1000);
                        } catch (Exception e) {
                            _logger.error(e.getMessage(), e);
                            errorMsg = ExceptionUtil.stacktraceToString(e);
                        }

                        // 2. 记录升级日志
                        SystemUpdateLog updateLog = new SystemUpdateLog();
                        updateLog.setNo(no);
                        updateLog.setName(name);
                        updateLog.setVer(i.getVer());
                        updateLog.setVerNo(i.getVerNo());
                        updateLog.setRemark(i.getInfo());
                        updateLog.setLog(i.getSql() + "\r\n" + errorMsg);

                        super.save(updateLog);
                    });
        } finally {
            conn.close();
        }
    }

    private FaSqlHeader getSqlFileHeader(org.springframework.core.io.Resource resource) throws IOException {
        String sqlStr = FaResourceUtils.getResourceString(resource);
        if (!sqlStr.contains(SQL_SPLITTER)) {
            throw new RuntimeException("SQL初始化文件未包含正确的文件头，请检查，文件名：" + resource.getFilename());
        }
        String info = sqlStr.substring(sqlStr.indexOf(SQL_SPLITTER) + SQL_SPLITTER.length(), sqlStr.lastIndexOf(SQL_SPLITTER));
        String[] ss = info.trim().split("\n");

        FaSqlHeader header = new FaSqlHeader();
        header.setSql(sqlStr);

        for (String line : ss) {
            String key = line.substring(0, line.indexOf(":")).substring(5);
            String value = line.substring(line.indexOf(":") + 1).trim();
//            log.debug(key + ":" + value);

            switch (key) {
                case "ver":
                    header.setVer(Long.parseLong(value.replace("_", "").replace("L", "")));

                    String[] verSs = value.replace("L", "").split("_");
                    String verNo = "V" + Integer.parseInt(verSs[0]) + "." + Integer.parseInt(verSs[1]) + "." + Integer.parseInt(verSs[2]);
                    header.setVerNo(verNo);
                    break;
                case "info":
                    header.setInfo(value);
                    break;
            }
        }

        return header;
    }

    /**
     * 查询数据库当前记录最新的版本
     *
     * @param no
     * @return
     */
    public SystemUpdateLog getLatestByNo(String no) {
        try {
            // 首先检查表是否存在
            if (!isTableExists()) {
                _logger.warn("表 base_system_update_log 不存在，跳过查询");
                return null;
            }
            _logger.info("表存在性检查通过，继续查询");

            // 使用MyBatis Plus的分页功能，兼容不同数据库
            List<SystemUpdateLog> list = lambdaQuery()
                    .eq(SystemUpdateLog::getNo, no)
                    .orderByDesc(SystemUpdateLog::getVer)
                    .list();
            return list.isEmpty() ? null : list.get(0);
        } catch (Exception e) {
            _logger.error("查询最新版本日志失败: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 检查表是否存在
     */
    private boolean isTableExists() {
        try {
            Connection conn = dataSource.getConnection();
            String databaseProductName = conn.getMetaData().getDatabaseProductName();
            _logger.info("数据库类型: {}", databaseProductName);

            if ("DM DBMS".equalsIgnoreCase(databaseProductName) || databaseProductName.toLowerCase().contains("dm")) {
                // 达梦数据库，先查询当前模式下的表
                String currentSchema = conn.getSchema();
                _logger.info("当前模式: {}", currentSchema);

                // 查询用户表
                String sql = "SELECT TABLE_NAME FROM USER_TABLES WHERE TABLE_NAME LIKE '%JOB_LOG%' OR TABLE_NAME LIKE '%SYSTEM_UPDATE_LOG%'";
                var stmt = conn.prepareStatement(sql);
                var rs = stmt.executeQuery();
                _logger.info("查询达梦数据库中的相关表:");
                while (rs.next()) {
                    String tableName = rs.getString("TABLE_NAME");
                    _logger.info("找到表: {}", tableName);
                }
                rs.close();
                stmt.close();

                // 查询base_job_log表的列结构
                try {
                    String colSql = "SELECT COLUMN_NAME, DATA_TYPE FROM USER_TAB_COLUMNS WHERE TABLE_NAME = 'BASE_JOB_LOG' ORDER BY COLUMN_ID";
                    var colStmt = conn.prepareStatement(colSql);
                    var colRs = colStmt.executeQuery();
                    _logger.info("base_job_log表的列结构:");
                    while (colRs.next()) {
                        String columnName = colRs.getString("COLUMN_NAME");
                        String dataType = colRs.getString("DATA_TYPE");
                        _logger.info("列: {} - 类型: {}", columnName, dataType);
                    }
                    colRs.close();
                    colStmt.close();
                } catch (Exception e) {
                    _logger.warn("查询base_job_log表结构失败: {}", e.getMessage());
                }

                // 尝试不同的表名格式
                String[] tableNames = {
                    "zdj_dev.\"base_system_update_log\"", // 推荐格式：模式名.表名（模式名不加引号）
                    "\"base_system_update_log\"",
                    "base_system_update_log",
                    "BASE_SYSTEM_UPDATE_LOG"
                };

                for (String tableName : tableNames) {
                    try {
                        String testSql = "SELECT 1 FROM " + tableName + " WHERE 1=0";
                        conn.prepareStatement(testSql).execute();
                        _logger.info("表存在: {}", tableName);
                        conn.close();
                        return true;
                    } catch (Exception e) {
                        _logger.debug("表不存在: {} - {}", tableName, e.getMessage());
                    }
                }
            } else {
                // MySQL使用反引号
                String sql = "SELECT 1 FROM `base_system_update_log` WHERE 1=0";
                conn.prepareStatement(sql).execute();
                conn.close();
                return true;
            }

            conn.close();
            return false;
        } catch (Exception e) {
            _logger.error("检查表存在性失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 处理达梦数据库SQL兼容性
     * @param conn 数据库连接
     * @param sql 原始SQL
     * @return 处理后的SQL
     */
    private String processSqlForDameng(Connection conn, String sql) {
        try {
            String databaseProductName = conn.getMetaData().getDatabaseProductName();
            if ("DM DBMS".equalsIgnoreCase(databaseProductName) || databaseProductName.toLowerCase().contains("dm")) {
                _logger.info("处理达梦数据库SQL兼容性");

                // 达梦数据库SQL处理
                sql = sql.replaceAll("(?i)SET\\s+NAMES\\s+utf8mb4\\s*;", "-- SET NAMES utf8mb4; -- 达梦数据库不支持");
                sql = sql.replaceAll("(?i)SET\\s+FOREIGN_KEY_CHECKS\\s*=\\s*[01]\\s*;", "-- SET FOREIGN_KEY_CHECKS; -- 达梦数据库不支持");

                // 处理反引号
                sql = sql.replaceAll("`([^`]+)`", "\"$1\"");

                // 特殊处理：确保表名被双引号包围并加上模式名（模式名不加引号）
                sql = sql.replaceAll("\\bFROM\\s+\"([a-zA-Z_][a-zA-Z0-9_]*)\"", "FROM zdj_dev.\"$1\"");
                sql = sql.replaceAll("\\bFROM\\s+([a-zA-Z_][a-zA-Z0-9_]*)", "FROM zdj_dev.\"$1\"");
                sql = sql.replaceAll("\\bINTO\\s+\"([a-zA-Z_][a-zA-Z0-9_]*)\"", "INTO zdj_dev.\"$1\"");
                sql = sql.replaceAll("\\bINTO\\s+([a-zA-Z_][a-zA-Z0-9_]*)", "INTO zdj_dev.\"$1\"");
                sql = sql.replaceAll("\\bUPDATE\\s+\"([a-zA-Z_][a-zA-Z0-9_]*)\"", "UPDATE zdj_dev.\"$1\"");
                sql = sql.replaceAll("\\bUPDATE\\s+([a-zA-Z_][a-zA-Z0-9_]*)", "UPDATE zdj_dev.\"$1\"");
                sql = sql.replaceAll("\\bJOIN\\s+\"([a-zA-Z_][a-zA-Z0-9_]*)\"", "JOIN zdj_dev.\"$1\"");
                sql = sql.replaceAll("\\bJOIN\\s+([a-zA-Z_][a-zA-Z0-9_]*)", "JOIN zdj_dev.\"$1\"");

                // 处理IFNULL函数
                sql = sql.replaceAll("(?i)IFNULL\\s*\\(", "COALESCE(");

                // 处理MySQL特有的数据类型和语法
                sql = sql.replaceAll("(?i)\\s+unsigned", ""); // 移除unsigned
                sql = sql.replaceAll("(?i)\\s+zerofill", ""); // 移除zerofill
                sql = sql.replaceAll("(?i)AUTO_INCREMENT", "IDENTITY(1,1)"); // 替换自增
                sql = sql.replaceAll("(?i)ENGINE=InnoDB", ""); // 移除引擎设置
                sql = sql.replaceAll("(?i)DEFAULT\\s+CHARSET=utf8[^\\s]*", ""); // 移除字符集设置
                sql = sql.replaceAll("(?i)USING\\s+BTREE", ""); // 移除索引类型
                sql = sql.replaceAll("(?i)USING\\s+HASH", ""); // 移除索引类型

                // 处理数据类型
                sql = sql.replaceAll("(?i)\\bmediumint\\b", "INT"); // mediumint -> INT
                sql = sql.replaceAll("(?i)\\btinyint\\(1\\)", "TINYINT"); // tinyint(1) -> TINYINT
                sql = sql.replaceAll("(?i)\\bbigint\\(\\d+\\)", "BIGINT"); // bigint(14) -> BIGINT
                sql = sql.replaceAll("(?i)\\bint\\(\\d+\\)", "INT"); // int(11) -> INT
                sql = sql.replaceAll("(?i)\\bmediumint\\(\\d+\\)", "INT"); // mediumint(7) -> INT

                // 处理KEY语句，达梦数据库使用CREATE INDEX
                sql = sql.replaceAll("(?i),\\s*KEY\\s+\"([^\"]+)\"\\s*\\([^)]+\\)", ""); // 移除KEY定义
                sql = sql.replaceAll("(?i),\\s*UNIQUE\\s+KEY\\s+\"([^\"]+)\"\\s*\\([^)]+\\)", ""); // 移除UNIQUE KEY定义

                _logger.debug("处理后的SQL: {}", sql);
            }
        } catch (Exception e) {
            _logger.warn("处理达梦数据库SQL兼容性时出错: {}", e.getMessage());
        }
        return sql;
    }

}
