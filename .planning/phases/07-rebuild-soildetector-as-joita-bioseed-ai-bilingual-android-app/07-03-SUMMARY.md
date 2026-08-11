---
phase: 07-rebuild-soildetector-as-joita-bioseed-ai-bilingual-android-app
plan: "03"
status: complete
completed: 2026-08-11
commits:
  - f1df3d9
  - c266cf2
---

# Plan 07-03 Summary

Generated a private 4096-bit JOITA BIOSEED AI release key valid through 2053, protected its local key and credentials with mode 600, and excluded all secrets from Git. Produced R8/resource-shrunk, signed APK and AAB artifacts with stable handoff names, hashes, installation guide, release notes and reproducible locale/release verification scripts.

Automated release gates passed: JVM tests, full release lint (0 errors), release APK/AAB assembly, bilingual parity, package/version/min/target SDK, optional hardware features, four common ABIs, exact logo hash, approved permissions, artifact hash and v1/v2/v3 signature.

