package com.example.logisticks.models;

public class OrderStatus {

    public static enum Status{

        PLACED(0),
        DISPATCHED(1),
        ARRIVED(2),
        OUTFORDELIVERY(3),
        DELIVERED(4);

        private final int value;
        private Status(int value){
            this.value = value;
        }
        public int getValue(){
            return value;
        }
    }
    private int orderId;
    private int currentLocationId;
    private Status status;

    public OrderStatus(int orderId, int currentLocationId, Status status) {
        this.orderId = orderId;
        this.currentLocationId = currentLocationId;
        this.status = status;
    }

    public OrderStatus() {
    }

    public int getOrderId() {
        return orderId;
    }

    public int getCurrentLocationId() {
        return currentLocationId;
    }

    public Status getStatus() {
        return status;
    }
    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }
    public void setCurrentLocationId(int currentLocationId) {
        this.currentLocationId = currentLocationId;
    }
    public void setStatus(Status status) {
        this.status = status;
    }
}
