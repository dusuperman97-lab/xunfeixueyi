from __future__ import annotations

from dataclasses import asdict
from typing import List

from course_multi_agent.agents.base import BaseAgent
from course_multi_agent.models import CuratedResource, SectionDraftRequest, SectionDraftResponse, SectionPlan
from course_multi_agent.prompts import section_assembly_system_prompt, section_assembly_user_prompt


class ContentAssemblyAgent(BaseAgent):
    def compose(
        self,
        request: SectionDraftRequest,
        plan: SectionPlan,
        resources: List[CuratedResource],
        verification_summary: str,
    ) -> SectionDraftResponse:
        payload = self.client.chat_json(
            section_assembly_system_prompt(request.language),
            section_assembly_user_prompt(
                {
                    "courseName": request.courseName,
                    "courseDescription": request.courseDescription,
                    "chapterName": request.chapterName,
                    "sectionName": request.sectionName,
                    "videoCandidates": [asdict(item) for item in request.videoCandidates],
                    "imageCandidates": [asdict(item) for item in request.imageCandidates],
                },
                plan.to_dict(),
                [asdict(item) for item in resources],
                verification_summary,
            ),
        )

        return SectionDraftResponse(
            sectionName=request.sectionName,
            durationSeconds=int(payload.get("durationSeconds", max(plan.estimatedMinutes, 8) * 60) or max(plan.estimatedMinutes, 8) * 60),
            contentMarkdown=str(payload.get("contentMarkdown", "")).strip(),
            selectedVideoUrl=str(payload.get("selectedVideoUrl", "")).strip(),
            selectedImageUrl=str(payload.get("selectedImageUrl", "")).strip(),
            videoSearchKeyword=str(payload.get("videoSearchKeyword", "")).strip(),
            imageSearchKeyword=str(payload.get("imageSearchKeyword", "")).strip(),
            verificationSummary=verification_summary,
            resources=resources,
        )

