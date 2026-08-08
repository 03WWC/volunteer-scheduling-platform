package com.volunteer.platform.activity.query;

public class ActivityQuery {

    private String nameKeyword;
    private Integer pageNo = 1;
    private Integer pageSize = 20;

    public String getNameKeyword() {
        return nameKeyword;
    }

    public void setNameKeyword(String nameKeyword) {
        this.nameKeyword = nameKeyword;
    }

    public Integer getPageNo() {
        return pageNo;
    }

    public void setPageNo(Integer pageNo) {
        this.pageNo = pageNo;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public int getLimit() {
        if (pageSize == null || pageSize < 1) {
            return 20;
        }
        return pageSize;
    }

    public int getOffset() {
        int safePageNo = pageNo == null || pageNo < 1 ? 1 : pageNo;
        return (safePageNo - 1) * getLimit();
    }
}
