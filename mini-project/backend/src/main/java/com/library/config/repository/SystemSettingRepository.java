package com.library.config.repository;

import com.library.config.model.SystemSetting;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SystemSettingRepository extends MongoRepository<SystemSetting, String> {
    Optional<SystemSetting> findByKey(String key);
}
