package com.gmail.dev.zhilin.analizator.services.db;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.gmail.dev.zhilin.analizator.entities.Item;
import com.gmail.dev.zhilin.analizator.entities.Sale;
import com.gmail.dev.zhilin.analizator.entities.Warehouse;
import com.gmail.dev.zhilin.analizator.repositories.SaleRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Service
@Transactional(readOnly = true)
public class SaleService {

    @PersistenceContext
    private EntityManager entityManager;
    private SaleRepository saleRepository;
    private ItemService itemService;

    @Autowired
    public SaleService(SaleRepository saleRepository, ItemService itemService) {
        this.saleRepository = saleRepository;
        this.itemService = itemService;
    }

    @Transactional
    public void refreshSales(LocalDate date, Warehouse warehouse) {
        List<Item> items = itemService.selectAllJoinItemLogBetweenDates(date, date.plusDays(1), warehouse);
        List<Sale> existingSales = saleRepository.findAllBySaleDateAndWarehouse(date, warehouse);
        List<Sale> sales = new ArrayList<>();
        Sale sale;
        int sold;

        for (Item item : items) {
            sold = item.stockOn(date) - item.stockOn(date.plusDays(1));

            if (sold > 0) {
                sale = new Sale();

                sale.setSaleDate(date);
                sale.setItem(item);
                sale.setWarehouse(warehouse);

                if (existingSales.contains(sale)) {
                    sale = existingSales.get(existingSales.indexOf(sale));

                    sale.setPrice(item.priceOn(date));
                    sale.setQuantity(sold);
                    existingSales.remove(sale);
                } else {
                    sale.setPrice(item.priceOn(date));
                    sale.setQuantity(sold);
                    sales.add(sale);
                }
            }
        }

        saleRepository.saveAll(sales);
        saleRepository.deleteAll(existingSales);
    }

    @SuppressWarnings("unchecked")
    public List<Sale> selectAllBetween(LocalDate from, LocalDate to) {
        List<Sale> result;
        Query query = entityManager
                .createQuery("from Sale s left join fetch s.item where s.saleDate between :from and : to", Sale.class);

        query.setParameter("from", from);
        query.setParameter("to", to);

        result = query.getResultList();

        return result;
    }

    @SuppressWarnings("unchecked")
    public List<Sale> selectAllByWarehouseBetween(LocalDate from, LocalDate to, Warehouse warehouse) {
        List<Sale> result;
        Query query = entityManager.createQuery("from Sale s left join fetch s.item"
                + " where s.warehouse=:warehouse and s.saleDate between :from and : to", Sale.class);

        query.setParameter("from", from);
        query.setParameter("to", to);
        query.setParameter("warehouse", warehouse);

        result = query.getResultList();

        return result;
    }

}
