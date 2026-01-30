package com.salesdialer.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

/**
 * 客户模型
 */
@Entity(tableName = "customers")
public class Customer implements Serializable {
    
    @PrimaryKey
    @SerializedName("id")
    private int id;
    
    @SerializedName("name")
    private String name;
    
    @SerializedName("phone")
    private String phone;
    
    @SerializedName("wechat_name")
    private String wechatName;
    
    @SerializedName("customer_type")
    private String customerType;  // public, private
    
    @SerializedName("level")
    private String level;  // a, b, c, d
    
    @SerializedName("status")
    private String status;  // valid, pending, purchased, invalid
    
    @SerializedName("requirement")
    private String requirement;
    
    @SerializedName("remark")
    private String remark;
    
    @SerializedName("intended_property")
    private String intendedProperty;
    
    @SerializedName("owner_id")
    private int ownerId;
    
    @SerializedName("follow_count")
    private int followCount;
    
    @SerializedName("write_date")
    private String writeDate;
    
    @SerializedName("create_date")
    private String createDate;
    
    // 本地字段
    private boolean isSynced = true;
    private long lastCallTime = 0;
    private int callCount = 0;
    private boolean isDialing = false;
    
    // Constructors
    public Customer() {
    }
    
    public Customer(int id, String name, String phone) {
        this.id = id;
        this.name = name;
        this.phone = phone;
    }
    
    // Getters and Setters
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getPhone() {
        return phone;
    }
    
    public void setPhone(String phone) {
        this.phone = phone;
    }
    
    public String getWechatName() {
        return wechatName;
    }
    
    public void setWechatName(String wechatName) {
        this.wechatName = wechatName;
    }
    
    public String getCustomerType() {
        return customerType;
    }
    
    public void setCustomerType(String customerType) {
        this.customerType = customerType;
    }
    
    public String getLevel() {
        return level;
    }
    
    public void setLevel(String level) {
        this.level = level;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getRequirement() {
        return requirement;
    }
    
    public void setRequirement(String requirement) {
        this.requirement = requirement;
    }
    
    public String getRemark() {
        return remark;
    }
    
    public void setRemark(String remark) {
        this.remark = remark;
    }
    
    public String getIntendedProperty() {
        return intendedProperty;
    }
    
    public void setIntendedProperty(String intendedProperty) {
        this.intendedProperty = intendedProperty;
    }
    
    public int getOwnerId() {
        return ownerId;
    }
    
    public void setOwnerId(int ownerId) {
        this.ownerId = ownerId;
    }
    
    public int getFollowCount() {
        return followCount;
    }
    
    public void setFollowCount(int followCount) {
        this.followCount = followCount;
    }
    
    public String getWriteDate() {
        return writeDate;
    }
    
    public void setWriteDate(String writeDate) {
        this.writeDate = writeDate;
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
    
    public long getLastCallTime() {
        return lastCallTime;
    }
    
    public void setLastCallTime(long lastCallTime) {
        this.lastCallTime = lastCallTime;
    }
    
    public int getCallCount() {
        return callCount;
    }
    
    public void setCallCount(int callCount) {
        this.callCount = callCount;
    }
    
    public boolean isDialing() {
        return isDialing;
    }
    
    public void setDialing(boolean dialing) {
        isDialing = dialing;
    }
    
    // Helper methods
    public String getLevelText() {
        switch (level) {
            case "a": return "A级（重要）";
            case "b": return "B级（一般）";
            case "c": return "C级（待开发）";
            case "d": return "D级（长期维护）";
            default: return "未知";
        }
    }
    
    public String getStatusText() {
        switch (status) {
            case "valid": return "有效";
            case "pending": return "暂缓";
            case "purchased": return "已购";
            case "invalid": return "无效";
            default: return "未知";
        }
    }
    
    public String getCustomerTypeText() {
        return "private".equals(customerType) ? "私客" : "公客";
    }
    
    public boolean isValidPhone() {
        return phone != null && !phone.isEmpty() && phone.length() >= 11;
    }
    
    public int getPriority() {
        // 根据客户等级返回优先级（数字越小优先级越高）
        switch (level) {
            case "a": return 1;
            case "b": return 2;
            case "c": return 3;
            case "d": return 4;
            default: return 999;
        }
    }
    
    @Override
    public String toString() {
        return "Customer{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", phone='" + phone + '\'' +
                ", level='" + level + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}
