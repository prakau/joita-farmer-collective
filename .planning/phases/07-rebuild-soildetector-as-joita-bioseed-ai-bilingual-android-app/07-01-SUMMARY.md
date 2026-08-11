---
phase: 07-rebuild-soildetector-as-joita-bioseed-ai-bilingual-android-app
plan: "01"
status: complete
completed: 2026-08-11
commit: 6f829e9
---

# Plan 07-01 Summary

Created the API-36/JDK-17 Android foundation in `joita-biosoil-android`, package `ai.joita.biosoil`, version 4.0.0/min API 23. Added offline farmer/field/test SQLite persistence, deterministic advisory scoring, optional USB-host integration, the recovered 9600/8N1 poll protocol, defensive 19-byte buffering and JVM tests.

Verification: protocol/advisor unit tests pass; debug compilation and assembly pass. The original reference APK remained unchanged at SHA-256 `3c2def88a20457ddc5ab0c8274337d0ca2bc2a7d6470e2c79dcb05a2eedc9e63`.

Deviation: USB serial was updated from planned 3.10.0 to pinned 3.11.0 after the release tooling identified the newer compatible version.

