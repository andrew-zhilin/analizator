package com.gmail.dev.zhilin.analizator.rows;

import java.time.LocalDate;

public class PriceChangeRow {

    private LocalDate date;
    private Integer priceUp = 0;
    private Integer priceDown = 0;

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public Integer getPriceUp() {
        return priceUp;
    }

    public void setPriceUp(Integer priceUp) {
        this.priceUp = priceUp;
    }

    public Integer getPriceDown() {
        return priceDown;
    }

    public void setPriceDown(Integer priceDown) {
        this.priceDown = priceDown;
    }

    public void increasePriceUp() {
        this.priceUp++;
    }

    public void increasePriceDown() {
        this.priceDown++;
    }

}
