package com.faber.api.base.admin.biz;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import com.faber.api.base.admin.entity.User;
import com.faber.api.base.admin.vo.query.RemoteClientQueryVo;
import com.faber.api.base.admin.vo.ret.RemoteClientVo;
import com.faber.api.base.rbac.mapper.RbacUserRoleMapper;
import com.faber.config.websocket.WsChatEndpoint;
import com.faber.config.websocket.WsClientInfoEntity;
import com.faber.core.constant.CommonConstants;
import com.faber.core.context.BaseContextHandler;
import com.faber.core.exception.auth.UserNoPermissionException;
import com.faber.core.exception.auth.UserTokenException;
import com.faber.core.vo.msg.TableRet;
import com.faber.core.vo.query.BasePageQuery;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
public class RemoteClientBiz {
    public static final String VIEW_PERMISSION = "/admin/system/monitor/remoteClient";
    private static final long ONLINE_WINDOW_MILLIS = 60_000L;

    @Resource
    private RbacUserRoleMapper roleMapper;

    public TableRet<RemoteClientVo> page(BasePageQuery<RemoteClientQueryVo> params) {
        requireAccess();
        int size = Math.min(100, Math.max(1, params.getPageSize()));
        int current = Math.max(1, params.getCurrent());
        long now = System.currentTimeMillis();
        RemoteClientQueryVo query = params.getQuery() == null ? new RemoteClientQueryVo() : params.getQuery();
        List<RemoteClientVo> matches = WsChatEndpoint.getRemoteClientConnections().stream()
                .filter(client -> now - client.getLastSeenAt() <= ONLINE_WINDOW_MILLIS)
                .filter(client -> StrUtil.isBlank(query.getClientType())
                        || Objects.equals(query.getClientType(), client.getClientType()))
                .filter(client -> matches(client, query.getKeyword()))
                .sorted(Comparator.comparingLong(WsClientInfoEntity::getLastSeenAt).reversed())
                .map(this::toVo)
                .toList();
        current = Math.min(current, Math.max(1, (matches.size() + size - 1) / size));
        List<RemoteClientVo> rows = matches.stream().skip((long) (current - 1) * size).limit(size).toList();
        TableRet.Pagination pagination = new TableRet.Pagination();
        pagination.setCurrent(current);
        pagination.setPageSize(size);
        pagination.setTotal(matches.size());
        pagination.setPages((matches.size() + size - 1L) / size);
        return new TableRet<>(pagination, rows);
    }

    private boolean matches(WsClientInfoEntity client, String keyword) {
        if (StrUtil.isBlank(keyword)) return true;
        String value = keyword.trim();
        User user = client.getUser();
        return StrUtil.containsIgnoreCase(client.getAppName(), value)
                || StrUtil.containsIgnoreCase(client.getAppCode(), value)
                || StrUtil.containsIgnoreCase(client.getRelease(), value)
                || (user != null && (StrUtil.containsIgnoreCase(user.getUsername(), value)
                    || StrUtil.containsIgnoreCase(user.getName(), value)));
    }

    private RemoteClientVo toVo(WsClientInfoEntity client) {
        RemoteClientVo vo = new RemoteClientVo();
        vo.setId(client.getSession().getId());
        vo.setClientType(client.getClientType());
        vo.setRuntime(client.getRuntime());
        vo.setAppCode(client.getAppCode());
        vo.setAppName(client.getAppName());
        vo.setRelease(client.getRelease());
        vo.setEnvironment(client.getEnvironment());
        vo.setPlatform(client.getPlatform());
        vo.setOsName(client.getOsName());
        vo.setOsVersion(client.getOsVersion());
        vo.setDeviceModel(client.getDeviceModel());
        User user = client.getUser();
        if (user != null) {
            vo.setUserId(user.getId());
            vo.setUsername(user.getUsername());
            vo.setName(user.getName());
        }
        vo.setConnectedAt(client.getConnectedAt());
        vo.setLastSeenAt(client.getLastSeenAt());
        return vo;
    }

    private void requireAccess() {
        String userId = BaseContextHandler.getUserId();
        String token = StpUtil.getTokenValue();
        if (StrUtil.isBlank(userId) || !userId.equals(StpUtil.getLoginIdByToken(token))) {
            throw new UserNoPermissionException("无在线客户端查看权限");
        }
        String device = StpUtil.getLoginDeviceByToken(token);
        if (StrUtil.isBlank(device)) {
            throw new UserTokenException("登录会话信息不完整，请重新登录");
        }
        if (!"web".equals(device)
                || (!CommonConstants.SUPER_ADMIN_ID.equals(userId)
                    && roleMapper.countPlatformPermission(userId, VIEW_PERMISSION) == 0)) {
            throw new UserNoPermissionException("无在线客户端查看权限");
        }
    }
}
