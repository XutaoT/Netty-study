package com.xutao.netty.httpXml.pojo;

/**
 * Created by Tau Hsu on 2017/5/30.
 */
public class Order {
    private long orderNumber;
    private Customer customer;
    private Address billTo;
    private Shipping shopping;
    private Address shipTo;
    private Float total;

    public long getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(long orderNumber) {
        this.orderNumber = orderNumber;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public Address getBillTo() {
        return billTo;
    }

    public void setBillTo(Address billTo) {
        this.billTo = billTo;
    }

    public Shipping getShopping() {
        return shopping;
    }

    public void setShopping(Shipping shopping) {
        this.shopping = shopping;
    }

    public Address getShipTo() {
        return shipTo;
    }

    public void setShipTo(Address shipTo) {
        this.shipTo = shipTo;
    }

    public Float getTotal() {
        return total;
    }

    public void setTotal(Float total) {
        this.total = total;
    }
}
