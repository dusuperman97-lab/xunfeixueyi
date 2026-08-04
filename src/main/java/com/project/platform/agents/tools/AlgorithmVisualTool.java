package com.project.platform.agents.tools;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 算法可视化工具
 * <p>
 * 当用户问到关于 DFS（深度优先搜索）、BFS（广度优先搜索）、路径规划、迷宫寻路等
 * 算法相关问题时，AI 会自动调用此工具，获取算法可视化代码生成规则。
 * AI 按照规则生成完整的单文件 HTML（Canvas 2D + 状态机模式），
 * 前端检测到后以 iframe 沙箱渲染。
 * <p>
 * 提示词规则参考 VisualPrompts.CODEGEN_RULES_VISUALIZATION。
 *
 * @author duyongchao
 * @date 2026/7/31
 */
@Slf4j
@Component
public class AlgorithmVisualTool {

    /**
     * DFS 走地图/迷宫可视化代码生成规则
     */
    private static final String DFS_CODEGEN_RULES = """
            渲染类型：HTML（2D 动态可视化 - DFS 迷宫寻路）。输出一个完整、自包含的单文件 HTML 页面。
            无外部依赖，使用 Canvas 2D 渲染。

            架构规范（必须遵守）：
            - 状态机模式：先预生成所有步骤状态（generateStates() -> [state0, state1, ...]），再逐帧播放。
              禁止边算边画。单次播放周期内状态数组只生成一次；循环播放时由 reset() 重新生成。
            - 每个状态是不可变快照：{ grid, currentPos, visitedPath, backtrackedPath, stack, narration }
              grid: 二维数组，0=通路，1=墙壁，2=起点，3=终点
              currentPos: 当前探索的格子坐标 {row, col}
              visitedPath: 已访问路径数组 [{row, col}, ...]
              backtrackedPath: 已回溯的路径数组
              stack: 当前递归栈/显式栈内容 [{row, col}, ...]
              narration: 当前步骤的中文讲解文字
            - 状态生成和渲染严格分离：生成函数只产出状态数组，渲染函数只读取状态绘制。
            - 单个 requestAnimationFrame 循环管理所有动画，按时间间隔推进步骤，
              间隔 = 1000 / stepsPerSecond。
            - 动画暂停或完成后停止重绘（isDone 时 return，不调用 draw）。
              循环时用 setTimeout 延迟重置。
            - 已访问标记必须累积（visitedPath 只添加不重置）。

            DFS 迷宫算法规范：
            - 迷宫生成：可使用预设迷宫或随机生成（递归分割法或 DFS 生成法）
            - DFS 寻路逻辑：
              1. 从起点出发，将起点压入栈
              2. 查看当前格子的四个方向（上、右、下、左）的邻居
              3. 找到一个未访问的通路邻居，移动到该邻居，压入栈，标记为已访问
              4. 如果四个方向都没有未访问的通路邻居，则回溯（弹出栈顶）
              5. 重复 2-4 直到到达终点或栈为空
            - 方向优先级：上→右→下→左（可配置）
            - 必须展示：递归栈/显式栈的实时状态、当前探索位置、已访问路径、回溯路径

            Canvas 渲染：
            - 必须 DPR 适配：canvas.width = logicalWidth * dpr
            - CSS 尺寸用 style.width = '100%'，用 aspect-ratio 保持比例
            - 格子大小自适应：cellSize = canvasWidth / gridCols
            - 每个格子绘制：墙壁(深色填充)、通路(浅色)、起点(绿色)、终点(红色)
            - 已访问路径：半透明蓝色覆盖
            - 当前位置：高亮边框 + 脉冲动画
            - 回溯路径：半透明灰色虚线标记
            - 栈可视化：在画布右侧或下方绘制栈的实时状态（竖向排列格子坐标）

            视觉规范：
            - 使用 CSS 变量管理配色，支持 prefers-color-scheme 暗色模式
            - body 背景透明，不要 .card 包裹层、不要额外 border/background
            - 必须包含：
              * 标题+副标题（算法名+复杂度 O(V+E)）
              * 图例（颜色含义说明：墙壁/通路/起点/终点/已访问/当前/回溯）
              * 进度条
              * 步骤计数器（当前步/总步数）
              * 播放控制：播放/暂停、单步前进/后退、重置、速度调节
              * 栈状态实时显示
              * narration 讲解文字区域（每步解释"为什么"）

            教学规范：
            - 每个关键步骤必须有 narration 讲解文字，同步显示在可视化区域
            - 讲解要解释"为什么"，不是"是什么"，长度不超过 80 字
            - 显示算法名称（深度优先搜索 DFS）、时间复杂度 O(V+E)、空间复杂度 O(V)
            - 算法特点：不一定找到最短路径、适合迷宫类问题、可能需要回溯

            禁止事项：
            - 禁止外部 <script src> 引用（单文件无依赖原则）
            - 禁止 eval、Function 构造器、fetch、XMLHttpRequest
            - 禁止单次播放周期内重复生成状态
            - 禁止空闲时持续重绘
            - 禁止缺少 narration 或图例
            - 禁止容器嵌套（方框套方框）
            """;

    /**
     * BFS 走地图/迷宫可视化代码生成规则（附加）
     */
    private static final String BFS_CODEGEN_RULES = """
            当需要 BFS 可视化时，在 DFS 规则基础上做以下调整：
            - 数据结构：用队列（Queue）代替栈
            - 探索逻辑：从队列头部取出当前格子，将所有未访问的通路邻居加入队列尾部
            - 方向优先级：上→右→下→左
            - BFS 能找到最短路径（无权图），时间复杂度 O(V+E)
            - 栈可视化改为队列可视化（横向排列）
            - narration 要强调"层序遍历""最短路径"的特点
            """;

    /**
     * DFS 算法可视化工具
     * <p>
     * 当用户提到 DFS、深度优先搜索、迷宫寻路、走地图、路径规划等关键词时调用此工具。
     * 工具返回算法可视化代码生成的专业规则，AI 收到后按照规则生成完整的单文件 HTML 代码。
     *
     * @param algorithmRequest 用户要可视化的算法描述，如"DFS走迷宫"、"深度优先搜索寻路"等
     * @return 算法可视化代码生成规则，供 AI 生成 HTML 代码
     */
    @Tool(
            name = "algorithmVisual",
            value = "生成算法交互演示场景。当用户问到关于DFS（深度优先搜索）、BFS（广度优先搜索）、" +
                    "迷宫寻路、走地图、路径规划、图遍历等算法可视化/演示需求时，必须调用此工具。" +
                    "传入参数为用户要演示的算法和场景描述。" +
                    "调用后，按照返回的规则生成完整的单文件HTML代码（Canvas 2D + 状态机动画），" +
                    "用```html代码块包裹返回给用户。生成的HTML包含迷宫地图、DFS搜索过程动画、" +
                    "播放控制（播放/暂停/单步/速度）、栈状态显示和步骤讲解。"
    )
    public String generateAlgorithmVisual(
            @P(value = "用户要演示的算法和场景描述。如：'用DFS走一个10x10的迷宫，从左上角到右下角'、" +
                    "'深度优先搜索路径规划演示'、'BFS最短路径迷宫寻路'") String algorithmRequest) {

        log.info("=== 算法可视化工具被调用！algorithmRequest = [{}]", algorithmRequest);

        StringBuilder result = new StringBuilder();
        result.append("已接收算法可视化请求：").append(algorithmRequest).append("\n\n");
        result.append("请严格按照以下规则生成完整的算法交互演示 HTML 代码：\n\n");

        // 根据请求内容决定使用 DFS 还是 BFS 规则
        String lowerReq = algorithmRequest.toLowerCase();
        if (lowerReq.contains("bfs") || lowerReq.contains("广度优先") || lowerReq.contains("最短路径")) {
            result.append("算法类型：BFS（广度优先搜索）\n\n");
            result.append(DFS_CODEGEN_RULES);
            result.append("\n\n").append(BFS_CODEGEN_RULES);
        } else {
            result.append("算法类型：DFS（深度优先搜索）\n\n");
            result.append(DFS_CODEGEN_RULES);
        }

        result.append("\n\n用户请求场景：").append(algorithmRequest);
        result.append("\n\n请生成完整的单文件 HTML 代码，用 ```html 代码块包裹。");
        result.append("HTML 必须自包含、无外部依赖、可直接在浏览器中运行。");
        result.append("包含迷宫地图渲染、DFS/BFS搜索过程动画、播放控制（播放/暂停/单步前进/后退/重置/速度调节）、");
        result.append("栈/队列状态实时显示、图例、步骤计数器和narration讲解文字。");
        result.append("用简短的中文文字说明算法原理和学习要点。");

        log.info("=== 算法可视化工具返回规则，AI 将据此生成 HTML 代码");
        return result.toString();
    }
}
