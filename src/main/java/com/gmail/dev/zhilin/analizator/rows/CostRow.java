package com.gmail.dev.zhilin.analizator.rows;

import java.time.LocalDate;

public class CostRow {

    private LocalDate date;
    private Integer numberOfItems = 0;
    private Integer value = 0;

    public CostRow() {
        
    }

    public CostRow(LocalDate date) {
        this.date = date;
    }

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

    public Integer getValue() {
        return value;
    }

    public void setValue(Integer value) {
        this.value = value;
    }

    public void increaseValue(Integer value) {
        this.value += value;
    }
    
    public void incrementNumberOfItems() {
        this.numberOfItems++;
    }
    
}
