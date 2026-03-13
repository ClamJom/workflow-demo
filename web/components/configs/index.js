/**
 * 在此处实现不同配置的样式并绑定属性
 */

import MapConfig from './MapConfig.vue';
import NumberConfig from './NumberConfig.vue';
import ListConfig from './ListConfig.vue';
import StringConfig from './StringConfig.vue';
import ConditionConfig from './ConditionConfig.vue';
import BooleanConfig from './BooleanConfig.vue';
import SliderConfig from './SliderConfig.vue';
import SelectConfig from './SelectConfig.vue';

export {
    MapConfig,
    NumberConfig,
    ListConfig,
    StringConfig,
    ConditionConfig,
    BooleanConfig,
    SliderConfig,
    SelectConfig
};

/**
 * 解析数字类型
 * @param config WConfig
 */
function parseNumberConfig(config){
    if(config.k === 1) return parseInt(config.value);
    let v = parseFloat(config.value) / config.k;
    return v.toFixed(config.quantize);
}

/**
 * 格式化配置并返回对象
 * 备用，其实前端无需将配置值提取出来，后端在运行工作流之前就已经处理了相关值
 * @param configs Array<WConfig>
 */
export function parseConfig(configs){
    let config = {};
    configs.forEach(item=>{
        switch(item.type){
            case "Boolean":
                config[item.name] = item.value.toLowerCase() === "true";
                break;
            case "Slider":
            case "Number":
                config[item.name] = parseNumberConfig(item);
                break;
            case "Map":
            case "List":
            case "Condition":
                config[item.name] = JSON.parse(item.value);
                break;
            default:
                // String, Select, etc
                config[item.name] = item.value;
                break;
        }
    })
    return config;
}