package com.gmail.dev.zhilin.analizator.entities;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.validation.constraints.NotEmpty;

@FilterDef(name = "saleDateBetween", parameters = {
        @ParamDef(name = "from", type = LocalDate.class),
        @ParamDef(name = "to", type = LocalDate.class) })
@FilterDef(name = "arrivalDateBetween", parameters = {
        @ParamDef(name = "from", type = LocalDate.class),
        @ParamDef(name = "to", type = LocalDate.class) })
@FilterDef(name = "priceChangeDateBetween", parameters = {
        @ParamDef(name = "from", type = LocalDate.class),
        @ParamDef(name = "to", type = LocalDate.class) })
@FilterDef(name = "itemLogDateBetween", parameters = {
        @ParamDef(name = "from", type = LocalDate.class),
        @ParamDef(name = "to", type = LocalDate.class) })
@FilterDef(name = "saleWarehouse", parameters = @ParamDef(name = "warehouseId", type = Long.class))
@FilterDef(name = "arrivalWarehouse", parameters = @ParamDef(name = "warehouseId", type = Long.class))
@FilterDef(name = "itemLogWarehouse", parameters = @ParamDef(name = "warehouseId", type = Long.class))

@Entity
@Table(name = "item")
public class Item {

    @Id
    @SequenceGenerator(name = "item_id_seq", sequenceName = "item_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "item_id_seq")
    @Column(name = "id")
    private Long id;

    @NotEmpty
    @Column(name = "code")
    private String code;

    @Column(name = "manufacturer")
    private String manufacturer;

    @Column(name = "part_number")
    private String partNumber;

    @Column(name = "cross_reference")
    private String crossReference;

    @NotEmpty
    @Column(name = "name")
    private String name;

    @Column(name = "measure")
    private String measure;

    @Temporal(TemporalType.DATE)
    @Column(name = "created_at")
    private LocalDate createdAt;

    @OneToMany(mappedBy = "item")
    @Filter(name = "saleDateBetween", condition = "sale_date between :from and :to")
    @Filter(name = "saleWarehouse", condition = "warehouse_id=:warehouseId")
    private List<Sale> sales;

    @OneToMany(mappedBy = "item")
    @Filter(name = "arrivalDateBetween", condition = "arrival_date between :from and :to")
    @Filter(name = "arrivalWarehouse", condition = "warehouse_id=:warehouseId")
    private List<Arrival> arrivals;

    @OneToMany(mappedBy = "item")
    @Filter(name = "priceChangeDateBetween", condition = "change_date between :from and :to")
    private List<PriceChange> priceChanges;

    @OneToMany(mappedBy = "item")
    @Filter(name = "itemLogDateBetween", condition = "log_date between :from and :to")
    @Filter(name = "itemLogWarehouse", condition = "warehouse_id=:warehouseId")
    private List<ItemLog> itemLogs;

    public Item() {
        super();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public String getPartNumber() {
        return partNumber;
    }

    public void setPartNumber(String partNumber) {
        this.partNumber = partNumber;
    }

    public String getCrossReference() {
        return crossReference;
    }

    public void setCrossReference(String crossReference) {
        this.crossReference = crossReference;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMeasure() {
        return measure;
    }

    public void setMeasure(String measure) {
        this.measure = measure;
    }

    public LocalDate getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDate createdAt) {
        this.createdAt = createdAt;
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

    public List<PriceChange> getPriceChanges() {
        return priceChanges;
    }

    public void setPriceChanges(List<PriceChange> priceChanges) {
        this.priceChanges = priceChanges;
    }

    public List<ItemLog> getItemLogs() {
        return itemLogs;
    }

    public void setItemLogs(List<ItemLog> itemLogs) {
        this.itemLogs = itemLogs;
    }

    @Override
    public int hashCode() {
        return Objects.hash(code);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Item other = (Item) obj;
        return Objects.equals(code, other.code);
    }

    @Override
    public String toString() {
        return "Item [id=" + id + ", code=" + code + ", name=" + name + "]";
    }

    public Integer costOn(LocalDate date) {
        if (this.itemLogs == null)
            return 0;

        return this.itemLogs.stream().filter(l -> l.getLogDate().equals(date))
                .mapToInt(l -> l.getStock() * l.getPrice()).sum();
    }

    public Integer stockOn(LocalDate date) {
        if (this.itemLogs == null)
            return 0;

        return this.itemLogs.stream().filter(l -> l.getLogDate().equals(date)).mapToInt(l -> l.getStock()).sum();
    }

    public Integer arrivedOn(LocalDate date) {
        return this.arrivals.stream().filter(a -> a.getArrivalDate().equals(date)).mapToInt(Arrival::getQuantity).sum();
    }

    public Integer soldOn(LocalDate date) {
        return this.sales.stream().filter(s -> s.getSaleDate().equals(date)).mapToInt(Sale::getQuantity).sum();
    }

    // the method is inclusive
    public Integer soldBetween(LocalDate from, LocalDate to) {
        if (from.isAfter(to))
            throw new IllegalArgumentException("date 'from' can't be after date 'to'");

        return this.sales.stream()
                .filter(s -> s.getSaleDate().isAfter(from.minusDays(1)) && s.getSaleDate().isBefore(to.plusDays(1)))
                .mapToInt(Sale::getQuantity).sum();
    }

    // the method is inclusive
    public Integer arrivedBetween(LocalDate from, LocalDate to) {
        if (from.isAfter(to))
            throw new IllegalArgumentException("date 'from' can't be after date 'to'");

        return this.arrivals.stream().filter(
                a -> a.getArrivalDate().isAfter(from.minusDays(1)) && a.getArrivalDate().isBefore(to.plusDays(1)))
                .mapToInt(Arrival::getQuantity).sum();
    }

    public Integer amountOfSales(LocalDate date) {
        return this.sales.stream().filter(s -> s.getSaleDate().equals(date))
                .mapToInt(s -> s.getQuantity() * s.getPrice()).sum();
    }

    // the method is inclusive
    public Integer amountOfSales(LocalDate from, LocalDate to) {
        if (from.isAfter(to))
            throw new IllegalArgumentException("date 'from' can't be after date 'to'");

        return this.sales.stream()
                .filter(s -> s.getSaleDate().isAfter(from.minusDays(1)) && s.getSaleDate().isBefore(to.plusDays(1)))
                .mapToInt(s -> s.getQuantity() * s.getPrice()).sum();
    }

    public Integer amountOfArrivals(LocalDate date) {
        return this.arrivals.stream().filter(a -> a.getArrivalDate().equals(date))
                .mapToInt(a -> a.getQuantity() * a.getPrice()).sum();
    }

    // the method is inclusive
    public Integer amountOfArrivals(LocalDate from, LocalDate to) {
        if (from.isAfter(to))
            throw new IllegalArgumentException("date 'from' can't be after date 'to'");

        return this.arrivals.stream().filter(
                a -> a.getArrivalDate().isAfter(from.minusDays(1)) && a.getArrivalDate().isBefore(to.plusDays(1)))
                .mapToInt(a -> a.getQuantity() * a.getPrice()).sum();
    }

    public Integer priceOn(LocalDate date) {
        if (this.itemLogs == null)
            return 0;

        Optional<ItemLog> log = this.itemLogs.stream().filter(l -> l.getLogDate().equals(date)).findFirst();

        if (log.isPresent())
            return log.get().getPrice();

        return 0;
    }

    public PriceChange priceChangeOn(LocalDate date) {
        Optional<PriceChange> change = this.priceChanges.stream().filter(c -> c.getChangeDate().equals(date))
                .findFirst();

        if (change.isPresent())
            return change.get();

        return new PriceChange(date);
    }

    public float priceDifferencePercentage(LocalDate date) {
        if (this.priceChanges == null)
            return 0.0f;

        Optional<PriceChange> change = this.priceChanges.stream().filter(p -> p.getChangeDate().equals(date))
                .findFirst();

        if (change.isPresent())
            return ((change.get().getNewPrice() * 100.0f) / change.get().getOldPrice()) - 100;

        return 0.0f;
    }

    public boolean inStock(LocalDate date) {
        if (this.itemLogs == null)
            return false;

        Optional<ItemLog> log = this.itemLogs.stream().filter(l -> l.getLogDate().equals(date)).findFirst();

        return log.isPresent();
    }

}
