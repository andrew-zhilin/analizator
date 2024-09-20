package com.gmail.dev.zhilin.analizator.services.db;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.gmail.dev.zhilin.analizator.entities.Arrival;
import com.gmail.dev.zhilin.analizator.entities.Item;
import com.gmail.dev.zhilin.analizator.entities.Warehouse;
import com.gmail.dev.zhilin.analizator.repositories.ArrivalRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Service
@Transactional(readOnly = true)
public class ArrivalService {

    @PersistenceContext
    private EntityManager entityManager;
    private ArrivalRepository arrivalRepository;
    private ItemService itemService;

    @Autowired
    public ArrivalService(ArrivalRepository arrivalRepository, ItemService itemService) {
        this.arrivalRepository = arrivalRepository;
        this.itemService = itemService;
    }

    @Transactional
    public void refreshArrivals(LocalDate date, Warehouse warehouse) {
        List<Item> items = itemService.selectAllJoinItemLogBetweenDates(date, date.plusDays(1), warehouse);
        List<Arrival> existingArrivals = arrivalRepository.findAllByArrivalDateAndWarehouse(date, warehouse);
        List<Arrival> arrivals = new ArrayList<>();
        Arrival arrival;
        int arrived;

        for (Item item : items) {
            arrived = item.stockOn(date.plusDays(1)) - item.stockOn(date);

            if (arrived > 0) {
                arrival = new Arrival();

                arrival.setArrivalDate(date);
                arrival.setItem(item);
                arrival.setWarehouse(warehouse);

                if (existingArrivals.contains(arrival)) {
                    arrival = existingArrivals.get(existingArrivals.indexOf(arrival));

                    arrival.setPrice(item.priceOn(date.plusDays(1)));
                    arrival.setQuantity(arrived);
                    existingArrivals.remove(arrival);
                } else {
                    arrival.setPrice(item.priceOn(date.plusDays(1)));
                    arrival.setQuantity(arrived);
                    arrivals.add(arrival);
                }
            }
        }

        arrivalRepository.saveAll(arrivals);
        arrivalRepository.deleteAll(existingArrivals);
    }

    @SuppressWarnings("unchecked")
    public List<Arrival> selectAllBetween(LocalDate from, LocalDate to) {
        List<Arrival> result;
        Query query = entityManager.createQuery(
                "from Arrival a left join fetch a.item where a.arrivalDate between :from and : to", Arrival.class);

        query.setParameter("from", from);
        query.setParameter("to", to);

        result = query.getResultList();

        return result;
    }

    @SuppressWarnings("unchecked")
    public List<Arrival> selectAllByWarehouseBetween(LocalDate from, LocalDate to, Warehouse warehouse) {
        List<Arrival> result;
        Query query = entityManager.createQuery("from Arrival a left join fetch a.item"
                + " where a.warehouse=:warehouse and a.arrivalDate between :from and : to", Arrival.class);

        query.setParameter("from", from);
        query.setParameter("to", to);
        query.setParameter("warehouse", warehouse);

        result = query.getResultList();

        return result;
    }

}
