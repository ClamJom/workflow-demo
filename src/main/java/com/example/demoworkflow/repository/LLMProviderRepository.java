package com.example.demoworkflow.repository;

import com.example.demoworkflow.pojo.LLMProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * LLM供应商Repository
 */
@Repository
public interface LLMProviderRepository extends JpaRepository<LLMProvider, Long> {

    /**
     * 根据类型查找供应商
     */
    List<LLMProvider> findByType(String type);

    /**
     * 根据名称查找供应商
     */
    Optional<LLMProvider> findByName(String name);

    /**
     * 查找启用的供应商
     */
    List<LLMProvider> findByStatus(String status);

    /**
     * 查找默认供应商
     */
    @Query("SELECT p FROM LLMProvider p WHERE p.isDefault = true AND p.status = 'active'")
    Optional<LLMProvider> findDefaultProvider();

    /**
     * 查找所有启用的供应商
     */
    @Query("SELECT p FROM LLMProvider p WHERE p.status = 'active' ORDER BY p.isDefault DESC, p.name ASC")
    List<LLMProvider> findAllActiveProviders();
}
