package com.salesdialer.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

/**
 * 跟进记录模型
 */
@Entity(tableName = "follow_ups")
public class FollowUp implements Serializable {
    
    @PrimaryKey(autoGenerate = true)
    private int localId;
    
    @SerializedName("id")
    private int id;
    
    @SerializedName("customer_id")
    private int customerId;
    
    @SerializedName("follower_id")
    private int followerId;
    
    @SerializedName("follow_content")
    private String followContent;
    
    @SerializedName("follow_time")
    private String followTime;
    
    @SerializedName("result")
    private String result;  // pending, contacted, interested, no_interest, closed
    
    @SerializedName("next_follow_time")
    private String nextFollowTime;
    
    @SerializedName("create_date")
    private String createDate;
    
    // 本地字段
    private boolean isSynced = false;
    private long callDuration = 0;  // 通话时长（秒）
    
    // Constructors
    public FollowUp() {
    }
    
    public FollowUp(int customerId, String followContent, String result) {
        this.customerId = customerId;
        this.followContent = followContent;
        this.result = result;
    }
    
    // Getters and Setters
    public int getLocalId() {
        return localId;
    }
    
    public void setLocalId(int localId) {
        this.localId = localId;
    }
    
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public int getCustomerId() {
        return customerId;
    }
    
    public void setCustomerId(int customerId) {
        this.customerId = customerId;
    }
    
    public int getFollowerId() {
        return followerId;
    }
    
    public void setFollowerId(int followerId) {
        this.followerId = followerId;
    }
    
    public String getFollowContent() {
        return followContent;
    }
    
    public void setFollowContent(String followContent) {
        this.followContent = followContent;
    }
    
    public String getFollowTime() {
        return followTime;
    }
    
    public void setFollowTime(String followTime) {
        this.followTime = followTime;
    }
    
    public String getResult() {
        return result;
    }
    
    public void setResult(String result) {
        this.result = result;
    }
    
    public String getNextFollowTime() {
        return nextFollowTime;
    }
    
    public void setNextFollowTime(String nextFollowTime) {
        this.nextFollowTime = nextFollowTime;
    }
    
    public String getCreateDate() {
        return createDate;
    }
    
    public void setCreateDate(String createDate) {
        this.createDate = createDate;
    }
    
    public boolean isSynced() {
        return isSynced;
    }
    
    public void setSynced(boolean synced) {
        isSynced = synced;
    }
    
    public long getCallDuration() {
        return callDuration;
    }
    
    public void setCallDuration(long callDuration) {
        this.callDuration = callDuration;
    }
    
    // Helper methods
    public String getResultText() {
        switch (result) {
            case "pending": return "待跟进";
            case "contacted": return "已联系";
            case "interested": return "有意向";
            case "no_interest": return "无意向";
            case "closed": return "已成交";
            default: return "未知";
        }
    }
    
    public String getCallDurationText() {
        if (callDuration < 60) {
            return callDuration + "秒";
        } else {
            long minutes = callDuration / 60;
            long seconds = callDuration % 60;
            return minutes + "分" + seconds + "秒";
        }
    }
}
