package com.faber.api.base.calendar.service;

import java.time.LocalDate;

/**
 * 统一日历查询服务。
 *
 * <p>业务模块只依赖这里的语义，不应直接查询日历表。未配置具体日期时，服务暂以周一至周五
 * 作为过渡兜底；年度日历导入后，单日事实优先级高于该兜底。</p>
 */
public interface CalendarService {

    String CN_OA = "CN_OA";
    String CN_A_SHARE = "CN_A_SHARE";
    String HK_STOCK = "HK_STOCK";
    String US_STOCK = "US_STOCK";

    /** 判断指定日历在某日是否开放。 */
    boolean isOpen(String calendarCode, LocalDate date);

    /** 判断中国 OA 是否为公共工作日。 */
    default boolean isWorkday(LocalDate date) {
        return isOpen(CN_OA, date);
    }

    /** 按市场判断交易日；SH/SZ 共用 CN_A_SHARE。 */
    boolean isTradingDay(String market, LocalDate date);

    /** 返回不晚于指定日期的最近开放日。 */
    LocalDate previousOrSameOpenDate(String calendarCode, LocalDate date);

    /** 返回市场对应的日历代码。 */
    String calendarCodeForMarket(String market);
}
