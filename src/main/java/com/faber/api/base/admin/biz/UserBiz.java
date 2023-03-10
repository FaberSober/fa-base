package com.faber.api.base.admin.biz;

import cn.hutool.core.collection.IterUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alicp.jetcache.anno.CacheInvalidate;
import com.alicp.jetcache.anno.Cached;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.faber.api.base.admin.entity.Department;
import com.faber.api.base.admin.entity.User;
import com.faber.api.base.admin.entity.UserToken;
import com.faber.api.base.admin.mapper.UserMapper;
import com.faber.api.base.admin.vo.query.*;
import com.faber.api.base.rbac.biz.RbacRoleBiz;
import com.faber.api.base.rbac.biz.RbacUserRoleBiz;
import com.faber.api.base.rbac.entity.RbacRole;
import com.faber.config.utils.user.UserCheckUtil;
import com.faber.core.config.redis.annotation.FaCacheClear;
import com.faber.core.constant.CommonConstants;
import com.faber.core.constant.FaSetting;
import com.faber.core.context.BaseContextHandler;
import com.faber.core.exception.BuzzException;
import com.faber.core.exception.NoDataException;
import com.faber.core.exception.auth.UserInvalidException;
import com.faber.core.utils.FaPwdUtils;
import com.faber.core.vo.query.QueryParams;
import com.faber.core.web.biz.BaseBiz;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * ??????
 *
 * @author wanghaobin
 * @create 2017-06-08 16:23
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class UserBiz extends BaseBiz<UserMapper, User> {

    @Lazy
    @Resource
    private DepartmentBiz departmentBiz;

    @Lazy
    @Resource
    private RbacUserRoleBiz rbacUserRoleBiz;

    @Lazy
    @Resource
    private RbacRoleBiz rbacRoleBiz;

    @Lazy
    @Resource
    private UserTokenBiz userTokenBiz;

    @Resource
    private FaSetting faSetting;

    @Autowired
    private RedissonClient redisson;

    @Value("${spring.redis.sysName}")
    private String redisKeySysName;

    private BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(CommonConstants.PW_ENCODER_SALT);

    /**
     * ????????????
     *
     * @param account
     * @param password
     * @return
     */
    public User validate(String account, String password) {
        User user = this.getUserByUsername(account);
        if (user == null) {
            user = this.getUserByTel(account);
        }
        UserCheckUtil.checkUserValid(user);

        if (!encoder.matches(password, user.getPassword())) {
            throw new UserInvalidException("???????????????????????????");
        }
        return user;
    }

    public void validateCurrentUserPwd(String password) {
        User user = this.getUserByUsername(BaseContextHandler.getUsername());
        UserCheckUtil.checkUserValid(user);
        if (encoder.matches(password, user.getPassword())) {
            return;
        }
        throw new UserInvalidException("???????????????????????????");
    }

    public User getLoginUser() {
        User user = getById(BaseContextHandler.getUserId());
        if (!user.getStatus()) throw new BuzzException("????????????");
        user.setPassword(null);
        return user;
    }

    /**
     * ??????????????????????????????????????????
     *
     * @param entity
     */
    private void checkBeanValid(User entity) {
        // ????????????????????????????????????
        long telCount = lambdaQuery()
                .eq(User::getTel, entity.getTel())
                .ne(entity.getId() != null, User::getId, entity.getId())
                .count();
        if (telCount > 0) throw new BuzzException("???????????????");

        // ???????????????????????????
        long usernameCount = lambdaQuery()
                .eq(User::getUsername, entity.getUsername())
                .ne(entity.getId() != null, User::getId, entity.getId())
                .count();
        if (usernameCount > 0) throw new BuzzException("????????????");
    }

    /**
     * ??????????????????
     *
     * @param entity
     */
    public void updateUserRoles(User entity) {
        // ????????????
        rbacUserRoleBiz.changeUserRoles(entity.getId(), entity.getRoleIds());
        // ??????????????????
        List<RbacRole> roleList = rbacUserRoleBiz.getUserRoles(entity.getId());
        String roleNames = IterUtil.join(roleList.stream().map(RbacRole::getName).iterator(), ",");

        lambdaUpdate().eq(User::getId, entity.getId()).set(User::getRoleNames, roleNames).update();
        entity.setRoleNames(roleNames);
    }

    @Cached(name = "user:", key = "#id")
    @Override
    public User getById(Serializable id) {
        return super.getById(id);
    }

    @Override
    public boolean save(User entity) {
        this.checkBeanValid(entity);

        // ??????????????????
        String password = encoder.encode(entity.getPassword());
        entity.setPassword(password);

        super.save(entity);

        this.updateUserRoles(entity);

        return true;
    }

    @CacheInvalidate(name = "user:", key = "#entity.id")
    @FaCacheClear(pre = "rbac:userMenus:", key = "id")
    @Override
    public boolean updateById(User entity) {
        User beanDB = getById(entity.getId());
        if (beanDB == null) throw new NoDataException();

        this.checkBeanValid(entity);

        // ???????????????????????????????????????
        entity.setPassword(beanDB.getPassword());

        this.updateUserRoles(entity);

        return super.updateById(entity);
    }

    @CacheInvalidate(name = "user:", key = "#id")
    @Override
    public boolean removeById(Serializable id) {
        // ???????????????????????????admin??????
        if (ObjectUtil.equal(id, "1") || ObjectUtil.equal(id, getCurrentUserId())) {
            throw new BuzzException("????????????????????????");
        }

        return super.removeById(id);
    }


    /**
     * ????????????????????????????????? FIX-ME: ???????????????????????????????????????ID?????????
     *
     * @param username ?????????
     */
    public User getUserByUsername(String username) {
        return lambdaQuery().eq(User::getUsername, username).one();
    }

    public User getUserByTel(String tel) {
        return lambdaQuery().eq(User::getTel, tel).one();
    }

    public User findByApiToken(String token) {
        if (StringUtils.isEmpty(token)) throw new BuzzException("token??????");

        return lambdaQuery().eq(User::getApiToken, token).one();
    }

    @Override
    protected void preProcessQuery(QueryParams query) {
        // ????????????????????????
        Map<String, Object> queryMap = query.getQuery();
        if (queryMap.containsKey("departmentIdSuper")) {
            String departmentId = MapUtils.getString(queryMap, "departmentIdSuper");
            queryMap.remove("departmentIdSuper");

            List<Department> departmentList = departmentBiz.findAllChildren(departmentId);
            if (departmentList != null && !departmentList.isEmpty()) {
                List<String> departmentIdList = departmentList.stream().map(Department::getId).collect(Collectors.toList());
                queryMap.put("departmentId#$in", departmentIdList);
                queryMap.remove("departmentId");
            }
        }
    }

    @Override
    public void decorateOne(User i) {
        Department department = departmentBiz.getByIdWithCache(i.getDepartmentId());
        i.setDepartmentName(department.getName());
    }

    public boolean resetPwd(Map<String, Object> params) {
        Integer id = (Integer) params.get("id");
        String newPwd = (String) params.get("newPwd");

        User beanDB = getById(id);
        if (beanDB == null) throw new NoDataException();

        String password = FaPwdUtils.encryptPwd(newPwd);
        beanDB.setPassword(password);
        return super.updateById(beanDB);
    }

    @CacheInvalidate(name = "user:", key = "#userId")
    public boolean updateMine(String userId, UserAccountVo vo) {
        // ????????????????????????????????????
        long telCount = lambdaQuery()
                .eq(User::getTel, vo.getTel())
                .ne(User::getId, userId)
                .count();
        if (telCount > 0) throw new BuzzException("???????????????");

        // ???????????????????????????
        long usernameCount = lambdaQuery()
                .eq(User::getUsername, vo.getUsername())
                .ne(User::getId, userId)
                .count();
        if (usernameCount > 0) throw new BuzzException("????????????");

        User user = getById(userId);
        BeanUtils.copyProperties(vo, user);

        return super.updateById(user);
    }

    @CacheInvalidate(name = "user:", key = "#userId")
    public boolean updateMyPwd(String userId, Map<String, Object> params) {
        String oldPwd = (String) params.get("oldPwd");
        String newPwd = (String) params.get("newPwd");

        if (oldPwd.equals(newPwd)) throw new BuzzException("????????????????????????");

        User user = getById(userId);

        this.validateCurrentUserPwd(oldPwd);

        String password = FaPwdUtils.encryptPwd(newPwd);
        user.setPassword(password);
        return super.updateById(user);
    }

    @CacheInvalidate(name = "user:", key = "#userId")
    public boolean updateMyApiToken(String userId) {
        User user = getById(userId);
        user.setApiToken(UUID.fastUUID().toString(true));
        return super.updateById(user);
    }

    @Transactional
    public void updateBatchDept(UserBatchUpdateDeptVo params) {
        lambdaUpdate()
                .set(User::getDepartmentId, params.getDepartmentId())
                .in(User::getId, params.getUserIds())
                .update();

        delUserCacheByIds(params.getUserIds());
    }

    @Transactional
    public void updateBatchRole(UserBatchUpdateRoleVo params) {
        params.getUserIds().forEach(userId -> {
            rbacUserRoleBiz.changeUserRoles(userId, params.getRoleIds());
        });

        delUserCacheByIds(params.getUserIds());
    }

    public void updateBatchPwd(UserBatchUpdatePwdVo params) {
        this.validateCurrentUserPwd(params.getPasswordCheck());

        String newPwd = params.getNewPwd();
        if (StringUtils.isEmpty(newPwd)) {
            throw new BuzzException("?????????????????????");
        }

        // TODO ???????????????????????????????????????
//        if (newPwd.trim().length() < 6 || newPwd.trim().length() > 32) {
//            throw new BuzzException("?????????????????????");
//        }

        String password = FaPwdUtils.encryptPwd(newPwd.trim());
        params.getUserIds().forEach(id -> {
            User user = getById(id);
            user.setPassword(password);
            super.updateById(user);
        });
    }

    public void delUserCacheByIds(List<String> userIds) {
        userIds.forEach(i -> {
            delUserCacheById(i);
        });
    }

    public void delUserCacheById(String userId) {
        redisson.getKeys().deleteByPattern(redisKeySysName + ":user:" + userId);
    }

    public void accountAdminDelete(Map<String, Object> params) {
        String passwordCheck = (String) params.get("passwordCheck");
        List<Integer> ids = (List<Integer>) params.get("ids");

        this.validateCurrentUserPwd(passwordCheck);

        ids.forEach(id -> {
            removeById(id);
        });
    }

    public User getUserFromApiToken() {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        String token = request.getHeader(faSetting.getApi().getTokenApiHeader());
        if (StrUtil.isEmpty(token)) throw new BuzzException("Token Is Empty");
        UserToken userToken = userTokenBiz.getById(token);
        if (userToken == null || !userToken.getValid()) throw new BuzzException("Token Not Valid");

        return super.getById(userToken.getUserId());
    }

    public Long jumpCount(UserJumpCountVo query) {
        if (ObjectUtil.isAllEmpty(query.getUsername(), query.getTel())) return 0L;
        return lambdaQuery()
                .eq(query.getUsername() != null, User::getUsername, query.getUsername())
                .eq(query.getTel() != null, User::getTel, query.getTel())
                .count();
    }

    public void registry(UserRegistryVo params) {
        User entity = new User();
        BeanUtils.copyProperties(params, entity);

        this.checkBeanValid(entity);

        // ???????????????888888
        String password = encoder.encode(params.getPassword());
        entity.setPassword(password);

        entity.setStatus(true);
        entity.setDepartmentId("1");

        super.save(entity);

        // ??????????????????
        RbacRole rbacRole = rbacRoleBiz.getRoleByName(CommonConstants.REGISTRY_USER_ROLE_NAME);
        entity.setRoleId(rbacRole.getId());

        this.updateUserRoles(entity);
    }

    public void forgetResetPwd(UserForgetResetPwdVo params) {
        LambdaQueryChainWrapper<User> chainWrapper = lambdaQuery()
                .eq(User::getUsername, params.getUsername())
                .eq(User::getTel, params.getTel());
        long count = chainWrapper.count();
        if (count != 1) throw new BuzzException("???????????????????????????????????????????????????");

        User user = chainWrapper.one();

        String password = FaPwdUtils.encryptPwd(params.getPassword());
        lambdaUpdate()
                .set(User::getPassword, password)
                .eq(User::getId, user.getId())
                .update();
    }

}
