import os
import re
import html
from reportlab.lib.pagesizes import LETTER
from reportlab.lib.units import inch
from reportlab.lib.styles import ParagraphStyle
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, Image, KeepTogether
from reportlab.lib.enums import TA_LEFT
from PIL import Image as PILImage
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.lib.fonts import addMapping

# Arial incorporato nel PDF (come nelle relazioni precedenti): con l'Helvetica standard
# alcuni programmi non mostravano il grassetto delle etichette
FONT_DIR = "/System/Library/Fonts/Supplemental"
pdfmetrics.registerFont(TTFont("Arial", os.path.join(FONT_DIR, "Arial.ttf")))
pdfmetrics.registerFont(TTFont("Arial-Bold", os.path.join(FONT_DIR, "Arial Bold.ttf")))
pdfmetrics.registerFont(TTFont("Arial-Italic", os.path.join(FONT_DIR, "Arial Italic.ttf")))
pdfmetrics.registerFont(TTFont("Arial-BoldItalic", os.path.join(FONT_DIR, "Arial Bold Italic.ttf")))
addMapping("Arial", 0, 0, "Arial")  # così <b> dentro un paragrafo usa davvero Arial-Bold
addMapping("Arial", 1, 0, "Arial-Bold")
addMapping("Arial", 0, 1, "Arial-Italic")
addMapping("Arial", 1, 1, "Arial-BoldItalic")

CARTELLA = os.path.dirname(os.path.abspath(__file__))  # la cartella in cui si trova questo script
SRC = os.path.join(CARTELLA, "relazione_forza4.md")
OUT = os.path.join(CARTELLA, "Relazione N.24 Informatica.pdf")

LARGHEZZA_UTILE = LETTER[0] - 2 * inch  # margini di 1 pollice per lato
ALTEZZA_MASSIMA_IMMAGINE = 7.8 * inch  # per non far traboccare un'immagine molto verticale su una sola pagina

with open(SRC, encoding="utf-8") as f:
    testo = f.read()

paragrafi = [p.strip() for p in testo.split("\n\n") if p.strip()]

stile_normale = ParagraphStyle(
    "Normale", fontName="Arial", fontSize=11.5, leading=16.5,
    spaceAfter=14, alignment=TA_LEFT, textColor="black",
)
stile_intestazione = ParagraphStyle(
    "Intestazione", fontName="Arial-Bold", fontSize=11.5, leading=16.5,
    spaceAfter=14, alignment=TA_LEFT, textColor="black", spaceBefore=6,
)
stile_didascalia = ParagraphStyle(
    "Didascalia", fontName="Arial-Italic", fontSize=9.5, leading=13,
    spaceAfter=16, alignment=TA_LEFT, textColor="black",
)

elementi = []

for paragrafo in paragrafi:

    m_immagine = re.match(r"^!\[(.*?)\]\((.*?)\)$", paragrafo, re.DOTALL)
    if m_immagine:
        didascalia, percorso_relativo = m_immagine.group(1), m_immagine.group(2)
        percorso_assoluto = os.path.join(CARTELLA, percorso_relativo)

        larghezza_px, altezza_px = PILImage.open(percorso_assoluto).size
        larghezza = LARGHEZZA_UTILE
        altezza = larghezza * altezza_px / larghezza_px
        if altezza > ALTEZZA_MASSIMA_IMMAGINE:  # scala anche in base all'altezza, se troppo verticale
            altezza = ALTEZZA_MASSIMA_IMMAGINE
            larghezza = altezza * larghezza_px / altezza_px

        # immagine e didascalia tenute insieme, così la didascalia non finisce da sola sulla pagina dopo
        elementi.append(KeepTogether([
            Image(percorso_assoluto, width=larghezza, height=altezza),
            Paragraph(html.escape(didascalia), stile_didascalia),
        ]))
        continue

    paragrafo = paragrafo.replace("\n", " ")

    if paragrafo == "Corpo della relazione" or (paragrafo.endswith(":") and len(paragrafo) < 40):
        elementi.append(Spacer(1, 4))
        elementi.append(Paragraph(html.escape(paragrafo), stile_intestazione))
        continue

    m = re.match(r"^([^:]{2,90}?:)\s(.*)$", paragrafo, re.DOTALL)
    # in grassetto solo le etichette brevi (es. "Modello su carta:"), non le frasi normali che contengono i due punti
    if m and len(m.group(1).split()) <= 6:
        etichetta, resto = m.group(1), m.group(2)
        corpo = f"<b>{html.escape(etichetta)}</b> {html.escape(resto)}"
    else:
        corpo = html.escape(paragrafo)

    elementi.append(Paragraph(corpo, stile_normale))

doc = SimpleDocTemplate(
    OUT, pagesize=LETTER,
    leftMargin=1 * inch, rightMargin=1 * inch,
    topMargin=1 * inch, bottomMargin=1 * inch,
    title="Relazione Forza 4", author="Razvon Lukian",
)
doc.build(elementi)
print("PDF creato:", OUT)
