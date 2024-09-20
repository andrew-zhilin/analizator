package com.gmail.dev.zhilin.analizator.rows;

import java.time.LocalDate;

public class ArrivalRow {

    private LocalDate date;
    private Integer numberOfItems = 0;
    private Integer amount = 0;

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public Integer getNumberOfItems() {
        return numberOfItems;
    }

    public void setNumberOfItems(Integer numberOfItems) {
        this.numberOfItems = numberOfItems;
    }

    public Integer getAmount() {
        return amount;
    }

    public void setAmount(Integer amount) {
        this.amount = amount;
    }

    public void increaseAmount(Integer value) {
        this.amount += value;
    }

    public void incrementNumberOfItems() {
        this.numberOfItems++;
    }

}
