package com.faber.api.base.admin.biz;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import com.faber.api.base.admin.entity.User;
import com.faber.api.base.admin.vo.query.OnlineUserKickoutVo;
import com.faber.api.base.admin.vo.query.OnlineUserPresenceQueryVo;
import com.faber.api.base.admin.vo.query.OnlineUserQueryVo;
import com.faber.api.base.admin.vo.ret.OnlineUserPresenceDeviceVo;
import com.faber.api.base.admin.vo.ret.OnlineUserPresenceSummaryVo;
import com.faber.api.base.admin.vo.ret.OnlineUserStatsVo;
import com.faber.api.base.admin.vo.ret.OnlineUserVo;
import com.faber.config.websocket.WsClientPresenceRecord;
import com.faber.config.websocket.WsClientPresenceStore;
import com.faber.api.base.rbac.mapper.RbacUserRoleMapper;
import com.faber.config.auth.OnlineUserSession;
import com.faber.config.auth.OnlineUserStore;
import com.faber.config.auth.OnlineUserTracker;
import com.faber.core.constant.CommonConstants;
import com.faber.core.context.BaseContextHandler;
import com.faber.core.exception.BuzzException;
import com.faber.core.exception.auth.UserNoPermissionException;
import com.faber.core.exception.auth.UserTokenException;
import com.faber.core.vo.msg.TableRet;
import com.faber.core.vo.query.BasePageQuery;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class OnlineUserBiz {
    public static final String VIEW_PERMISSION = "/admin/system/monitor/onlineUser";
    public static final String KICK_PERMISSION = VIEW_PERMISSION + ":kickout";
    private static final long SNAPSHOT_MILLIS = 5000;

    @Resource
    private OnlineUserStore store;
    @Resource
    private WsClientPresenceStore clientPresenceStore;
    @Resource
    private UserBiz userBiz;
    @Resource
    private RbacUserRoleMapper roleMapper;
    @Value("${fa.online-user.active-window-seconds:300}")
    private long activeWindowSeconds;

    private volatile Snapshot snapshot;
    private record Snapshot(long revision, long time, List<OnlineUserSession> sessions) {}
    private record DeviceKey(String clientType, String clientInstanceId) {}
    private record PresenceDevice(String clientType, String clientInstanceId,
                                  WsClientPresenceRecord metadata, long connectedAt, long lastSeenAt) {}

    public TableRet<OnlineUserVo> page(BasePageQuery<OnlineUserQueryVo> params) {
        requireAccess(VIEW_PERMISSION);
        int size = Math.min(100, Math.max(1, params.getPageSize()));
        int current = Math.max(1, params.getCurrent());
        long now = System.currentTimeMillis();
        OnlineUserQueryVo query = params.getQuery() == null ? new OnlineUserQueryVo() : params.getQuery();
        List<OnlineUserSession> matches = sessions().stream()
                .filter(s -> !expired(s, now))
                .filter(s -> StrUtil.isBlank(query.getSource()) || "web".equals(query.getSource()))
                .filter(s -> matches(s, query.getKeyword()))
                .filter(s -> query.getActive() == null || query.getActive() == active(s, now))
                .sorted(Comparator.comparingLong(OnlineUserSession::getLastAccessTime).reversed()
                        .thenComparing(OnlineUserSession::getId))
                .toList();
        current = Math.min(current, Math.max(1, (matches.size() + size - 1) / size));
        List<OnlineUserVo> rows = matches.stream().skip((long) (current - 1) * size).limit(size)
                .map(s -> toVo(s, now)).toList();
        TableRet.Pagination pagination = new TableRet.Pagination();
        pagination.setCurrent(current);
        pagination.setPageSize(size);
        pagination.setTotal(matches.size());
        pagination.setPages((matches.size() + size - 1L) / size);
        return new TableRet<>(pagination, rows);
    }

    public OnlineUserStatsVo stats() {
        requireAccess(VIEW_PERMISSION);
        long now = System.currentTimeMillis();
        List<OnlineUserSession> sessions = sessions().stream().filter(s -> !expired(s, now)).toList();
        OnlineUserStatsVo result = new OnlineUserStatsVo();
        result.setSessionCount(sessions.size());
        result.setUserCount(sessions.stream().map(OnlineUserSession::getUserId).distinct().count());
        result.setActiveUserCount(sessions.stream().filter(s -> active(s, now))
                .map(OnlineUserSession::getUserId).distinct().count());
        result.setActiveWindowSeconds(Math.max(1, activeWindowSeconds));
        return result;
    }

    /** 汇总在线设备后分页，避免一个用户的多个连接占据多行。 */
    public TableRet<OnlineUserPresenceSummaryVo> presencePage(
            BasePageQuery<OnlineUserPresenceQueryVo> params) {
        requireAccess(VIEW_PERMISSION);
        int size = Math.min(100, Math.max(1, params.getPageSize()));
        int current = Math.max(1, params.getCurrent());
        OnlineUserPresenceQueryVo query = params.getQuery() == null
                ? new OnlineUserPresenceQueryVo() : params.getQuery();
        Map<String, List<PresenceDevice>> devicesByUser = currentDevicesByUser(System.currentTimeMillis());

        Map<String, User> usersById = new HashMap<>();
        if (!devicesByUser.isEmpty()) {
            userBiz.listByIds(devicesByUser.keySet()).forEach(user -> usersById.put(user.getId(), user));
        }
        List<OnlineUserPresenceSummaryVo> matches = new ArrayList<>();
        for (Map.Entry<String, List<PresenceDevice>> entry : devicesByUser.entrySet()) {
            User user = usersById.get(entry.getKey());
            if (!matches(user, entry.getKey(), query.getKeyword())) continue;
            matches.add(toPresenceSummary(entry.getKey(), user, entry.getValue()));
        }
        matches.sort(Comparator.comparingLong(OnlineUserPresenceSummaryVo::getLastSeenAt).reversed()
                .thenComparing(OnlineUserPresenceSummaryVo::getUserId));

        current = Math.min(current, Math.max(1, (matches.size() + size - 1) / size));
        List<OnlineUserPresenceSummaryVo> rows = matches.stream()
                .skip((long) (current - 1) * size).limit(size).toList();
        TableRet.Pagination pagination = new TableRet.Pagination();
        pagination.setCurrent(current);
        pagination.setPageSize(size);
        pagination.setTotal(matches.size());
        pagination.setPages((matches.size() + size - 1L) / size);
        return new TableRet<>(pagination, rows);
    }

    /** 查询指定用户当前活跃的去重设备，不向响应暴露实例 ID 或连接标识。 */
    public List<OnlineUserPresenceDeviceVo> presenceDevices(String userId) {
        requireAccess(VIEW_PERMISSION);
        return currentDevicesByUser(System.currentTimeMillis()).getOrDefault(userId, List.of()).stream()
                .sorted(Comparator.comparingLong(PresenceDevice::lastSeenAt).reversed())
                .map(this::toPresenceDevice)
                .toList();
    }

    public int kickout(OnlineUserKickoutVo params) {
        requireAccess(VIEW_PERMISSION);
        requireAccess(KICK_PERMISSION);
        BaseContextHandler.setLogOprRemark("会话=" + params.getId() + ", 范围="
                + (params.isAllSessions() ? "用户全部后台会话" : "指定会话"));
        OnlineUserSession session = store.get(params.getId());
        if (session == null || !valid(session)) {
            store.remove(params.getId());
            store.invalidate();
            BaseContextHandler.setLogOprRemark("会话=" + params.getId() + ", 结果=已失效，无需重复下线");
            return 0;
        }
        if (Objects.equals(session.getToken(), StpUtil.getTokenValue())
                || (params.isAllSessions() && Objects.equals(session.getUserId(), BaseContextHandler.getUserId()))) {
            throw new BuzzException("不能下线包含当前登录会话的操作，请使用其他管理员账号处理");
        }
        BaseContextHandler.setLogOprRemark("目标用户=" + session.getUserId() + "(" + session.getUsername()
                + "), 范围=" + (params.isAllSessions() ? "全部后台会话" : "指定会话") + ", 结果=执行中");
        List<String> tokens = params.isAllSessions()
                ? StpUtil.getTokenValueListByLoginId(session.getUserId(), "web") : List.of(session.getToken());
        int count = 0;
        try {
            for (String token : tokens) {
                StpUtil.kickoutByTokenValue(token);
                count++;
                store.remove(OnlineUserTracker.sessionId(token));
            }
            return count;
        } finally {
            snapshot = null;
            store.invalidate();
            BaseContextHandler.setLogOprRemark("目标用户=" + session.getUserId() + "(" + session.getUsername()
                    + "), 范围=" + (params.isAllSessions() ? "全部后台会话" : "指定会话")
                    + ", 已下线=" + count + "/" + tokens.size());
        }
    }

    private void requireAccess(String permission) {
        String userId = BaseContextHandler.getUserId();
        // API Token 和 portal 会话均不能调用平台在线会话管理接口。
        String token = StpUtil.getTokenValue();
        if (StrUtil.isBlank(userId) || !userId.equals(StpUtil.getLoginIdByToken(token))) {
            throw new UserNoPermissionException("无在线用户平台管理权限");
        }
        String device = StpUtil.getLoginDeviceByToken(token);
        if (StrUtil.isBlank(device)) {
            // 历史 Redis 适配器可能未持久化设备索引，不能把未知来源当作后台会话放行。
            throw new UserTokenException("登录会话信息不完整，请重新登录");
        }
        if (!"web".equals(device)
                || (!CommonConstants.SUPER_ADMIN_ID.equals(userId)
                    && roleMapper.countPlatformPermission(userId, permission) == 0)) {
            throw new UserNoPermissionException("无在线用户平台管理权限");
        }
    }

    /** 五秒快照供分页/统计共享，强制下线通过 Redis revision 使各节点立即重建。 */
    private synchronized List<OnlineUserSession> sessions() {
        long revision = store.revision();
        long now = System.currentTimeMillis();
        Snapshot cached = snapshot;
        if (cached != null && cached.revision() == revision && now - cached.time() < SNAPSHOT_MILLIS) {
            return cached.sessions();
        }
        List<OnlineUserSession> live = new ArrayList<>();
        for (OnlineUserSession session : store.all()) {
            if (valid(session)) live.add(session);
            else store.remove(session.getId());
        }
        snapshot = new Snapshot(revision, now, List.copyOf(live));
        return snapshot.sessions();
    }

    private boolean valid(OnlineUserSession session) {
        return !expired(session, System.currentTimeMillis())
                && session.getUserId().equals(StpUtil.getLoginIdByToken(session.getToken()))
                && "web".equals(StpUtil.getLoginDeviceByToken(session.getToken()));
    }

    private boolean expired(OnlineUserSession session, long now) {
        return session.getExpiresAt() != null && session.getExpiresAt() <= now;
    }

    private boolean active(OnlineUserSession session, long now) {
        return now - session.getLastAccessTime() <= Math.max(1, activeWindowSeconds) * 1000;
    }

    private boolean matches(OnlineUserSession session, String keyword) {
        return StrUtil.isBlank(keyword) || StrUtil.containsIgnoreCase(session.getUsername(), keyword.trim())
                || StrUtil.containsIgnoreCase(session.getName(), keyword.trim());
    }

    private boolean matches(User user, String userId, String keyword) {
        return StrUtil.isBlank(keyword)
                || StrUtil.containsIgnoreCase(userId, keyword.trim())
                || (user != null && (StrUtil.containsIgnoreCase(user.getUsername(), keyword.trim())
                    || StrUtil.containsIgnoreCase(user.getName(), keyword.trim())));
    }

    private Map<String, List<PresenceDevice>> currentDevicesByUser(long now) {
        long activeAfter = now - WsClientPresenceStore.ONLINE_TTL_SECONDS * 1000;
        Map<String, Map<DeviceKey, PresenceDevice>> devicesByUser = new HashMap<>();
        for (WsClientPresenceRecord record : clientPresenceStore.all().values()) {
            if (record == null || StrUtil.isBlank(record.getUserId())
                    || record.getLastSeenAt() < activeAfter || record.getLastSeenAt() <= 0) continue;

            String clientType = record.getClientType();
            if (!isPresenceClientType(clientType)) continue;
            String clientInstanceId = StrUtil.blankToDefault(record.getClientInstanceId(), record.getSessionId());
            if (StrUtil.isBlank(clientInstanceId)) continue;

            DeviceKey key = new DeviceKey(clientType, clientInstanceId);
            Map<DeviceKey, PresenceDevice> userDevices = devicesByUser.computeIfAbsent(
                    record.getUserId(), ignored -> new HashMap<>());
            PresenceDevice existing = userDevices.get(key);
            long connectedAt = earliest(existing == null ? 0 : existing.connectedAt(), record.getConnectedAt());
            long lastSeenAt = Math.max(existing == null ? 0 : existing.lastSeenAt(), record.getLastSeenAt());
            WsClientPresenceRecord metadata = existing == null || record.getLastSeenAt() >= existing.lastSeenAt()
                    ? record : existing.metadata();
            userDevices.put(key, new PresenceDevice(clientType, clientInstanceId,
                    metadata, connectedAt, lastSeenAt));
        }

        Map<String, List<PresenceDevice>> result = new HashMap<>();
        devicesByUser.forEach((userId, devices) -> result.put(userId, List.copyOf(devices.values())));
        return result;
    }

    private boolean isPresenceClientType(String clientType) {
        return "WEB".equals(clientType) || "MOBILE".equals(clientType) || "DESKTOP".equals(clientType);
    }

    private long earliest(long first, long second) {
        if (first <= 0) return second;
        if (second <= 0) return first;
        return Math.min(first, second);
    }

    private OnlineUserPresenceSummaryVo toPresenceSummary(
            String userId, User user, List<PresenceDevice> devices) {
        OnlineUserPresenceSummaryVo vo = new OnlineUserPresenceSummaryVo();
        vo.setUserId(userId);
        if (user != null) {
            vo.setUsername(user.getUsername());
            vo.setName(user.getName());
        }
        vo.setWebCount(devices.stream().filter(device -> "WEB".equals(device.clientType())).count());
        vo.setAppCount(devices.stream().filter(device -> "MOBILE".equals(device.clientType())).count());
        vo.setDesktopCount(devices.stream().filter(device -> "DESKTOP".equals(device.clientType())).count());
        vo.setDeviceCount(devices.size());
        vo.setLastSeenAt(devices.stream().mapToLong(PresenceDevice::lastSeenAt).max().orElse(0));
        return vo;
    }

    private OnlineUserPresenceDeviceVo toPresenceDevice(PresenceDevice device) {
        WsClientPresenceRecord metadata = device.metadata();
        OnlineUserPresenceDeviceVo vo = new OnlineUserPresenceDeviceVo();
        vo.setClientType(device.clientType());
        vo.setAppCode(metadata.getAppCode());
        vo.setAppName(metadata.getAppName());
        vo.setRelease(metadata.getRelease());
        vo.setEnvironment(metadata.getEnvironment());
        vo.setPlatform(metadata.getPlatform());
        vo.setOsName(metadata.getOsName());
        vo.setOsVersion(metadata.getOsVersion());
        vo.setDeviceModel(metadata.getDeviceModel());
        vo.setConnectedAt(device.connectedAt());
        vo.setLastSeenAt(device.lastSeenAt());
        return vo;
    }

    private OnlineUserVo toVo(OnlineUserSession session, long now) {
        OnlineUserVo vo = new OnlineUserVo();
        vo.setId(session.getId());
        vo.setUserId(session.getUserId());
        vo.setUsername(session.getUsername());
        vo.setName(session.getName());
        vo.setSource("web");
        vo.setLoginTime(session.getLoginTime());
        vo.setLastAccessTime(session.getLastAccessTime());
        vo.setIp(session.getIp());
        vo.setBrowser(session.getBrowser());
        vo.setOs(session.getOs());
        vo.setExpiresAt(session.getExpiresAt());
        vo.setActive(active(session, now));
        vo.setCurrent(Objects.equals(session.getToken(), StpUtil.getTokenValue()));
        vo.setCurrentUser(Objects.equals(session.getUserId(), BaseContextHandler.getUserId()));
        return vo;
    }
}
