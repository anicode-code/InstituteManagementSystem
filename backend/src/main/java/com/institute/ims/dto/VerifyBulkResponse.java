package com.institute.ims.dto;

import java.util.List;

public class VerifyBulkResponse {

    private int verifiedCount;
    private List<Long> verifiedIds;
    private String message;

    public VerifyBulkResponse() {
    }

    public VerifyBulkResponse(int verifiedCount, List<Long> verifiedIds, String message) {
        this.verifiedCount = verifiedCount;
        this.verifiedIds = verifiedIds;
        this.message = message;
    }

    public int getVerifiedCount() {
        return verifiedCount;
    }

    public List<Long> getVerifiedIds() {
        return verifiedIds;
    }

    public String getMessage() {
        return message;
    }
}
