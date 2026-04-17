/**
 * 工作流自动布局工具
 * 基于拓扑排序（Kahn 算法）实现横向/纵向分层布局
 */

const NODE_WIDTH = 160;
const NODE_HEIGHT = 60;
const H_GAP = 80;   // 横向间距
const V_GAP = 60;   // 纵向间距

/**
 * 对工作流节点进行自动布局
 * @param {Array} nodes - WNode 数组（含 id 字段）
 * @param {Array} edges - WEdge 数组（含 from/to 字段）
 * @param {'horizontal'|'vertical'} direction - 布局方向，默认横向
 * @returns {Array} 带有 position 字段的节点数组（新对象，不修改原数组）
 */
export function autoLayout(nodes, edges, direction = 'horizontal') {
    if (!nodes || nodes.length === 0) return nodes;

    // 构建邻接表和入度表
    const inDegree = {};
    const outEdges = {};

    nodes.forEach(n => {
        inDegree[n.id] = 0;
        outEdges[n.id] = [];
    });

    edges.forEach(e => {
        if (inDegree[e.to] !== undefined) {
            inDegree[e.to]++;
        }
        if (outEdges[e.from] !== undefined) {
            outEdges[e.from].push(e.to);
        }
    });

    // Kahn 算法拓扑排序，按层分组
    const layers = [];
    let queue = nodes.filter(n => inDegree[n.id] === 0).map(n => n.id);

    // 若存在环（无入度为0的节点），则直接按顺序排列
    if (queue.length === 0) {
        return nodes.map((n, i) => ({
            ...n,
            position: direction === 'horizontal'
                ? { x: i * (NODE_WIDTH + H_GAP), y: 0 }
                : { x: 0, y: i * (NODE_HEIGHT + V_GAP) }
        }));
    }

    const visited = new Set();

    while (queue.length > 0) {
        layers.push([...queue]);
        const nextQueue = [];
        queue.forEach(nodeId => {
            visited.add(nodeId);
            outEdges[nodeId].forEach(toId => {
                inDegree[toId]--;
                if (inDegree[toId] === 0 && !visited.has(toId)) {
                    nextQueue.push(toId);
                }
            });
        });
        queue = nextQueue;
    }

    // 将未被访问的节点（环中节点）追加到最后一层
    const unvisited = nodes.filter(n => !visited.has(n.id)).map(n => n.id);
    if (unvisited.length > 0) {
        layers.push(unvisited);
    }

    // 构建 id → position 映射
    const positionMap = {};

    if (direction === 'horizontal') {
        // 横向布局：层 = 列，同层节点纵向排列
        layers.forEach((layer, colIndex) => {
            const totalHeight = layer.length * NODE_HEIGHT + (layer.length - 1) * V_GAP;
            const startY = -totalHeight / 2;
            layer.forEach((nodeId, rowIndex) => {
                positionMap[nodeId] = {
                    x: colIndex * (NODE_WIDTH + H_GAP),
                    y: startY + rowIndex * (NODE_HEIGHT + V_GAP)
                };
            });
        });
    } else {
        // 纵向布局：层 = 行，同层节点横向排列
        layers.forEach((layer, rowIndex) => {
            const totalWidth = layer.length * NODE_WIDTH + (layer.length - 1) * H_GAP;
            const startX = -totalWidth / 2;
            layer.forEach((nodeId, colIndex) => {
                positionMap[nodeId] = {
                    x: startX + colIndex * (NODE_WIDTH + H_GAP),
                    y: rowIndex * (NODE_HEIGHT + V_GAP)
                };
            });
        });
    }

    // 返回带有 position 的新节点数组
    return nodes.map(n => ({
        ...n,
        position: positionMap[n.id] || { x: 0, y: 0 }
    }));
}

/**
 * 对 Vue-Flow 格式的节点进行自动布局
 * @param {Array} vfNodes - Vue-Flow Node 数组（含 id、position 字段）
 * @param {Array} vfEdges - Vue-Flow Edge 数组（含 source、target 字段）
 * @param {'horizontal'|'vertical'} direction - 布局方向
 * @returns {Array} 带有更新后 position 的 Vue-Flow 节点数组
 */
export function autoLayoutVueFlow(vfNodes, vfEdges, direction = 'horizontal') {
    // 将 Vue-Flow 格式转换为内部格式
    const nodes = vfNodes.map(n => ({ id: n.id }));
    const edges = vfEdges.map(e => ({ from: e.source, to: e.target }));

    const laid = autoLayout(nodes, edges, direction);
    const posMap = {};
    laid.forEach(n => { posMap[n.id] = n.position; });

    return vfNodes.map(n => ({
        ...n,
        position: posMap[n.id] || n.position || { x: 0, y: 0 }
    }));
}
