import axios from "axios";
import {fetchEventSource} from "@microsoft/fetch-event-source";

const api = axios.create({
    baseURL: "/api/v3",
})

export function getNodeTypes(){
    return api.get("/node/types");
}

export function getNodeConfigs(nodeCode){
    return api.get(`/node/${nodeCode}/config`);
}

export function getNodeOutputs(nodeCode){
    return api.get(`/node/${nodeCode}/output`);
}

export function runWorkflow(data, config){
    return fetchEventSource("/api/v3/workflow/run",{
        method: "POST",
        headers: {
            "Content-Type": "application/json",
        },
        body: JSON.stringify(data),
        ...config
    });
}

export function saveWorkflow(data){
    return api.post("/workflow", data);
}

export function getWorkflow(uuid){
    return api.get(`/workflow`, {
        params: {
            uuid: uuid
        }
    });
}

export function getWorkflows(){
    return api.get("/workflow/all");
}

export function updateWorkflow(data, uuid){
    return api.put("/workflow", data, {
        params: {
            uuid: uuid
        }
    });
}

export function deleteWorkflow(uuid){
    return api.delete("/workflow", {
        params:{
            uuid: uuid
        }
    });
}

export default {
    getNodeTypes,
    getNodeConfigs,
    getNodeOutputs,
    runWorkflow,
    saveWorkflow,
    getWorkflow,
    getWorkflows,
    updateWorkflow,
    deleteWorkflow,
}