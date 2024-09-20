package com.gmail.dev.zhilin.analizator.controllers;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import com.gmail.dev.zhilin.analizator.entities.Item;
import com.gmail.dev.zhilin.analizator.services.db.ItemService;

@Controller
@RequestMapping("/search")
public class SearchController {

    private static final Integer PAGE_SIZE = 30;
    
    private final ItemService itemService;

    @Autowired
    public SearchController(ItemService itemService) {
        this.itemService = itemService;
    }

    @RequestMapping(method = RequestMethod.GET)
    public String getPage(Model model, @RequestParam("page") Optional<Integer> page, @RequestParam("query") String query) {
        int currentPage = page.orElse(1);
        Page<Item> itemPage = itemService
                .findPaginated(PageRequest.of(currentPage - 1, PAGE_SIZE, Sort.by("createdAt").descending()), query);
        int totalPages = itemPage.getTotalPages();

        model.addAttribute("query", query);
        model.addAttribute("itemPage", itemPage);

        if (totalPages > 0) {
            List<Integer> pageNumbers = IntStream.rangeClosed(1, totalPages).boxed().collect(Collectors.toList());

            model.addAttribute("pageNumbers", pageNumbers);
        }

        return "search/search-page";
    }
    
}
