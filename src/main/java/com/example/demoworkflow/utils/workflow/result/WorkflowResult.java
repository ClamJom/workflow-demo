package com.example.demoworkflow.utils.workflow.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowResult {
    public String token;

    public String from;

    public String nodeId;

    public int state;

    public String msg;

    public Object extData;

    public String getFrom(){
        return from == null ? "system" : from;
    }
}
