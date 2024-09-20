package com.gmail.dev.zhilin.analizator.repositories;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.gmail.dev.zhilin.analizator.entities.PriceChange;

@Repository
public interface PriceChangeRepository extends JpaRepository<PriceChange, Long> {
    
    List<PriceChange> findAllByChangeDate(LocalDate date);
    
}
