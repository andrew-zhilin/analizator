package com.gmail.dev.zhilin.analizator.controllers;

import java.time.LocalDate;
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
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import com.gmail.dev.zhilin.analizator.entities.Warehouse;
import com.gmail.dev.zhilin.analizator.rows.ManufacturerRow;
import com.gmail.dev.zhilin.analizator.services.db.ItemLogService;
import com.gmail.dev.zhilin.analizator.services.db.UpdateLogService;
import com.gmail.dev.zhilin.analizator.services.db.WarehouseService;

@Controller
@RequestMapping("/manufacturers")
public class ManufacturerController {

    private static final Integer PAGE_SIZE = 30;

    private final ItemLogService itemLogService;
    private final UpdateLogService updateLogService;
    private final WarehouseService warehouseService;

    @Autowired
    public ManufacturerController(ItemLogService itemLogService, UpdateLogService updateLogService,
            WarehouseService warehouseService) {
        this.itemLogService = itemLogService;
        this.updateLogService = updateLogService;
        this.warehouseService = warehouseService;
    }

    @RequestMapping(method = RequestMethod.GET)
    public String getPage(@RequestParam("page") Optional<Integer> page, Model model) {
        int currentPage = page.orElse(1);
        Page<ManufacturerRow> manufacturerPage = calculateManufacturerRows(currentPage);

        model.addAttribute("date", updateLogService.findLastDate().get());
        model.addAttribute("manufacturerPage", manufacturerPage);

        if (manufacturerPage.getTotalPages() > 0) {
            List<Integer> pageNumbers = IntStream.rangeClosed(1, manufacturerPage.getTotalPages()).boxed()
                    .collect(Collectors.toList());

            model.addAttribute("pageNumbers", pageNumbers);
        }

        return "manufacturer/list-of-manufacturers";
    }

    @RequestMapping(method = RequestMethod.GET, params = "wh")
    public String getPage(@RequestParam("page") Optional<Integer> page, @RequestParam(name = "wh") String wh,
            Model model) {
        int currentPage = page.orElse(1);
        Warehouse warehouse = warehouseService.findByName(wh).get();
        Page<ManufacturerRow> manufacturerPage = calculateManufacturerRows(currentPage, warehouse);

        model.addAttribute("warehouse", warehouse);
        model.addAttribute("manufacturerPage", manufacturerPage);

        if (manufacturerPage.getTotalPages() > 0) {
            List<Integer> pageNumbers = IntStream.rangeClosed(1, manufacturerPage.getTotalPages()).boxed()
                    .collect(Collectors.toList());

            model.addAttribute("pageNumbers", pageNumbers);
        }

        return "manufacturer/list-of-manufacturers";
    }

    private Page<ManufacturerRow> calculateManufacturerRows(int page) {
        LocalDate date = updateLogService.findLastDate().get();
        List<ManufacturerRow> list = itemLogService.calculateManufacturerCost(date);
        int startIndex = (page - 1) * PAGE_SIZE;
        int lastIndex = startIndex + PAGE_SIZE;

        if (lastIndex > list.size())
            lastIndex = list.size();

        Collections.sort(list, Comparator.comparing(ManufacturerRow::getCost).reversed());

        return new PageImpl<ManufacturerRow>(list.subList(startIndex, lastIndex), PageRequest.of(page - 1, PAGE_SIZE),
                list.size());
    }

    private Page<ManufacturerRow> calculateManufacturerRows(int page, Warehouse warehouse) {
        LocalDate date = updateLogService.findLastDate().get();
        List<ManufacturerRow> list = itemLogService.calculateManufacturerCost(date, warehouse);
        int startIndex = (page - 1) * PAGE_SIZE;
        int lastIndex = startIndex + PAGE_SIZE;
        
        if (lastIndex > list.size())
            lastIndex = list.size();
        
        Collections.sort(list, Comparator.comparing(ManufacturerRow::getCost).reversed());
        
        return new PageImpl<ManufacturerRow>(list.subList(startIndex, lastIndex), PageRequest.of(page - 1, PAGE_SIZE),
                list.size());
    }

}
