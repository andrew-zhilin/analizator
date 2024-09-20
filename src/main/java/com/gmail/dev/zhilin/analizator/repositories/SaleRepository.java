package com.gmail.dev.zhilin.analizator.repositories;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.gmail.dev.zhilin.analizator.entities.Sale;
import com.gmail.dev.zhilin.analizator.entities.Warehouse;

@Repository
public interface SaleRepository extends JpaRepository<Sale, Long> {
    
    List<Sale> findAllBySaleDate(LocalDate date);

    List<Sale> findAllBySaleDateAndWarehouse(LocalDate date, Warehouse warehouse);
    
}
