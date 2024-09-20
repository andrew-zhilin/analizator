package com.gmail.dev.zhilin.analizator.schedule;

import java.io.File;
import java.io.FileInputStream;
import java.time.LocalDate;
import java.time.Month;
import java.util.Optional;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.gmail.dev.zhilin.analizator.entities.UpdateLog;
import com.gmail.dev.zhilin.analizator.entities.Warehouse;
import com.gmail.dev.zhilin.analizator.mailbox.MailBox;
import com.gmail.dev.zhilin.analizator.services.UpdateService;
import com.gmail.dev.zhilin.analizator.services.db.UpdateLogService;
import com.gmail.dev.zhilin.analizator.services.db.WarehouseService;

@Component
public class UpdateTask {

    private static final LocalDate START_DATE = LocalDate.of(2022, Month.AUGUST, 1);

    private MailBox mailBox;
    private UpdateLogService updateLogService;
    private WarehouseService warehouseService;
    private UpdateService updateService;

    @Autowired
    public UpdateTask(MailBox mmailBox, UpdateLogService updateLogService, WarehouseService warehouseService,
            UpdateService updateService) {
        this.mailBox = mmailBox;
        this.updateLogService = updateLogService;
        this.warehouseService = warehouseService;
        this.updateService = updateService;
    }

    @Scheduled(cron = "00 59 15 * * *") // 15:59 VDK / 08:59 MSK
    public void updatePriceLists() {
        Optional<UpdateLog> log = updateLogService.findLast();
        LocalDate updateDate;
        LocalDate tomorrow = LocalDate.now().plusDays(1);

        if (log.isEmpty()) {
            updateDate = START_DATE;
        } else {
            updateDate = log.get().getLogDate().plusDays(1);
        }

        for (LocalDate date = updateDate; date.isBefore(tomorrow); date = date.plusDays(1)) {
            updateMskPriceList(date);
            updateVdkPriceList(date);
        }
    }

    public void updateMskPriceList(LocalDate date) {
        Warehouse warehouse = warehouseService.findById(2L).get();
        Optional<File> priceList = mailBox.retrieveMskPriceList(date);

        if (priceList.isPresent()) {
            updateService.update(extractWorkbook(priceList.get()), date, warehouse);
            priceList.get().delete();
        } else {
            updateService.copyData(date.minusDays(1), date, warehouse);
        }
    }

    public void updateVdkPriceList(LocalDate date) {
        Warehouse warehouse = warehouseService.findById(1L).get();
        Optional<File> priceList = mailBox.retrieveVdkPriceList(date);

        if (priceList.isPresent()) {
            updateService.update(extractWorkbook(priceList.get()), date, warehouse);
            priceList.get().delete();
        } else {
            updateService.copyData(date.minusDays(1), date, warehouse);
        }
    }

    private Workbook extractWorkbook(File file) {
        Workbook workbook = null;

        try (FileInputStream input = new FileInputStream(file)) {
            workbook = new XSSFWorkbook(input);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return workbook;
    }

}
