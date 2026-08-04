package com.project.platform.agents.tools;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Three.js 3D 建模工具
 * <p>
 * 当用户问到关于 3D 建模、3D 场景、Three.js 可视化等相关问题时，
 * AI 会自动调用此工具，获取 Three.js 代码生成规则。
 * 随后 AI 会按照这些规则生成完整的单文件 HTML（含 Three.js 3D 场景），
 * 前端检测到后以 iframe 沙箱渲染。
 * <p>
 * 提示词规则移植自 CloudAI-X/threejs-skills 及 VisualPrompts.CODEGEN_RULES_THREEJS。
 *
 * @author duyongchao
 * @date 2026/7/31
 */
@Slf4j
@Component
public class ThreejsModelingTool {

    /**
     * Three.js 代码生成规则（供 AI 模型遵循）
     */
    private static final String THREEJS_CODEGEN_RULES = """
            渲染类型：HTML（Three.js 3D 交互场景）。输出完整单文件 HTML。
            - 用 <script src="https://cdnjs.cloudflare.com/ajax/libs/three.js/r128/three.min.js">
              加载 Three.js（非 ES module 方式，兼容 file:// 协议）
            - 加载 OrbitControls：
              <script src="https://cdn.jsdelivr.net/npm/three@0.128.0/examples/js/controls/OrbitControls.js">
            - 用户输入在「prompt」参数中，包含要创建的 3D 场景描述
            - 必须包含完整三件套：Scene -> Camera -> Renderer -> animate loop
            - 场景深色背景（0x000011 或相近），自适应 resize
            - 使用 requestAnimationFrame 驱动动画循环
            - 用中文添加场景说明标签（绝对定位覆盖层）
            - 遵守以下规则：
              1. 几何体用 SphereGeometry / BoxGeometry / CylinderGeometry 等构造
              2. 材质用 MeshStandardMaterial（PBR），设 roughness/metalness
              3. 光照：AmbientLight + DirectionalLight/PointLight 组合
              4. 土星环：TorusGeometry + MeshStandardMaterial(DoubleSide)
              5. 轨道：Line + BufferGeometry.setFromPoints(points)，半透明
              6. 发光效果：Sprite + CanvasTexture + AdditiveBlending
              7. 星空背景：Points + PointsMaterial + BufferGeometry 随机球面分布
              8. 每个独立物体用 Group 组织，方便整体变换
              9. 交互：OrbitControls 允许鼠标拖拽旋转、滚轮缩放
             10. 性能：几何体复用、材质共享、合理的远近平面设置
            """;

    /**
     * Three.js 3D 场景生成工具
     * <p>
     * 当用户提到 3D 建模、3D 模型、三维场景、Three.js、WebGL 可视化等关键词时调用此工具。
     * 工具返回 Three.js 代码生成的专业规则，AI 收到后按照规则生成完整的单文件 HTML 代码。
     *
     * @param sceneDescription 用户要创建的 3D 场景描述，如"太阳系"、"DNA双螺旋"、"分子结构"等
     * @return Three.js 代码生成规则和场景描述，供 AI 生成 HTML 代码
     */
    @Tool(
            name = "threejsModeling",
            value = "生成3D交互场景。当用户问到关于3D建模、3D模型、三维场景、Three.js、" +
                    "WebGL可视化、3D动画、立体结构（如太阳系、分子结构、DNA双螺旋、建筑3D模型等）" +
                    "相关问题时，必须调用此工具。传入参数为用户要创建的3D场景的详细描述。" +
                    "调用后，按照返回的规则生成完整的单文件HTML代码（包含Three.js 3D场景），" +
                    "用```html代码块包裹返回给用户。"
    )
    public String generate3DScene(
            @P(value = "用户要创建的3D场景的详细描述，包括场景中包含的物体、颜色、动画效果等。如：'太阳系，包含太阳和八大行星，行星按轨道运行，有星空背景'") String sceneDescription) {

        log.info("=== Three.js 3D建模工具被调用！sceneDescription = [{}]", sceneDescription);

        StringBuilder result = new StringBuilder();
        result.append("已接收3D场景创建请求：").append(sceneDescription).append("\n\n");
        result.append("请严格按照以下规则生成完整的 Three.js 3D 交互场景 HTML 代码：\n\n");
        result.append(THREEJS_CODEGEN_RULES);
        result.append("\n\n场景描述：").append(sceneDescription);
        result.append("\n\n请生成完整的单文件 HTML 代码，用 ```html 代码块包裹。");
        result.append("HTML 必须自包含、可直接在浏览器中运行，包含场景搭建、相机、渲染器、");
        result.append("动画循环、OrbitControls 交互控制，以及中文场景说明标签。");

        log.info("=== Three.js 工具返回规则，AI 将据此生成 HTML 代码");
        return result.toString();
    }
}
