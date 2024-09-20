package com.gmail.dev.zhilin.analizator.controllers;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import com.gmail.dev.zhilin.analizator.entities.Item;
import com.gmail.dev.zhilin.analizator.entities.Sale;
import com.gmail.dev.zhilin.analizator.entities.Warehouse;
import com.gmail.dev.zhilin.analizator.rows.SaleRow;
import com.gmail.dev.zhilin.analizator.services.db.ItemService;
import com.gmail.dev.zhilin.analizator.services.db.SaleService;
import com.gmail.dev.zhilin.analizator.services.db.UpdateLogService;
import com.gmail.dev.zhilin.analizator.services.db.WarehouseService;

@Controller
@RequestMapping("/sales")
public class SaleController {

    private static final Integer PAGE_SIZE = 30;

    private final WarehouseService warehouseService;
    private final UpdateLogService updateLogService;
    private final SaleService saleService;
    private final ItemService itemService;

    @Autowired
    public SaleController(WarehouseService warehouseService, UpdateLogService updateLogService,
            SaleService saleService, ItemService itemService) {
        this.warehouseService = warehouseService;
        this.updateLogService = updateLogService;
        this.saleService = saleService;
        this.itemService = itemService;
    }

    @RequestMapping(method = RequestMethod.GET)
    public String getPage(@RequestParam("page") Optional<Integer> page, Model model) {
        int currentPage = page.orElse(1);
        Page<SaleRow> salePage = calculateSaleRows(currentPage);
        List<Integer> pageNumbers = IntStream.rangeClosed(1, salePage.getTotalPages()).boxed()
                .collect(Collectors.toList());

        model.addAttribute("salePage", salePage);
        model.addAttribute("pageNumbers", pageNumbers);

        return "sale/list-of-sales";
    }

    @RequestMapping(method = RequestMethod.GET, params = "wh")
    public String getPage(@RequestParam("page") Optional<Integer> page, @RequestParam(name = "wh") String wh,
            Model model) {
        Warehouse warehouse = warehouseService.findByName(wh).get();
        int currentPage = page.orElse(1);
        Page<SaleRow> salePage = calculateSaleRows(currentPage);
        List<Integer> pageNumbers = IntStream.rangeClosed(1, salePage.getTotalPages()).boxed()
                .collect(Collectors.toList());

        salePage = calculateSaleRows(currentPage, warehouse);

        model.addAttribute("warehouse", warehouse);
        model.addAttribute("salePage", salePage);
        model.addAttribute("pageNumbers", pageNumbers);

        return "sale/list-of-sales";
    }

    @RequestMapping(value = "/sale", method = RequestMethod.GET)
    public String getSalePage(@RequestParam("from") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate from,
            @RequestParam("to") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate to,
            @RequestParam("page") Optional<Integer> page, Model model) {
        LocalDate minDate = updateLogService.findFirstDate().get();
        LocalDate maxDate = updateLogService.findLastDate().get().minusDays(1);
        int currentPage = page.orElse(1);
        Page<Item> itemSalePage = null;
        
        if (from.isBefore(minDate) || to.isAfter(maxDate) || from.isAfter(to))
            throw new IllegalArgumentException();

        itemSalePage = calculateItemSaleRows(currentPage, from, to);

        model.addAttribute("minDate", minDate);
        model.addAttribute("maxDate", maxDate);
        model.addAttribute("dateFrom", from);
        model.addAttribute("dateTo", to);
        model.addAttribute("itemSalePage", itemSalePage);

        if (itemSalePage.getTotalPages() > 0) {
            List<Integer> pageNumbers = IntStream.rangeClosed(1, itemSalePage.getTotalPages()).boxed()
                    .collect(Collectors.toList());

            model.addAttribute("pageNumbers", pageNumbers);
        }

        return "sale/sale-page";
    }

    @RequestMapping(value = "/sale", method = RequestMethod.GET, params = "wh")
    public String getSalePage(@RequestParam("from") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate from,
            @RequestParam("to") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate to,
            @RequestParam("page") Optional<Integer> page, @RequestParam(name = "wh") String wh, Model model) {
        Warehouse warehouse = warehouseService.findByName(wh).get();
        LocalDate minDate = updateLogService.findFirstDate().get();
        LocalDate maxDate = updateLogService.findLastDate().get().minusDays(1);
        int currentPage = page.orElse(1);
        Page<Item> itemSalePage = null;
        
        if (from.isBefore(minDate) || to.isAfter(maxDate) || from.isAfter(to))
            throw new IllegalArgumentException();
        
        itemSalePage = calculateItemSaleRows(currentPage, from, to, warehouse);
        
        model.addAttribute("warehouse", warehouse);
        model.addAttribute("minDate", minDate);
        model.addAttribute("maxDate", maxDate);
        model.addAttribute("dateFrom", from);
        model.addAttribute("dateTo", to);
        model.addAttribute("itemSalePage", itemSalePage);
        
        if (itemSalePage.getTotalPages() > 0) {
            List<Integer> pageNumbers = IntStream.rangeClosed(1, itemSalePage.getTotalPages()).boxed()
                    .collect(Collectors.toList());
            
            model.addAttribute("pageNumbers", pageNumbers);
        }
        
        return "sale/sale-page";
    }
    
    private Page<Item> calculateItemSaleRows(int page, LocalDate from, LocalDate to) {
        int startIndex = (page - 1) * PAGE_SIZE;
        int lastIndex = startIndex + PAGE_SIZE;
        List<Item> items = itemService.selectSoldBetweenDates(from, to);

        Collections.sort(items, new Comparator<Item>() {
            @Override
            public int compare(Item o1, Item o2) {

                if (o1.amountOfSales(from, to) < o2.amountOfSales(from, to))
                    return 1;

                if (o1.amountOfSales(from, to) > o2.amountOfSales(from, to))
                    return -1;

                return 0;
            }
        });

        if (lastIndex > items.size())
            lastIndex = items.size();

        return new PageImpl<Item>(items.subList(startIndex, lastIndex), PageRequest.of(page - 1, PAGE_SIZE),
                items.size());
    }

    private Page<Item> calculateItemSaleRows(int page, LocalDate from, LocalDate to, Warehouse warehouse) {
        int startIndex = (page - 1) * PAGE_SIZE;
        int lastIndex = startIndex + PAGE_SIZE;
        List<Item> items = itemService.selectSoldBetweenDates(from, to, warehouse);

        Collections.sort(items, new Comparator<Item>() {
            @Override
            public int compare(Item o1, Item o2) {

                if (o1.amountOfSales(from, to) < o2.amountOfSales(from, to))
                    return 1;

                if (o1.amountOfSales(from, to) > o2.amountOfSales(from, to))
                    return -1;

                return 0;
            }
        });

        if (lastIndex > items.size())
            lastIndex = items.size();

        return new PageImpl<Item>(items.subList(startIndex, lastIndex), PageRequest.of(page - 1, PAGE_SIZE),
                items.size());
    }

    private Page<SaleRow> calculateSaleRows(int page) {
        LocalDate firstDate = updateLogService.findFirstDate().get();
        LocalDate lastDate = updateLogService.findLastDate().get().minusDays(1);
        LocalDate startDate = lastDate.minusDays((page - 1) * PAGE_SIZE);
        LocalDate finishDate = startDate.minusDays(PAGE_SIZE);
        List<Sale> sales = saleService.selectAllBetween(finishDate, startDate);
        List<SaleRow> result = new ArrayList<>();
        Set<Item> uniqueItems;
        SaleRow row;
        int amount;

        if (finishDate.isBefore(firstDate))
            finishDate = firstDate;

        for (LocalDate date = startDate; date.isAfter(finishDate); date = date.minusDays(1)) {
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

        return new PageImpl<SaleRow>(result, PageRequest.of(page - 1, PAGE_SIZE),
                ChronoUnit.DAYS.between(firstDate, lastDate) + 1);
    }

    private Page<SaleRow> calculateSaleRows(int page, Warehouse warehouse) {
        LocalDate firstDate = updateLogService.findFirstDate().get();
        LocalDate lastDate = updateLogService.findLastDate().get().minusDays(1);
        LocalDate startDate = lastDate.minusDays((page - 1) * PAGE_SIZE);
        LocalDate finishDate = startDate.minusDays(PAGE_SIZE);
        List<Sale> sales = saleService.selectAllByWarehouseBetween(finishDate, startDate, warehouse);
        List<SaleRow> result = new ArrayList<>();
        Set<Item> uniqueItems;
        SaleRow row;
        int amount;

        if (finishDate.isBefore(firstDate))
            finishDate = firstDate;

        for (LocalDate date = startDate; date.isAfter(finishDate); date = date.minusDays(1)) {
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

        return new PageImpl<SaleRow>(result, PageRequest.of(page - 1, PAGE_SIZE),
                ChronoUnit.DAYS.between(firstDate, lastDate) + 1);
    }

}
