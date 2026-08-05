from __future__ import annotations

from statistics import mean
from typing import List, Tuple
from urllib.parse import urlparse

from course_multi_agent.models import CuratedResource, VerificationSummary


class CredibilityVerificationAgent:
    def verify(self, resources: List[CuratedResource]) -> Tuple[List[CuratedResource], VerificationSummary]:
        if not resources:
            summary = VerificationSummary(
                summary="No external resources were approved. The content should stay conservative and concept-focused.",
                approvedCount=0,
                rejectedCount=0,
                averageScore=0.0,
            )
            return [], summary

        approved: List[CuratedResource] = []
        rejected_count = 0
        scores: List[float] = []

        for resource in resources:
            score = self._score_resource(resource.url, resource.title, resource.snippet, resource.authorityScore)
            scores.append(score)
            resource.authorityScore = round(score, 2)
            if score >= 0.82:
                resource.credibilityLabel = "verified"
                resource.reason = resource.reason or "Domain and content pattern suggest a high-confidence learning source."
                approved.append(resource)
            elif score >= 0.58:
                resource.credibilityLabel = "caution"
                resource.reason = resource.reason or "Useful as a supporting reference, but it should not be the sole authority."
                approved.append(resource)
            else:
                resource.credibilityLabel = "low-confidence"
                resource.selected = False
                resource.reason = resource.reason or "Weak domain signals or low evidence quality."
                rejected_count += 1

        average_score = round(mean(scores), 2) if scores else 0.0
        summary = VerificationSummary(
            summary=(
                f"Verified {len(approved)} candidate resources, filtered out {rejected_count}, "
                f"average authority score {average_score}."
            ),
            approvedCount=len(approved),
            rejectedCount=rejected_count,
            averageScore=average_score,
        )
        return approved, summary

    def _score_resource(self, url: str, title: str, snippet: str, base_score: float) -> float:
        score = max(0.0, min(1.0, float(base_score or 0.0)))
        host = urlparse(url).netloc.lower()
        text = f"{title} {snippet}".lower()

        if any(token in host for token in ("official", "docs", "developer", "python.org", "mozilla.org")):
            score += 0.28
        if host.endswith(".edu") or host.endswith(".gov") or host.endswith(".org"):
            score += 0.22
        if any(token in text for token in ("official", "documentation", "standard", "guide", "tutorial")):
            score += 0.12
        if any(token in host for token in ("github.io", "medium.com", "csdn.net", "zhihu.com")):
            score -= 0.08
        if not url:
            score -= 0.2

        return max(0.0, min(1.0, score))

