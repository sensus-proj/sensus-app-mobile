#!/usr/bin/env python3
"""check_coverage.py - Valida el umbral de cobertura Kover.

Reglas:
  RF-03-08  Falla si la cobertura de LINEAS del reporte Kover es menor al umbral.
            Umbral por defecto: 80% (mandatorio para todo codigo nuevo en unitarias, integracion y E2E).
            Con --require-report, falla si el reporte falta o no contiene cobertura medible.

Uso:
    python scripts/check_coverage.py [--root .] [--min 80] [--advisory] [--report <xml>] [--require-report]

Salida:
    OK|FAIL <ruta> <cobertura>% (<umbral>%)
    PASS|FAIL|SKIP
"""

from __future__ import annotations

import argparse
import sys
from pathlib import Path
from xml.parsers import expat

DEFAULT_REPORTS = (
    Path("app/build/reports/kover/reportDebug.xml"),
    Path("app/build/reports/kover/report.xml"),
    Path("app/build/reports/kover/reportRelease.xml"),
)


def read_line_coverage(report: Path) -> tuple[int, int] | None:
    parser = expat.ParserCreate()
    depth = 0
    result = None

    def start(name: str, attributes: dict[str, str]) -> None:
        nonlocal depth, result
        depth += 1
        if depth == 2 and name == "counter" and attributes.get("type") == "LINE" and result is None:
            covered = int(attributes["covered"])
            missed = int(attributes["missed"])
            if min(covered, missed) >= 0:
                result = covered, missed

    def end(_name: str) -> None:
        nonlocal depth
        depth -= 1

    def reject_doctype(*_args: object) -> None:
        raise ValueError("No se permiten declaraciones DTD en el reporte Kover")

    parser.StartElementHandler = start
    parser.EndElementHandler = end
    parser.StartDoctypeDeclHandler = reject_doctype
    try:
        with report.open("rb") as stream:
            parser.ParseFile(stream)
    except (expat.ExpatError, KeyError, OSError, ValueError):
        return None
    return result


def main() -> int:
    parser = argparse.ArgumentParser(description="Valida cobertura Kover.")
    parser.add_argument("--root", default=".", help="Raiz del repositorio (default: .)")
    parser.add_argument("--min", type=float, default=80.0, help="Umbral minimo (default: 80)")
    parser.add_argument("--advisory", action="store_true", help="No falla, solo informa")
    parser.add_argument("--report", default=None, help="Ruta explicita al XML de Kover")
    parser.add_argument("--require-report", action="store_true", help="Falla si falta un reporte de cobertura válido")
    args = parser.parse_args()

    root = Path(args.root).resolve()
    candidates = [root / args.report] if args.report else [root / p for p in DEFAULT_REPORTS]

    report = next((path for path in candidates if path.is_file()), None)
    result = read_line_coverage(report) if report is not None else None
    if result is None or sum(result) == 0:
        status = "FAIL" if args.require_report else "SKIP"
        print(f"{status} (sin reporte Kover con cobertura de líneas medible)")
        return int(args.require_report)

    covered, missed = result
    total = covered + missed
    percentage = covered * 100.0 / total
    status = "OK" if percentage >= args.min else "FAIL"
    print(f"{status} {report.as_posix()} {percentage:.2f}% (umbral {args.min:.2f}%)")

    if percentage >= args.min:
        print("PASS")
        return 0

    if args.advisory:
        print("ADVISORY (no bloquea)")
        return 0

    print("FAIL")
    return 1


if __name__ == "__main__":
    sys.exit(main())
