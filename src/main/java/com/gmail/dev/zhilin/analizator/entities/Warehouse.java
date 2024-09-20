package com.gmail.dev.zhilin.analizator.entities;

import java.util.List;
import java.util.Objects;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotEmpty;

@Entity
@Table(name = "warehouse")
public class Warehouse {

    @Id
    @SequenceGenerator(name = "warehouse_id_seq", sequenceName = "warehouse_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "warehouse_id_seq")
    @Column(name = "id")
    private Long id;

    @NotEmpty(message = "name can't be empty")
    @Column(name = "name")
    private String name;

    @OneToMany(mappedBy = "warehouse")
    private List<Sale> sales;

    @OneToMany(mappedBy = "warehouse")
    private List<Arrival> arrivals;
    
    @OneToMany(mappedBy = "warehouse")
    private List<ItemLog> itemLogs;
    
    public Warehouse() {
    }
    
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Sale> getSales() {
        return sales;
    }

    public void setSales(List<Sale> sales) {
        this.sales = sales;
    }

    public List<Arrival> getArrivals() {
        return arrivals;
    }

    public void setArrivals(List<Arrival> arrivals) {
        this.arrivals = arrivals;
    }

    public List<ItemLog> getItemLogs() {
        return itemLogs;
    }

    public void setItemLogs(List<ItemLog> itemLogs) {
        this.itemLogs = itemLogs;
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
        Warehouse other = (Warehouse) obj;
        return Objects.equals(name, other.name);
    }
    
}
