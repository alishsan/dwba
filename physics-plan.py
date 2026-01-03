from reportlab.lib import colors
from reportlab.lib.pagesizes import A4
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle, Image, PageBreak
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.lib.enums import TA_CENTER, TA_LEFT

file_path = "./NewUU_Physics_Department_Strategy_2026_2030_Branded.pdf"

doc = SimpleDocTemplate(file_path, pagesize=A4, rightMargin=50, leftMargin=50, topMargin=60, bottomMargin=60)
styles = getSampleStyleSheet()
styles.add(ParagraphStyle(name='TitleStyle', fontSize=20, leading=24, alignment=TA_CENTER, spaceAfter=20, textColor=colors.HexColor('#003366')))
styles.add(ParagraphStyle(name='SubTitle', fontSize=14, leading=18, alignment=TA_CENTER, spaceAfter=15, textColor=colors.HexColor('#666666')))
styles.add(ParagraphStyle(name='Header', fontSize=14, leading=18, textColor=colors.HexColor('#003366'), spaceBefore=12, spaceAfter=8))
styles.add(ParagraphStyle(name='Body', fontSize=11, leading=16, alignment=TA_LEFT))
styles.add(ParagraphStyle(name='List', fontSize=11, leftIndent=15, leading=15))

content = []

# Cover Page
content.append(Paragraph("Physics Department Strategy 2026–2030", styles['TitleStyle']))
content.append(Paragraph("New Uzbekistan University", styles['SubTitle']))
content.append(Spacer(1, 20))
content.append(Paragraph("<b>Driving the Economy through Innovation</b>", styles['SubTitle']))
content.append(Spacer(1, 60))

# Executive Summary
content.append(Paragraph("Executive Summary", styles['Header']))
content.append(Paragraph("""<b>Mission:</b> To advance scientific discovery and technological innovation that drives Uzbekistan’s knowledge economy through research, education, and collaboration in frontier areas of physics.""", styles['Body']))
content.append(Paragraph("""<b>Vision:</b> By 2030, the Physics Department of New Uzbekistan University will be recognized as a regional leader in applied and fundamental physics — linking science with innovation in energy, nuclear technologies, astrophysics, and materials.""", styles['Body']))

content.append(Spacer(1, 10))
content.append(Paragraph("<b>Strategic Research Pillars:</b>", styles['Header']))
pillars = [
    "⚡ Energy & Environmental Physics – renewable energy systems, thermoelectrics, and atmospheric physics.",
    "☢️ Nuclear & Detector Physics – radiation applications, nuclear structure studies, and detector technologies.",
    "🌌 Astrophysics & Space Science – cosmic and planetary research, space data, and modeling.",
    "🧪 Advanced Functional Materials & Nanophysics – materials under extreme conditions and quantum materials."
]
for p in pillars:
    content.append(Paragraph("• " + p, styles['List']))

content.append(Spacer(1, 8))
content.append(Paragraph("<b>Cross-Cutting Platform:</b> 💻 Computational & AI-Driven Physics – enabling all research pillars through modeling, simulation, and data science.", styles['Body']))

content.append(Spacer(1, 10))
content.append(Paragraph("<b>Strategic Objectives (2026–2030):</b>", styles['Header']))
objectives = [
    "Build world-class infrastructure and laboratories.",
    "Train a new generation of physicists skilled in computation, innovation, and entrepreneurship.",
    "Forge partnerships with international labs (FAIR, TRIUMF) and industry.",
    "Establish the NewUU Center for Physics and Technology Innovation by 2030."
]
for o in objectives:
    content.append(Paragraph("• " + o, styles['List']))

content.append(Spacer(1, 10))
content.append(Paragraph("<b>Outcome:</b> A globally connected, innovation-driven physics department contributing directly to Uzbekistan’s sustainable economic growth and technological independence.", styles['Body']))
content.append(PageBreak())

# Year-by-Year Plan
content.append(Paragraph("Five-Year Strategic Plan (2026–2030)", styles['TitleStyle']))

years = {
    "2026 – Foundation": [
        "Establish research clusters in all four pillars.",
        "Recruit faculty with computational and data-science expertise.",
        "Launch Energy and Environmental Physics lab and basic computational facility.",
        "Define industrial and governmental partners in energy, nuclear, and technology sectors."
    ],
    "2027 – Capacity Growth": [
        "Develop Radiation Applications Unit and Computational Physics Lab.",
        "Introduce new courses: Computational Physics, AI in Science, Energy Systems Physics.",
        "Initiate applied research projects in thermoelectrics and dosimetry.",
        "Submit first competitive national and international proposals."
    ],
    "2028 – Internationalization": [
        "Strengthen partnerships with FAIR, TRIUMF, and space-data consortia.",
        "Establish Astrophysics and Space Data Group.",
        "Launch student and staff exchange programs.",
        "Develop joint research infrastructure with the Chemical & Materials Engineering Department."
    ],
    "2029 – Innovation and Integration": [
        "Create the Physics–Engineering Innovation Hub to translate research into prototypes.",
        "Integrate AI and data analytics in all research pillars.",
        "File initial patents; support spin-offs in energy monitoring and radiation detection.",
        "Organize first NewUU Physics and Innovation Workshop."
    ],
    "2030 – Impact and Leadership": [
        "Launch NewUU Center for Physics and Technology Innovation.",
        "Achieve international research visibility through flagship projects.",
        "Host International Conference on Physics and Innovation 2030.",
        "Demonstrate measurable national impact in renewable energy, materials resilience, nuclear safety, and space science."
    ]
}

for year, goals in years.items():
    content.append(Spacer(1, 10))
    content.append(Paragraph(year, styles['Header']))
    for g in goals:
        content.append(Paragraph("• " + g, styles['List']))

doc.build(content)
file_path
