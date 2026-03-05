package com.example.demoworkflow.control;

import com.example.demoworkflow.pojo.LLMProvider;
import com.example.demoworkflow.services.LLMProviderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * LLM供应商管理控制器
 */
@RestController
@RequestMapping("/api/v3/llm")
@Tag(name = "LLM供应商管理", description = "管理大语言模型供应商配置")
@Slf4j
public class LLMProviderController {

    @Resource
    private LLMProviderService llmProviderService;

    @Operation(description = "获取所有启用的LLM供应商")
    @GetMapping("/providers")
    public List<LLMProvider> getAllProviders() {
        return llmProviderService.getAllActiveProviders();
    }

    @Operation(description = "获取默认供应商")
    @GetMapping("/providers/default")
    public Map<String, Object> getDefaultProvider() {
        Map<String, Object> result = new HashMap<>();
        Optional<LLMProvider> provider = llmProviderService.getDefaultProvider();
        if (provider.isPresent()) {
            result.put("success", true);
            result.put("data", provider.get());
        } else {
            result.put("success", false);
            result.put("msg", "未设置默认供应商");
        }
        return result;
    }

    @Operation(description = "根据ID获取供应商")
    @GetMapping("/providers/{id}")
    public Map<String, Object> getProviderById(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        Optional<LLMProvider> provider = llmProviderService.getProviderById(id);
        if (provider.isPresent()) {
            result.put("success", true);
            result.put("data", provider.get());
        } else {
            result.put("success", false);
            result.put("msg", "供应商不存在");
        }
        return result;
    }

    @Operation(description = "创建LLM供应商")
    @PostMapping("/providers")
    public Map<String, Object> createProvider(@RequestBody LLMProvider provider) {
        Map<String, Object> result = new HashMap<>();
        try {
            LLMProvider saved = llmProviderService.saveProvider(provider);
            result.put("success", true);
            result.put("data", saved);
            result.put("msg", "供应商创建成功");
        } catch (Exception e) {
            log.error("创建供应商失败", e);
            result.put("success", false);
            result.put("msg", e.getMessage());
        }
        return result;
    }

    @Operation(description = "更新LLM供应商")
    @PutMapping("/providers/{id}")
    public Map<String, Object> updateProvider(@PathVariable Long id, @RequestBody LLMProvider provider) {
        Map<String, Object> result = new HashMap<>();
        try {
            Optional<LLMProvider> existing = llmProviderService.getProviderById(id);
            if (!existing.isPresent()) {
                result.put("success", false);
                result.put("msg", "供应商不存在");
                return result;
            }
            
            provider.setId(id);
            LLMProvider updated = llmProviderService.saveProvider(provider);
            result.put("success", true);
            result.put("data", updated);
            result.put("msg", "供应商更新成功");
        } catch (Exception e) {
            log.error("更新供应商失败", e);
            result.put("success", false);
            result.put("msg", e.getMessage());
        }
        return result;
    }

    @Operation(description = "删除LLM供应商")
    @DeleteMapping("/providers/{id}")
    public Map<String, Object> deleteProvider(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            llmProviderService.deleteProvider(id);
            result.put("success", true);
            result.put("msg", "供应商删除成功");
        } catch (Exception e) {
            log.error("删除供应商失败", e);
            result.put("success", false);
            result.put("msg", e.getMessage());
        }
        return result;
    }

    @Operation(description = "设置默认供应商")
    @PostMapping("/providers/{id}/set-default")
    public Map<String, Object> setDefaultProvider(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            llmProviderService.setDefaultProvider(id);
            result.put("success", true);
            result.put("msg", "默认供应商设置成功");
        } catch (Exception e) {
            log.error("设置默认供应商失败", e);
            result.put("success", false);
            result.put("msg", e.getMessage());
        }
        return result;
    }

    @Operation(description = "初始化默认供应商")
    @PostMapping("/providers/init-default")
    public Map<String, Object> initDefaultProviders(HttpServletResponse response) throws IOException {
        Map<String, Object> result = new HashMap<>();
        try {
            llmProviderService.initDefaultProviders();
            result.put("success", true);
            result.put("msg", "默认供应商初始化成功");
        } catch (Exception e) {
            log.error("初始化默认供应商失败", e);
            result.put("success", false);
            result.put("msg", e.getMessage());
        }
        return result;
    }
}
