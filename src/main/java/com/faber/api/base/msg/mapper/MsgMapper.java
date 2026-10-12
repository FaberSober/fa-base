package com.faber.api.base.msg.mapper;

import com.faber.api.base.msg.entity.Msg;
import com.faber.api.base.msg.vo.MsgTenantUnreadCountVO;
import com.faber.core.config.mybatis.base.FaBaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 系统-消息
 * 
 * @author Farando
 * @email faberxu@gmail.com
 * @date 2020-12-13 21:19:53
 */
public interface MsgMapper extends FaBaseMapper<Msg> {

    @Select({
            "<script>",
            "SELECT tenant_id AS tenant_id, COUNT(*) AS unread_count FROM base_msg",
            "WHERE to_user_id = #{userId} AND is_read = false AND deleted = false",
            "AND tenant_id IN",
            "<foreach collection='tenantIds' item='tenantId' open='(' separator=',' close=')'>",
            "#{tenantId}",
            "</foreach>",
            "GROUP BY tenant_id",
            "</script>"
    })
    @Results({
            @Result(column = "tenant_id", property = "tenantId"),
            @Result(column = "unread_count", property = "unreadCount")
    })
    List<MsgTenantUnreadCountVO> countUnreadByTenantIds(
            @Param("userId") String userId,
            @Param("tenantIds") List<String> tenantIds
    );
}
