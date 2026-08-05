from __future__ import annotations

from dataclasses import asdict
from typing import List

from course_multi_agent.agents.base import BaseAgent
from course_multi_agent.models import CuratedResource, ResourceCandidate, SectionDraftRequest, SectionPlan
from course_multi_agent.prompts import resource_curation_system_prompt, resource_curation_user_prompt


class LearningResourceAgent(BaseAgent):
    def curate(self, request: SectionDraftRequest, plan: SectionPlan) -> List[CuratedResource]:
        if not request.resourceCandidates:
            return []

        try:
            payload = self.client.chat_json(
                resource_curation_system_prompt(request.language),
                resource_curation_user_prompt(
                    plan.to_dict(),
                    [asdict(item) for item in request.resourceCandidates],
                    [asdict(item) for item in request.videoCandidates],
                    [asdict(item) for item in request.imageCandidates],
                ),
            )
            resources = [CuratedResource.from_dict(item) for item in payload.get("resources", [])]
            selected = [item for item in resources if item.selected and item.title]
            if selected:
                return selected
        except Exception:
            pass

        return self._fallback_selection(request.resourceCandidates)

    def _fallback_selection(self, candidates: List[ResourceCandidate]) -> List[CuratedResource]:
        top_candidates = sorted(candidates, key=lambda item: item.score, reverse=True)[:4]
        return [
            CuratedResource(
                title=item.title,
                url=item.url,
                snippet=item.snippet,
                authorityScore=float(item.score or 0.0),
                credibilityLabel="caution",
                reason="Selected by fallback ranking from pre-fetched search results.",
                selected=True,
            )
            for item in top_candidates
            if item.title
        ]

