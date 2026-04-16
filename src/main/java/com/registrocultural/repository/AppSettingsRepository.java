package com.registrocultural.repository;

import com.registrocultural.model.AppSettings;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AppSettingsRepository extends MongoRepository<AppSettings, String> {
}
