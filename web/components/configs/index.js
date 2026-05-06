/**
 * 在此处实现不同配置的样式并绑定属性
 */

import MapConfig from './MapConfig.vue';
import NumberConfig from './NumberConfig.vue';
import ListConfig from './ListConfig.vue';
import QueueConfig from './QueueConfig.vue';
import StringConfig from './StringConfig.vue';
import ConditionConfig from './ConditionConfig.vue';
import BooleanConfig from './BooleanConfig.vue';
import SliderConfig from './SliderConfig.vue';
import SelectConfig from './SelectConfig.vue';

export {
    MapConfig,
    NumberConfig,
    ListConfig,
    QueueConfig,
    StringConfig,
    ConditionConfig,
    BooleanConfig,
    SliderConfig,
    SelectConfig
};

/**
 * 配置类型到Vue组件的映射表
 * 类型字符串与后端 ConfigTypes.java 中的常量保持一致
 */
const configComponentMap = {
    'Number': NumberConfig,
    'String': StringConfig,
    'Boolean': BooleanConfig,
    'List': ListConfig,
    'Queue': QueueConfig,
    'Map': MapConfig,
    'Condition': ConditionConfig,
    'Select': SelectConfig,
    'Slider': SliderConfig,
};

/**
 * 根据配置类型字符串获取对应的Vue组件
 * @param {string} type - 配置类型，与 ConfigTypes.java 中的常量对应
 * @returns {Component|null} 对应的Vue组件，若类型未知则返回 null
 */
export function getConfigComponent(type) {
    return configComponentMap[type] || null;
}

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
            case "Queue":
                config[item.name] = JSON.parse(item.value);
                break;
            case "Condition": {
                const parsed = JSON.parse(item.value);
                config[item.name] = Array.isArray(parsed)
                    ? (parsed[0] && typeof parsed[0] === 'object' ? parsed[0] : {})
                    : parsed;
                break;
            }
            default:
                // String, Select, etc
                config[item.name] = item.value;
                break;
        }
    })
    return config;
}