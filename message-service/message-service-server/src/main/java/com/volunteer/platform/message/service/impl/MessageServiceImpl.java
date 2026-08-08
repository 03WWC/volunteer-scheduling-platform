package com.volunteer.platform.message.service.impl;

import com.volunteer.platform.common.exception.BusinessException;
import com.volunteer.platform.message.dao.MessageNoticeDAO;
import com.volunteer.platform.message.dto.SendNoticeDTO;
import com.volunteer.platform.message.entity.MessageNoticeDO;
import com.volunteer.platform.message.sender.NoticeSender;
import com.volunteer.platform.message.service.MessageService;
import com.volunteer.platform.message.vo.MessageNoticeVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MessageServiceImpl implements MessageService {

    private static final String SUCCESS_STATUS = "SUCCESS";

    private final MessageNoticeDAO messageNoticeDAO;

    private final NoticeSender noticeSender;

    private final ReceiverContactResolver receiverContactResolver;

    public MessageServiceImpl(MessageNoticeDAO messageNoticeDAO, NoticeSender noticeSender) {
        this(messageNoticeDAO, noticeSender, new EmptyReceiverContactResolver());
    }

    @Autowired
    public MessageServiceImpl(MessageNoticeDAO messageNoticeDAO, NoticeSender noticeSender,
                              ReceiverContactResolver receiverContactResolver) {
        this.messageNoticeDAO = messageNoticeDAO;
        this.noticeSender = noticeSender;
        this.receiverContactResolver = receiverContactResolver;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MessageNoticeVO send(SendNoticeDTO dto) {
        validateSend(dto);
        MessageNoticeDO consumedNoticeDO = getConsumedNotice(dto.getEventKey());
        if (consumedNoticeDO != null) {
            return toVO(consumedNoticeDO);
        }
        enrichReceiverContact(dto);
        noticeSender.send(dto);
        MessageNoticeDO noticeDO = toNoticeDO(dto);
        messageNoticeDAO.insert(noticeDO);
        return toVO(noticeDO);
    }

    @Override
    public List<MessageNoticeVO> listByReceiver(Long receiverId) {
        if (receiverId == null) {
            throw new BusinessException(400, "receiver id is required");
        }
        return messageNoticeDAO.selectByReceiverId(receiverId).stream().map(this::toVO).toList();
    }

    @Override
    public MessageNoticeVO markRead(Long id) {
        if (id == null) {
            throw new BusinessException(400, "notice id is required");
        }
        messageNoticeDAO.updateRead(id);
        MessageNoticeDO noticeDO = messageNoticeDAO.selectById(id);
        if (noticeDO == null) {
            throw new BusinessException(404, "notice not found");
        }
        return toVO(noticeDO);
    }

    private void validateSend(SendNoticeDTO dto) {
        if (dto == null || dto.getReceiverId() == null || dto.getNoticeType() == null) {
            throw new BusinessException(400, "notice target is required");
        }
        if (dto.getTitle() == null || dto.getContent() == null || dto.getSendChannel() == null) {
            throw new BusinessException(400, "notice content is required");
        }
    }

    private MessageNoticeDO getConsumedNotice(String eventKey) {
        if (eventKey == null || eventKey.isBlank()) {
            return null;
        }
        return messageNoticeDAO.selectByEventKey(eventKey);
    }

    private void enrichReceiverContact(SendNoticeDTO dto) {
        ReceiverContact contact = receiverContactResolver.resolve(dto.getReceiverId());
        if ((dto.getReceiverOpenid() == null || dto.getReceiverOpenid().isBlank()) && contact.openid() != null) {
            dto.setReceiverOpenid(contact.openid());
        }
        if ((dto.getReceiverMobile() == null || dto.getReceiverMobile().isBlank()) && contact.mobile() != null) {
            dto.setReceiverMobile(contact.mobile());
        }
    }

    private MessageNoticeDO toNoticeDO(SendNoticeDTO dto) {
        MessageNoticeDO noticeDO = new MessageNoticeDO();
        noticeDO.setReceiverId(dto.getReceiverId());
        noticeDO.setEventKey(dto.getEventKey());
        noticeDO.setNoticeType(dto.getNoticeType());
        noticeDO.setTitle(dto.getTitle());
        noticeDO.setContent(dto.getContent());
        noticeDO.setSendChannel(dto.getSendChannel());
        noticeDO.setSendStatus(SUCCESS_STATUS);
        noticeDO.setSendTime(LocalDateTime.now());
        noticeDO.setIsRead(Boolean.FALSE);
        return noticeDO;
    }

    private MessageNoticeVO toVO(MessageNoticeDO noticeDO) {
        MessageNoticeVO vo = new MessageNoticeVO();
        vo.setId(noticeDO.getId());
        vo.setReceiverId(noticeDO.getReceiverId());
        vo.setEventKey(noticeDO.getEventKey());
        vo.setNoticeType(noticeDO.getNoticeType());
        vo.setTitle(noticeDO.getTitle());
        vo.setContent(noticeDO.getContent());
        vo.setSendChannel(noticeDO.getSendChannel());
        vo.setSendStatus(noticeDO.getSendStatus());
        vo.setSendTime(noticeDO.getSendTime());
        vo.setIsRead(noticeDO.getIsRead());
        return vo;
    }
}
