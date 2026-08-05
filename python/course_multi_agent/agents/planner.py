from __future__ import annotations

from course_multi_agent.agents.base import BaseAgent
from course_multi_agent.models import CourseOutlineRequest, CourseOutlineResponse, ChapterOutline, SectionDraftRequest, SectionPlan
from course_multi_agent.prompts import (
    course_outline_system_prompt,
    course_outline_user_prompt,
    section_plan_system_prompt,
    section_plan_user_prompt,
)


class TaskPlanningAgent(BaseAgent):
    def generate_outline(self, request: CourseOutlineRequest) -> CourseOutlineResponse:
        payload = self.client.chat_json(
            course_outline_system_prompt(request.language),
            course_outline_user_prompt(request.message),
        )
        outline = [ChapterOutline.from_dict(item) for item in payload.get("outline", [])]
        return CourseOutlineResponse(
            courseName=str(payload.get("courseName", "")).strip(),
            description=str(payload.get("description", "")).strip(),
            level=str(payload.get("level", "beginner")).strip() or "beginner",
            duration=str(payload.get("duration", "30 days")).strip() or "30 days",
            style=str(payload.get("style", "project-based")).strip() or "project-based",
            outline=outline,
        )

    def plan_section(self, request: SectionDraftRequest) -> SectionPlan:
        payload = self.client.chat_json(
            section_plan_system_prompt(request.language),
            section_plan_user_prompt(
                request.courseName,
                request.courseDescription,
                request.chapterName,
                request.sectionName,
            ),
        )
        return SectionPlan.from_dict(payload)

