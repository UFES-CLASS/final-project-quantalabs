package com.quantalabs.jamusync.model;

public class Transaction {
    private int id;
    private String orderType; // 'walk-in' or 'whatsapp'
    private String buyerName;
    private Integer voucherId; // Nullable
    private double subtotal;
    private double discount;
    private double total;
    private String status; // 'Pending', 'Completed', 'Cancelled'
    private int recordedBy;
    private String createdAt;
    private String updatedAt;

    // Join helper field for display
    private String recordedByUsername;

    public Transaction() {}

    public Transaction(int id, String orderType, String buyerName, Integer voucherId,
                       double subtotal, double discount, double total, String status,
                       int recordedBy, String createdAt, String updatedAt) {
        this.id = id;
        this.orderType = orderType;
        this.buyerName = buyerName;
        this.voucherId = voucherId;
        this.subtotal = subtotal;
        this.discount = discount;
        this.total = total;
        this.status = status;
        this.recordedBy = recordedBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getOrderType() {
        return orderType;
    }

    public void setOrderType(String orderType) {
        this.orderType = orderType;
    }

    public String getBuyerName() {
        return buyerName;
    }

    public void setBuyerName(String buyerName) {
        this.buyerName = buyerName;
    }

    public Integer getVoucherId() {
        return voucherId;
    }

    public void setVoucherId(Integer voucherId) {
        this.voucherId = voucherId;
    }

    public double getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(double subtotal) {
        this.subtotal = subtotal;
    }

    public double getDiscount() {
        return discount;
    }

    public void setDiscount(double discount) {
        this.discount = discount;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getRecordedBy() {
        return recordedBy;
    }

    public void setRecordedBy(int recordedBy) {
        this.recordedBy = recordedBy;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getRecordedByUsername() {
        return recordedByUsername;
    }

    public void setRecordedByUsername(String recordedByUsername) {
        this.recordedByUsername = recordedByUsername;
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "id=" + id +
                ", orderType='" + orderType + '\'' +
                ", buyerName='" + buyerName + '\'' +
                ", total=" + total +
                ", status='" + status + '\'' +
                '}';
    }
}
