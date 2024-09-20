package com.gmail.dev.zhilin.analizator.controllers;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
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
import com.gmail.dev.zhilin.analizator.entities.Warehouse;
import com.gmail.dev.zhilin.analizator.rows.CostRow;
import com.gmail.dev.zhilin.analizator.services.db.ItemLogService;
import com.gmail.dev.zhilin.analizator.services.db.ItemService;
import com.gmail.dev.zhilin.analizator.services.db.UpdateLogService;
import com.gmail.dev.zhilin.analizator.services.db.WarehouseService;

@Controller
@RequestMapping("/cost")
public class CostController {

    private static final Integer PAGE_SIZE = 30;

    private final ItemLogService itemLogService;
    private final UpdateLogService updateLogService;
    private final WarehouseService warehouseService;
    private final ItemService itemService;

    @Autowired
    public CostController(ItemLogService itemLogService, UpdateLogService updateLogService,
            WarehouseService warehouseService, ItemService itemService) {
        this.itemLogService = itemLogService;
        this.updateLogService = updateLogService;
        this.warehouseService = warehouseService;
        this.itemService = itemService;
    }

    @RequestMapping(method = RequestMethod.GET)
    public String getPage(@RequestParam("page") Optional<Integer> page, Model model) {
        int currentPage = page.orElse(1);
        Page<CostRow> costPage = calculateCostRows(currentPage);

        model.addAttribute("costPage", costPage);

        if (costPage.getTotalPages() > 0) {
            List<Integer> pageNumbers = IntStream.rangeClosed(1, costPage.getTotalPages()).boxed()
                    .collect(Collectors.toList());

            model.addAttribute("pageNumbers", pageNumbers);
        }

        return "cost/list-of-cost";
    }

    @RequestMapping(method = RequestMethod.GET, params = "wh")
    public String getPage(@RequestParam("page") Optional<Integer> page, @RequestParam(name = "wh") String wh,
            Model model) {
        Warehouse warehouse = warehouseService.findByName(wh).get();
        int currentPage = page.orElse(1);
        Page<CostRow> costPage = calculateCostRows(currentPage, warehouse);

        model.addAttribute("warehouse", warehouse);
        model.addAttribute("costPage", costPage);

        if (costPage.getTotalPages() > 0) {
            List<Integer> pageNumbers = IntStream.rangeClosed(1, costPage.getTotalPages()).boxed()
                    .collect(Collectors.toList());

            model.addAttribute("pageNumbers", pageNumbers);
        }

        return "cost/list-of-cost";
    }

    @RequestMapping(value = "/cost", method = RequestMethod.GET)
    public String getItemCostPage(@RequestParam("date") @DateTimeFormat(pattern = "yyyy-MM.dd") LocalDate date,
            @RequestParam("page") Optional<Integer> page, @RequestParam("manufacturer") Optional<String> manufacturer,
            Model model) {
        LocalDate minDate = updateLogService.findFirstDate().get();
        LocalDate maxDate = updateLogService.findLastDate().get();
        int currentPage = page.orElse(1);
        Page<Item> itemCostPage = null;

        if (date.isBefore(minDate) || date.isAfter(maxDate))
            throw new IllegalArgumentException();
        
        if (manufacturer.isPresent()) {
            itemCostPage = calculateItemCostRows(currentPage, date, manufacturer.get());
            
            model.addAttribute("manufacturer", manufacturer.get());
        } else {
            itemCostPage = calculateItemCostRows(currentPage, date);
        }
        
        model.addAttribute("minDate", minDate);
        model.addAttribute("maxDate", maxDate);
        model.addAttribute("date", date);
        model.addAttribute("itemCostPage", itemCostPage);

        if (itemCostPage.getTotalPages() > 0) {
            List<Integer> pageNumbers = IntStream.rangeClosed(1, itemCostPage.getTotalPages()).boxed()
                    .collect(Collectors.toList());

            model.addAttribute("pageNumbers", pageNumbers);
        }

        return "cost/cost-page";
    }

    @RequestMapping(value = "/cost", method = RequestMethod.GET, params = "wh")
    public String getItemCostPage(@RequestParam("date") @DateTimeFormat(pattern = "yyyy-MM.dd") LocalDate date,
            @RequestParam("page") Optional<Integer> page, @RequestParam("manufacturer") Optional<String> manufacturer,
            @RequestParam(name = "wh") String wh, Model model) {
        Warehouse warehouse = warehouseService.findByName(wh).get();
        LocalDate minDate = updateLogService.findFirstDate().get();
        LocalDate maxDate = updateLogService.findLastDate().get();
        int currentPage = page.orElse(1);
        Page<Item> itemCostPage = null;
        
        if (date.isBefore(minDate) || date.isAfter(maxDate))
            throw new IllegalArgumentException();
        
        if (manufacturer.isPresent()) {
            itemCostPage = calculateItemCostRows(currentPage, date, manufacturer.get(), warehouse);
            
            model.addAttribute("manufacturer", manufacturer.get());
        } else {
            itemCostPage = calculateItemCostRows(currentPage, date, warehouse);
        }
        
        model.addAttribute("warehouse", warehouse);
        model.addAttribute("minDate", minDate);
        model.addAttribute("maxDate", maxDate);
        model.addAttribute("date", date);
        model.addAttribute("itemCostPage", itemCostPage);
        
        if (itemCostPage.getTotalPages() > 0) {
            List<Integer> pageNumbers = IntStream.rangeClosed(1, itemCostPage.getTotalPages()).boxed()
                    .collect(Collectors.toList());
            
            model.addAttribute("pageNumbers", pageNumbers);
        }
        
        return "cost/cost-page";
    }

    private Page<CostRow> calculateCostRows(int page) {
        LocalDate firstDate = updateLogService.findFirstDate().get();
        LocalDate lastDate = updateLogService.findLastDate().get().minusDays(1);
        LocalDate startDate = lastDate.minusDays((page - 1) * PAGE_SIZE);
        LocalDate finishDate = startDate.minusDays(PAGE_SIZE);
        List<CostRow> result;

        if (finishDate.isBefore(firstDate))
            finishDate = firstDate;

        result = itemLogService.calculateCostRows(finishDate, startDate);

        return new PageImpl<CostRow>(result, PageRequest.of(page - 1, PAGE_SIZE),
                ChronoUnit.DAYS.between(firstDate, lastDate) + 1);
    }

    private Page<CostRow> calculateCostRows(int page, Warehouse warehouse) {
        LocalDate firstDate = updateLogService.findFirstDate().get();
        LocalDate lastDate = updateLogService.findLastDate().get().minusDays(1);
        LocalDate startDate = lastDate.minusDays((page - 1) * PAGE_SIZE);
        LocalDate finishDate = startDate.minusDays(PAGE_SIZE);
        List<CostRow> result;

        if (finishDate.isBefore(firstDate))
            finishDate = firstDate;

        result = itemLogService.calculateCostRows(finishDate, startDate, warehouse);

        return new PageImpl<CostRow>(result, PageRequest.of(page - 1, PAGE_SIZE),
                ChronoUnit.DAYS.between(firstDate, lastDate) + 1);
    }

    private Page<Item> calculateItemCostRows(int page, LocalDate date) {
        int startIndex = (page - 1) * PAGE_SIZE;
        int lastIndex = startIndex + PAGE_SIZE;
        List<Item> items = itemService.selectInStockByDate(date);

        Collections.sort(items, new Comparator<Item>() {
            @Override
            public int compare(Item o1, Item o2) {

                if (o1.costOn(date) < o2.costOn(date))
                    return 1;

                if (o1.costOn(date) > o2.costOn(date))
                    return -1;

                return 0;
            }
        });

        if (lastIndex > items.size())
            lastIndex = items.size();

        return new PageImpl<Item>(items.subList(startIndex, lastIndex), PageRequest.of(page - 1, PAGE_SIZE),
                items.size());
    }

    private Page<Item> calculateItemCostRows(int page, LocalDate date, Warehouse warehouse) {
        int startIndex = (page - 1) * PAGE_SIZE;
        int lastIndex = startIndex + PAGE_SIZE;
        List<Item> items = itemService.selectInStockByDateAndWarehouse(date, warehouse);
        
        Collections.sort(items, new Comparator<Item>() {
            @Override
            public int compare(Item o1, Item o2) {
                
                if (o1.costOn(date) < o2.costOn(date))
                    return 1;
                
                if (o1.costOn(date) > o2.costOn(date))
                    return -1;
                
                return 0;
            }
        });
        
        if (lastIndex > items.size())
            lastIndex = items.size();
        
        return new PageImpl<Item>(items.subList(startIndex, lastIndex), PageRequest.of(page - 1, PAGE_SIZE),
                items.size());
    }

    private Page<Item> calculateItemCostRows(int page, LocalDate date, String manufacturer) {
        int startIndex = (page - 1) * PAGE_SIZE;
        int lastIndex = startIndex + PAGE_SIZE;
        List<Item> items = itemService.selectInStockByDateAndManufacturer(date, manufacturer);

        Collections.sort(items, new Comparator<Item>() {
            @Override
            public int compare(Item o1, Item o2) {

                if (o1.costOn(date) < o2.costOn(date))
                    return 1;

                if (o1.costOn(date) > o2.costOn(date))
                    return -1;

                return 0;
            }
        });

        if (lastIndex > items.size())
            lastIndex = items.size();

        return new PageImpl<Item>(items.subList(startIndex, lastIndex), PageRequest.of(page - 1, PAGE_SIZE),
                items.size());
    }

    private Page<Item> calculateItemCostRows(int page, LocalDate date, String manufacturer, Warehouse warehouse) {
        int startIndex = (page - 1) * PAGE_SIZE;
        int lastIndex = startIndex + PAGE_SIZE;
        List<Item> items = itemService.selectInStockByDateAndManufacturerAndWarehouse(date, manufacturer, warehouse);
        
        Collections.sort(items, new Comparator<Item>() {
            @Override
            public int compare(Item o1, Item o2) {
                
                if (o1.costOn(date) < o2.costOn(date))
                    return 1;
                
                if (o1.costOn(date) > o2.costOn(date))
                    return -1;
                
                return 0;
            }
        });
        
        if (lastIndex > items.size())
            lastIndex = items.size();
        
        return new PageImpl<Item>(items.subList(startIndex, lastIndex), PageRequest.of(page - 1, PAGE_SIZE),
                items.size());
    }

}
