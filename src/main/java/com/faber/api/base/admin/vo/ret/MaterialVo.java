package com.faber.api.base.admin.vo.ret;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.faber.core.annotation.FaModalName;
import com.faber.core.annotation.SqlEquals;
import com.faber.core.bean.BaseDelEntity;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;


/**
 * beam_生产与报验管理_拌合楼管理
 *
 * @author
 * @email
 * @date 2024-12-06 16:53:53
 */
@FaModalName(name = "材料管理")
@TableName("beam_material")
@Data
public class MaterialVo extends BaseDelEntity {

    @ColumnWidth(8)
    @ExcelProperty("序号")
    @TableId(type = IdType.AUTO)
    private Integer id;

    @ExcelProperty("材料名称")
    private String name;

    @ExcelProperty("分类名称")
    private String typeName;

    @SqlEquals
    @ExcelProperty("分类id")
    private Integer typeId;

    @ExcelProperty("材料编码")
    private String materialCode;

    @ExcelProperty("材料单位")
    private String materialMode;

    /**
     * 材料在拌合站1对应的字段，如：246--对应--246;1-2石子;碎石;骨料石子;石灰岩 246;石灰岩  246;石灰岩 瓜子片
     * 主要由于每个拌合站材料类型不同
     */
    @ExcelProperty("拌合站1字段")
    private String bhzOneName;

    /**
     * 材料在拌合站2对应的字段，如：246--对应--246;1-2石子;碎石;骨料石子;石灰岩 246;石灰岩  246;石灰岩 瓜子片
     */
    @ExcelProperty("拌合站2对应字段")
    private String bhzTwoName;

    @ExcelProperty("地磅对应字段")
    private String weighbridgeName;

    @ExcelProperty("工程量单位")
    private String importMaterialMode;

    @TableField(exist = false)
    @ExcelProperty("理论库存量")
    private BigDecimal theoryNum;

    @ExcelProperty("最新入库时间")
    @TableField(exist = false)
    private LocalDateTime insertDate;

    @ExcelProperty("最新入库量")
    @TableField(exist = false)
    private BigDecimal insertNum;

    @ExcelProperty("上次盘库时间")
    @TableField(exist = false)
    private LocalDateTime reStartDate;

    @ExcelProperty("上次盘库量")
    @TableField(exist = false)
    private BigDecimal reStartNum;

    @ExcelProperty("本日入库量")
    @TableField(exist = false)
    private BigDecimal daySum;

    @ExcelProperty("本月入库量")
    @TableField(exist = false)
    private BigDecimal mouthSum;

    @ExcelProperty("预警值")
    private BigDecimal alertValue;

    //告警信息编号
    @ExcelIgnore
    private Integer alertId;

    //是否预警 0: 不预警,  1:预警
    @ExcelIgnore
    @TableField(exist = false)
    private String isAlert;

}
