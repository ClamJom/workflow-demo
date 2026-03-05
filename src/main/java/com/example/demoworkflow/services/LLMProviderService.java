package com.example.demoworkflow.services;

import com.example.demoworkflow.pojo.LLMProvider;
import com.example.demoworkflow.repository.LLMProviderRepository;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * LLM供应商服务
 */
@Service
@Slf4j
public class LLMProviderService {

    @Resource
    private LLMProviderRepository llmProviderRepository;

    /**
     * 获取所有启用的供应商
     */
    public List<LLMProvider> getAllActiveProviders() {
        return llmProviderRepository.findAllActiveProviders();
    }

    /**
     * 获取默认供应商
     */
    public Optional<LLMProvider> getDefaultProvider() {
        return llmProviderRepository.findDefaultProvider();
    }

    /**
     * 根据ID获取供应商
     */
    public Optional<LLMProvider> getProviderById(Long id) {
        return llmProviderRepository.findById(id);
    }

    /**
     * 根据名称获取供应商
     */
    public Optional<LLMProvider> getProviderByName(String name) {
        return llmProviderRepository.findByName(name);
    }

    /**
     * 根据类型获取供应商
     */
    public List<LLMProvider> getProvidersByType(String type) {
        return llmProviderRepository.findByType(type);
    }

    /**
     * 保存供应商
     */
    @Transactional
    public LLMProvider saveProvider(LLMProvider provider) {
        // 如果设置为默认供应商，先取消其他默认供应商
        if (Boolean.TRUE.equals(provider.getIsDefault())) {
            llmProviderRepository.findAllActiveProviders().forEach(p -> {
                if (Boolean.TRUE.equals(p.getIsDefault()) && !p.getId().equals(provider.getId())) {
                    p.setIsDefault(false);
                    llmProviderRepository.save(p);
                }
            });
        }
        return llmProviderRepository.save(provider);
    }

    /**
     * 删除供应商
     */
    @Transactional
    public void deleteProvider(Long id) {
        llmProviderRepository.deleteById(id);
    }

    /**
     * 设置默认供应商
     */
    @Transactional
    public void setDefaultProvider(Long id) {
        // 取消所有默认供应商
        llmProviderRepository.findAllActiveProviders().forEach(p -> {
            if (Boolean.TRUE.equals(p.getIsDefault())) {
                p.setIsDefault(false);
                llmProviderRepository.save(p);
            }
        });
        
        // 设置新的默认供应商
        Optional<LLMProvider> provider = llmProviderRepository.findById(id);
        provider.ifPresent(p -> {
            p.setIsDefault(true);
            llmProviderRepository.save(p);
        });
    }

    /**
     * 初始化默认供应商（如果不存在）
     */
    @Transactional
    public void initDefaultProviders() {
        if (llmProviderRepository.count() == 0) {
            // 添加OpenAI默认配置
            LLMProvider openai = LLMProvider.builder()
                    .name("OpenAI")
                    .type("openai")
                    .baseUrl("https://api.openai.com/v1")
                    .model("gpt-3.5-turbo")
                    .timeout(60000)
                    .maxTokens(4096)
                    .temperature(0.7f)
                    .stream(false)
                    .description("OpenAI GPT系列模型")
                    .isDefault(true)
                    .status("active")
                    .build();
            llmProviderRepository.save(openai);

            // 添加Azure OpenAI配置
            LLMProvider azure = LLMProvider.builder()
                    .name("Azure OpenAI")
                    .type("azure")
                    .baseUrl("https://{your-resource-name}.openai.azure.com/openai/deployments/{deployment-name}")
                    .model("gpt-35-turbo")
                    .timeout(60000)
                    .maxTokens(4096)
                    .temperature(0.7f)
                    .stream(false)
                    .description("Azure OpenAI服务")
                    .isDefault(false)
                    .status("active")
                    .build();
            llmProviderRepository.save(azure);

            // 添加本地模型配置
            LLMProvider local = LLMProvider.builder()
                    .name("Local LLM")
                    .type("local")
                    .baseUrl("http://localhost:8080/v1")
                    .model("llama2")
                    .timeout(120000)
                    .maxTokens(4096)
                    .temperature(0.7f)
                    .stream(false)
                    .description("本地部署的大语言模型")
                    .isDefault(false)
                    .status("inactive")
                    .build();
            llmProviderRepository.save(local);

            log.info("已初始化默认LLM供应商配置");
        }
    }
}
