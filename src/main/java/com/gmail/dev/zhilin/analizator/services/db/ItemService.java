package com.gmail.dev.zhilin.analizator.services.db;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.gmail.dev.zhilin.analizator.entities.Item;
import com.gmail.dev.zhilin.analizator.entities.Warehouse;
import com.gmail.dev.zhilin.analizator.repositories.ItemRepository;
import com.gmail.dev.zhilin.analizator.util.HibernateUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Transactional(readOnly = true)
@Service
public class ItemService {

    @PersistenceContext
    private EntityManager entityManager;
    private final ItemRepository itemRepository;

    @Autowired
    public ItemService(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    public Optional<Item> findById(Long id) {
        return itemRepository.findById(id);
    }

    public List<Item> findAll() {
        return itemRepository.findAll();
    }

    @Transactional
    public void upsert(List<Item> items) {
        List<Item> db = itemRepository.findAll();
        Item itemInDb;

        for (Item item : items) {
            if (db.contains(item)) {
                itemInDb = db.get(db.indexOf(item));

                itemInDb.setManufacturer(item.getManufacturer());
                itemInDb.setPartNumber(item.getPartNumber());
                itemInDb.setCrossReference(item.getCrossReference());
                itemInDb.setName(item.getName());
                itemInDb.setMeasure(item.getMeasure());
            } else {
                itemRepository.save(item);
            }
        }
    }

    public Optional<LocalDate> findCreatedAt(Long id) {
        Optional<Item> item = findById(id);

        if (item.isEmpty())
            return Optional.empty();

        return Optional.of(item.get().getCreatedAt());
    }

    public List<Item> findByOrderByCreatedAtDesc(Integer limit) {
        return itemRepository.findByOrderByCreatedAtDesc(Limit.of(limit));
    }

    @SuppressWarnings("unchecked")
    public Optional<Item> selectByIdJoinItemLogsAndSalesAndArrivals(Long id, LocalDate from, LocalDate to) {
        Session session = entityManager.unwrap(Session.class);
        List<Item> result;
        Query logQuery = entityManager.createQuery("from Item i left join fetch i.itemLogs where i.id=:id", Item.class);
        Query saleQuery = entityManager.createQuery("from Item i left join fetch i.sales where i.id=:id", Item.class);
        Query arrivalQuery = entityManager.createQuery("from Item i left join fetch i.arrivals where i.id=:id",
                Item.class);

        logQuery.setParameter("id", id);
        saleQuery.setParameter("id", id);
        arrivalQuery.setParameter("id", id);
        session.enableFilter("itemLogDateBetween").setParameter("from", from).setParameter("to", to);
        session.enableFilter("saleDateBetween").setParameter("from", from).setParameter("to", to);
        session.enableFilter("arrivalDateBetween").setParameter("from", from).setParameter("to", to);

        result = logQuery.getResultList();
        result = saleQuery.getResultList();
        result = arrivalQuery.getResultList();

        for (Item item : result) {
            item.setItemLogs(HibernateUtil.nullIfNotInitialized(item.getItemLogs()));
            item.setSales(HibernateUtil.nullIfNotInitialized(item.getSales()));
            item.setArrivals(HibernateUtil.nullIfNotInitialized(item.getArrivals()));
        }

        if (result.isEmpty())
            return Optional.empty();

        return Optional.of(result.get(0));
    }

    @SuppressWarnings("unchecked")
    public Optional<Item> selectByIdAndWarehouseJoinItemLogsAndSalesAndArrivals(Long id, Warehouse warehouse,
            LocalDate from, LocalDate to) {
        Session session = entityManager.unwrap(Session.class);
        List<Item> result;
        Query logQuery = entityManager.createQuery("from Item i left join fetch i.itemLogs l where i.id=:id",
                Item.class);
        Query saleQuery = entityManager.createQuery("from Item i left join fetch i.sales where i.id=:id", Item.class);
        Query arrivalQuery = entityManager.createQuery("from Item i left join fetch i.arrivals where i.id=:id",
                Item.class);

        logQuery.setParameter("id", id);
        saleQuery.setParameter("id", id);
        arrivalQuery.setParameter("id", id);

        session.enableFilter("itemLogDateBetween").setParameter("from", from).setParameter("to", to);
        session.enableFilter("saleDateBetween").setParameter("from", from).setParameter("to", to);
        session.enableFilter("arrivalDateBetween").setParameter("from", from).setParameter("to", to);

        session.enableFilter("itemLogWarehouse").setParameter("warehouseId", warehouse.getId());
        session.enableFilter("saleWarehouse").setParameter("warehouseId", warehouse.getId());
        session.enableFilter("arrivalWarehouse").setParameter("warehouseId", warehouse.getId());

        result = logQuery.getResultList();
        result = saleQuery.getResultList();
        result = arrivalQuery.getResultList();

        for (Item item : result) {
            item.setItemLogs(HibernateUtil.nullIfNotInitialized(item.getItemLogs()));
            item.setSales(HibernateUtil.nullIfNotInitialized(item.getSales()));
            item.setArrivals(HibernateUtil.nullIfNotInitialized(item.getArrivals()));
        }

        if (result.isEmpty())
            return Optional.empty();

        return Optional.of(result.get(0));
    }

    @SuppressWarnings("unchecked")
    public List<Item> selectAllJoinItemLogAndSaleAndArrivalAndPriceChangeBetweenDates(LocalDate from, LocalDate to) {
        Session session = entityManager.unwrap(Session.class);
        List<Item> result;
        Query saleQuery = entityManager.createQuery("from Item i left join fetch i.sales", Item.class);
        Query arrivalQuery = entityManager.createQuery("from Item i left join fetch i.arrivals", Item.class);
        Query priceChangeQuery = entityManager.createQuery("from Item i left join fetch i.priceChanges", Item.class);
        Query logQuery = entityManager.createQuery("from Item i left join fetch i.itemLogs", Item.class);

        session.enableFilter("saleDateBetween").setParameter("from", from).setParameter("to", to);
        session.enableFilter("arrivalDateBetween").setParameter("from", from).setParameter("to", to);
        session.enableFilter("priceChangeDateBetween").setParameter("from", from).setParameter("to", to);
        session.enableFilter("itemLogDateBetween").setParameter("from", from).setParameter("to", to);

        result = saleQuery.getResultList();
        result = arrivalQuery.getResultList();
        result = priceChangeQuery.getResultList();
        result = logQuery.getResultList();

        for (Item item : result) {
            item.setItemLogs(HibernateUtil.nullIfNotInitialized(item.getItemLogs()));
            item.setSales(HibernateUtil.nullIfNotInitialized(item.getSales()));
            item.setArrivals(HibernateUtil.nullIfNotInitialized(item.getArrivals()));
            item.setPriceChanges(HibernateUtil.nullIfNotInitialized(item.getPriceChanges()));
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    public List<Item> selectAllJoinItemLogBetweenDates(LocalDate from, LocalDate to) {
        Session session = entityManager.unwrap(Session.class);
        List<Item> result;
        Query logQuery = entityManager.createQuery("from Item i left join fetch i.itemLogs", Item.class);
        
        session.enableFilter("itemLogDateBetween").setParameter("from", from).setParameter("to", to);
        
        result = logQuery.getResultList();
        
        for (Item item : result) {
            item.setItemLogs(HibernateUtil.nullIfNotInitialized(item.getItemLogs()));
            item.setSales(HibernateUtil.nullIfNotInitialized(item.getSales()));
            item.setArrivals(HibernateUtil.nullIfNotInitialized(item.getArrivals()));
            item.setPriceChanges(HibernateUtil.nullIfNotInitialized(item.getPriceChanges()));
        }
        
        return result;
    }
    
    @SuppressWarnings("unchecked")
    public List<Item> selectAllJoinItemLogBetweenDates(LocalDate from, LocalDate to, Warehouse warehouse) {
        Session session = entityManager.unwrap(Session.class);
        List<Item> result;
        Query logQuery = entityManager.createQuery("from Item i left join fetch i.itemLogs", Item.class);

        session.enableFilter("itemLogDateBetween").setParameter("from", from).setParameter("to", to);
        session.enableFilter("itemLogWarehouse").setParameter("warehouseId", warehouse.getId());

        result = logQuery.getResultList();

        for (Item item : result) {
            item.setItemLogs(HibernateUtil.nullIfNotInitialized(item.getItemLogs()));
            item.setSales(HibernateUtil.nullIfNotInitialized(item.getSales()));
            item.setArrivals(HibernateUtil.nullIfNotInitialized(item.getArrivals()));
            item.setPriceChanges(HibernateUtil.nullIfNotInitialized(item.getPriceChanges()));
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    public List<Item> selectAllJoinItemLogAndSaleAndArrivalAndPriceChangeBetweenDates(LocalDate from, LocalDate to,
            Warehouse warehouse) {
        Session session = entityManager.unwrap(Session.class);
        List<Item> result;
        Query saleQuery = entityManager.createQuery("from Item i left join fetch i.sales", Item.class);
        Query arrivalQuery = entityManager.createQuery("from Item i left join fetch i.arrivals", Item.class);
        Query priceChangeQuery = entityManager.createQuery("from Item i left join fetch i.priceChanges", Item.class);
        Query logQuery = entityManager.createQuery("from Item i left join fetch i.itemLogs", Item.class);

        session.enableFilter("saleDateBetween").setParameter("from", from).setParameter("to", to);
        session.enableFilter("arrivalDateBetween").setParameter("from", from).setParameter("to", to);
        session.enableFilter("priceChangeDateBetween").setParameter("from", from).setParameter("to", to);
        session.enableFilter("itemLogDateBetween").setParameter("from", from).setParameter("to", to);

        result = saleQuery.getResultList();
        result = arrivalQuery.getResultList();
        result = priceChangeQuery.getResultList();
        result = logQuery.getResultList();

        for (Item item : result) {
            item.setItemLogs(HibernateUtil.nullIfNotInitialized(item.getItemLogs()));
            item.setSales(HibernateUtil.nullIfNotInitialized(item.getSales()));
            item.setArrivals(HibernateUtil.nullIfNotInitialized(item.getArrivals()));
            item.setPriceChanges(HibernateUtil.nullIfNotInitialized(item.getPriceChanges()));
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    public List<Item> selectSoldBetweenDates(LocalDate from, LocalDate to) {
        List<Item> result;
        Query query = entityManager.createQuery(
                "from Item i left join fetch i.sales s" + " where s.saleDate between :from and :to", Item.class);

        query.setParameter("from", from);
        query.setParameter("to", to);

        result = query.getResultList();

        for (Item item : result) {
            item.setSales(HibernateUtil.nullIfNotInitialized(item.getSales()));
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    public List<Item> selectSoldBetweenDates(LocalDate from, LocalDate to, Warehouse warehouse) {
        List<Item> result;
        Query query = entityManager.createQuery("from Item i left join fetch i.sales s"
                + " where s.warehouse=:warehouse and s.saleDate between :from and :to", Item.class);

        query.setParameter("warehouse", warehouse);
        query.setParameter("from", from);
        query.setParameter("to", to);

        result = query.getResultList();

        for (Item item : result) {
            item.setSales(HibernateUtil.nullIfNotInitialized(item.getSales()));
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    public List<Item> selectArrivedBetweenDates(LocalDate from, LocalDate to) {
        List<Item> result;
        Query query = entityManager.createQuery(
                "from Item i left join fetch i.arrivals a" + " where a.arrivalDate between :from and :to", Item.class);

        query.setParameter("from", from);
        query.setParameter("to", to);

        result = query.getResultList();

        for (Item item : result) {
            item.setArrivals(HibernateUtil.nullIfNotInitialized(item.getArrivals()));
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    public List<Item> selectArrivedBetweenDates(LocalDate from, LocalDate to, Warehouse warehouse) {
        List<Item> result;
        Query query = entityManager.createQuery("from Item i left join fetch i.arrivals a"
                + " where a.warehouse=:warehouse and a.arrivalDate between :from and :to", Item.class);

        query.setParameter("warehouse", warehouse);
        query.setParameter("from", from);
        query.setParameter("to", to);

        result = query.getResultList();

        for (Item item : result) {
            item.setArrivals(HibernateUtil.nullIfNotInitialized(item.getArrivals()));
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    public List<Item> selectWithChangedPriceOn(LocalDate date) {
        List<Item> result;
        Query priceChangeQuery = entityManager.createQuery(
                "from Item i left join fetch i.priceChanges c" + " where c.changeDate = :date", Item.class);

        priceChangeQuery.setParameter("date", date);

        result = priceChangeQuery.getResultList();

        for (Item item : result) {
            item.setPriceChanges(HibernateUtil.nullIfNotInitialized(item.getPriceChanges()));
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    public List<Item> saleQuery(LocalDate from, LocalDate to) {
        Session session = entityManager.unwrap(Session.class);
        List<Item> result;
        Query saleQuery = entityManager.createQuery("from Item i left join fetch i.sales", Item.class);

        session.enableFilter("saleDateBetween").setParameter("from", from).setParameter("to", to);

        result = saleQuery.getResultList();

        for (Item item : result) {
            item.setItemLogs(HibernateUtil.nullIfNotInitialized(item.getItemLogs()));
            item.setSales(HibernateUtil.nullIfNotInitialized(item.getSales()));
            item.setArrivals(HibernateUtil.nullIfNotInitialized(item.getArrivals()));
            item.setPriceChanges(HibernateUtil.nullIfNotInitialized(item.getPriceChanges()));
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    public List<Item> selectInStockByDate(LocalDate date) {
        List<Item> result;
        Query query = entityManager.createQuery("from Item i inner join fetch i.itemLogs l where l.logDate=:date",
                Item.class);

        query.setParameter("date", date);

        result = query.getResultList();

        for (Item item : result) {
            item.setItemLogs(HibernateUtil.nullIfNotInitialized(item.getItemLogs()));
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    public List<Item> selectInStockByDateAndWarehouse(LocalDate date, Warehouse warehouse) {
        List<Item> result;
        Query query = entityManager.createQuery(
                "from Item i inner join fetch i.itemLogs l" + " where l.logDate=:date and l.warehouse=:warehouse",
                Item.class);

        query.setParameter("date", date);
        query.setParameter("warehouse", warehouse);

        result = query.getResultList();

        for (Item item : result) {
            item.setItemLogs(HibernateUtil.nullIfNotInitialized(item.getItemLogs()));
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    public List<Item> selectInStockByDateAndManufacturer(LocalDate date, String manufacturer) {
        List<Item> result;
        Query query = entityManager.createQuery(
                "from Item i inner join fetch i.itemLogs l" + " where l.logDate=:date and i.manufacturer=:manufacturer",
                Item.class);

        query.setParameter("date", date);
        query.setParameter("manufacturer", manufacturer);

        result = query.getResultList();

        for (Item item : result) {
            item.setItemLogs(HibernateUtil.nullIfNotInitialized(item.getItemLogs()));
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    public List<Item> selectInStockByDateAndManufacturerAndWarehouse(LocalDate date, String manufacturer,
            Warehouse warehouse) {
        List<Item> result;
        Query query = entityManager.createQuery(
                "from Item i inner join fetch i.itemLogs l"
                        + " where l.logDate=:date and i.manufacturer=:manufacturer and l.warehouse=:warehouse",
                Item.class);

        query.setParameter("date", date);
        query.setParameter("manufacturer", manufacturer);
        query.setParameter("warehouse", warehouse);

        result = query.getResultList();

        for (Item item : result) {
            item.setItemLogs(HibernateUtil.nullIfNotInitialized(item.getItemLogs()));
        }

        return result;
    }

    public Page<Item> findPaginated(Pageable pageable) {
        int pageSize = pageable.getPageSize();
        int currentPage = pageable.getPageNumber();
        int startItem = currentPage * pageSize;
        List<Item> items = itemRepository.findAll(pageable.getSort());
        List<Item> list;

        if (items.size() < startItem) {
            list = Collections.emptyList();
        } else {
            int toIndex = (int) Math.min(startItem + pageSize, items.size());
            list = items.subList(startItem, toIndex);
        }

        Page<Item> page = new PageImpl<Item>(list, PageRequest.of(currentPage, pageSize), items.size());

        return page;
    }

    public Page<Item> findPaginated(Pageable pageable, String query) {
        int pageSize = pageable.getPageSize();
        int currentPage = pageable.getPageNumber();
        int startItem = currentPage * pageSize;
        List<Item> items = itemRepository.findByPartNumberContainsIgnoreCaseOrCrossReferenceContainsIgnoreCase(query,
                query);
        List<Item> list;

        if (items.size() < startItem) {
            list = Collections.emptyList();
        } else {
            int toIndex = (int) Math.min(startItem + pageSize, items.size());
            list = items.subList(startItem, toIndex);
        }

        Page<Item> page = new PageImpl<Item>(list, PageRequest.of(currentPage, pageSize), items.size());

        return page;
    }

}
