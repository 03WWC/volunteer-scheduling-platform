package com.volunteer.platform.message.dao;

import com.volunteer.platform.message.entity.MessageNoticeDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface MessageNoticeDAO {

    int insert(MessageNoticeDO noticeDO);

    MessageNoticeDO selectById(Long id);

    MessageNoticeDO selectByEventKey(String eventKey);

    List<MessageNoticeDO> selectByReceiverId(Long receiverId);

    int updateRead(Long id);
}
