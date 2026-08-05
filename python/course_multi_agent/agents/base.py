from __future__ import annotations

from course_multi_agent.llm import OpenAICompatibleChatClient


class BaseAgent:
    def __init__(self, client: OpenAICompatibleChatClient, language: str = "zh-CN") -> None:
        self.client = client
        self.language = language or "zh-CN"

