package com.institute.ims.dto;

import java.util.List;

public class VerifyBulkRequest {

    private List<Long> userIds;

    public VerifyBulkRequest() {
    }

    public VerifyBulkRequest(List<Long> userIds) {
        this.userIds = userIds;
    }

    public List<Long> getUserIds() {
        return userIds;
    }

    public void setUserIds(List<Long> userIds) {
        this.userIds = userIds;
    }
}
