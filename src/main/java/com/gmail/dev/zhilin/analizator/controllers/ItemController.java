package com.gmail.dev.zhilin.analizator.controllers;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import com.gmail.dev.zhilin.analizator.entities.Item;
import com.gmail.dev.zhilin.analizator.entities.Warehouse;
import com.gmail.dev.zhilin.analizator.services.db.ItemService;
import com.gmail.dev.zhilin.analizator.services.db.UpdateLogService;
import com.gmail.dev.zhilin.analizator.services.db.WarehouseService;

@Controller
@RequestMapping("/items")
public class ItemController {

    private static final Integer PAGE_SIZE = 30;
    private static final Integer DAYS_BY_DEFAULT = 90;
    
    private final ItemService itemService;
    private final WarehouseService warehouseService;
    private final UpdateLogService updateLogService;

    @Autowired
    public ItemController(ItemService itemService, WarehouseService warehouseService, UpdateLogService updateLogService) {
        this.itemService = itemService;
        this.warehouseService = warehouseService;
        this.updateLogService = updateLogService;
    }

    @RequestMapping(method = RequestMethod.GET)
    public String getPage(Model model, @RequestParam("page") Optional<Integer> page) {
        int currentPage = page.orElse(1);
        Page<Item> itemPage = itemService
                .findPaginated(PageRequest.of(currentPage - 1, PAGE_SIZE, Sort.by("createdAt").descending()));
        int totalPages = itemPage.getTotalPages();

        model.addAttribute("itemPage", itemPage);

        if (totalPages > 0) {
            List<Integer> pageNumbers = IntStream.rangeClosed(1, totalPages).boxed().collect(Collectors.toList());

            model.addAttribute("pageNumbers", pageNumbers);
        }

        return "item/list-of-items";
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public String getPage(@PathVariable("id") Long itemId,
            @RequestParam("from") @DateTimeFormat(pattern = "yyyy-MM.dd") Optional<LocalDate> dateFrom,
            @RequestParam("to") @DateTimeFormat(pattern = "yyyy-MM.dd") Optional<LocalDate> dateTo,
            Model model) {
        LocalDate maxDate = updateLogService.findLastDate().get();
        LocalDate minDate = itemService.findCreatedAt(itemId).get();
        LocalDate to;
        LocalDate from;
        Item item;
        
        if (dateFrom.isEmpty() || dateTo.isEmpty()) {
            from = maxDate.minusDays(DAYS_BY_DEFAULT);
            to = maxDate;
        } else {
            from = dateFrom.get();
            to = dateTo.get();
        }
        
        if (from.isBefore(minDate))
            from = minDate;
        
        if (from.isBefore(minDate) || to.isAfter(maxDate) || from.isAfter(to))
            throw new IllegalArgumentException();
        
        item = itemService.selectByIdJoinItemLogsAndSalesAndArrivals(itemId, from, to).get();

        model.addAttribute("minDate", minDate);
        model.addAttribute("maxDate", maxDate);
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        model.addAttribute("beginningOfTheYear", maxDate.withDayOfYear(1));
        model.addAttribute("item", item);
        model.addAttribute("amountOfSalesMap", calculateAmountOfSales(item, from, to));
        model.addAttribute("saleMap", calculateSaleValues(item, from, to));
        model.addAttribute("amountOfArrivalsMap", calculateAmountOfArrivals(item, from, to));
        model.addAttribute("arrivalMap", calculateArrivalValues(item, from, to));
        model.addAttribute("priceMap", calculatePriceValues(item, from, to));
        model.addAttribute("stockMap", calculateStockValues(item, from, to));
        
        return "item/item-page";
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.GET, params = "wh")
    public String getPage(@PathVariable("id") Long itemId,
            @RequestParam("from") @DateTimeFormat(pattern = "yyyy-MM.dd") Optional<LocalDate> dateFrom,
            @RequestParam("to") @DateTimeFormat(pattern = "yyyy-MM.dd") Optional<LocalDate> dateTo,
            @RequestParam(name = "wh") String wh, Model model) {
        Warehouse warehouse = warehouseService.findByName(wh).get();
        LocalDate maxDate = updateLogService.findLastDate().get();
        LocalDate minDate = itemService.findCreatedAt(itemId).get();
        LocalDate to;
        LocalDate from;
        Item item;
        
        if (dateFrom.isEmpty() || dateTo.isEmpty()) {
            from = maxDate.minusDays(DAYS_BY_DEFAULT);
            to = maxDate;
        } else {
            from = dateFrom.get();
            to = dateTo.get();
        }
        
        if (from.isBefore(minDate))
            from = minDate;
        
        if (from.isBefore(minDate) || to.isAfter(maxDate) || from.isAfter(to))
            throw new IllegalArgumentException();
        
        item = itemService.selectByIdAndWarehouseJoinItemLogsAndSalesAndArrivals(itemId, warehouse, from, to).get();
        
        model.addAttribute("warehouse", warehouse);
        model.addAttribute("minDate", minDate);
        model.addAttribute("maxDate", maxDate);
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        model.addAttribute("beginningOfTheYear", maxDate.withDayOfYear(1));
        model.addAttribute("item", item);
        model.addAttribute("amountOfSalesMap", calculateAmountOfSales(item, from, to));
        model.addAttribute("saleMap", calculateSaleValues(item, from, to));
        model.addAttribute("amountOfArrivalsMap", calculateAmountOfArrivals(item, from, to));
        model.addAttribute("arrivalMap", calculateArrivalValues(item, from, to));
        model.addAttribute("priceMap", calculatePriceValues(item, from, to));
        model.addAttribute("stockMap", calculateStockValues(item, from, to));
        
        return "item/item-page";
    }

    private Map<String, Integer> calculateAmountOfSales(Item item, LocalDate from, LocalDate to) {
        Map<String, Integer> result = new LinkedHashMap<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        
        for (LocalDate date = from; date.isBefore(to.plusDays(1)); date = date.plusDays(1)) {
            result.put(formatter.format(date), item.amountOfSales(date));
        }
        
        return result;
    }
    
    private Map<String, Integer> calculateSaleValues(Item item, LocalDate from, LocalDate to) {
        Map<String, Integer> result = new LinkedHashMap<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        
        for (LocalDate date = from; date.isBefore(to.plusDays(1)); date = date.plusDays(1)) {
            result.put(formatter.format(date), item.soldOn(date));
        }
        
        return result;
    }

    private Map<String, Integer> calculateAmountOfArrivals(Item item, LocalDate from, LocalDate to) {
        Map<String, Integer> result = new LinkedHashMap<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        
        for (LocalDate date = from; date.isBefore(to.plusDays(1)); date = date.plusDays(1)) {
            result.put(formatter.format(date), item.amountOfArrivals(date));
        }
        
        return result;
    }

    private Map<String, Integer> calculateArrivalValues(Item item, LocalDate from, LocalDate to) {
        Map<String, Integer> result = new LinkedHashMap<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        
        for (LocalDate date = from; date.isBefore(to.plusDays(1)); date = date.plusDays(1)) {
            result.put(formatter.format(date), item.arrivedOn(date));
        }

        return result;
    }
    
    private Map<String, Integer> calculatePriceValues(Item item, LocalDate from, LocalDate to) {
        Map<String, Integer> result = new LinkedHashMap<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        
        for (LocalDate date = from; date.isBefore(to.plusDays(1)); date = date.plusDays(1)) {
            result.put(formatter.format(date), item.priceOn(date));
        }
        
        return result;
    }

    private Map<String, Integer> calculateStockValues(Item item, LocalDate from, LocalDate to) {
        Map<String, Integer> result = new LinkedHashMap<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        
        for (LocalDate date = from; date.isBefore(to.plusDays(1)); date = date.plusDays(1)) {
            result.put(formatter.format(date), item.stockOn(date));
        }
        
        return result;
    }
    
}
