package com.volunteer.platform.activity.service.impl;

import com.volunteer.platform.activity.dao.ActivityDAO;
import com.volunteer.platform.activity.dao.ActivitySignupDAO;
import com.volunteer.platform.activity.dto.CreateActivityDTO;
import com.volunteer.platform.activity.dto.SignupActivityDTO;
import com.volunteer.platform.activity.dto.UpdateActivityDTO;
import com.volunteer.platform.activity.entity.ActivityDO;
import com.volunteer.platform.activity.entity.ActivitySignupDO;
import com.volunteer.platform.activity.query.ActivityQuery;
import com.volunteer.platform.activity.query.ActivitySignupQuery;
import com.volunteer.platform.activity.service.ActivityService;
import com.volunteer.platform.activity.vo.ActivitySignupVO;
import com.volunteer.platform.activity.vo.ActivityVO;
import com.volunteer.platform.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ActivityServiceImpl implements ActivityService {

    private static final Integer NOT_FOUND_CODE = 404;
    private static final String SIGNUP_PENDING_STATUS = "PENDING";
    private static final String SIGNUP_APPROVED_STATUS = "APPROVED";
    private static final String SIGNUP_REJECTED_STATUS = "REJECTED";
    private static final String SIGNUP_CANCELLED_STATUS = "CANCELLED";

    private final ActivityDAO activityDAO;
    private final ActivitySignupDAO activitySignupDAO;

    public ActivityServiceImpl(ActivityDAO activityDAO) {
        this(activityDAO, null);
    }

    @Autowired
    public ActivityServiceImpl(ActivityDAO activityDAO, ActivitySignupDAO activitySignupDAO) {
        this.activityDAO = activityDAO;
        this.activitySignupDAO = activitySignupDAO;
    }

    @Override
    public ActivityVO create(CreateActivityDTO dto) {
        ActivityDO activityDO = toActivityDO(dto);
        activityDAO.insert(activityDO);
        return toActivityVO(activityDO);
    }

    @Override
    public List<ActivityVO> page(ActivityQuery query) {
        return activityDAO.selectPage(query).stream()
            .map(this::toActivityVO)
            .toList();
    }

    @Override
    public ActivityVO update(UpdateActivityDTO dto) {
        if (activityDAO.selectById(dto.getId()) == null) {
            throw new BusinessException(NOT_FOUND_CODE, "activity not found");
        }
        ActivityDO activityDO = toActivityDO(dto);
        activityDO.setId(dto.getId());
        activityDAO.updateById(activityDO);
        return toActivityVO(activityDAO.selectById(dto.getId()));
    }

    @Override
    public void delete(Long id) {
        if (activityDAO.deleteById(id) == 0) {
            throw new BusinessException(NOT_FOUND_CODE, "activity not found");
        }
    }

    @Override
    public ActivitySignupVO signup(Long activityId, SignupActivityDTO dto) {
        validateSignup(activityId, dto);
        if (activityDAO.selectById(activityId) == null) {
            throw new BusinessException(NOT_FOUND_CODE, "activity not found");
        }
        if (activitySignupDAO.selectExisting(activityId, dto.getUserId(), dto.getPositionId()) != null) {
            throw new BusinessException(409, "activity signup already exists");
        }
        ActivitySignupDO signupDO = new ActivitySignupDO();
        signupDO.setActivityId(activityId);
        signupDO.setPositionId(dto.getPositionId());
        signupDO.setUserId(dto.getUserId());
        signupDO.setSignupStatus(SIGNUP_PENDING_STATUS);
        signupDO.setRemark(dto.getRemark());
        activitySignupDAO.insert(signupDO);
        return toSignupVO(signupDO);
    }

    @Override
    public List<ActivitySignupVO> listUserSignups(Long userId) {
        if (activitySignupDAO == null) {
            throw new BusinessException(500, "activity signup dao is required");
        }
        if (userId == null) {
            throw new BusinessException(400, "user id is required");
        }
        return activitySignupDAO.selectByUserId(userId).stream()
            .map(this::toSignupVO)
            .toList();
    }

    @Override
    public List<ActivitySignupVO> listSignups(ActivitySignupQuery query) {
        requireSignupDAO();
        ActivitySignupQuery safeQuery = query == null ? new ActivitySignupQuery() : query;
        return activitySignupDAO.selectPage(safeQuery).stream()
            .map(this::toSignupVO)
            .toList();
    }

    @Override
    public ActivitySignupVO approveSignup(Long signupId) {
        return updateSignupStatus(signupId, SIGNUP_APPROVED_STATUS, null);
    }

    @Override
    public ActivitySignupVO rejectSignup(Long signupId) {
        return updateSignupStatus(signupId, SIGNUP_REJECTED_STATUS, null);
    }

    @Override
    public ActivitySignupVO cancelSignup(Long signupId, Long userId) {
        return updateSignupStatus(signupId, SIGNUP_CANCELLED_STATUS, userId);
    }

    private void validateSignup(Long activityId, SignupActivityDTO dto) {
        requireSignupDAO();
        if (activityId == null || dto == null || dto.getUserId() == null) {
            throw new BusinessException(400, "activity signup target is required");
        }
    }

    private ActivitySignupVO updateSignupStatus(Long signupId, String targetStatus, Long operatorUserId) {
        requireSignupDAO();
        if (signupId == null) {
            throw new BusinessException(400, "signup id is required");
        }
        ActivitySignupDO signupDO = activitySignupDAO.selectById(signupId);
        if (signupDO == null) {
            throw new BusinessException(NOT_FOUND_CODE, "activity signup not found");
        }
        if (operatorUserId != null && !operatorUserId.equals(signupDO.getUserId())) {
            throw new BusinessException(403, "activity signup user mismatch");
        }
        if (!SIGNUP_PENDING_STATUS.equals(signupDO.getSignupStatus())) {
            throw new BusinessException(409, "activity signup status cannot be changed");
        }
        activitySignupDAO.updateStatus(signupId, targetStatus);
        signupDO.setSignupStatus(targetStatus);
        return toSignupVO(signupDO);
    }

    private void requireSignupDAO() {
        if (activitySignupDAO == null) {
            throw new BusinessException(500, "activity signup dao is required");
        }
    }

    private ActivityDO toActivityDO(CreateActivityDTO dto) {
        ActivityDO activityDO = new ActivityDO();
        activityDO.setName(dto.getName());
        activityDO.setActivityType(dto.getActivityType());
        activityDO.setStartTime(dto.getStartTime());
        activityDO.setEndTime(dto.getEndTime());
        activityDO.setLocation(dto.getLocation());
        activityDO.setOwnerName(dto.getOwnerName());
        activityDO.setContactPhone(dto.getContactPhone());
        return activityDO;
    }

    private ActivityVO toActivityVO(ActivityDO activityDO) {
        ActivityVO activityVO = new ActivityVO();
        activityVO.setId(activityDO.getId());
        activityVO.setName(activityDO.getName());
        activityVO.setActivityType(activityDO.getActivityType());
        activityVO.setStartTime(activityDO.getStartTime());
        activityVO.setEndTime(activityDO.getEndTime());
        activityVO.setLocation(activityDO.getLocation());
        activityVO.setOwnerName(activityDO.getOwnerName());
        activityVO.setContactPhone(activityDO.getContactPhone());
        return activityVO;
    }

    private ActivitySignupVO toSignupVO(ActivitySignupDO signupDO) {
        ActivitySignupVO signupVO = new ActivitySignupVO();
        signupVO.setId(signupDO.getId());
        signupVO.setActivityId(signupDO.getActivityId());
        signupVO.setPositionId(signupDO.getPositionId());
        signupVO.setUserId(signupDO.getUserId());
        signupVO.setSignupStatus(signupDO.getSignupStatus());
        signupVO.setRemark(signupDO.getRemark());
        return signupVO;
    }
}
