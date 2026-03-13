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
}