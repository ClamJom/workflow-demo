import WConfig from "./WConfig.ts";
import WNode from "./WNode.ts"
import WEdge from "./WEdge.ts";
import WWorkflow from "./WWorkflow.ts";

/**
 * 所有的类型加W是为了防止和库中的类型命名冲突
 * @type {{Node: WNode, Config: WConfig, Edge: WEdge, Workflow: WWorkflow}}
 */
const workflowTypes = {
    Config: WConfig,
    Node: WNode,
    Edge: WEdge,
    Workflow: WWorkflow,
}

export default workflowTypes;