from __future__ import annotations

import json
import os
import urllib.error
import urllib.request
from dataclasses import dataclass
from typing import Any, Dict


@dataclass
class LLMConfig:
    api_base: str
    api_key: str
    model: str
    timeout_seconds: int = 90

    @classmethod
    def from_env(cls) -> "LLMConfig":
        api_base = os.getenv("COURSE_AGENT_API_BASE", "https://dashscope.aliyuncs.com/compatible-mode/v1")
        api_key = os.getenv("COURSE_AGENT_API_KEY") or os.getenv("DASHSCOPE_API_KEY", "")
        model = os.getenv("COURSE_AGENT_MODEL", "qwen-plus")
        timeout_seconds = int(os.getenv("COURSE_AGENT_TIMEOUT_SECONDS", "90"))
        return cls(
            api_base=api_base.rstrip("/"),
            api_key=api_key,
            model=model,
            timeout_seconds=timeout_seconds,
        )


class OpenAICompatibleChatClient:
    def __init__(self, config: LLMConfig | None = None) -> None:
        self.config = config or LLMConfig.from_env()

    def chat_json(self, system_prompt: str, user_prompt: str) -> Dict[str, Any]:
        if not self.config.api_key:
            raise RuntimeError("Missing model credentials. Set COURSE_AGENT_API_KEY or DASHSCOPE_API_KEY.")

        payload = {
            "model": self.config.model,
            "temperature": 0.2,
            "messages": [
                {"role": "system", "content": system_prompt},
                {"role": "user", "content": user_prompt},
            ],
        }

        try:
            raw = self._post_json({**payload, "response_format": {"type": "json_object"}})
        except RuntimeError as exc:
            if "HTTP 400" not in str(exc):
                raise
            raw = self._post_json(payload)

        data = json.loads(raw)
        content = data["choices"][0]["message"]["content"]
        json_text = _strip_code_fence(content)
        return json.loads(json_text)

    def _post_json(self, payload: Dict[str, Any]) -> str:
        request = urllib.request.Request(
            url=f"{self.config.api_base}/chat/completions",
            data=json.dumps(payload).encode("utf-8"),
            headers={
                "Authorization": f"Bearer {self.config.api_key}",
                "Content-Type": "application/json",
            },
            method="POST",
        )

        try:
            with urllib.request.urlopen(request, timeout=self.config.timeout_seconds) as response:
                return response.read().decode("utf-8")
        except urllib.error.HTTPError as exc:
            detail = exc.read().decode("utf-8", errors="ignore")
            raise RuntimeError(f"LLM request failed with HTTP {exc.code}: {detail}") from exc
        except urllib.error.URLError as exc:
            raise RuntimeError(f"LLM request failed: {exc.reason}") from exc


def _strip_code_fence(text: str) -> str:
    cleaned = (text or "").strip()
    if cleaned.startswith("```"):
        cleaned = cleaned.split("\n", 1)[-1]
        if cleaned.endswith("```"):
            cleaned = cleaned[:-3]
    return cleaned.strip()
