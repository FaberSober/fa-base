package com.faber.api.base.push.biz;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.faber.api.base.admin.biz.UserBiz;
import com.faber.api.base.admin.entity.User;
import com.faber.api.base.push.entity.PushDevice;
import com.faber.api.base.push.mapper.PushDeviceMapper;
import com.faber.api.base.push.vo.query.PushDeviceAdminQueryVo;
import com.faber.api.base.push.vo.ret.PushDeviceAdminVo;
import com.faber.api.base.rbac.mapper.RbacUserRoleMapper;
import com.faber.core.constant.CommonConstants;
import com.faber.core.context.BaseContextHandler;
import com.faber.core.exception.auth.UserNoPermissionException;
import com.faber.core.exception.auth.UserTokenException;
import com.faber.core.vo.msg.TableRet;
import com.faber.core.vo.query.BasePageQuery;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PushDeviceAdminBiz {
    public static final String VIEW_PERMISSION = "/admin/demo/advance/push";
    private static final int MAX_PAGE_SIZE = 100;

    @Resource
    private PushDeviceMapper pushDeviceMapper;
    @Resource
    private UserBiz userBiz;
    @Resource
    private RbacUserRoleMapper roleMapper;

    public TableRet<PushDeviceAdminVo> page(BasePageQuery<PushDeviceAdminQueryVo> params) {
        requireAdminAccess();

        BasePageQuery<PushDeviceAdminQueryVo> pageQuery = params == null ? new BasePageQuery<>() : params;
        PushDeviceAdminQueryVo query = pageQuery.getQuery() == null
                ? new PushDeviceAdminQueryVo() : pageQuery.getQuery();
        long current = Math.max(1, pageQuery.getCurrent());
        long pageSize = Math.min(MAX_PAGE_SIZE, Math.max(1, pageQuery.getPageSize()));

        LambdaQueryWrapper<PushDevice> wrapper = new LambdaQueryWrapper<PushDevice>()
                .eq(PushDevice::getProvider, "unipush")
                .eq(StrUtil.isNotBlank(query.getUserId()), PushDevice::getUserId, trim(query.getUserId()))
                .eq(StrUtil.isNotBlank(query.getPlatform()), PushDevice::getPlatform, trim(query.getPlatform()))
                .eq(StrUtil.isNotBlank(query.getAppId()), PushDevice::getAppId, trim(query.getAppId()))
                .eq(StrUtil.isNotBlank(query.getEnvironment()), PushDevice::getEnvironment,
                        trim(query.getEnvironment()))
                .eq(query.getEnabled() != null, PushDevice::getEnabled, query.getEnabled())
                .orderByDesc(PushDevice::getLastSeenTime)
                .orderByDesc(PushDevice::getId);

        Page<PushDevice> page = pushDeviceMapper.selectPage(new Page<>(current, pageSize), wrapper);
        Map<String, User> usersById = loadUsers(page.getRecords());
        List<PushDeviceAdminVo> rows = page.getRecords().stream()
                .map(device -> toVo(device, usersById.get(device.getUserId())))
                .toList();

        TableRet.Pagination pagination = new TableRet.Pagination();
        pagination.setCurrent(page.getCurrent());
        pagination.setPageSize(page.getSize());
        pagination.setTotal(page.getTotal());
        pagination.setPages(page.getPages());
        return new TableRet<>(pagination, rows);
    }

    private void requireAdminAccess() {
        String userId = BaseContextHandler.getUserId();
        String token = StpUtil.getTokenValue();
        if (StrUtil.isBlank(userId) || StrUtil.isBlank(token)
                || !userId.equals(StpUtil.getLoginIdByToken(token))) {
            throw new UserNoPermissionException("无推送设备管理权限");
        }

        String device = StpUtil.getLoginDeviceByToken(token);
        if (StrUtil.isBlank(device)) {
            throw new UserTokenException("登录会话信息不完整，请重新登录");
        }
        if (!"web".equals(device)
                || (!CommonConstants.SUPER_ADMIN_ID.equals(userId)
                    && roleMapper.countPlatformPermission(userId, VIEW_PERMISSION) == 0)) {
            throw new UserNoPermissionException("无推送设备管理权限");
        }
    }

    private Map<String, User> loadUsers(List<PushDevice> devices) {
        List<String> userIds = devices.stream()
                .map(PushDevice::getUserId)
                .filter(StrUtil::isNotBlank)
                .distinct()
                .toList();
        if (userIds.isEmpty()) return Map.of();

        Map<String, User> usersById = new HashMap<>();
        userBiz.listByIds(userIds).forEach(user -> usersById.put(user.getId(), user));
        return usersById;
    }

    private PushDeviceAdminVo toVo(PushDevice device, User user) {
        PushDeviceAdminVo vo = new PushDeviceAdminVo();
        vo.setId(device.getId());
        vo.setUserId(device.getUserId());
        if (user != null) {
            vo.setUsername(user.getUsername());
            vo.setName(user.getName());
        }
        vo.setProvider(device.getProvider());
        vo.setClientIdMasked(maskClientId(device.getClientId()));
        vo.setPlatform(device.getPlatform());
        vo.setAppId(device.getAppId());
        vo.setEnvironment(device.getEnvironment());
        vo.setEnabled(device.getEnabled());
        vo.setLastSeenTime(device.getLastSeenTime());
        vo.setInvalidTime(device.getInvalidTime());
        vo.setSelectable(Boolean.TRUE.equals(device.getEnabled())
                && device.getInvalidTime() == null && StrUtil.isNotBlank(device.getClientId()));
        return vo;
    }

    private String maskClientId(String clientId) {
        if (StrUtil.isBlank(clientId)) return null;
        if (clientId.length() <= 8) return "****";
        return clientId.substring(0, 4) + "****" + clientId.substring(clientId.length() - 4);
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}
