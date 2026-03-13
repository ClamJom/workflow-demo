# 开发手册

## 节点开发

1. 节点应当继承`com.example.demoworkflow.utils.workflow.nodes.NodeImpl`，并在相应的Hook中实现对应的功能。

2. 节点前置Hook（`before`）。应当在此处加载节点的配置相关信息，通过用户的配置项初始化节点运行所需的所有变量，当节点配置不满足运行节点的需求时，应当调用`onNodeError`方法或`onNodeDisabled`方法以终止流程或当前所在分支的运行。

3. 节点运行Hook（`run`）。在此处实现节点运行的核心逻辑，并将运行中可能需要向用户输出的结果通过`globalPool.pushWorkflowResult`方法输出；需要在运行结束后写入全局变量的数据应当通过`nodePool.put`方法写入，而不是直接向`globalPool`写入。

4. 节点后置Hook（`after`）。在此清理运行时产生的对象或过滤节点运行结果。

5. 节点在创建后，应当在`com.example.demoworkflow.utils.types.NodeType`中写入一个新的节点类型，并在静态方法中将新建的节点类放入`nodeClazzMap`。新建结果类型后，其相应的结果类型也应当在新建的节点初始化函数中使用`setNodeType`方法指定。

6. 节点若有默认的配置项，应当重写`getNodeConfigs`方法，便于前端获取使用。

7. 节点若有默认的输出项，应当重写`getNodeOutputs`方法，便于前端获取使用。

## 状态管理

1. 新的状态根据状态所属的实体类型（工作流、节点、结果处理），在相应的状态类中新建。其中，最好是按位划分状态，使得在状态使用时可以直接通过位运算符判断运行状态。其中，`-1`与`0x1`为预留的`NULL`态与`ERROR`态，分别代表“空”和“错误”，不应当对其进行更改。
    ```java
    // 判断节点状态是否处于失能态或完成态
    return state & (NodeStates.DONE | NodeStates.DISABLED) != 0;
    ```

2. 状态代码在定义时应当写为16进制数，状态更新时不要直接通过数值直接更新，否则将会带来维护困难。

3. 状态代码应当依照执行顺序逐渐变大，这样在可以通过左移运算直接更新状态信息。


## 配置类型

1. 现有的配置参考`com.example.demoworkflow.utils.workflow.nodes.NodeImpl`中的`parseConfig`方法，新增任意类型时需要更新相应的解析方法。

2. 前端需要提供相应的配置编辑方法，如`Boolean`对应开关、`List`与`Map`对应可拓展表、`Condition`对应一个含有左值，右值，运算符的条件编辑器、`Number`与`String`对应相应的输入框、`Select`对应下拉框，其中`Number`应当含有高级选项以配置取值区间与精度和浮点除数的选项。

3. 节点配置与节点的输出结果应当通过接口获取并动态生成。接口位于`com.example.demoworkflow.control.WorkflowController`，方法分别为`getNodeConfigsByCode`与`getNodeOutputsByCode`。

## Vue-Flow节点

1. 根据后端定义的节点类型，前端应当定义相应的Vue-flow节点。其中`StartNode`与`EndNode`应当显示为绿色与橙色的矩形，其它节点均为白色矩形。普通的工作节点应当至少含有一个Handler（即输出端）可供多个其它节点接入，但条件节点应根据条件数量设置Handler数量。节点不允许自指（从节点出发创建边指向自己）。

2. 节点在创建后应在初始化时便在相应的对象中创建`nodeId`（前端创建）。

3. 节点的`String`类型的配置应当通过`{{nodeId:variableName}}`的格式插入全局变量池中的变量，在前端中应当只能够提取当前分支以及更上游节点产生的变量，不允许插入并行分支的变量。

4. 每个工作流只允许拥有一个起始节点与一个结束节点。

5. 节点的连接应当允许使用快捷键`C`选中连接，节点与边的删除应当能通过`Del`键快捷删除。