package com.gmail.dev.zhilin.analizator.services.db;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.gmail.dev.zhilin.analizator.entities.Item;
import com.gmail.dev.zhilin.analizator.entities.PriceChange;
import com.gmail.dev.zhilin.analizator.repositories.PriceChangeRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Service
@Transactional(readOnly = true)
public class PriceChangeService {

    @PersistenceContext
    private EntityManager entityManager;
    private PriceChangeRepository priceChangeRepository;
    private ItemService itemService;

    @Autowired
    public PriceChangeService(PriceChangeRepository priceChangeRepository, ItemService itemService) {
        this.priceChangeRepository = priceChangeRepository;
        this.itemService = itemService;
    }
    
    @Transactional
    public void refreshPriceChanges(LocalDate date) {
        List<Item> items = itemService.selectAllJoinItemLogBetweenDates(date, date.plusDays(1));
        List<PriceChange> existingChanges = priceChangeRepository.findAllByChangeDate(date);
        List<PriceChange> changes = new ArrayList<>();
        PriceChange change;
        int oldPrice;
        int newPrice;
        
        for (Item item : items) {
            if (item.inStock(date) && item.inStock(date.plusDays(1))) {
                oldPrice = item.priceOn(date);
                newPrice = item.priceOn(date.plusDays(1));
                
                if (oldPrice != newPrice) {
                    change = new PriceChange();
                    
                    change.setChangeDate(date);
                    change.setItem(item);
                    
                    if (existingChanges.contains(change)) {
                        change = existingChanges.get(existingChanges.indexOf(change));

                        change.setOldPrice(oldPrice);
                        change.setNewPrice(newPrice);
                        existingChanges.remove(change);
                    } else {
                        change.setOldPrice(oldPrice);
                        change.setNewPrice(newPrice);
                        changes.add(change);
                    }
                }
            }
        }
        
        priceChangeRepository.saveAll(changes);
        priceChangeRepository.deleteAll(existingChanges);
    }

    @SuppressWarnings("unchecked")
    public List<PriceChange> selectAllBetween(LocalDate from, LocalDate to) {
        List<PriceChange> result;
        Query query = entityManager.createQuery("from PriceChange c left join fetch c.item where c.changeDate between :from and : to", PriceChange.class);

        query.setParameter("from", from);
        query.setParameter("to", to);
        
        result = query.getResultList();
        
        return result;
    }
    
}
