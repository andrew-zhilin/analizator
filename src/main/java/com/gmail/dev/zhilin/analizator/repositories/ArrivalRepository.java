package com.gmail.dev.zhilin.analizator.repositories;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.gmail.dev.zhilin.analizator.entities.Arrival;
import com.gmail.dev.zhilin.analizator.entities.Warehouse;

@Repository
public interface ArrivalRepository extends JpaRepository<Arrival, Long> {
    
    List<Arrival> findAllByArrivalDate(LocalDate date);
    
    List<Arrival> findAllByArrivalDateBetween(LocalDate from, LocalDate to);
    
    List<Arrival> findAllByArrivalDateAndWarehouse(LocalDate date, Warehouse warehouse);
    
}
