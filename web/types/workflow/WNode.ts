import {generateUUID} from "../../utils/token";
import WConfig from "./WConfig";

/**
 * 工作流节点
 */
export default class WNode{
    id: string = generateUUID();
    name?: string = '';
    type: number = 0x00;
    configs: Array<WConfig> = [];
    /** 节点在画布上的位置，首次加载时若为空则由布局算法自动计算 */
    position?: { x: number; y: number };
}