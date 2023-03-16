package com.faber.api.base.admin.biz;

import cn.hutool.core.collection.ListUtil;
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
import com.faber.core.web.biz.BaseBiz;
import lombok.SneakyThrows;
import org.apache.ibatis.jdbc.ScriptRunner;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
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
        org.springframework.core.io.Resource[] resources = resolver.getResources("classpath*:sql/" + no + "/*.sql");

        // 4. 解析sql文件
        ListUtil.of(resources).stream().map(resource -> {
                    try {
                        String sqlStr = FaResourceUtils.getResourceString(resource);
                        return getSqlFileHeader(sqlStr);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }).filter(i -> i != null)
                .filter(i -> {
                    if (latestLog == null) return true;
                    return i.getVer() > latestLog.getVer();
                }) // 过滤需要升级的sql
                .sorted(Comparator.comparing(FaSqlHeader::getVer)) // 按照版本号升序排列
                .forEach(i -> {
                    // 执行升级sql
                    try {
                        executeSql(i.getSql());
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }

                    // 2. 记录升级日志
                    SystemUpdateLog updateLog = new SystemUpdateLog();
                    updateLog.setNo(no);
                    updateLog.setName(name);
                    updateLog.setVer(i.getVer());
                    updateLog.setVerNo(i.getVerNo());
                    updateLog.setRemark(i.getInfo());
                    updateLog.setLog(i.getSql());

                    super.save(updateLog);
                });
    }

    private FaSqlHeader getSqlFileHeader(String sqlStr) {
        String info = sqlStr.substring(sqlStr.indexOf(SQL_SPLITTER) + SQL_SPLITTER.length(), sqlStr.lastIndexOf(SQL_SPLITTER));
        String[] ss = info.trim().split("\n");

        FaSqlHeader header = new FaSqlHeader();
        header.setSql(sqlStr);

        for (String line : ss) {
            String key = line.substring(0, line.indexOf(":")).substring(5);
            String value = line.substring(line.indexOf(":") + 1).trim();
            log.debug(key + ":" + value);

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


//    private void initOneBuzzOld(DbInit dbInit) {
//        // 1. 获取数据库操作信息
//        String no = dbInit.getNo();
//        String name = dbInit.getName();
//        List<FaDdl> list = dbInit.getDdlList();
//
//        // 2. 查询数据库当前记录最新的版本
//        SystemUpdateLog latestLog = this.getLatestByNo(no);
//
//        // 3. 过滤得到需要做更新的数据库操作list
//        List<FaDdl> todoList = list.stream().filter(i -> {
//            if (latestLog == null) return true;
//            if (i.getVer() > latestLog.getVer()) {
//                return true;
//            }
//            return false;
//        }).collect(Collectors.toList());
//
//        // 按ver版本号升序排列
//        todoList.sort(Comparator.comparing(FaDdl::getVer));
//
//        // 4. 循环执行数据库操作
//        for (FaDdl faDdl : todoList) {
//            initOneFaDdl(no, name, faDdl);
//        }
//    }

//    private void initOneFaDdl(String no, String name, FaDdl faDdl) {
//        _logger.info("init db no={} name={} ver={} verNo={}", no, name, faDdl.getVer(), faDdl.getVerNo());
//        StringBuilder sb = new StringBuilder();
//
//        // 1.1 执行导入sql-create table
//        for (FaDdlTableCreate tableCreate : faDdl.getTableCreateList()) {
//            try {
//                // 判断数据表是否存在
//                Map<String, Object> tableSchema = getTableSchema(tableCreate.getTableName());
//                if (tableSchema != null) {
//                    sb.append("-- " + FaDateUtils.nowLog() + "  WARN 表" + tableCreate.getTableName() + "已存在，不执行建表语句\r\n");
//                    continue;
//                }
//
//                sb.append("-- " + FaDateUtils.nowLog() + "  INFO 创建表" + tableCreate.getTableName() + "--->>>\r\n");
//                String sqlStr = FaResourceUtils.getResourceString("classpath:" + tableCreate.getSqlPath());
//
//                // 执行建表语句
//                executeSql(sqlStr);
//
//                sb.append(sqlStr).append("\r\n");
//            } catch (Exception e) {
//                _logger.error(e.getMessage(), e);
//                sb.append("-- " + FaDateUtils.nowLog() + "  ERROR 创建表" + tableCreate.getTableName() + "失败--->>>\r\n");
//                sb.append("-- " + e.getMessage() + "\r\n");
//                sb.append(e.getStackTrace().toString());
//            }
//        }
//
//        // 1.2 执行更新sql-add column
//        for (FaDdlAddColumn addColumn : faDdl.getAddColumnList()) {
//            try {
//                // 判断字段是否存在
//                Map<String, Object> colSchema = getColSchema(addColumn.getTableName(), addColumn.getColName());
//                if (colSchema != null) {
//                    sb.append("-- " + FaDateUtils.nowLog() + "  WARN 表字段" + addColumn.getTableName() + "." + addColumn.getColName() + "已存在，不执行add语句\r\n");
//                    continue;
//                }
//
//                sb.append("-- " + FaDateUtils.nowLog() + "  INFO 新增表字段" + addColumn.getTableName() + "." + addColumn.getColName() + "--->>>\r\n");
//                String sqlStr = FaResourceUtils.getResourceString("classpath:" + addColumn.getSqlPath());
//
//                // 执行语句
//                executeSql(sqlStr);
//
//                sb.append(sqlStr).append("\r\n");
//            } catch (Exception e) {
//                _logger.error(e.getMessage(), e);
//                sb.append("-- " + FaDateUtils.nowLog() + "  ERROR 新增表字段" + addColumn.getTableName() + "." + addColumn.getColName() + "失败--->>>\r\n");
//                sb.append("-- " + e.getMessage() + "\r\n");
//                sb.append(e.getStackTrace().toString());
//            }
//        }
//
//        // 1.3 执行通用DDL语句
//        for (FaDdlSql sql : faDdl.getSqlList()) {
//            try {
//                sb.append("-- " + FaDateUtils.nowLog() + "  INFO 执行SQL " + sql.getComment() + "--->>>\r\n");
//                String sqlStr = FaResourceUtils.getResourceString("classpath:" + sql.getSqlPath());
//
//                // 执行语句
//                executeSql(sqlStr);
//
//                sb.append(sqlStr).append("\r\n");
//            } catch (Exception e) {
//                _logger.error(e.getMessage(), e);
//                sb.append("-- " + FaDateUtils.nowLog() + "  ERROR 执行SQL" + sql.getComment() + "失败--->>>\r\n");
//                sb.append("-- " + e.getMessage() + "\r\n");
//                sb.append(e.getStackTrace().toString());
//            }
//        }
//
//        // 2. 记录升级日志
//        SystemUpdateLog updateLog = new SystemUpdateLog();
//        updateLog.setNo(no);
//        updateLog.setName(name);
//        updateLog.setVer(faDdl.getVer());
//        updateLog.setVerNo(faDdl.getVerNo());
//        updateLog.setRemark(faDdl.getRemark());
//        updateLog.setLog(sb.toString());
//
//        super.save(updateLog);
//    }

    public Map<String, Object> getTableSchema(String tableName) {
        try {
            return baseMapper.queryTableSchema(tableName);
        } catch (Exception e) {
            _logger.error(e.getMessage(), e);
        }
        return null;
    }

    public Map<String, Object> getColSchema(String tableName, String colName) {
        try {
            return baseMapper.queryColSchema(tableName, colName);
        } catch (Exception e) {
            _logger.error(e.getMessage(), e);
        }
        return null;
    }

    /**
     * 查询数据库当前记录最新的版本
     *
     * @param no
     * @return
     */
    public SystemUpdateLog getLatestByNo(String no) {
        try {
            return lambdaQuery()
                    .eq(SystemUpdateLog::getNo, no)
                    .orderByDesc(SystemUpdateLog::getVer)
                    .last("limit 1")
                    .one();
        } catch (Exception e) {
            _logger.error(e.getMessage(), e);
        }
        return null;
    }

    /**
     * 执行sql脚本
     *
     * @param sql
     * @throws SQLException
     */
    public void executeSql(String sql) throws SQLException {
        Connection conn = dataSource.getConnection();
        // 执行sql脚本
        ScriptRunner runner = new ScriptRunner(conn);
        runner.setFullLineDelimiter(false);
        runner.setDelimiter(";");//语句结束符号设置
        runner.setLogWriter(null);//日志数据输出，这样就不会输出过程
        runner.setSendFullScript(false);
        runner.setAutoCommit(true);
        runner.setStopOnError(true);
        runner.runScript(new StringReader(sql));
    }

    /**
     * 初始化&更新数据库。
     * 读取src/main/resources/data/updateLog.json文件，执行导入sql。
     */
//    public void initAndUpdateDb() throws IOException, SQLException {
//        JSONObject json = FaResourceUtils.getResourceJson("classpath:data/updateLog.json");
//
//        // 获取配置的数据源
//        DataSource dataSource = SpringUtil.getBean(DataSource.class);
//        Connection conn = dataSource.getConnection();
//
//        int curVer = baseMapper.getCurVerId();
//        JSONArray logs = json.getJSONArray("logs");
//        for (int i = 0; i < logs.size(); i++) {
//            JSONObject log = logs.getJSONObject(i);
//
//            long ver = log.getLong("ver");
//            if (ver <= curVer) continue;
//
//            // 记录执行日志
//            String verNo = log.getStr("verNo");
//            String remark = log.getStr("remark");
//            String sql = log.getStr("sql");
//
//            SystemUpdateLog logEntity = new SystemUpdateLog();
//            logEntity.setVer(ver);
//            logEntity.setVerNo(verNo);
//            logEntity.setRemark(remark);
//            logEntity.setCrtTime(new Date());
//            save(logEntity);
//
//            // 按行读取
//            File sqlFile = ResourceUtils.getFile("classpath:data/sql/" + sql);
//
//            // 执行sql脚本
//            ScriptRunner runner = new ScriptRunner(conn);
//            runner.setFullLineDelimiter(false);
//            runner.setDelimiter(";");//语句结束符号设置
//            runner.setLogWriter(null);//日志数据输出，这样就不会输出过程
//            runner.setSendFullScript(false);
//            runner.setAutoCommit(true);
//            runner.setStopOnError(true);
//            runner.runScript(new InputStreamReader(new FileInputStream(sqlFile), "utf8"));
//            _logger.info(String.format("【%s】执行成功", sql));
//        }
//    }

}