package com.project.platform.agents.tools;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 神经网络可视化工具
 * <p>
 * 当用户问到关于神经网络、前向传播、反向传播、激活函数、权重更新、
 * 深度学习模型结构等相关问题时，AI 会自动调用此工具，获取神经网络
 * 可视化代码生成规则。AI 按照规则生成完整的单文件 HTML
 * （Canvas 2D + 状态机模式），前端检测到后以 iframe 沙箱渲染。
 *
 * @author duyongchao
 * @date 2026/7/31
 */
@Slf4j
@Component
public class NeuralNetworkVisualTool {

    /**
     * 神经网络前向传播可视化代码生成规则
     */
    private static final String NN_CODEGEN_RULES = """
            渲染类型：HTML（2D 动态可视化 - 神经网络前向传播）。输出一个完整、自包含的单文件 HTML 页面。
            无外部依赖，使用 Canvas 2D 渲染。

            架构规范（必须遵守）：
            - 状态机模式：先预生成所有步骤状态（generateStates() -> [state0, state1, ...]），再逐帧播放。
              禁止边算边画。单次播放周期内状态数组只生成一次；循环播放时由 reset() 重新生成。
            - 每个状态是不可变快照：{ layers, activations, weights, biases, currentLayer, currentNeuron, narration }
              layers: 网络结构数组，如 [3, 4, 2] 表示输入层3个神经元、隐藏层4个、输出层2个
              activations: 各层当前激活值二维数组，如 [[0.5, 0.8, 0.3], [0, 0, 0, 0], [0, 0]]
              weights: 权重矩阵，weights[l][j][i] 表示第 l 层第 j 个神经元与上一层第 i 个神经元的连接权重
              biases: 偏置数组，biases[l][j] 表示第 l 层第 j 个神经元的偏置
              currentLayer: 当前正在计算的层索引
              currentNeuron: 当前正在计算的神经元索引
              narration: 当前步骤的中文讲解文字
            - 状态生成和渲染严格分离：生成函数只产出状态数组，渲染函数只读取状态绘制。
            - 单个 requestAnimationFrame 循环管理所有动画，按时间间隔推进步骤，
              间隔 = 1000 / stepsPerSecond。
            - 动画暂停或完成后停止重绘（isDone 时 return，不调用 draw）。
              循环时用 setTimeout 延迟重置。

            神经网络前向传播规范：
            - 网络结构：根据用户需求确定层数和每层神经元数量（默认：输入层→隐藏层→输出层）
            - 前向传播逻辑：
              1. 输入层接收输入数据（用户指定或随机生成）
              2. 对每个隐藏层/输出层神经元：
                 a. 计算加权求和：z = Σ(weight × input) + bias
                 b. 应用激活函数：a = activation(z)
              3. 激活函数可选：sigmoid、ReLU、tanh（默认 sigmoid）
              4. 逐层传播直到输出层产生最终结果
            - 必须展示：
              * 网络拓扑结构（神经元用圆圈表示，连接用线条表示）
              * 当前正在计算的神经元高亮
              * 权重用连接线粗细/颜色表示大小（正权重暖色，负权重冷色）
              * 激活值用神经元颜色深浅/半径大小表示
              * 数据流动方向箭头（从左到右）
              * 当前步骤的数学公式展示（z = w1·x1 + w2·x2 + ... + b）

            Canvas 渲染：
            - 必须 DPR 适配：canvas.width = logicalWidth * dpr
            - CSS 尺寸用 style.width = '100%'
            - 神经元布局：各层从左到右排列，层内神经元垂直居中分布
            - neuronRadius = 18~25px，层间距 = canvasWidth / (layerCount + 1)
            - 神经元绘制：圆形，内部显示激活值（保留2位小数）
            - 连接线绘制：从前一层各神经元到当前神经元的线条
              * 线宽 = Math.abs(weight) * scaleFactor
              * 正权重：暖色（橙/红），负权重：冷色（蓝/青）
              * 已传播的连接高亮，未传播的半透明
            - 数据流动画：用脉冲粒子沿连接线流动表示数据传输
            - 当前计算神经元：高亮边框 + 脉冲动画
            - 数学公式区域：在画布下方或侧边显示当前步骤的计算公式

            视觉规范：
            - 使用 CSS 变量管理配色，支持 prefers-color-scheme 暗色模式
            - body 背景透明，不要 .card 包裹层、不要额外 border/background
            - 必须包含：
              * 标题+副标题（网络结构如"3-4-2 全连接网络"+ 激活函数名）
              * 图例（颜色含义说明：正权重/负权重/已激活/待计算/当前计算）
              * 进度条
              * 步骤计数器（当前步/总步数）
              * 播放控制：播放/暂停、单步前进/后退、重置、速度调节
              * 网络结构参数显示（层数、各层神经元数、激活函数）
              * 当前步骤数学公式展示
              * narration 讲解文字区域（每步解释"在计算什么"）
              * 输出结果展示（最终分类/回归结果）

            教学规范：
            - 每个关键步骤必须有 narration 讲解文字，同步显示在可视化区域
            - 讲解要解释"为什么"，如"为什么用sigmoid""权重的作用是什么"，长度不超过 80 字
            - 显示网络结构、激活函数名称、前向传播公式
            - 知识点：加权求和、激活函数、非线性变换、层间信息传递

            禁止事项：
            - 禁止外部 <script src> 引用（单文件无依赖原则）
            - 禁止 eval、Function 构造器、fetch、XMLHttpRequest
            - 禁止单次播放周期内重复生成状态
            - 禁止空闲时持续重绘
            - 禁止缺少 narration 或图例
            - 禁止容器嵌套（方框套方框）
            """;

    /**
     * 反向传播附加规则
     */
    private static final String BACKPROP_CODEGEN_RULES = """
            当需要反向传播可视化时，在前向传播规则基础上做以下调整：
            - 状态机新增反向传播阶段：前向传播完成后，计算损失函数，再逐层反向传播梯度
            - 每个状态新增字段：{ gradients, weightGradients, currentBackwardLayer, lossValue }
            - 反向传播逻辑：
              1. 计算输出层误差：delta = predicted - actual
              2. 逐层反向计算梯度：delta_l = (W_{l+1}^T · delta_{l+1}) ⊙ activation'(z_l)
              3. 权重梯度：dW = delta · a_{l-1}^T
              4. 偏置梯度：db = delta
            - 梯度可视化：用箭头方向反转表示反向传播，梯度大小用颜色深浅表示
            - narration 要强调"梯度下降""链式法则""损失函数"的概念
            - 最终显示权重更新前后的对比
            """;

    /**
     * 神经网络可视化工具
     * <p>
     * 当用户提到神经网络、前向传播、反向传播、激活函数、深度学习模型等关键词时调用此工具。
     * 工具返回神经网络可视化代码生成的专业规则，AI 收到后按照规则生成完整的单文件 HTML 代码。
     *
     * @param networkRequest 用户要可视化的神经网络场景描述
     * @return 神经网络可视化代码生成规则，供 AI 生成 HTML 代码
     */
    @Tool(
            name = "neuralNetworkVisual",
            value = "生成神经网络交互演示场景。当用户问到关于神经网络、前向传播、反向传播、" +
                    "激活函数、权重、偏置、深度学习模型结构、全连接网络、多层感知机(MLP)、" +
                    "梯度下降、损失函数、链式法则等神经网络可视化/演示需求时，必须调用此工具。" +
                    "传入参数为用户要演示的神经网络场景描述。" +
                    "调用后，按照返回的规则生成完整的单文件HTML代码（Canvas 2D + 状态机动画），" +
                    "用```html代码块包裹返回给用户。生成的HTML包含网络拓扑结构、前向传播动画、" +
                    "神经元激活过程、权重连接可视化、播放控制和步骤讲解。"
    )
    public String generateNeuralNetworkVisual(
            @P(value = "用户要演示的神经网络场景描述。如：'展示一个3-4-2的全连接网络前向传播过程'、" +
                    "'神经网络反向传播动画演示'、'sigmoid激活函数的前向传播可视化'") String networkRequest) {

        log.info("=== 神经网络可视化工具被调用！networkRequest = [{}]", networkRequest);

        StringBuilder result = new StringBuilder();
        result.append("已接收神经网络可视化请求：").append(networkRequest).append("\n\n");
        result.append("请严格按照以下规则生成完整的神经网络交互演示 HTML 代码：\n\n");

        // 根据请求内容决定是否包含反向传播
        String lowerReq = networkRequest.toLowerCase();
        if (lowerReq.contains("反向传播") || lowerReq.contains("backprop")
                || lowerReq.contains("梯度下降") || lowerReq.contains("backpropagation")) {
            result.append("演示类型：前向传播 + 反向传播\n\n");
            result.append(NN_CODEGEN_RULES);
            result.append("\n\n").append(BACKPROP_CODEGEN_RULES);
        } else {
            result.append("演示类型：前向传播\n\n");
            result.append(NN_CODEGEN_RULES);
        }

        result.append("\n\n用户请求场景：").append(networkRequest);
        result.append("\n\n请生成完整的单文件 HTML 代码，用 ```html 代码块包裹。");
        result.append("HTML 必须自包含、无外部依赖、可直接在浏览器中运行。");
        result.append("包含网络拓扑结构渲染、前向传播过程动画、神经元激活值显示、权重连接可视化、");
        result.append("播放控制（播放/暂停/单步前进/后退/重置/速度调节）、图例、步骤计数器、");
        result.append("当前步骤数学公式展示和narration讲解文字。");
        result.append("用简短的中文文字说明神经网络原理和学习要点。");

        log.info("=== 神经网络可视化工具返回规则，AI 将据此生成 HTML 代码");
        return result.toString();
    }
}
