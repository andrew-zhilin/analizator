package com.gmail.dev.zhilin.analizator.entities;

import java.time.LocalDate;
import java.util.Objects;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

@Entity
@Table(name = "update_log")
public class UpdateLog {
    
    @Id
    @SequenceGenerator(name = "update_log_id_seq", sequenceName = "update_log_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "update_log_id_seq")
    @Column(name = "id")
    private Long id;
    
    @Column(name = "log_date")
    private LocalDate logDate;
    
    @Column(name = "number_of_items")
    private Long numberOfItems;
    
    @ManyToOne
    @JoinColumn(name = "warehouse_id", referencedColumnName = "id")
    private Warehouse warehouse;

    public UpdateLog() { }
    
    public UpdateLog(LocalDate logDate, Long numberOfItems, Warehouse warehouse) {
        this.logDate = logDate;
        this.numberOfItems = numberOfItems;
        this.warehouse = warehouse;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getLogDate() {
        return logDate;
    }

    public void setLogDate(LocalDate logDate) {
        this.logDate = logDate;
    }

    public Long getNumberOfItems() {
        return numberOfItems;
    }

    public void setNumberOfItems(Long numberOfItems) {
        this.numberOfItems = numberOfItems;
    }

    public Warehouse getWarehouse() {
        return warehouse;
    }

    public void setWarehouse(Warehouse warehouse) {
        this.warehouse = warehouse;
    }

    @Override
    public int hashCode() {
        return Objects.hash(logDate, warehouse);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        UpdateLog other = (UpdateLog) obj;
        return Objects.equals(logDate, other.logDate) && Objects.equals(warehouse, other.warehouse);
    }

    @Override
    public String toString() {
        return "UpdateLog [id=" + id + ", logDate=" + logDate + ", numberOfRows=" + numberOfItems + ", warehouse="
                + warehouse + "]";
    }
    
}
