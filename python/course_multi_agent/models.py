from __future__ import annotations

from dataclasses import asdict, dataclass, field
from typing import Any, Dict, List, Optional


def _pick(data: Dict[str, Any], *keys: str, default: Any = None) -> Any:
    for key in keys:
        if key in data and data[key] is not None:
            return data[key]
    return default


@dataclass
class ChapterOutline:
    chapter: str
    chapterDesc: str = ""
    sections: List[str] = field(default_factory=list)

    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> "ChapterOutline":
        return cls(
            chapter=str(_pick(data, "chapter", default="")).strip(),
            chapterDesc=str(_pick(data, "chapterDesc", "chapter_desc", default="")).strip(),
            sections=[str(item).strip() for item in _pick(data, "sections", default=[]) or [] if str(item).strip()],
        )


@dataclass
class CourseOutlineRequest:
    message: str
    userId: Optional[int] = None
    language: str = "zh-CN"

    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> "CourseOutlineRequest":
        return cls(
            message=str(_pick(data, "message", default="")).strip(),
            userId=_pick(data, "userId", "user_id"),
            language=str(_pick(data, "language", default="zh-CN")).strip() or "zh-CN",
        )


@dataclass
class CourseOutlineResponse:
    courseName: str
    description: str
    level: str
    duration: str
    style: str
    outline: List[ChapterOutline] = field(default_factory=list)

    def to_dict(self) -> Dict[str, Any]:
        return asdict(self)


@dataclass
class ResourceCandidate:
    title: str
    url: str = ""
    snippet: str = ""
    score: float = 0.0
    sourceType: str = "web"
    retrievedBy: str = "java-search"

    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> "ResourceCandidate":
        return cls(
            title=str(_pick(data, "title", default="")).strip(),
            url=str(_pick(data, "url", default="")).strip(),
            snippet=str(_pick(data, "snippet", "content", default="")).strip(),
            score=float(_pick(data, "score", default=0.0) or 0.0),
            sourceType=str(_pick(data, "sourceType", "source_type", default="web")).strip() or "web",
            retrievedBy=str(_pick(data, "retrievedBy", "retrieved_by", default="java-search")).strip() or "java-search",
        )


@dataclass
class MediaCandidate:
    keyword: str
    url: str = ""
    title: str = ""
    source: str = ""
    kind: str = ""

    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> "MediaCandidate":
        return cls(
            keyword=str(_pick(data, "keyword", default="")).strip(),
            url=str(_pick(data, "url", default="")).strip(),
            title=str(_pick(data, "title", default="")).strip(),
            source=str(_pick(data, "source", default="")).strip(),
            kind=str(_pick(data, "kind", default="")).strip(),
        )


@dataclass
class SectionDraftRequest:
    courseName: str
    courseDescription: str
    chapterName: str
    sectionName: str
    language: str = "zh-CN"
    resourceCandidates: List[ResourceCandidate] = field(default_factory=list)
    videoCandidates: List[MediaCandidate] = field(default_factory=list)
    imageCandidates: List[MediaCandidate] = field(default_factory=list)

    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> "SectionDraftRequest":
        return cls(
            courseName=str(_pick(data, "courseName", "course_name", default="")).strip(),
            courseDescription=str(_pick(data, "courseDescription", "course_description", default="")).strip(),
            chapterName=str(_pick(data, "chapterName", "chapter_name", default="")).strip(),
            sectionName=str(_pick(data, "sectionName", "section_name", default="")).strip(),
            language=str(_pick(data, "language", default="zh-CN")).strip() or "zh-CN",
            resourceCandidates=[ResourceCandidate.from_dict(item) for item in _pick(data, "resourceCandidates", "resource_candidates", default=[]) or []],
            videoCandidates=[MediaCandidate.from_dict(item) for item in _pick(data, "videoCandidates", "video_candidates", default=[]) or []],
            imageCandidates=[MediaCandidate.from_dict(item) for item in _pick(data, "imageCandidates", "image_candidates", default=[]) or []],
        )


@dataclass
class SectionPlan:
    targetAudience: str = "beginner"
    learningObjectives: List[str] = field(default_factory=list)
    corePoints: List[str] = field(default_factory=list)
    mustCover: List[str] = field(default_factory=list)
    resourceQueries: List[str] = field(default_factory=list)
    videoQueries: List[str] = field(default_factory=list)
    imageQueries: List[str] = field(default_factory=list)
    narrativeStrategy: str = ""
    estimatedMinutes: int = 15

    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> "SectionPlan":
        return cls(
            targetAudience=str(_pick(data, "targetAudience", "target_audience", default="beginner")).strip() or "beginner",
            learningObjectives=[str(item).strip() for item in _pick(data, "learningObjectives", "learning_objectives", default=[]) or [] if str(item).strip()],
            corePoints=[str(item).strip() for item in _pick(data, "corePoints", "core_points", default=[]) or [] if str(item).strip()],
            mustCover=[str(item).strip() for item in _pick(data, "mustCover", "must_cover", default=[]) or [] if str(item).strip()],
            resourceQueries=[str(item).strip() for item in _pick(data, "resourceQueries", "resource_queries", default=[]) or [] if str(item).strip()],
            videoQueries=[str(item).strip() for item in _pick(data, "videoQueries", "video_queries", default=[]) or [] if str(item).strip()],
            imageQueries=[str(item).strip() for item in _pick(data, "imageQueries", "image_queries", default=[]) or [] if str(item).strip()],
            narrativeStrategy=str(_pick(data, "narrativeStrategy", "narrative_strategy", default="")).strip(),
            estimatedMinutes=int(_pick(data, "estimatedMinutes", "estimated_minutes", default=15) or 15),
        )

    def to_dict(self) -> Dict[str, Any]:
        return asdict(self)


@dataclass
class CuratedResource:
    title: str
    url: str = ""
    snippet: str = ""
    authorityScore: float = 0.0
    credibilityLabel: str = "caution"
    reason: str = ""
    selected: bool = True

    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> "CuratedResource":
        return cls(
            title=str(_pick(data, "title", default="")).strip(),
            url=str(_pick(data, "url", default="")).strip(),
            snippet=str(_pick(data, "snippet", default="")).strip(),
            authorityScore=float(_pick(data, "authorityScore", "authority_score", default=0.0) or 0.0),
            credibilityLabel=str(_pick(data, "credibilityLabel", "credibility_label", default="caution")).strip() or "caution",
            reason=str(_pick(data, "reason", default="")).strip(),
            selected=bool(_pick(data, "selected", default=True)),
        )


@dataclass
class VerificationSummary:
    summary: str
    approvedCount: int
    rejectedCount: int
    averageScore: float

    def to_dict(self) -> Dict[str, Any]:
        return asdict(self)


@dataclass
class SectionDraftResponse:
    sectionName: str
    durationSeconds: int
    contentMarkdown: str
    selectedVideoUrl: str = ""
    selectedImageUrl: str = ""
    videoSearchKeyword: str = ""
    imageSearchKeyword: str = ""
    verificationSummary: str = ""
    resources: List[CuratedResource] = field(default_factory=list)

    def to_dict(self) -> Dict[str, Any]:
        return asdict(self)

