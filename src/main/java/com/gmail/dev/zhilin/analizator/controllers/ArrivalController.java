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
import com.gmail.dev.zhilin.analizator.entities.Arrival;
import com.gmail.dev.zhilin.analizator.entities.Item;
import com.gmail.dev.zhilin.analizator.entities.Warehouse;
import com.gmail.dev.zhilin.analizator.rows.ArrivalRow;
import com.gmail.dev.zhilin.analizator.services.db.ArrivalService;
import com.gmail.dev.zhilin.analizator.services.db.ItemService;
import com.gmail.dev.zhilin.analizator.services.db.UpdateLogService;
import com.gmail.dev.zhilin.analizator.services.db.WarehouseService;

@Controller
@RequestMapping("/arrivals")
public class ArrivalController {

    private static final Integer PAGE_SIZE = 30;

    private final WarehouseService warehouseService;
    private final UpdateLogService updateLogService;
    private final ArrivalService arrivalService;
    private final ItemService itemService;

    @Autowired
    public ArrivalController(WarehouseService warehouseService, UpdateLogService updateLogService,
            ArrivalService arrivalService, ItemService itemService) {
        this.warehouseService = warehouseService;
        this.updateLogService = updateLogService;
        this.arrivalService = arrivalService;
        this.itemService = itemService;
    }

    @RequestMapping(method = RequestMethod.GET)
    public String getPage(@RequestParam("page") Optional<Integer> page, Model model) {
        int currentPage = page.orElse(1);
        Page<ArrivalRow> arrivalPage = calculateArrivalRows(currentPage);
        List<Integer> pageNumbers = IntStream.rangeClosed(1, arrivalPage.getTotalPages()).boxed()
                .collect(Collectors.toList());

        model.addAttribute("arrivalPage", arrivalPage);
        model.addAttribute("pageNumbers", pageNumbers);

        return "arrival/list-of-arrivals";
    }

    @RequestMapping(method = RequestMethod.GET, params = "wh")
    public String getPage(@RequestParam("page") Optional<Integer> page, @RequestParam(name = "wh") String wh, Model model) {
        Warehouse warehouse = warehouseService.findByName(wh).get();
        int currentPage = page.orElse(1);
        Page<ArrivalRow> arrivalPage = calculateArrivalRows(currentPage, warehouse);
        List<Integer> pageNumbers = IntStream.rangeClosed(1, arrivalPage.getTotalPages()).boxed()
                .collect(Collectors.toList());
        
        model.addAttribute("warehouse", warehouse);
        model.addAttribute("arrivalPage", arrivalPage);
        model.addAttribute("pageNumbers", pageNumbers);
        
        return "arrival/list-of-arrivals";
    }
    
    @RequestMapping(value = "/arrival", method = RequestMethod.GET)
    public String getArrivalPage(@RequestParam("from") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate from,
            @RequestParam("to") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate to,
            @RequestParam("page") Optional<Integer> page, Model model) {
        LocalDate minDate = updateLogService.findFirstDate().get();
        LocalDate maxDate = updateLogService.findLastDate().get().minusDays(1);
        int currentPage = page.orElse(1);
        Page<Item> itemArrivalPage = null;
        
        if (from.isBefore(minDate) || to.isAfter(maxDate) || from.isAfter(to))
            throw new IllegalArgumentException();
        
        itemArrivalPage = calculateItemArrivalRows(currentPage, from, to);
        
        model.addAttribute("minDate", minDate);
        model.addAttribute("maxDate", maxDate);
        model.addAttribute("dateFrom", from);
        model.addAttribute("dateTo", to);
        model.addAttribute("itemArrivalPage", itemArrivalPage);
        
        if (itemArrivalPage.getTotalPages() > 0) {
            List<Integer> pageNumbers = IntStream.rangeClosed(1, itemArrivalPage.getTotalPages()).boxed()
                    .collect(Collectors.toList());
            
            model.addAttribute("pageNumbers", pageNumbers);
        }
        
        return "arrival/arrival-page";
    }
    
    @RequestMapping(value = "/arrival", method = RequestMethod.GET, params = "wh")
    public String getArrivalPage(@RequestParam("from") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate from,
            @RequestParam("to") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate to,
            @RequestParam("page") Optional<Integer> page, @RequestParam(name = "wh") String wh, Model model) {
        Warehouse warehouse = warehouseService.findByName(wh).get();
        LocalDate minDate = updateLogService.findFirstDate().get();
        LocalDate maxDate = updateLogService.findLastDate().get().minusDays(1);
        int currentPage = page.orElse(1);
        Page<Item> itemArrivalPage = null;
        
        if (from.isBefore(minDate) || to.isAfter(maxDate) || from.isAfter(to))
            throw new IllegalArgumentException();
        
        itemArrivalPage = calculateItemArrivalRows(currentPage, from, to, warehouse);
        
        model.addAttribute("warehouse", warehouse);
        model.addAttribute("minDate", minDate);
        model.addAttribute("maxDate", maxDate);
        model.addAttribute("dateFrom", from);
        model.addAttribute("dateTo", to);
        model.addAttribute("itemArrivalPage", itemArrivalPage);
        
        if (itemArrivalPage.getTotalPages() > 0) {
            List<Integer> pageNumbers = IntStream.rangeClosed(1, itemArrivalPage.getTotalPages()).boxed()
                    .collect(Collectors.toList());
            
            model.addAttribute("pageNumbers", pageNumbers);
        }
        
        return "arrival/arrival-page";
    }
    
    private Page<Item> calculateItemArrivalRows(int page, LocalDate from, LocalDate to) {
        int startIndex = (page - 1) * PAGE_SIZE;
        int lastIndex = startIndex + PAGE_SIZE;
        List<Item> items = itemService.selectArrivedBetweenDates(from, to);

        Collections.sort(items, new Comparator<Item>() {
            @Override
            public int compare(Item o1, Item o2) {

                if (o1.amountOfArrivals(from, to) < o2.amountOfArrivals(from, to))
                    return 1;

                if (o1.amountOfArrivals(from, to) > o2.amountOfArrivals(from, to))
                    return -1;

                return 0;
            }
        });

        if (lastIndex > items.size())
            lastIndex = items.size();

        return new PageImpl<Item>(items.subList(startIndex, lastIndex), PageRequest.of(page - 1, PAGE_SIZE),
                items.size());
    }

    private Page<Item> calculateItemArrivalRows(int page, LocalDate from, LocalDate to, Warehouse warehouse) {
        int startIndex = (page - 1) * PAGE_SIZE;
        int lastIndex = startIndex + PAGE_SIZE;
        List<Item> items = itemService.selectArrivedBetweenDates(from, to, warehouse);
        
        Collections.sort(items, new Comparator<Item>() {
            @Override
            public int compare(Item o1, Item o2) {
                
                if (o1.amountOfArrivals(from, to) < o2.amountOfArrivals(from, to))
                    return 1;
                
                if (o1.amountOfArrivals(from, to) > o2.amountOfArrivals(from, to))
                    return -1;
                
                return 0;
            }
        });
        
        if (lastIndex > items.size())
            lastIndex = items.size();
        
        return new PageImpl<Item>(items.subList(startIndex, lastIndex), PageRequest.of(page - 1, PAGE_SIZE),
                items.size());
    }
    
    private Page<ArrivalRow> calculateArrivalRows(int page) {
        LocalDate firstDate = updateLogService.findFirstDate().get();
        LocalDate lastDate = updateLogService.findLastDate().get().minusDays(1);
        LocalDate startDate = lastDate.minusDays((page - 1) * PAGE_SIZE);
        LocalDate finishDate = startDate.minusDays(PAGE_SIZE);
        List<Arrival> arrivals = arrivalService.selectAllBetween(finishDate, startDate);
        List<ArrivalRow> result = new ArrayList<>();
        Set<Item> uniqueItems;
        ArrivalRow row;
        int amount;

        if (finishDate.isBefore(firstDate))
            finishDate = firstDate;

        for (LocalDate date = startDate; date.isAfter(finishDate); date = date.minusDays(1)) {
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

        return new PageImpl<ArrivalRow>(result, PageRequest.of(page - 1, PAGE_SIZE),
                ChronoUnit.DAYS.between(firstDate, lastDate) + 1);
    }

    private Page<ArrivalRow> calculateArrivalRows(int page, Warehouse warehouse) {
        LocalDate firstDate = updateLogService.findFirstDate().get();
        LocalDate lastDate = updateLogService.findLastDate().get().minusDays(1);
        LocalDate startDate = lastDate.minusDays((page - 1) * PAGE_SIZE);
        LocalDate finishDate = startDate.minusDays(PAGE_SIZE);
        List<Arrival> arrivals = arrivalService.selectAllByWarehouseBetween(finishDate, startDate, warehouse);
        List<ArrivalRow> result = new ArrayList<>();
        Set<Item> uniqueItems;
        ArrivalRow row;
        int amount;
        
        if (finishDate.isBefore(firstDate))
            finishDate = firstDate;
        
        for (LocalDate date = startDate; date.isAfter(finishDate); date = date.minusDays(1)) {
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
        
        return new PageImpl<ArrivalRow>(result, PageRequest.of(page - 1, PAGE_SIZE),
                ChronoUnit.DAYS.between(firstDate, lastDate) + 1);
    }

}
