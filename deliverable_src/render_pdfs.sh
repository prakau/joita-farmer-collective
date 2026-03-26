#!/bin/zsh
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SRC="$ROOT/deliverable_src"
CHROME="/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"

render() {
  local html_name="$1"
  local pdf_name="$2"
  "$CHROME" \
    --headless=new \
    --disable-gpu \
    --allow-file-access-from-files \
    --run-all-compositor-stages-before-draw \
    --virtual-time-budget=3000 \
    --no-pdf-header-footer \
    --print-to-pdf="$ROOT/$pdf_name" \
    "file://$SRC/$html_name" >/dev/null 2>&1
}

render "er_diagram.html" "ER_Diagram.pdf"
render "tech_stack_brief.html" "The Tech Stack & Cost Optimization Brief.pdf"
render "india_scale_matrix.html" "The India Scale Matrix - FinTech & DPDP.pdf"
render "prototype_link_sheet.html" "The Link to your Interactive Prototype.pdf"
render "investor_showcase.html" "Joita Investor Showcase.pdf"
