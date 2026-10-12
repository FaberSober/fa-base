package com.faber.api.base.admin.biz;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import com.faber.api.base.admin.vo.query.RemoteClientQueryVo;
import com.faber.api.base.admin.vo.ret.RemoteClientVo;
import com.faber.api.base.rbac.mapper.RbacUserRoleMapper;
import com.faber.config.websocket.WsChatEndpoint;
import com.faber.config.websocket.WsClientInfoEntity;
import com.faber.config.websocket.WsClientPresenceRecord;
import com.faber.config.websocket.WsClientPresenceStore;
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

    @Resource
    private WsClientPresenceStore presenceStore;

    public TableRet<RemoteClientVo> page(BasePageQuery<RemoteClientQueryVo> params) {
        requireAccess();
        int size = Math.min(100, Math.max(1, params.getPageSize()));
        int current = Math.max(1, params.getCurrent());
        RemoteClientQueryVo query = params.getQuery() == null ? new RemoteClientQueryVo() : params.getQuery();
        long now = System.currentTimeMillis();
        List<RemoteClientVo> matches = presenceStore.all().values().stream()
                .filter(client -> client.getClientType() != null)
                .filter(client -> now - client.getLastSeenAt() <= ONLINE_WINDOW_MILLIS)
                .filter(client -> StrUtil.isBlank(query.getClientType())
                        || Objects.equals(query.getClientType(), client.getClientType()))
                .filter(client -> matches(client, query.getKeyword()))
                .map(this::toVo)
                .sorted(buildComparator(params.getSorter()))
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

    private Comparator<RemoteClientVo> buildComparator(String sorter) {
        // 默认按连接时间排序，避免心跳更新使列表跳动；ID 保证同值顺序稳定。
        Comparator<RemoteClientVo> defaultOrder = Comparator.comparingLong(RemoteClientVo::getConnectedAt)
                .reversed().thenComparing(RemoteClientVo::getId);
        if (StrUtil.isBlank(sorter)) return defaultOrder;
        String[] parts = sorter.trim().split("\\s+");
        if (parts.length != 2 || (!"ASC".equalsIgnoreCase(parts[1]) && !"DESC".equalsIgnoreCase(parts[1]))) {
            return defaultOrder;
        }
        Comparator<String> textOrder = Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER);
        Comparator<RemoteClientVo> order = switch (parts[0]) {
            case "username" -> Comparator.comparing(RemoteClientVo::getUsername, textOrder);
            case "name" -> Comparator.comparing(RemoteClientVo::getName, textOrder);
            case "client_type" -> Comparator.comparing(RemoteClientVo::getClientType, textOrder);
            case "app_name" -> Comparator.comparing(RemoteClientVo::getAppName, textOrder);
            case "connected_at" -> Comparator.comparingLong(RemoteClientVo::getConnectedAt);
            case "last_seen_at" -> Comparator.comparingLong(RemoteClientVo::getLastSeenAt);
            default -> null;
        };
        if (order == null) return defaultOrder;
        if ("DESC".equalsIgnoreCase(parts[1])) order = order.reversed();
        return order.thenComparing(defaultOrder);
    }

    public WsClientInfoEntity getOnlineClient(String sessionId) {
        if (StrUtil.isBlank(sessionId)) return null;
        return getOnlineClients().stream()
                .filter(client -> sessionId.equals(client.getConnectionId()))
                .findFirst()
                .orElse(null);
    }

    private List<WsClientInfoEntity> getOnlineClients() {
        long now = System.currentTimeMillis();
        return WsChatEndpoint.getRemoteClientConnections().stream()
                .filter(client -> now - client.getLastSeenAt() <= ONLINE_WINDOW_MILLIS)
                .toList();
    }

    private boolean matches(WsClientPresenceRecord client, String keyword) {
        if (StrUtil.isBlank(keyword)) return true;
        String value = keyword.trim();
        return StrUtil.containsIgnoreCase(client.getAppName(), value)
                || StrUtil.containsIgnoreCase(client.getAppCode(), value)
                || StrUtil.containsIgnoreCase(client.getRelease(), value)
                || StrUtil.containsIgnoreCase(client.getUsername(), value)
                || StrUtil.containsIgnoreCase(client.getName(), value);
    }

    private RemoteClientVo toVo(WsClientPresenceRecord client) {
        RemoteClientVo vo = new RemoteClientVo();
        vo.setId(client.getSessionId());
        vo.setClientInstanceId(client.getClientInstanceId());
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
        vo.setUserId(client.getUserId());
        vo.setUsername(client.getUsername());
        vo.setName(client.getName());
        vo.setConnectedAt(client.getConnectedAt());
        vo.setLastSeenAt(client.getLastSeenAt());
        return vo;
    }

    private void requireAccess() {
        requireAccess(BaseContextHandler.getUserId(), StpUtil.getTokenValue());
    }

    public void requireAccess(String userId, String token) {
        if (StrUtil.isBlank(userId) || !userId.equals(StpUtil.getLoginIdByToken(token))) {
            throw new UserNoPermissionException("无在线调试查看权限");
        }
        String device = StpUtil.getLoginDeviceByToken(token);
        if (StrUtil.isBlank(device)) {
            throw new UserTokenException("登录会话信息不完整，请重新登录");
        }
        if (!"web".equals(device)
                || (!CommonConstants.SUPER_ADMIN_ID.equals(userId)
                    && roleMapper.countPlatformPermission(userId, VIEW_PERMISSION) == 0)) {
            throw new UserNoPermissionException("无在线调试查看权限");
        }
    }
}
