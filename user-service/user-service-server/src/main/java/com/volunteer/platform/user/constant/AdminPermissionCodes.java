package com.volunteer.platform.user.constant;

import java.util.List;

public final class AdminPermissionCodes {

    public static final String DASHBOARD_VIEW = "dashboard:view";
    public static final String ACTIVITY_MANAGE = "activity:manage";
    public static final String SIGNUP_REVIEW = "signup:review";
    public static final String AREA_MANAGE = "area:manage";
    public static final String POSITION_MANAGE = "position:manage";
    public static final String VOLUNTEER_MANAGE = "volunteer:manage";
    public static final String SCHEDULE_MANAGE = "schedule:manage";
    public static final String DISPATCH_MANAGE = "dispatch:manage";
    public static final String CHECKIN_MANAGE = "checkin:manage";
    public static final String SETTLEMENT_MANAGE = "settlement:manage";
    public static final String MESSAGE_MANAGE = "message:manage";
    public static final String SYSTEM_MANAGE = "system:manage";

    private static final List<String> ALL_CODES = List.of(
        DASHBOARD_VIEW,
        ACTIVITY_MANAGE,
        SIGNUP_REVIEW,
        AREA_MANAGE,
        POSITION_MANAGE,
        VOLUNTEER_MANAGE,
        SCHEDULE_MANAGE,
        DISPATCH_MANAGE,
        CHECKIN_MANAGE,
        SETTLEMENT_MANAGE,
        MESSAGE_MANAGE,
        SYSTEM_MANAGE
    );

    private AdminPermissionCodes() {
    }

    public static List<String> allCodes() {
        return ALL_CODES;
    }
}
