package com.gmail.dev.zhilin.analizator.repositories;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.gmail.dev.zhilin.analizator.entities.ItemLog;

@Repository
public interface ItemLogRepository extends JpaRepository<ItemLog, Long> {
    
    List<ItemLog> findAllByLogDate(LocalDate date);
    
}
