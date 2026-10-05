import tempfile
import os
from reportlab.lib.pagesizes import LETTER, landscape
from reportlab.lib.units import inch
from reportlab.lib.styles import ParagraphStyle
from reportlab.lib.enums import TA_CENTER, TA_LEFT
from reportlab.platypus import Paragraph
from reportlab.pdfgen import canvas
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont

FONT_DIR = "/System/Library/Fonts/Supplemental"  # Arial incorporato, come nella relazione
pdfmetrics.registerFont(TTFont("Arial", os.path.join(FONT_DIR, "Arial.ttf")))
pdfmetrics.registerFont(TTFont("Arial-Bold", os.path.join(FONT_DIR, "Arial Bold.ttf")))
from PIL import Image as PILImage

CARTELLA = os.path.dirname(os.path.abspath(__file__))  # la cartella in cui si trova questo script
CARTELLA_FOTO = os.path.join(CARTELLA, "..", "docs", "screenshots")
OUT = os.path.join(CARTELLA, "Galleria GUI Forza4.pdf")

# cartella temporanea per le immagini ridotte (fuori dal progetto, per non sporcare il repo)
CARTELLA_TMP = os.path.join(tempfile.gettempdir(), "galleria_forza4")  # immagini ridotte, fuori dal progetto

MARGINE = 1 * inch  # margini di 1 pollice per lato, come nella relazione
LARGHEZZA_MAX_PX = 1600  # le foto sono a 2x: 1600 px bastano per una stampa nitida
QUALITA_JPEG = 85
SPAZIO_TITOLO = 0.55 * inch  # spazio riservato in alto al titolo della sezione
SPAZIO_DIDASCALIA = 0.9 * inch  # spazio riservato in basso alla didascalia

# elenco delle foto nell'ordine del flusso d'uso del programma: (file, titolo sezione, didascalia)
FOTO = [  # (file, didascalia breve)
    ("01-schermata-iniziale.png", "schermata iniziale (Umano vs Umano)"),
    ("02-schermata-iniziale-ai.png", "schermata iniziale (Umano vs AI)"),
    ("03-tooltip-chiave-api.png", "aiuto per la chiave API"),
    ("04-partita-4x4.png", "partita in corso, griglia 4x4"),
    ("05-partita-4x8.png", "partita in corso, griglia 4x8"),
    ("06-partita-6x7.png", "partita in corso, griglia 6x7"),
    ("07-partita-8x4.png", "partita in corso, griglia 8x4"),
    ("08-partita-8x8.png", "partita in corso, griglia 8x8"),
    ("09-vittoria-4x4.png", "vittoria, griglia 4x4"),
    ("10-vittoria-4x8.png", "vittoria, griglia 4x8"),
    ("11-vittoria-6x7.png", "vittoria, griglia 6x7"),
    ("12-vittoria-8x4.png", "vittoria, griglia 8x4"),
    ("13-vittoria-8x8.png", "vittoria, griglia 8x8"),
    ("14-pareggio-4x4.png", "pareggio, griglia 4x4"),
    ("15-errore-modalita-ai.png", "messaggio di errore della modalità AI"),
]

stile_titolo_sezione = ParagraphStyle(
    "TitoloSezione", fontName="Helvetica-Bold", fontSize=15, leading=19,
    alignment=TA_LEFT, textColor="black",
)
stile_didascalia = ParagraphStyle(
    "Didascalia", fontName="Arial", fontSize=11, leading=14,
    alignment=TA_CENTER, textColor="black",
)
stile_copertina_titolo = ParagraphStyle(
    "CopertinaTitolo", fontName="Arial-Bold", fontSize=20, leading=26,
    alignment=TA_CENTER, textColor="black",
)
stile_copertina = ParagraphStyle(
    "Copertina", fontName="Arial", fontSize=12, leading=18,
    alignment=TA_CENTER, textColor="black",
)


def riduci_immagine(nome_file):
    """Ridimensiona e comprime la foto in JPEG nella cartella temporanea, restituisce percorso e misure."""
    os.makedirs(CARTELLA_TMP, exist_ok=True)
    immagine = PILImage.open(os.path.join(CARTELLA_FOTO, nome_file)).convert("RGB")
    if immagine.width > LARGHEZZA_MAX_PX:  # solo riduzione, mai ingrandimento
        nuova_altezza = round(immagine.height * LARGHEZZA_MAX_PX / immagine.width)
        immagine = immagine.resize((LARGHEZZA_MAX_PX, nuova_altezza), PILImage.LANCZOS)
    percorso = os.path.join(CARTELLA_TMP, os.path.splitext(nome_file)[0] + ".jpg")
    immagine.save(percorso, "JPEG", quality=QUALITA_JPEG, optimize=True)
    return percorso, immagine.width, immagine.height


def misura_in_pagina(pagina, larghezza_px, altezza_px, piccola):
    """Calcola la misura dell'immagine nella pagina, mantenendo le proporzioni."""
    larghezza_utile = pagina[0] - 2 * MARGINE
    altezza_utile = pagina[1] - 2 * MARGINE - SPAZIO_TITOLO - SPAZIO_DIDASCALIA
    scala = min(larghezza_utile / larghezza_px, altezza_utile / altezza_px)
    if piccola:  # tooltip e dialog: non ingrandirli oltre la loro misura reale a schermo (foto a 2x)
        scala = min(scala, 0.5)
    return larghezza_px * scala, altezza_px * scala


def scegli_pagina(larghezza_px, altezza_px, piccola):
    """Sceglie verticale o orizzontale: vince l'orientamento in cui l'immagine viene più grande."""
    verticale, orizzontale = LETTER, landscape(LETTER)
    area_v = misura_in_pagina(verticale, larghezza_px, altezza_px, piccola)[0]
    area_o = misura_in_pagina(orizzontale, larghezza_px, altezza_px, piccola)[0]
    return orizzontale if area_o > area_v * 1.05 else verticale


def disegna_paragrafo(c, paragrafo, x, y_alto, larghezza):
    """Disegna un paragrafo con il bordo superiore in y_alto e restituisce la sua altezza."""
    _, altezza = paragrafo.wrap(larghezza, 1000)
    paragrafo.drawOn(c, x, y_alto - altezza)
    return altezza


def disegna_copertina(c):
    larghezza, altezza = LETTER
    larghezza_testo = larghezza - 2 * MARGINE
    y = altezza - 3.5 * inch
    y -= disegna_paragrafo(c, Paragraph("Forza 4", stile_copertina_titolo), MARGINE, y, larghezza_testo)
    y -= 0.1 * inch
    y -= disegna_paragrafo(c, Paragraph("Schermate dell'interfaccia grafica", stile_copertina),
                           MARGINE, y, larghezza_testo)
    y -= 0.6 * inch
    disegna_paragrafo(c, Paragraph("Razvon Lukian<br/>Classe 5D<br/>05/10/2026", stile_copertina),
                      MARGINE, y, larghezza_testo)
    c.showPage()


def disegna_pagina_foto(c, numero, nome_file, didascalia):
    piccola = nome_file in ("03-tooltip-chiave-api.png", "15-errore-modalita-ai.png")
    percorso, larghezza_px, altezza_px = riduci_immagine(nome_file)
    # le misure si calcolano sulla foto originale a 2x, così tooltip e dialog restano a misura reale
    larghezza_orig, altezza_orig = PILImage.open(os.path.join(CARTELLA_FOTO, nome_file)).size
    pagina = scegli_pagina(larghezza_orig, altezza_orig, piccola)
    c.setPageSize(pagina)
    larghezza_pagina, altezza_pagina = pagina
    larghezza_testo = larghezza_pagina - 2 * MARGINE

    # immagine centrata nello spazio tra titolo e didascalia, con un bordino grigio sottile
    larghezza_img, altezza_img = misura_in_pagina(pagina, larghezza_orig, altezza_orig, piccola)
    alto_area = altezza_pagina - MARGINE
    basso_area = MARGINE + SPAZIO_DIDASCALIA
    x = (larghezza_pagina - larghezza_img) / 2
    y = basso_area + (alto_area - basso_area - altezza_img) / 2
    c.drawImage(percorso, x, y, width=larghezza_img, height=altezza_img)
    c.setStrokeGray(0.6)
    c.setLineWidth(0.5)
    c.rect(x, y, larghezza_img, altezza_img)

    # didascalia subito sotto l'immagine, larga quanto lo spazio utile
    disegna_paragrafo(c, Paragraph(f"Figura {numero}: {didascalia}", stile_didascalia),
                      MARGINE, y - 0.15 * inch, larghezza_testo)

    # numero di pagina in basso al centro
    c.setFont("Arial", 9)
    c.drawCentredString(larghezza_pagina / 2, MARGINE / 2, str(c.getPageNumber()))
    c.showPage()


c = canvas.Canvas(OUT, pagesize=LETTER)
c.setTitle("Forza 4 - Schermate dell'interfaccia grafica")
c.setAuthor("Razvon Lukian")
disegna_copertina(c)
for numero, (nome_file, didascalia) in enumerate(FOTO, start=1):
    disegna_pagina_foto(c, numero, nome_file, didascalia)
c.save()
print("PDF creato:", OUT, f"({os.path.getsize(OUT) / 1024 / 1024:.1f} MB)")
