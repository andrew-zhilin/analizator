package com.gmail.dev.zhilin.analizator.rows;

import java.time.LocalDate;
import java.util.Objects;

public class ManufacturerRow {

    private LocalDate date;
    private String name;
    private Integer numberOfItems = 0;
    private Integer cost = 0;

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getNumberOfItems() {
        return numberOfItems;
    }

    public void setNumberOfItems(Integer numberOfItems) {
        this.numberOfItems = numberOfItems;
    }

    public Integer getCost() {
        return cost;
    }

    public void setCost(Integer cost) {
        this.cost = cost;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ManufacturerRow other = (ManufacturerRow) obj;
        return Objects.equals(name, other.name);
    }
    
}
