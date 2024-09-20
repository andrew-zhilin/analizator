package com.gmail.dev.zhilin.analizator.services.db;

import java.time.LocalDate;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.gmail.dev.zhilin.analizator.entities.UpdateLog;
import com.gmail.dev.zhilin.analizator.entities.Warehouse;
import com.gmail.dev.zhilin.analizator.repositories.UpdateLogRepository;

@Service
@Transactional(readOnly = true)
public class UpdateLogService {

    private final UpdateLogRepository updateLogRepository;

    @Autowired
    public UpdateLogService(UpdateLogRepository updateLogRepository) {
        this.updateLogRepository = updateLogRepository;
    }

    public Optional<UpdateLog> findLastByWarehouse(Warehouse warehouse) {
        return updateLogRepository.findFirstByWarehouseOrderByLogDateDesc(warehouse);
    }

    public Optional<UpdateLog> findLast() {
        return updateLogRepository.findFirstByOrderByLogDateDesc();
    }

    @Transactional
    public void upsert(UpdateLog updateLog) {
        Optional<UpdateLog> log = updateLogRepository.findFirstByWarehouseAndLogDate(updateLog.getWarehouse(),
                updateLog.getLogDate());

        if (log.isPresent()) {
            log.get().setNumberOfItems(updateLog.getNumberOfItems());
        } else {
            updateLogRepository.save(updateLog);
        }
    }
    
    public Optional<LocalDate> findLastDate() {
        Optional<UpdateLog> optional = updateLogRepository.findFirstByOrderByLogDateDesc();
        Optional<LocalDate> date = Optional.empty();

        if (optional.isPresent()) {
            date = Optional.of(optional.get().getLogDate());
        }

        return date;
    }

    public Optional<LocalDate> findFirstDate() {
        Optional<UpdateLog> optional = updateLogRepository.findFirstByOrderByLogDate();
        Optional<LocalDate> date = Optional.empty();

        if (optional.isPresent()) {
            return Optional.of(optional.get().getLogDate());
        }

        return date;
    }
    
}
