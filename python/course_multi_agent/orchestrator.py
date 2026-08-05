from __future__ import annotations

from typing import Callable

from course_multi_agent.agents.assembler import ContentAssemblyAgent
from course_multi_agent.agents.planner import TaskPlanningAgent
from course_multi_agent.agents.resource_fetcher import LearningResourceAgent
from course_multi_agent.agents.verifier import CredibilityVerificationAgent
from course_multi_agent.llm import OpenAICompatibleChatClient
from course_multi_agent.models import (
    CourseOutlineRequest,
    CourseOutlineResponse,
    SectionDraftRequest,
    SectionDraftResponse,
)


class CourseWorkflowCoordinator:
    def __init__(
        self,
        client: OpenAICompatibleChatClient | None = None,
        language: str = "zh-CN",
        reporter: Callable[[dict], None] | None = None,
    ) -> None:
        self.client = client or OpenAICompatibleChatClient()
        self.language = language or "zh-CN"
        self.reporter = reporter
        self.planner = TaskPlanningAgent(self.client, self.language)
        self.resource_agent = LearningResourceAgent(self.client, self.language)
        self.verifier = CredibilityVerificationAgent()
        self.assembler = ContentAssemblyAgent(self.client, self.language)

    def generate_outline(self, request: CourseOutlineRequest) -> CourseOutlineResponse:
        self._emit(
            "start_step",
            stageId="outline_generation",
            detail="任务规划专家正在生成课程章节结构",
        )
        self._emit(
            "activity",
            stageId="outline_generation",
            agentName="任务规划专家",
            agentRole="需求拆解与路径设计",
            type="analysis",
            label="课程建模",
            message=f"正在根据课程目标生成章节路径：{request.message[:60]}",
        )

        response = self.planner.generate_outline(request)

        self._emit(
            "activity",
            stageId="outline_generation",
            agentName="任务规划专家",
            agentRole="需求拆解与路径设计",
            type="result",
            label="骨架完成",
            message=f"已输出 {len(response.outline)} 个章节的课程结构。",
        )
        self._emit(
            "complete_step",
            stageId="outline_generation",
            detail=f"已生成 {len(response.outline)} 个章节的课程骨架",
        )
        return response

    def generate_section(self, request: SectionDraftRequest) -> SectionDraftResponse:
        plan = self.planner.plan_section(request)
        self._emit(
            "activity",
            stageId="demand_analysis",
            agentName="任务规划专家",
            agentRole="需求拆解与路径设计",
            type="analysis",
            label="讲义规划",
            message=f"已完成《{request.sectionName}》的讲义结构设计与目标拆解。",
        )

        curated_resources = self.resource_agent.curate(request, plan)
        self._emit(
            "activity",
            stageId="resource_collection",
            agentName="学习资源获取专家",
            agentRole="外部资料与媒体检索",
            type="result",
            label="候选筛选",
            message=f"已从候选集中筛选出 {len(curated_resources)} 条优先资源。",
        )

        self._emit(
            "start_step",
            stageId="credibility_verification",
            detail="资料可信度验证专家正在评估候选资源",
        )
        verified_resources, verification_summary = self.verifier.verify(curated_resources)
        self._emit(
            "activity",
            stageId="credibility_verification",
            agentName="资料可信度验证专家",
            agentRole="证据交叉验证",
            type="verification",
            label="可信度评估",
            message=verification_summary.summary,
        )
        self._emit(
            "complete_step",
            stageId="credibility_verification",
            detail=verification_summary.summary,
        )

        self._emit(
            "start_step",
            stageId="content_assembly",
            detail="内容整理专家正在组装讲义与媒体建议",
        )
        response = self.assembler.compose(request, plan, verified_resources, verification_summary.summary)
        self._emit(
            "activity",
            stageId="content_assembly",
            agentName="内容整理专家",
            agentRole="讲义组装与表达优化",
            type="writing",
            label="讲义成稿",
            message=f"已生成《{request.sectionName}》讲义初稿，并附带引用与媒体建议。",
        )

        if not response.videoSearchKeyword:
            response.videoSearchKeyword = next((item.keyword for item in request.videoCandidates if item.keyword), "")
        if not response.imageSearchKeyword:
            response.imageSearchKeyword = next((item.keyword for item in request.imageCandidates if item.keyword), "")

        response.selectedVideoUrl = self._coerce_media_url(
            response.selectedVideoUrl,
            request.videoCandidates,
        )
        response.selectedImageUrl = self._coerce_media_url(
            response.selectedImageUrl,
            request.imageCandidates,
        )

        if not response.contentMarkdown:
            raise RuntimeError("Content assembly returned an empty markdown payload.")

        response.contentMarkdown = self._append_reference_sections(
            response.contentMarkdown,
            verified_resources,
            verification_summary.summary,
        )
        self._emit(
            "complete_step",
            stageId="content_assembly",
            detail="讲义初稿、媒体建议与引用信息已整理完成",
        )
        return response

    def _emit(self, event: str, **payload) -> None:
        if self.reporter is None:
            return
        self.reporter({"event": event, **payload})

    def _append_reference_sections(self, markdown: str, resources, verification_summary: str) -> str:
        content = (markdown or "").rstrip()
        sections = [content]

        if resources:
            sections.append("\n\n---\n\n## 参考资源")
            for resource in resources[:4]:
                title = resource.title or "未命名资源"
                credibility = self._credibility_label(resource.credibilityLabel)
                if resource.url:
                    line = f"- [{title}]({resource.url})"
                else:
                    line = f"- {title}"
                if resource.reason:
                    line += f"（{resource.reason}）"
                line += f"（{credibility}）"
                sections.append(line)

        if verification_summary:
            sections.append("\n\n## 可信度说明")
            sections.append(f"> {verification_summary}")

        return "\n".join(part for part in sections if part)

    def _credibility_label(self, label: str) -> str:
        mapping = {
            "verified": "高可信",
            "caution": "谨慎参考",
            "low-confidence": "低可信",
        }
        return mapping.get(label or "", "已筛选")

    def _coerce_media_url(self, selected_url: str, candidates) -> str:
        candidate_urls = [item.url for item in candidates if item.url]
        if selected_url and selected_url in candidate_urls:
            return selected_url
        return candidate_urls[0] if candidate_urls else ""
