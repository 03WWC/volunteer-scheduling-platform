package com.volunteer.platform.activity.service;

import com.volunteer.platform.activity.dao.ActivityDAO;
import com.volunteer.platform.activity.dao.ActivitySignupDAO;
import com.volunteer.platform.activity.dto.CreateActivityDTO;
import com.volunteer.platform.activity.dto.SignupActivityDTO;
import com.volunteer.platform.activity.dto.UpdateActivityDTO;
import com.volunteer.platform.activity.entity.ActivityDO;
import com.volunteer.platform.activity.entity.ActivitySignupDO;
import com.volunteer.platform.activity.query.ActivityQuery;
import com.volunteer.platform.activity.query.ActivitySignupQuery;
import com.volunteer.platform.activity.service.impl.ActivityServiceImpl;
import com.volunteer.platform.activity.vo.ActivitySignupVO;
import com.volunteer.platform.activity.vo.ActivityVO;
import com.volunteer.platform.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ActivityServiceImplTest {

    @Test
    void createsActivityAndReturnsViewObject() {
        ActivityService service = new ActivityServiceImpl(new InMemoryActivityDAO());
        CreateActivityDTO dto = createActivityDTO("Shanghai Music Festival");

        ActivityVO activity = service.create(dto);

        assertThat(activity.getId()).isEqualTo(1L);
        assertThat(activity.getName()).isEqualTo("Shanghai Music Festival");
        assertThat(activity.getLocation()).isEqualTo("Shanghai Stadium");
    }

    @Test
    void pagesActivitiesByNameKeyword() {
        ActivityService service = new ActivityServiceImpl(new InMemoryActivityDAO());
        service.create(createActivityDTO("Shanghai Music Festival"));
        service.create(createActivityDTO("Hangzhou Marathon"));

        ActivityQuery query = new ActivityQuery();
        query.setNameKeyword("Music");

        List<ActivityVO> activities = service.page(query);

        assertThat(activities).extracting(ActivityVO::getName).containsExactly("Shanghai Music Festival");
    }

    @Test
    void updateThrowsBusinessExceptionWhenActivityDoesNotExist() {
        ActivityService service = new ActivityServiceImpl(new InMemoryActivityDAO());
        UpdateActivityDTO dto = new UpdateActivityDTO();
        dto.setId(404L);
        dto.setName("Missing Activity");

        assertThatThrownBy(() -> service.update(dto))
            .isInstanceOf(BusinessException.class)
            .hasMessage("activity not found");
    }

    @Test
    void signsUpActivityAndRejectsDuplicateSignup() {
        InMemoryActivityDAO activityDAO = new InMemoryActivityDAO();
        InMemoryActivitySignupDAO signupDAO = new InMemoryActivitySignupDAO();
        ActivityService service = new ActivityServiceImpl(activityDAO, signupDAO);
        ActivityVO activity = service.create(createActivityDTO("Shanghai Music Festival"));
        SignupActivityDTO dto = new SignupActivityDTO();
        dto.setUserId(100L);
        dto.setPositionId(200L);
        dto.setRemark("available all day");

        ActivitySignupVO signupVO = service.signup(activity.getId(), dto);

        assertThat(signupVO.getId()).isEqualTo(1L);
        assertThat(signupVO.getActivityId()).isEqualTo(activity.getId());
        assertThat(signupVO.getUserId()).isEqualTo(100L);
        assertThat(signupVO.getSignupStatus()).isEqualTo("PENDING");
        assertThatThrownBy(() -> service.signup(activity.getId(), dto))
            .isInstanceOf(BusinessException.class)
            .hasMessage("activity signup already exists");
    }

    @Test
    void listsSignupsByUser() {
        InMemoryActivityDAO activityDAO = new InMemoryActivityDAO();
        InMemoryActivitySignupDAO signupDAO = new InMemoryActivitySignupDAO();
        ActivityService service = new ActivityServiceImpl(activityDAO, signupDAO);
        ActivityVO activity = service.create(createActivityDTO("Hangzhou Marathon"));
        SignupActivityDTO dto = new SignupActivityDTO();
        dto.setUserId(101L);

        service.signup(activity.getId(), dto);

        assertThat(service.listUserSignups(101L)).extracting(ActivitySignupVO::getActivityId)
            .containsExactly(activity.getId());
    }

    @Test
    void approvesAndRejectsOnlyPendingSignup() {
        InMemoryActivityDAO activityDAO = new InMemoryActivityDAO();
        InMemoryActivitySignupDAO signupDAO = new InMemoryActivitySignupDAO();
        ActivityService service = new ActivityServiceImpl(activityDAO, signupDAO);
        ActivityVO activity = service.create(createActivityDTO("Community Care"));
        SignupActivityDTO first = new SignupActivityDTO();
        first.setUserId(102L);
        ActivitySignupVO pending = service.signup(activity.getId(), first);

        ActivitySignupVO approved = service.approveSignup(pending.getId());

        assertThat(approved.getSignupStatus()).isEqualTo("APPROVED");
        assertThatThrownBy(() -> service.rejectSignup(pending.getId()))
            .isInstanceOf(BusinessException.class)
            .hasMessage("activity signup status cannot be changed");
    }

    @Test
    void cancelsOwnPendingSignupAndRejectsOtherUser() {
        InMemoryActivityDAO activityDAO = new InMemoryActivityDAO();
        InMemoryActivitySignupDAO signupDAO = new InMemoryActivitySignupDAO();
        ActivityService service = new ActivityServiceImpl(activityDAO, signupDAO);
        ActivityVO activity = service.create(createActivityDTO("Library Reading"));
        SignupActivityDTO dto = new SignupActivityDTO();
        dto.setUserId(103L);
        ActivitySignupVO pending = service.signup(activity.getId(), dto);

        assertThatThrownBy(() -> service.cancelSignup(pending.getId(), 999L))
            .isInstanceOf(BusinessException.class)
            .hasMessage("activity signup user mismatch");

        ActivitySignupVO cancelled = service.cancelSignup(pending.getId(), 103L);

        assertThat(cancelled.getSignupStatus()).isEqualTo("CANCELLED");
    }

    @Test
    void listsSignupsByStatus() {
        InMemoryActivityDAO activityDAO = new InMemoryActivityDAO();
        InMemoryActivitySignupDAO signupDAO = new InMemoryActivitySignupDAO();
        ActivityService service = new ActivityServiceImpl(activityDAO, signupDAO);
        ActivityVO activity = service.create(createActivityDTO("City Guide"));
        SignupActivityDTO first = new SignupActivityDTO();
        first.setUserId(104L);
        SignupActivityDTO second = new SignupActivityDTO();
        second.setUserId(105L);
        service.approveSignup(service.signup(activity.getId(), first).getId());
        service.signup(activity.getId(), second);
        ActivitySignupQuery query = new ActivitySignupQuery();
        query.setSignupStatus("PENDING");

        assertThat(service.listSignups(query)).extracting(ActivitySignupVO::getUserId)
            .containsExactly(105L);
    }

    private CreateActivityDTO createActivityDTO(String name) {
        CreateActivityDTO dto = new CreateActivityDTO();
        dto.setName(name);
        dto.setActivityType("MUSIC_FESTIVAL");
        dto.setStartTime(LocalDateTime.of(2026, 8, 1, 8, 0));
        dto.setEndTime(LocalDateTime.of(2026, 8, 1, 22, 0));
        dto.setLocation("Shanghai Stadium");
        dto.setOwnerName("Alice");
        dto.setContactPhone("13800000000");
        return dto;
    }

    private static class InMemoryActivityDAO implements ActivityDAO {

        private final AtomicLong idGenerator = new AtomicLong(1L);
        private final List<ActivityDO> activities = new ArrayList<>();

        @Override
        public int insert(ActivityDO activityDO) {
            activityDO.setId(idGenerator.getAndIncrement());
            activities.add(activityDO);
            return 1;
        }

        @Override
        public int updateById(ActivityDO activityDO) {
            ActivityDO existing = selectById(activityDO.getId());
            if (existing == null) {
                return 0;
            }
            existing.setName(activityDO.getName());
            existing.setActivityType(activityDO.getActivityType());
            existing.setStartTime(activityDO.getStartTime());
            existing.setEndTime(activityDO.getEndTime());
            existing.setLocation(activityDO.getLocation());
            existing.setOwnerName(activityDO.getOwnerName());
            existing.setContactPhone(activityDO.getContactPhone());
            return 1;
        }

        @Override
        public ActivityDO selectById(Long id) {
            return activities.stream()
                .filter(activity -> activity.getId().equals(id))
                .findFirst()
                .orElse(null);
        }

        @Override
        public List<ActivityDO> selectPage(ActivityQuery query) {
            return activities.stream()
                .filter(activity -> query.getNameKeyword() == null
                    || activity.getName().contains(query.getNameKeyword()))
                .toList();
        }

        @Override
        public int deleteById(Long id) {
            return activities.removeIf(activity -> activity.getId().equals(id)) ? 1 : 0;
        }
    }

    private static class InMemoryActivitySignupDAO implements ActivitySignupDAO {

        private final AtomicLong idGenerator = new AtomicLong(1L);
        private final List<ActivitySignupDO> signups = new ArrayList<>();

        @Override
        public int insert(ActivitySignupDO signupDO) {
            signupDO.setId(idGenerator.getAndIncrement());
            signups.add(signupDO);
            return 1;
        }

        @Override
        public ActivitySignupDO selectExisting(Long activityId, Long userId, Long positionId) {
            return signups.stream()
                .filter(signup -> signup.getActivityId().equals(activityId))
                .filter(signup -> signup.getUserId().equals(userId))
                .filter(signup -> positionId == null ? signup.getPositionId() == null
                    : positionId.equals(signup.getPositionId()))
                .findFirst()
                .orElse(null);
        }

        @Override
        public ActivitySignupDO selectById(Long id) {
            return signups.stream()
                .filter(signup -> signup.getId().equals(id))
                .findFirst()
                .orElse(null);
        }

        @Override
        public List<ActivitySignupDO> selectPage(ActivitySignupQuery query) {
            return signups.stream()
                .filter(signup -> query.getActivityId() == null || query.getActivityId().equals(signup.getActivityId()))
                .filter(signup -> query.getUserId() == null || query.getUserId().equals(signup.getUserId()))
                .filter(signup -> query.getSignupStatus() == null || query.getSignupStatus().equals(signup.getSignupStatus()))
                .toList();
        }

        @Override
        public List<ActivitySignupDO> selectByUserId(Long userId) {
            return signups.stream()
                .filter(signup -> signup.getUserId().equals(userId))
                .toList();
        }

        @Override
        public int updateStatus(Long id, String signupStatus) {
            ActivitySignupDO existing = selectById(id);
            if (existing == null) {
                return 0;
            }
            existing.setSignupStatus(signupStatus);
            return 1;
        }
    }
}
