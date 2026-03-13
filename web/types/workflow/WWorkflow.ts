import WNode from "./WNode";
import WEdge from "./WEdge";

export default class WWorkflow{
    name?: string = "";
    nodes: Array<WNode> = [];
    edges: Array<WEdge> = [];
}