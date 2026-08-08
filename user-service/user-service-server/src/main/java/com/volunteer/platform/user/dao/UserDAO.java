package com.volunteer.platform.user.dao;

import com.volunteer.platform.user.entity.UserDO;
import com.volunteer.platform.user.query.UserQuery;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface UserDAO {

    int insert(UserDO userDO);

    int updateAuth(UserDO userDO);

    UserDO selectById(@Param("id") Long id);

    UserDO selectByOpenid(@Param("openid") String openid);

    List<UserDO> selectVolunteers(@Param("query") UserQuery query);

    int deleteById(@Param("id") Long id);
}
