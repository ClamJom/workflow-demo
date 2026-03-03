package com.example.demoworkflow.control;

import com.alibaba.fastjson2.JSON;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v3")
public class BaseController {

    @Operation(description = "Test")
    @PostMapping("/test")
    public String test(@RequestBody Map<String, Object> body){
        body.keySet().forEach(k->{
            if(body.get(k) instanceof String && ((String) body.get(k)).startsWith("{") && ((String) body.get(k)).endsWith("}"))
                body.put(k, JSON.parse((String) body.get(k)));
        });
        return JSON.toJSONString(body);
    }
}
