#!/usr/bin/env python3
"""Summarize anonymized Sensus performance samples from a CSV file."""

from __future__ import annotations

import argparse
import csv
import math
import re
import sys
from collections.abc import Iterable
from dataclasses import dataclass, field
from pathlib import Path
from typing import TextIO


GROUP_FIELDS = ("flow", "metric", "build", "device", "network", "cache")
INPUT_FIELDS = (*GROUP_FIELDS, "outcome", "duration_ms")
BUDGETS_MS = {"feedback": 100, "usable": 2000, "confirmed": 2000, "propagated": 2000}
MIN_ATTEMPTS = 20
LABEL = re.compile(r"[A-Za-z0-9][A-Za-z0-9._-]{0,63}")


@dataclass
class Samples:
    durations: list[int] = field(default_factory=list)
    errors: int = 0
    timeouts: int = 0

    @property
    def attempts(self) -> int:
        return len(self.durations) + self.errors + self.timeouts


def record_sample(samples: Samples, outcome: str, duration: str, line: int) -> None:
    if outcome == "ok":
        try:
            milliseconds = int(duration)
        except ValueError:
            raise ValueError(f"Fila {line}: duración inválida") from None
        if milliseconds < 0:
            raise ValueError(f"Fila {line}: duración inválida")
        samples.durations.append(milliseconds)
        return

    if duration or outcome not in {"error", "timeout"}:
        raise ValueError(f"Fila {line}: resultado o duración inválidos")
    if outcome == "error":
        samples.errors += 1
    else:
        samples.timeouts += 1


def read_samples(rows: Iterable[list[str]]) -> dict[tuple[str, ...], Samples]:
    reader = iter(rows)
    if tuple(next(reader, [])) != INPUT_FIELDS:
        raise ValueError(f"Encabezado esperado: {','.join(INPUT_FIELDS)}")

    groups: dict[tuple[str, ...], Samples] = {}
    for line, row in enumerate(reader, start=2):
        if len(row) != len(INPUT_FIELDS):
            raise ValueError(f"Fila {line}: número de columnas inválido")
        *group, outcome, duration = (value.strip() for value in row)
        if not all(LABEL.fullmatch(value) for value in group) or group[1] not in BUDGETS_MS:
            raise ValueError(f"Fila {line}: condición o métrica inválida")
        if group[-2] not in {"wifi", "mobile", "offline"} or group[-1] not in {"cold", "warm"}:
            raise ValueError(f"Fila {line}: condición de red o caché inválida")
        samples = groups.setdefault(tuple(group), Samples())
        record_sample(samples, outcome, duration, line)

    if not groups:
        raise ValueError("El archivo debe contener al menos un intento")
    return groups


def write_summary(destination: TextIO, groups: dict[tuple[str, ...], Samples]) -> None:
    writer = csv.writer(destination, lineterminator="\n")
    writer.writerow((
        *GROUP_FIELDS, "attempts", "errors", "timeouts", "p50_ok_ms", "p95_ok_ms",
        "budget_ms", "within_budget", "within_budget_pct", "status",
    ))
    for group, samples in sorted(groups.items()):
        durations = sorted(samples.durations)
        count = len(durations)
        p50 = durations[(count - 1) // 2] if count else ""
        p95 = durations[math.ceil(0.95 * count) - 1] if count else ""
        budget = BUDGETS_MS[group[1]]
        within = sum(duration <= budget for duration in durations)
        writer.writerow((
            *group, samples.attempts, samples.errors, samples.timeouts, p50, p95, budget, within,
            f"{100 * within / samples.attempts:.2f}", budget_status(samples.attempts, within),
        ))


def budget_status(attempts: int, within: int) -> str:
    if attempts < MIN_ATTEMPTS:
        return "insufficient_samples"
    return "met" if 100 * within >= 95 * attempts else "missed"


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("samples", type=Path)
    args = parser.parse_args()
    try:
        with args.samples.open(encoding="utf-8-sig", newline="") as source:
            groups = read_samples(csv.reader(source, strict=True))
    except (OSError, ValueError, csv.Error) as error:
        parser.exit(2, f"Error: {error}\n")
    write_summary(sys.stdout, groups)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
