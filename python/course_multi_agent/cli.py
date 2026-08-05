from __future__ import annotations

import argparse
import json
import sys

from course_multi_agent.models import CourseOutlineRequest, SectionDraftRequest
from course_multi_agent.orchestrator import CourseWorkflowCoordinator


if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")
if hasattr(sys.stderr, "reconfigure"):
    sys.stderr.reconfigure(encoding="utf-8")


def emit_progress(event: dict) -> None:
    sys.stderr.write("PROGRESS " + json.dumps(event, ensure_ascii=False) + "\n")
    sys.stderr.flush()


def main() -> int:
    parser = argparse.ArgumentParser(description="Course multi-agent orchestration entrypoint")
    parser.add_argument("command", choices=("outline", "section"))
    args = parser.parse_args()

    payload = json.load(sys.stdin)
    coordinator = CourseWorkflowCoordinator(
        language=str(payload.get("language", "zh-CN")),
        reporter=emit_progress,
    )

    if args.command == "outline":
        request = CourseOutlineRequest.from_dict(payload)
        response = coordinator.generate_outline(request)
        sys.stdout.write(json.dumps(response.to_dict(), ensure_ascii=False))
        return 0

    request = SectionDraftRequest.from_dict(payload)
    response = coordinator.generate_section(request)
    sys.stdout.write(json.dumps(response.to_dict(), ensure_ascii=False))
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except Exception as exc:
        sys.stderr.write(f"{exc}\n")
        raise SystemExit(1)
