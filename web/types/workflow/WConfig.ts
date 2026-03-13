export default class WConfig{
    id?: number;
    name: string; // 配置名称
    des?: string; // 配置描述
    type: string = "String"; // 配置类型
    value?: string; // 配置值
    options?: string; // 为Select类型配置定义的专属属性，其值一般为数组类型，元素类型为字符串
    min?: number;   // 为Number类型配置定义的专属属性，定义其取值范围的最小值。注意！Number类型配置的value值只能为整数字符串
    max?: number;   // 为Number类型配置定义的专属属性，定义其取值范围的最大值
    k: number = 1;  // 为Number类型配置定义的专属属性，当需要取值带有小数部分时，这里取除数，即得到的最终值为 `value / k`
    quantize: number = 0;   // 为Number类型配置定义的专属属性，当需要取值带有小数部分时，该值定义精度，即取多少位小数
    required: boolean = false;  // 是否必填项
    parent?: number;    // 是否是某一配置的子配置项，若是，则指向父配置的id。注意！如果父配置为Boolean类型，该配置应当在父配置为true时显示

    constructor(config: Object){
        for(let k of Object.keys(config)){
            this[k] = config[k];
        }
    }
}