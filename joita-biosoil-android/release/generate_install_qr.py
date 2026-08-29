"""Regenerate the one field-staff install QR after setting the final HTTPS URL."""
from pathlib import Path

import qrcode
import qrcode.image.svg

INSTALL_URL = "https://REPLACE-BEFORE-RELEASE.example/joita-farmer-collective"
OUTPUT = Path(__file__).with_name("JOITA-Farmer-Collective-INSTALL-QR.svg")

image = qrcode.make(
    INSTALL_URL,
    image_factory=qrcode.image.svg.SvgPathImage,
    box_size=12,
    border=4,
)
image.save(OUTPUT)
print(f"Wrote {OUTPUT} -> {INSTALL_URL}")
