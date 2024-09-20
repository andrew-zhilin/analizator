package com.gmail.dev.zhilin.analizator.services;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.gmail.dev.zhilin.analizator.entities.Item;
import com.gmail.dev.zhilin.analizator.entities.ItemLog;
import com.gmail.dev.zhilin.analizator.entities.UpdateLog;
import com.gmail.dev.zhilin.analizator.entities.Warehouse;
import com.gmail.dev.zhilin.analizator.services.db.ArrivalService;
import com.gmail.dev.zhilin.analizator.services.db.ItemLogService;
import com.gmail.dev.zhilin.analizator.services.db.ItemService;
import com.gmail.dev.zhilin.analizator.services.db.PriceChangeService;
import com.gmail.dev.zhilin.analizator.services.db.SaleService;
import com.gmail.dev.zhilin.analizator.services.db.UpdateLogService;

@Service
public class UpdateService {

    private final UpdateLogService updateLogService;
    private final ItemService itemService;
    private final ItemLogService itemLogService;
    private final SaleService saleService;
    private final ArrivalService arrivalService;
    private final PriceChangeService priceChangeService;

    @Autowired
    public UpdateService(UpdateLogService updateLogService, ItemService itemService, ItemLogService itemLogService,
            SaleService saleService, ArrivalService arrivalService, PriceChangeService priceChangeService) {
        this.updateLogService = updateLogService;
        this.itemService = itemService;
        this.itemLogService = itemLogService;
        this.saleService = saleService;
        this.arrivalService = arrivalService;
        this.priceChangeService = priceChangeService;
    }

    public void update(Workbook workbook, LocalDate date, Warehouse warehouse) {
        Sheet sheet = workbook.getSheetAt(0);
        List<Item> items;
        List<ItemLog> itemLogs;

        if (sheet.getLastRowNum() > 9000) {
            items = parseItems(sheet, date);
            itemLogs = parseItemLogs(sheet, date, warehouse);

            itemService.upsert(items);
            itemLogService.upsert(itemLogs);
            saleService.refreshSales(date.minusDays(1), warehouse);
            arrivalService.refreshArrivals(date.minusDays(1), warehouse);
            priceChangeService.refreshPriceChanges(date.minusDays(1));
            updateLogService.upsert(new UpdateLog(date, (long) items.size(), warehouse));
        } else {
            copyData(date.minusDays(1), date, warehouse);
        }
    }

    public void copyData(LocalDate source, LocalDate target, Warehouse warehouse) {
        itemLogService.copyItemLogs(source, target, warehouse);
        updateLogService.upsert(new UpdateLog(target, 0L, warehouse));
    }

    private List<Item> parseItems(Sheet sheet, LocalDate date) {
        List<Item> items = new ArrayList<>();
        Item item;

        for (Row row : sheet) {
            if (row.getRowNum() != 0) {
                item = new Item();

                row.getCell(0).setCellType(CellType.STRING);
                item.setCode(row.getCell(0).getStringCellValue());

                if (row.getCell(1) != null) {
                    row.getCell(1).setCellType(CellType.STRING);
                    item.setManufacturer(row.getCell(1).getStringCellValue());
                }

                if (row.getCell(2) != null) {
                    row.getCell(2).setCellType(CellType.STRING);
                    item.setPartNumber(row.getCell(2).getStringCellValue());
                }

                if (row.getCell(4) != null) {
                    row.getCell(4).setCellType(CellType.STRING);
                    item.setCrossReference(row.getCell(4).getStringCellValue());
                }

                row.getCell(3).setCellType(CellType.STRING);
                item.setName(row.getCell(3).getStringCellValue());

                if (row.getCell(5) != null) {
                    row.getCell(5).setCellType(CellType.STRING);
                    item.setName(item.getName() + " " + row.getCell(5).getStringCellValue());
                }

                if (row.getCell(6) != null) {
                    row.getCell(6).setCellType(CellType.STRING);
                    item.setName(item.getName() + " " + row.getCell(6).getStringCellValue());
                }

                item.setName(item.getName().trim());

                if (row.getCell(9) != null) {
                    row.getCell(9).setCellType(CellType.STRING);
                    item.setMeasure(row.getCell(9).getStringCellValue());
                }

                item.setCreatedAt(date);

                items.add(item);
            }
        }

        return items;
    }

    private List<ItemLog> parseItemLogs(Sheet sheet, LocalDate date, Warehouse warehouse) {
        Map<String, Item> items = itemService.findAll().stream()
                .collect(Collectors.toMap(Item::getCode, Function.identity()));
        List<ItemLog> itemLogs = new ArrayList<>();
        ItemLog log;
        Item item;

        for (Row row : sheet) {
            if (row.getRowNum() != 0) {
                row.getCell(0).setCellType(CellType.STRING);

                item = items.get(row.getCell(0).getStringCellValue());

                if (item != null) {
                    log = new ItemLog();

                    row.getCell(7).setCellType(CellType.STRING);
                    row.getCell(8).setCellType(CellType.STRING);

                    log.setLogDate(date);
                    log.setStock(Integer.parseInt(row.getCell(8).getStringCellValue()));
                    log.setPrice((int) Double.parseDouble(row.getCell(7).getStringCellValue().replaceAll(",", ".")));
                    log.setItem(item);
                    log.setWarehouse(warehouse);
                    itemLogs.add(log);
                }
            }
        }

        return itemLogs;
    }

}
