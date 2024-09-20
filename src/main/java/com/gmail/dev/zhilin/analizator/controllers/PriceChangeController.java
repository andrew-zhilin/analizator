package com.gmail.dev.zhilin.analizator.controllers;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
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
import com.gmail.dev.zhilin.analizator.entities.PriceChange;
import com.gmail.dev.zhilin.analizator.rows.PriceChangeRow;
import com.gmail.dev.zhilin.analizator.services.db.ItemService;
import com.gmail.dev.zhilin.analizator.services.db.PriceChangeService;
import com.gmail.dev.zhilin.analizator.services.db.UpdateLogService;

@Controller
@RequestMapping("/price-changes")
public class PriceChangeController {

    private static final Integer PAGE_SIZE = 30;

    private final UpdateLogService updateLogService;
    private final ItemService itemService;
    private final PriceChangeService priceChangeService;

    @Autowired
    public PriceChangeController(UpdateLogService updateLogService, ItemService itemService,
            PriceChangeService priceChangeService) {
        this.itemService = itemService;
        this.updateLogService = updateLogService;
        this.priceChangeService = priceChangeService;
    }

    @RequestMapping(method = RequestMethod.GET)
    public String getPage(@RequestParam("page") Optional<Integer> page, Model model) {
        int currentPage = page.orElse(1);
        Page<PriceChangeRow> priceChangePage = calculatePriceChangeRows(currentPage);
        List<Integer> pageNumbers = IntStream.rangeClosed(1, priceChangePage.getTotalPages()).boxed()
                .collect(Collectors.toList());

        model.addAttribute("priceChangePage", priceChangePage);
        model.addAttribute("pageNumbers", pageNumbers);

        return "price-change/list-of-price-changes";
    }

    @RequestMapping(value = "/price-change", method = RequestMethod.GET)
    public String getSalesPage(@RequestParam("date") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date,
            @RequestParam("page") Optional<Integer> page, Model model) {
        int currentPage = page.orElse(1);
        LocalDate minDate = updateLogService.findFirstDate().get();
        LocalDate maxDate = updateLogService.findLastDate().get().minusDays(1);
        Page<Item> priceChangePage = null;
        
        if (date.isBefore(minDate) || date.isAfter(maxDate))
            throw new IllegalArgumentException();

        priceChangePage = calculateItemPriceChangeRows(currentPage, date);

        model.addAttribute("minDate", minDate);
        model.addAttribute("maxDate", maxDate);
        model.addAttribute("date", date);
        model.addAttribute("priceChangePage", priceChangePage);

        if (priceChangePage.getTotalPages() > 0) {
            List<Integer> pageNumbers = IntStream.rangeClosed(1, priceChangePage.getTotalPages()).boxed()
                    .collect(Collectors.toList());

            model.addAttribute("pageNumbers", pageNumbers);
        }

        return "price-change/price-change-page";
    }

    private Page<Item> calculateItemPriceChangeRows(int page, LocalDate date) {
        int startIndex = (page - 1) * PAGE_SIZE;
        int lastIndex = startIndex + PAGE_SIZE;
        List<Item> items = itemService.selectWithChangedPriceOn(date);

        Collections.sort(items, new Comparator<Item>() {
            @Override
            public int compare(Item o1, Item o2) {

                if (o1.priceDifferencePercentage(date) < o2.priceDifferencePercentage(date))
                    return 1;

                if (o1.priceDifferencePercentage(date) > o2.priceDifferencePercentage(date))
                    return -1;

                return 0;
            }
        });

        if (lastIndex > items.size())
            lastIndex = items.size();

        return new PageImpl<Item>(items.subList(startIndex, lastIndex), PageRequest.of(page - 1, PAGE_SIZE),
                items.size());
    }

    private Page<PriceChangeRow> calculatePriceChangeRows(int page) {
        LocalDate firstDate = updateLogService.findFirstDate().get();
        LocalDate lastDate = updateLogService.findLastDate().get().minusDays(1);
        LocalDate startDate = lastDate.minusDays((page - 1) * PAGE_SIZE);
        LocalDate finishDate = startDate.minusDays(PAGE_SIZE);
        List<PriceChange> changes = priceChangeService.selectAllBetween(finishDate, startDate);
        List<PriceChangeRow> result = new ArrayList<>();
        PriceChangeRow row;

        if (finishDate.isBefore(firstDate))
            finishDate = firstDate;

        for (LocalDate date = startDate; date.isAfter(finishDate); date = date.minusDays(1)) {
            row = new PriceChangeRow();

            for (PriceChange change : changes) {
                if (change.getChangeDate().equals(date)) {
                    if (change.getOldPrice() < change.getNewPrice()) {
                        row.increasePriceUp();
                    } else if (change.getOldPrice() > change.getNewPrice()) {
                        row.increasePriceDown();
                    }
                }
            }

            row.setDate(date);
            result.add(row);
        }

        Collections.sort(result, Comparator.comparing(PriceChangeRow::getDate).reversed());

        return new PageImpl<PriceChangeRow>(result, PageRequest.of(page - 1, PAGE_SIZE),
                ChronoUnit.DAYS.between(firstDate, lastDate) + 1);
    }

}
