package com.volunteer.platform.user.dao;

import com.volunteer.platform.user.entity.UserSkillDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface UserSkillDAO {

    int insert(UserSkillDO userSkillDO);

    UserSkillDO selectById(@Param("id") Long id);

    int deleteById(@Param("id") Long id);

    List<UserSkillDO> selectByUserId(@Param("userId") Long userId);
}
