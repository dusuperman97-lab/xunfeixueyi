from __future__ import annotations

import json
from typing import Iterable


def _json_block(data: object) -> str:
    return json.dumps(data, ensure_ascii=False, indent=2)


def course_outline_system_prompt(language: str) -> str:
    return (
        "You are the Task Planning Specialist inside a course-generation multi-agent system. "
        "Read the learner request and return strict JSON only. "
        "Design a practical learning path with clear progression. "
        f"Write the course name and description in {language}. "
        "Return an object with keys: courseName, description, level, duration, style, outline. "
        "outline must be an array of objects with keys: chapter, chapterDesc, sections."
    )


def course_outline_user_prompt(message: str) -> str:
    return f"Learner request:\n{message}\n\nReturn JSON only."


def section_plan_system_prompt(language: str) -> str:
    return (
        "You are the Task Planning Specialist for a section-writing workflow. "
        "Return strict JSON only. "
        f"Design a section plan in {language}. "
        "Return keys: targetAudience, learningObjectives, corePoints, mustCover, "
        "resourceQueries, videoQueries, imageQueries, narrativeStrategy, estimatedMinutes."
    )


def section_plan_user_prompt(course_name: str, course_description: str, chapter_name: str, section_name: str) -> str:
    return (
        "Create the teaching plan for one section.\n"
        f"Course: {course_name}\n"
        f"Course description: {course_description}\n"
        f"Chapter: {chapter_name}\n"
        f"Section: {section_name}\n"
        "Return JSON only."
    )


def resource_curation_system_prompt(language: str) -> str:
    return (
        "You are the Learning Resource Specialist in a multi-agent system. "
        "Select the most useful resource candidates for the current section. "
        "Prefer official documentation, reputable educational sites, standards, and university content. "
        f"Write reasons in {language}. "
        "Return strict JSON only with key resources. "
        "resources must be an array of objects with keys: title, url, snippet, reason, selected."
    )


def resource_curation_user_prompt(plan_payload: dict, resource_candidates: Iterable[dict], video_candidates: Iterable[dict], image_candidates: Iterable[dict]) -> str:
    return (
        "Section plan:\n"
        f"{_json_block(plan_payload)}\n\n"
        "Resource candidates:\n"
        f"{_json_block(list(resource_candidates))}\n\n"
        "Video candidates:\n"
        f"{_json_block(list(video_candidates))}\n\n"
        "Image candidates:\n"
        f"{_json_block(list(image_candidates))}\n\n"
        "Return JSON only."
    )


def section_assembly_system_prompt(language: str) -> str:
    return (
        "You are the Content Assembly Specialist in a multi-agent learning workflow. "
        "Produce polished lesson markdown grounded in the approved resources only. "
        f"Write all teaching content in {language}. "
        "Do not invent external links that were not provided. "
        "Make the lesson readable, layered, and suitable for direct rendering on a learning platform. "
        "Use markdown headings, short paragraphs, bullets, and tables only when helpful. "
        "Use **strong emphasis** for key ideas. "
        "Return strict JSON only with keys: contentMarkdown, durationSeconds, "
        "selectedVideoUrl, selectedImageUrl, videoSearchKeyword, imageSearchKeyword."
    )


def section_assembly_user_prompt(request_payload: dict, plan_payload: dict, verified_resources: Iterable[dict], verification_summary: str) -> str:
    return (
        "Section request:\n"
        f"{_json_block(request_payload)}\n\n"
        "Section plan:\n"
        f"{_json_block(plan_payload)}\n\n"
        "Verified resources:\n"
        f"{_json_block(list(verified_resources))}\n\n"
        f"Verification summary:\n{verification_summary}\n\n"
        "Return JSON only."
    )

