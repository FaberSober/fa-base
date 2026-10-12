package com.faber.api.base.calendar.biz;

import com.faber.api.base.calendar.entity.BaseCalendar;
import com.faber.api.base.calendar.mapper.BaseCalendarMapper;
import com.faber.core.exception.BuzzException;
import com.faber.core.web.biz.BaseBiz;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.Locale;

/** 统一日历定义管理。日历编码一旦被使用，不允许通过后台修改。 */
@Service
public class BaseCalendarBiz extends BaseBiz<BaseCalendarMapper, BaseCalendar> {

    @Override
    public boolean save(BaseCalendar entity) {
        normalize(entity);
        if (lambdaQuery().eq(BaseCalendar::getCalendarCode, entity.getCalendarCode()).count() > 0) {
            throw new BuzzException("日历编码重复: " + entity.getCalendarCode());
        }
        return super.save(entity);
    }

    @Override
    public boolean updateById(BaseCalendar entity) {
        if (entity == null || entity.getId() == null) {
            throw new BuzzException("日历ID不能为空");
        }
        BaseCalendar current = getById(entity.getId());
        if (current == null) {
            throw new BuzzException("日历不存在");
        }
        entity.setCalendarCode(current.getCalendarCode());
        normalize(entity);
        return super.updateById(entity);
    }

    @Override
    public boolean removeById(Serializable id) {
        throw new BuzzException("统一日历不支持删除，请停用日历");
    }

    private void normalize(BaseCalendar entity) {
        if (entity == null) {
            throw new BuzzException("日历不能为空");
        }
        if (entity.getCalendarCode() != null) {
            entity.setCalendarCode(entity.getCalendarCode().trim().toUpperCase(Locale.ROOT));
        }
        if (entity.getEnabled() == null) {
            entity.setEnabled(true);
        }
    }
}
