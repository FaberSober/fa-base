package com.faber.api.base.generator.utils;

import cn.hutool.core.util.StrUtil;
import com.faber.api.base.generator.vo.req.CodeGenReqVo;
import com.faber.api.base.generator.vo.ret.ColumnVo;
import com.faber.api.base.generator.vo.ret.TableVo;
import com.faber.core.utils.FaFileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.*;
import java.util.zip.ZipOutputStream;

/**
 * 代码生成器   工具类
 */
public class GeneratorUtils {

    public static String mysqlTypeToJavaType(String mysqlType) {
        switch (mysqlType) {
            // ----------------------- number -----------------------
            case "tinyint":
            case "smallint":
            case "mediumint":
            case "int":
            case "integer":
                return "Integer";
            case "bigint":
                return "Long";
            case "float":
                return "Float";
            case "double":
                return "Double";
            case "decimal":
                return "BigDecimal";
            case "bit":
                return "Boolean";

            // ----------------------- string -----------------------
            case "char":
            case "varchar":
            case "tinytext":
            case "text":
            case "mediumtext":
            case "longtext":
            case "enum":
                return "String";

            // ----------------------- date -----------------------
            case "date":
            case "datetime":
            case "timestamp":
                return "Date";

            default:
                return "String";
        }
    }

    public static List<String> getTemplates() {
        List<String> templates = new ArrayList<String>();
//        templates.add("template/index.js.vm");
//        templates.add("template/index.vue.vm");
        // SpringBoot Java代码
        templates.add("template/mapper.xml.vm");
        templates.add("template/biz.java.vm");
        templates.add("template/entity.java.vm");
        templates.add("template/mapper.java.vm");
        templates.add("template/controller.java.vm");
        // RN 前端页面
        templates.add("template/rn_service.ts.vm");
        templates.add("template/rn_list.tsx.vm");
        templates.add("template/rn_modal.tsx.vm");
        templates.add("template/rn_prop.ts.vm");
        return templates;
    }

    /**
     * 生成代码
     */
    public static String generatorCode(CodeGenReqVo codeGenReqVo, TableVo table, List<ColumnVo> columns) {
        //表名转换成Java类名
        String className = tableToJava(table.getTableName(), codeGenReqVo.getTablePrefix());

        //列信息
        ColumnVo pkCol = null;
        for (ColumnVo column : columns) {
            //列名转换成Java属性名
            String attrName = columnToJava(column.getColumnName());
//            column.setAttrName(attrName);
            column.setAttrname(StringUtils.uncapitalize(attrName));

            //列的数据类型，转换成Java类型
            String attrType = mysqlTypeToJavaType(column.getDataType());
            column.setAttrType(attrType);

            //是否主键
            if ("PRI".equalsIgnoreCase(column.getColumnKey())) {
                pkCol = column;
            }
        }

        //没主键，则第一个字段为主键
        if (pkCol == null) {
            pkCol = columns.get(0);
        }

        //设置velocity资源加载器
        Properties prop = new Properties();
        prop.put("file.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
        Velocity.init(prop);

        //封装模板数据
        Map<String, Object> map = new HashMap<>();
        map.put("tableName", table.getTableName());
        map.put("comments", table.getTableComment());
        map.put("pk", pkCol);
        map.put("className", className);
        map.put("classNameLowerCaseFirstOne", toLowerCaseFirstOne(className));
        map.put("pathName", className.toLowerCase());
        map.put("columns", columns);
        map.put("package", codeGenReqVo.getPackageName());
        map.put("author", codeGenReqVo.getAuthor());
        map.put("email", codeGenReqVo.getEmail());
        map.put("datetime", DateUtils.format(new Date(), DateUtils.DATE_TIME_PATTERN));
        map.put("moduleName", codeGenReqVo.getMainModule());
        map.put("moduleNameSlash", codeGenReqVo.getMainModule().replaceAll("\\.", "/")); // 将前端模块base.admin ---> base/admin，用于controller的路径中
        map.put("moduleNameUpperCaseFirstOne", toUpperCaseFirstOne(StrUtil.toCamelCase(codeGenReqVo.getMainModule(), '.')));
        map.put("secondModuleName", toLowerCaseFirstOne(className));
        VelocityContext context = new VelocityContext(map);

        //渲染模板
        String template = "template/" + codeGenReqVo.getType().getDesc();
        StringWriter sw = new StringWriter();
        Template tpl = Velocity.getTemplate(template, "UTF-8");
        tpl.merge(context, sw);
        return sw.toString();
    }


    /**
     * 列名转换成Java属性名
     */
    public static String columnToJava(String columnName) {
        return WordUtils.capitalizeFully(columnName, new char[]{'_'}).replace("_", "");
    }

    /**
     * 表名转换成Java类名
     */
    public static String tableToJava(String tableName, String tablePrefix) {
        if (StringUtils.isNotBlank(tablePrefix)) {
            tableName = tableName.replace(tablePrefix, "");
        }
        return columnToJava(tableName);
    }

    public static String getJavaCopyPath(CodeGenReqVo codeGenReqVo) throws IOException {
        String rootDir = FaFileUtils.getProjectRootDir();

        String packagePath = "src" + File.separator + "main" + File.separator + "java" + File.separator;

        if (StringUtils.isNotBlank(codeGenReqVo.getPackageName())) {
            packagePath += codeGenReqVo.getPackageName().replace(".", File.separator) + File.separator;
        }

        String className = tableToJava(codeGenReqVo.getTableName(), codeGenReqVo.getTablePrefix());
        String javaPath = rootDir + File.separator + codeGenReqVo.getJavaCopyPath() + File.separator + packagePath;
        String resourcePath = rootDir + File.separator + codeGenReqVo.getJavaCopyPath() + File.separator + "src" + File.separator + "main" + File.separator + "resources" + File.separator;
        switch (codeGenReqVo.getType()) {
            case JAVA_ENTITY:
                return javaPath + "entity" + File.separator + className + ".java";
            case JAVA_MAPPER:
                return javaPath + "mapper" + File.separator + className + "Mapper.java";
            case JAVA_BIZ:
                return javaPath + "biz" + File.separator + className + "Biz.java";
            case JAVA_CONTROLLER:
                return javaPath + "rest" + File.separator + className + "Controller.java";
            case XML_MAPPER:
                return resourcePath + "mapper" + File.separator + codeGenReqVo.getMainModule().replaceAll("\\.", "/") + File.separator + className + "Mapper.xml";
        }
        return "";
    }

    /**
     * 获取文件名
     */
    public static String getFileName(String template, String className, String packageName, String moduleName) {
        String packagePath = "main" + File.separator + "java" + File.separator;
        String frontPath = "ui" + File.separator;
        String rnFrontPath = "ui-rn" + File.separator;
        if (StringUtils.isNotBlank(packageName)) {
            packagePath += packageName.replace(".", File.separator) + File.separator;
        }

        // rn
        if (template.contains("rn_service.ts.vm")) {
            return rnFrontPath + "src" + File.separator + "services" + File.separator + moduleName + File.separator + toLowerCaseFirstOne(className) + ".ts";
        }
        if (template.contains("rn_list.tsx.vm")) {
            return rnFrontPath + "src" + File.separator + "pages" + File.separator + moduleName + File.separator + toUpperCaseFirstOne(className) + "List.tsx";
        }
        if (template.contains("rn_modal.tsx.vm")) {
            return rnFrontPath + "src" + File.separator + "pages" + File.separator + moduleName + File.separator + "modal" + File.separator + toUpperCaseFirstOne(className) + "Modal.tsx";
        }
        if (template.contains("rn_prop.ts.vm")) {
            return rnFrontPath + "src" + File.separator + "props" + File.separator + moduleName + File.separator + toLowerCaseFirstOne(className) + ".ts";
        }

        // vue 前端代码
//        if (template.contains("index.js.vm")) {
//            return frontPath + "api" + File.separator + moduleName + File.separator + toLowerCaseFirstOne(className) + File.separator + "index.js";
//        }
//
//        if (template.contains("index.vue.vm")) {
//            return frontPath + "views" + File.separator + moduleName + File.separator + toLowerCaseFirstOne(className) + File.separator + "index.vue";
//        }

        // java 部分代码
        if (template.contains("biz.java.vm")) {
            return packagePath + "biz" + File.separator + className + "Biz.java";
        }
        if (template.contains("mapper.java.vm")) {
            return packagePath + "mapper" + File.separator + className + "Mapper.java";
        }
        if (template.contains("entity.java.vm")) {
            return packagePath + "entity" + File.separator + className + ".java";
        }
        if (template.contains("controller.java.vm")) {
            return packagePath + "rest" + File.separator + className + "Controller.java";
        }
        if (template.contains("mapper.xml.vm")) {
            return "main" + File.separator + "resources" + File.separator + "mapper" + File.separator + File.separator + moduleName + File.separator + className + "Mapper.xml";
        }

        return null;
    }

    //首字母转小写
    public static String toLowerCaseFirstOne(String s) {
        if (Character.isLowerCase(s.charAt(0))) {
            return s;
        } else {
            return (new StringBuilder()).append(Character.toLowerCase(s.charAt(0))).append(s.substring(1)).toString();
        }
    }

    //首字母转大写
    public static String toUpperCaseFirstOne(String s) {
        if (Character.isUpperCase(s.charAt(0))) {
            return s;
        } else {
            return (new StringBuilder()).append(Character.toUpperCase(s.charAt(0))).append(s.substring(1)).toString();
        }
    }
}
