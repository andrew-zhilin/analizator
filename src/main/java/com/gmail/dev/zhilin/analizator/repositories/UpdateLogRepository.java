package com.gmail.dev.zhilin.analizator.repositories;

import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.gmail.dev.zhilin.analizator.entities.UpdateLog;
import com.gmail.dev.zhilin.analizator.entities.Warehouse;

@Repository
public interface UpdateLogRepository extends JpaRepository<UpdateLog, Long> {
    
    Optional<UpdateLog> findFirstByWarehouseOrderByLogDateDesc(Warehouse warehouse);

    Optional<UpdateLog> findFirstByOrderByLogDateDesc();

    Optional<UpdateLog> findFirstByWarehouseAndLogDate(Warehouse warehouse, LocalDate date);

    Optional<UpdateLog> findFirstByOrderByLogDate();

}
