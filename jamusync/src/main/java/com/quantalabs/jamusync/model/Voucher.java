package com.quantalabs.jamusync.model;

// Voucher "extends BaseModel", inheriting id and createdAt from the parent.
public class Voucher extends BaseModel {
    private String code;
    private String discountType; // 'fixed' or 'percentage'
    private double discountValue;
    private int usageLimit;
    private int usageCount;
    private String expiryDate;
    private boolean isActive;

    public Voucher() {}

    // getId()/setId() and getCreatedAt()/setCreatedAt() are inherited from
    // BaseModel, so they are not repeated here.

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDiscountType() {
        return discountType;
    }

    public void setDiscountType(String discountType) {
        this.discountType = discountType;
    }

    public double getDiscountValue() {
        return discountValue;
    }

    public void setDiscountValue(double discountValue) {
        this.discountValue = discountValue;
    }

    public int getUsageLimit() {
        return usageLimit;
    }

    public void setUsageLimit(int usageLimit) {
        this.usageLimit = usageLimit;
    }

    public int getUsageCount() {
        return usageCount;
    }

    public void setUsageCount(int usageCount) {
        this.usageCount = usageCount;
    }

    public String getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(String expiryDate) {
        this.expiryDate = expiryDate;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    // Required by BaseModel: a short description of this voucher.
    @Override
    public String getSummary() {
        return "Voucher " + code + " (" + discountType + " " + discountValue + ")";
    }

    public double calculateDiscount(double subtotal) {
        if ("percentage".equalsIgnoreCase(discountType)) {
            return subtotal * (discountValue / 100.0);
        }
        return Math.min(discountValue, subtotal);
    }
}
