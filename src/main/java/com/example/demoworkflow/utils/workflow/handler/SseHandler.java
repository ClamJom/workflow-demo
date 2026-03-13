package com.example.demoworkflow.utils.workflow.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class SseHandler {
    private final Map<String, SseEmitter> sseEmitters = new ConcurrentHashMap<>();

    public SseEmitter connect(String token){
        sseEmitters.computeIfAbsent(token, k->{
            sseEmitters.remove(token);
            return null;
        });
        SseEmitter sseEmitter = new SseEmitter();
        sseEmitter.onError(this::onError);
        sseEmitters.put(token, sseEmitter);
        return sseEmitter;
    }

    public void send(String token, String msg){
        try{
            SseEmitter emitter = sseEmitters.get(token);
            if(emitter == null) return;
            emitter.send(msg);
        }catch(Exception e){
            log.error(e.getMessage());
        }
    }

    public void close(String token){
        sseEmitters.computeIfPresent(token, (k, v)->{
           v.complete();
           sseEmitters.remove(token);
           return null;
        });
    }

    private void onError(Throwable throwable){
        log.error(throwable.getMessage());
    }
}
