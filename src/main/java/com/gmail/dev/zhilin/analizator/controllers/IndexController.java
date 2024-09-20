package com.gmail.dev.zhilin.analizator.controllers;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import com.gmail.dev.zhilin.analizator.entities.Arrival;
import com.gmail.dev.zhilin.analizator.entities.Item;
import com.gmail.dev.zhilin.analizator.entities.PriceChange;
import com.gmail.dev.zhilin.analizator.entities.Sale;
import com.gmail.dev.zhilin.analizator.entities.Warehouse;
import com.gmail.dev.zhilin.analizator.rows.ArrivalRow;
import com.gmail.dev.zhilin.analizator.rows.CostRow;
import com.gmail.dev.zhilin.analizator.rows.ManufacturerRow;
import com.gmail.dev.zhilin.analizator.rows.PriceChangeRow;
import com.gmail.dev.zhilin.analizator.rows.SaleRow;
import com.gmail.dev.zhilin.analizator.services.db.ArrivalService;
import com.gmail.dev.zhilin.analizator.services.db.ItemLogService;
import com.gmail.dev.zhilin.analizator.services.db.ItemService;
import com.gmail.dev.zhilin.analizator.services.db.PriceChangeService;
import com.gmail.dev.zhilin.analizator.services.db.SaleService;
import com.gmail.dev.zhilin.analizator.services.db.UpdateLogService;
import com.gmail.dev.zhilin.analizator.services.db.WarehouseService;

@Controller
@RequestMapping("/")
public class IndexController {

    private static final Integer TABLE_SIZE = 8;

    private final ItemService itemService;
    private final WarehouseService warehouseService;
    private final UpdateLogService updateLogService;
    private final SaleService saleService;
    private final ArrivalService arrivalService;
    private final PriceChangeService priceChangeService;
    private final ItemLogService itemLogService;

    @Autowired
    public IndexController(ItemService itemService, WarehouseService warehouseService,
            UpdateLogService updateLogService, SaleService saleService, ArrivalService arrivalService,
            PriceChangeService priceChangeService, ItemLogService itemLogService) {
        this.itemService = itemService;
        this.warehouseService = warehouseService;
        this.updateLogService = updateLogService;
        this.saleService = saleService;
        this.arrivalService = arrivalService;
        this.priceChangeService = priceChangeService;
        this.itemLogService = itemLogService;
    }

    @RequestMapping(method = RequestMethod.GET)
    public String getPage(Model model) {
        Optional<LocalDate> startDate = updateLogService.findFirstDate();
        Optional<LocalDate> finishDate = updateLogService.findLastDate();
        List<SaleRow> sales = null;
        List<ArrivalRow> arrivals = null;
        List<PriceChangeRow> priceChanges = null;
        List<CostRow> cost = null;
        List<ManufacturerRow> manufacturers = null;

        if (finishDate.isPresent()) {
            if (startDate.get().isBefore(finishDate.get().minusDays(TABLE_SIZE))) {
                startDate = Optional.of(finishDate.get().minusDays(TABLE_SIZE));
            }

            if (!finishDate.get().equals(startDate.get())) {
                sales = calculateSaleRows(startDate.get(), finishDate.get().minusDays(1));
                arrivals = calculateArrivalRows(startDate.get(), finishDate.get().minusDays(1));
                priceChanges = calculatePriceChangeRows(startDate.get(), finishDate.get().minusDays(1));
            }

            cost = calculateCost(startDate.get().plusDays(1), finishDate.get());
            manufacturers = calculateManufacturers(finishDate.get());
        }
        
        model.addAttribute("sales", sales);
        model.addAttribute("arrivals", arrivals);
        model.addAttribute("priceChanges", priceChanges);
        model.addAttribute("cost", cost);
        model.addAttribute("manufacturers", manufacturers);
        model.addAttribute("items", itemService.findByOrderByCreatedAtDesc(TABLE_SIZE));

        return "index";
    }

    @RequestMapping(method = RequestMethod.GET, params = "wh")
    public String getPage(@RequestParam(name = "wh") String wh, Model model) {
        Warehouse warehouse = warehouseService.findByName(wh).get();
        Optional<LocalDate> startDate = updateLogService.findFirstDate();
        Optional<LocalDate> finishDate = updateLogService.findLastDate();
        List<SaleRow> sales = null;
        List<ArrivalRow> arrivals = null;
        List<PriceChangeRow> priceChanges = null;
        List<CostRow> cost = null;
        List<ManufacturerRow> manufacturers = null;

        if (finishDate.isPresent()) {
            if (startDate.get().isBefore(finishDate.get().minusDays(TABLE_SIZE))) {
                startDate = Optional.of(finishDate.get().minusDays(TABLE_SIZE));
            }

            if (!finishDate.get().equals(startDate.get())) {
                sales = calculateSaleRows(startDate.get(), finishDate.get().minusDays(1), warehouse);
                arrivals = calculateArrivalRows(startDate.get(), finishDate.get().minusDays(1), warehouse);
                priceChanges = calculatePriceChangeRows(startDate.get(), finishDate.get().minusDays(1));
            }

            cost = calculateCost(startDate.get().plusDays(1), finishDate.get(), warehouse);
            manufacturers = calculateManufacturers(finishDate.get(), warehouse);
        }

        model.addAttribute("warehouse", warehouse);
        model.addAttribute("sales", sales);
        model.addAttribute("arrivals", arrivals);
        model.addAttribute("priceChanges", priceChanges);
        model.addAttribute("cost", cost);
        model.addAttribute("manufacturers", manufacturers);
        model.addAttribute("items", itemService.findByOrderByCreatedAtDesc(TABLE_SIZE));

        return "index";
    }

    private List<SaleRow> calculateSaleRows(LocalDate from, LocalDate to) {
        List<Sale> sales = saleService.selectAllBetween(from, to);
        List<SaleRow> result = new ArrayList<>();
        Set<Item> uniqueItems;
        SaleRow row;
        int amount;

        for (LocalDate date = from; date.isBefore(to.plusDays(1)); date = date.plusDays(1)) {
            uniqueItems = new HashSet<>();
            row = new SaleRow();
            amount = 0;

            for (Sale sale : sales) {
                if (sale.getSaleDate().equals(date)) {
                    uniqueItems.add(sale.getItem());

                    amount += (sale.getPrice() * sale.getQuantity());
                }
            }

            row.setNumberOfItems(uniqueItems.size());
            row.setAmount(amount);
            row.setDate(date);
            result.add(row);
        }

        Collections.sort(result, Comparator.comparing(SaleRow::getDate).reversed());

        return result;
    }

    private List<SaleRow> calculateSaleRows(LocalDate from, LocalDate to, Warehouse warehouse) {
        List<Sale> sales = saleService.selectAllByWarehouseBetween(from, to, warehouse);
        List<SaleRow> result = new ArrayList<>();
        Set<Item> uniqueItems;
        SaleRow row;
        int amount;

        for (LocalDate date = from; date.isBefore(to.plusDays(1)); date = date.plusDays(1)) {
            uniqueItems = new HashSet<>();
            row = new SaleRow();
            amount = 0;

            for (Sale sale : sales) {
                if (sale.getSaleDate().equals(date)) {
                    uniqueItems.add(sale.getItem());

                    amount += (sale.getPrice() * sale.getQuantity());
                }
            }

            row.setNumberOfItems(uniqueItems.size());
            row.setAmount(amount);
            row.setDate(date);
            result.add(row);
        }

        Collections.sort(result, Comparator.comparing(SaleRow::getDate).reversed());

        return result;
    }

    private List<ArrivalRow> calculateArrivalRows(LocalDate from, LocalDate to) {
        List<Arrival> arrivals = arrivalService.selectAllBetween(from, to);
        List<ArrivalRow> result = new ArrayList<>();
        Set<Item> uniqueItems;
        ArrivalRow row;
        int amount;

        for (LocalDate date = from; date.isBefore(to.plusDays(1)); date = date.plusDays(1)) {
            uniqueItems = new HashSet<>();
            row = new ArrivalRow();
            amount = 0;

            for (Arrival arrival : arrivals) {
                if (arrival.getArrivalDate().equals(date)) {
                    uniqueItems.add(arrival.getItem());

                    amount += (arrival.getPrice() * arrival.getQuantity());
                }
            }

            row.setNumberOfItems(uniqueItems.size());
            row.setAmount(amount);
            row.setDate(date);
            result.add(row);
        }

        Collections.sort(result, Comparator.comparing(ArrivalRow::getDate).reversed());

        return result;
    }

    private List<ArrivalRow> calculateArrivalRows(LocalDate from, LocalDate to, Warehouse warehouse) {
        List<Arrival> arrivals = arrivalService.selectAllByWarehouseBetween(from, to, warehouse);
        List<ArrivalRow> result = new ArrayList<>();
        Set<Item> uniqueItems;
        ArrivalRow row;
        int amount;

        for (LocalDate date = from; date.isBefore(to.plusDays(1)); date = date.plusDays(1)) {
            uniqueItems = new HashSet<>();
            row = new ArrivalRow();
            amount = 0;

            for (Arrival arrival : arrivals) {
                if (arrival.getArrivalDate().equals(date)) {
                    uniqueItems.add(arrival.getItem());

                    amount += (arrival.getPrice() * arrival.getQuantity());
                }
            }

            row.setNumberOfItems(uniqueItems.size());
            row.setAmount(amount);
            row.setDate(date);
            result.add(row);
        }

        Collections.sort(result, Comparator.comparing(ArrivalRow::getDate).reversed());

        return result;
    }

    private List<PriceChangeRow> calculatePriceChangeRows(LocalDate from, LocalDate to) {
        List<PriceChange> changes = priceChangeService.selectAllBetween(from, to);
        List<PriceChangeRow> result = new ArrayList<>();
        PriceChangeRow row;
        int oldPrice;
        int newPrice;

        for (LocalDate date = from; date.isBefore(to); date = date.plusDays(1)) {
            row = new PriceChangeRow();

            row.setDate(date);

            for (PriceChange change : changes) {
                if (change.getChangeDate().equals(date)) {
                    oldPrice = change.getOldPrice();
                    newPrice = change.getNewPrice();

                    if (oldPrice > newPrice) {
                        row.increasePriceDown();
                    } else if (oldPrice < newPrice) {
                        row.increasePriceUp();
                    }
                }
            }

            result.add(row);
        }

        Collections.sort(result, Comparator.comparing(PriceChangeRow::getDate).reversed());

        return result;
    }

    private List<CostRow> calculateCost(LocalDate from, LocalDate to) {
        return itemLogService.calculateCostRows(from, to);
    }

    private List<CostRow> calculateCost(LocalDate from, LocalDate to, Warehouse warehouse) {
        return itemLogService.calculateCostRows(from, to, warehouse);
    }

    private List<ManufacturerRow> calculateManufacturers(LocalDate date) {
        return itemLogService.calculateManufacturerCost(date).subList(0, 8);
    }

    private List<ManufacturerRow> calculateManufacturers(LocalDate date, Warehouse warehouse) {
        return itemLogService.calculateManufacturerCost(date, warehouse).subList(0, 8);
    }

}
