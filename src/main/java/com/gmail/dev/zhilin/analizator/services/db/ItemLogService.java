package com.gmail.dev.zhilin.analizator.services.db;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.gmail.dev.zhilin.analizator.entities.ItemLog;
import com.gmail.dev.zhilin.analizator.entities.Warehouse;
import com.gmail.dev.zhilin.analizator.repositories.ItemLogRepository;
import com.gmail.dev.zhilin.analizator.rows.CostRow;
import com.gmail.dev.zhilin.analizator.rows.ManufacturerRow;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.persistence.Tuple;

@Service
@Transactional(readOnly = true)
public class ItemLogService {

    @PersistenceContext
    private EntityManager entityManager;
    private final ItemLogRepository itemLogRepository;

    @Autowired
    public ItemLogService(ItemLogRepository itemLogRepository) {
        this.itemLogRepository = itemLogRepository;
    }

    @Transactional
    public void upsert(List<ItemLog> logs) {
        Set<LocalDate> dates = logs.stream().map(ItemLog::getLogDate).collect(Collectors.toSet());
        List<ItemLog> db = dates.stream().map(d -> itemLogRepository.findAllByLogDate(d)).flatMap(List::stream)
                .collect(Collectors.toList());
        ItemLog logInDb;

        for (ItemLog log : logs) {
            if (db.contains(log)) {
                logInDb = db.get(db.indexOf(log));

                logInDb.setPrice(log.getPrice());
                logInDb.setStock(log.getStock());
            } else {
                itemLogRepository.save(log);
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Transactional
    public void copyItemLogs(LocalDate source, LocalDate target, Warehouse warehouse) {
        Query sourceLogsQuery = entityManager.createQuery("from ItemLog where warehouse = :warehouse and logDate = :date");
        Query existingLogsQuery = entityManager.createQuery("from ItemLog where warehouse = :warehouse and logDate = :date");
        List<ItemLog> sourceLogs;
        List<ItemLog> existingLogs;
        List<ItemLog> logs = new ArrayList<>();
        ItemLog log;
        
        sourceLogsQuery.setParameter("warehouse", warehouse);
        sourceLogsQuery.setParameter("date", source);
        existingLogsQuery.setParameter("warehouse", warehouse);
        existingLogsQuery.setParameter("date", target);
        
        sourceLogs = sourceLogsQuery.getResultList();
        existingLogs = existingLogsQuery.getResultList();
        
        for (ItemLog entity : sourceLogs) {
            log = new ItemLog();
            
            log.setItem(entity.getItem());
            log.setLogDate(target);
            log.setPrice(entity.getPrice());
            log.setStock(entity.getStock());
            log.setWarehouse(warehouse);
            
            if (existingLogs.contains(log)) {
                log = existingLogs.get(existingLogs.indexOf(log));
                
                log.setPrice(entity.getPrice());
                log.setStock(entity.getStock());
                existingLogs.remove(log);
            } else {
                log.setPrice(entity.getPrice());
                log.setStock(entity.getStock());
                logs.add(log);
            }
        }
        
        itemLogRepository.saveAll(logs);
        itemLogRepository.deleteAll(existingLogs);
    }
    
    @SuppressWarnings("unchecked")
    public List<CostRow> calculateCostRows(LocalDate from, LocalDate to) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        Query query = entityManager.createQuery(
                "SELECT t1.date as date, t2.items as items, t1.total as total"
                        + " from (select logDate as date, sum(stock * price) as total from ItemLog"
                        + " where logDate between :from AND :to GROUP BY logDate) as t1"
                        + " join (select logDate as date, count(distinct item) as items from ItemLog"
                        + " where logDate between :from AND :to group by logDate) as t2"
                        + " on t1.date = t2.date order by t1.date desc",
                        Tuple.class);
        
        query.setParameter("from", from);
        query.setParameter("to", to);
        
        Map<LocalDate, CostRow> map = new HashMap<>();
        List<Tuple> tuples = query.getResultList();
        CostRow row;
        List<CostRow> result;
        
        for (LocalDate date = from; date.isBefore(to.plusDays(1)); date = date.plusDays(1)) {
            map.put(date, new CostRow(date));
        }
        
        for (Tuple tuple : tuples) {
            row = map.get((LocalDate) LocalDate.parse(tuple.get("date").toString(), formatter));

            row.setValue(((Number) tuple.get("total")).intValue());
            row.setNumberOfItems(((Number) tuple.get("items")).intValue());
        }

        result = new ArrayList<>(map.values());
        
        Collections.sort(result, Comparator.comparing(CostRow::getDate).reversed());
        
        return result;
    }

    @SuppressWarnings("unchecked")
    public List<CostRow> calculateCostRows(LocalDate from, LocalDate to, Warehouse warehouse) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        Query query = entityManager.createQuery(
                "SELECT t1.date as date, t2.items as items, t1.total as total"
                        + " from (select logDate as date, sum(stock * price) as total from ItemLog"
                        + " where warehouse=:warehouse and logDate between :from AND :to GROUP BY logDate) as t1"
                        + " join (select logDate as date, count(distinct item) as items from ItemLog"
                        + " where warehouse=:warehouse and logDate between :from AND :to group by logDate) as t2"
                        + " on t1.date = t2.date order by t1.date desc",
                        Tuple.class);
        
        query.setParameter("from", from);
        query.setParameter("to", to);
        query.setParameter("warehouse", warehouse);
        
        Map<LocalDate, CostRow> map = new HashMap<>();
        List<Tuple> tuples = query.getResultList();
        CostRow row;
        List<CostRow> result;
        
        for (LocalDate date = from; date.isBefore(to.plusDays(1)); date = date.plusDays(1)) {
            map.put(date, new CostRow(date));
        }
        
        for (Tuple tuple : tuples) {
            row = map.get((LocalDate) LocalDate.parse(tuple.get("date").toString(), formatter));

            row.setValue(((Number) tuple.get("total")).intValue());
            row.setNumberOfItems(((Number) tuple.get("items")).intValue());
        }

        result = new ArrayList<>(map.values());
        
        Collections.sort(result, Comparator.comparing(CostRow::getDate).reversed());
        
        return result;
    }

    @SuppressWarnings("unchecked")
    public List<ManufacturerRow> calculateManufacturerCost(LocalDate date) {
        Query query = entityManager.createQuery(
                "select t1.manufacturer as manufacturer, count(distinct t1.code) as items, sum(t2.total) as total"
                + " from (select id as id, code as code, manufacturer as manufacturer from Item where manufacturer is not null) as t1"
                + " inner join (select item as item, sum(price * stock) as total from ItemLog where logDate=:date group by item) as t2"
                + " on t1.id = t2.item.id group by manufacturer order by total desc", Tuple.class);
        
        query.setParameter("date", date);
        
        List<Tuple> re = query.getResultList();
        List<ManufacturerRow> result = new ArrayList<>();
        ManufacturerRow row;
        
        for (Tuple tuple : re) {
            row = new ManufacturerRow();
            
            row.setDate(date);
            row.setName(tuple.get("manufacturer").toString());
            row.setNumberOfItems(((Number) tuple.get("items")).intValue());
            row.setCost(((Number) tuple.get("total")).intValue());
            result.add(row);
        }
        
        return result;
    }

    @SuppressWarnings("unchecked")
    public List<ManufacturerRow> calculateManufacturerCost(LocalDate date, Warehouse warehouse) {
        Query query = entityManager.createQuery(
                "select t1.manufacturer as manufacturer, count(distinct t1.code) as items, sum(t2.total) as total"
                        + " from (select id as id, code as code, manufacturer as manufacturer from Item"
                        + " where manufacturer is not null) as t1"
                        + " inner join (select item as item, sum(price * stock) as total from ItemLog"
                        + " where warehouse=:warehouse and logDate=:date group by item) as t2"
                        + " on t1.id = t2.item.id group by manufacturer order by total desc", Tuple.class);
        
        query.setParameter("date", date);
        query.setParameter("warehouse", warehouse);
        
        List<Tuple> tuples = query.getResultList();
        List<ManufacturerRow> result = new ArrayList<>();
        ManufacturerRow row;
        
        for (Tuple tuple : tuples) {
            row = new ManufacturerRow();
            
            row.setDate(date);
            row.setName(tuple.get("manufacturer").toString());
            row.setNumberOfItems(((Number) tuple.get("items")).intValue());
            row.setCost(((Number) tuple.get("total")).intValue());
            result.add(row);
        }
        
        return result;
    }
    
}
