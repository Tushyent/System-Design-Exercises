import os
from reportlab.lib.pagesizes import letter
from reportlab.lib import colors
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle, Image, PageBreak
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle

def generate_pdf():
    report_md_path = os.path.join(os.path.dirname(__file__), "Scalable_Library_Management_System_Report.md")
    pdf_path = os.path.join(os.path.dirname(__file__), "Scalable_Library_Management_System_Report.pdf")
    figures_dir = os.path.join(os.path.dirname(__file__), "..", "results", "figures")

    doc = SimpleDocTemplate(
        pdf_path,
        pagesize=letter,
        rightMargin=45,
        leftMargin=45,
        topMargin=45,
        bottomMargin=45
    )

    styles = getSampleStyleSheet()

    title_style = ParagraphStyle(
        'DocTitle',
        parent=styles['Heading1'],
        fontName='Helvetica-Bold',
        fontSize=20,
        leading=24,
        textColor=colors.HexColor('#1E293B'),
        spaceAfter=12
    )

    h2_style = ParagraphStyle(
        'DocH2',
        parent=styles['Heading2'],
        fontName='Helvetica-Bold',
        fontSize=14,
        leading=18,
        textColor=colors.HexColor('#0284C7'),
        spaceBefore=14,
        spaceAfter=8
    )

    h3_style = ParagraphStyle(
        'DocH3',
        parent=styles['Heading3'],
        fontName='Helvetica-Bold',
        fontSize=11,
        leading=15,
        textColor=colors.HexColor('#334155'),
        spaceBefore=10,
        spaceAfter=6
    )

    body_style = ParagraphStyle(
        'DocBody',
        parent=styles['Normal'],
        fontName='Helvetica',
        fontSize=9.5,
        leading=13.5,
        textColor=colors.HexColor('#1E293B'),
        spaceAfter=6
    )

    callout_style = ParagraphStyle(
        'DocCallout',
        parent=styles['Normal'],
        fontName='Helvetica-Oblique',
        fontSize=9,
        leading=13,
        textColor=colors.HexColor('#475569'),
        spaceAfter=8
    )

    story = []

    # Title & Header
    story.append(Paragraph("Scalable Library Management System", title_style))
    story.append(Paragraph("<b>Coursework</b>: UCS3513 — System Design Laboratory | SSN College of Engineering", callout_style))
    story.append(Paragraph("<b>Problem Statement 3</b>: Distributed Architecture, Prototype, and Performance Evaluation", callout_style))
    story.append(Spacer(1, 10))

    # Executive Summary
    story.append(Paragraph("1. Executive Summary", h2_style))
    story.append(Paragraph(
        "Athena LMS is a scalable, distributed Library Management System architected for high concurrency, zero "
        "double-borrowing under peak loan contention, sub-millisecond autocomplete suggestions, and horizontal database scaling. "
        "The project incorporates all 8 mandated system design components: Token Bucket Rate Limiting, MongoDB Sharding, "
        "Consistent Hashing with Virtual Nodes, Redis Key-Value Caching, Snowflake 64-bit ID Generation, Base62 URL Shortening, "
        "Asynchronous External Web Crawling, and a Prefix Trie Autocomplete engine.",
        body_style
    ))

    # Architecture Overview
    story.append(Paragraph("2. System Design Components Summary", h2_style))
    component_data = [
        ["Component", "Target Functionality", "Implementation Principle", "Measured Status"],
        ["Rate Limiter", "Auth & Search API Throttling", "Redis Lua Token Bucket (cap=10, refill=2/s)", "80% Rejection Rate on Burst"],
        ["Sharding", "Member & Catalogue Datasets", "Hashed memberId & Compound Categories", "Horizontal partition design"],
        ["Consistent Hashing", "Server & Cache Key Routing", "MD5 Ring (160 virtual nodes/node)", "28.3% Churn vs 75.1% Modulo"],
        ["Key-Value Store", "Catalogue Read-Through Cache", "Redis Cache-Aside with Invalidation", "Sub-2ms cache hits"],
        ["ID Generator", "Barcodes, Loans, Payments", "Twitter Snowflake 64-bit ID", "0 Collisions in 50k tests"],
        ["URL Shortener", "Book Resource Sharing", "Bijective Base62 over Snowflake IDs", "25.19ms redirection latency"],
        ["Web Crawler", "Catalogue Metadata Ingestion", "Multi-threaded REST Worker & ISBN Dedup", "56.91 books/sec throughput"],
        ["Autocomplete", "Instant Search Dropdown", "In-Memory Prefix Trie with Score Ranking", "< 1ms latency, 0.57 Precision@5"]
    ]

    t = Table(component_data, colWidths=[90, 130, 180, 120])
    t.setStyle(TableStyle([
        ('BACKGROUND', (0, 0), (-1, 0), colors.HexColor('#0284C7')),
        ('TEXTCOLOR', (0, 0), (-1, 0), colors.whitesmoke),
        ('FONTNAME', (0, 0), (-1, 0), 'Helvetica-Bold'),
        ('FONTSIZE', (0, 0), (-1, -1), 8),
        ('BOTTOMPADDING', (0, 0), (-1, -1), 4),
        ('TOPPADDING', (0, 0), (-1, -1), 4),
        ('GRID', (0, 0), (-1, -1), 0.5, colors.HexColor('#CBD5E1')),
        ('ROWBACKGROUNDS', (0, 1), (-1, -1), [colors.white, colors.HexColor('#F8FAFC')])
    ]))
    story.append(t)
    story.append(Spacer(1, 12))

    # Workload Results
    story.append(Paragraph("3. Quantitative Performance Evaluation across Workload Tiers", h2_style))
    story.append(Paragraph(
        "The system was stress-tested under 3 concurrent workload levels: Low (10 users, 200 requests), "
        "Medium (50 users, 500 requests), and High (100 users, 1000 requests). All 1700 benchmark requests "
        "completed with a 100% success rate and zero 5xx server errors.",
        body_style
    ))

    workload_data = [
        ["Workload Level", "Concurrency", "Total Reqs", "Avg Latency", "P95 Latency", "Throughput", "Success Rate"],
        ["Low", "10 users", "200", "31.67 ms", "49.55 ms", "274.19 req/s", "100.0%"],
        ["Medium", "50 users", "500", "87.87 ms", "195.85 ms", "216.59 req/s", "100.0%"],
        ["High", "100 users", "1000", "272.54 ms", "525.58 ms", "203.91 req/s", "100.0%"]
    ]
    tw = Table(workload_data, colWidths=[80, 75, 65, 75, 75, 80, 70])
    tw.setStyle(TableStyle([
        ('BACKGROUND', (0, 0), (-1, 0), colors.HexColor('#1E293B')),
        ('TEXTCOLOR', (0, 0), (-1, 0), colors.whitesmoke),
        ('FONTNAME', (0, 0), (-1, 0), 'Helvetica-Bold'),
        ('FONTSIZE', (0, 0), (-1, -1), 8.5),
        ('BOTTOMPADDING', (0, 0), (-1, -1), 4),
        ('TOPPADDING', (0, 0), (-1, -1), 4),
        ('GRID', (0, 0), (-1, -1), 0.5, colors.HexColor('#CBD5E1')),
        ('ROWBACKGROUNDS', (0, 1), (-1, -1), [colors.white, colors.HexColor('#F8FAFC')])
    ]))
    story.append(tw)
    story.append(Spacer(1, 14))

    # Add Chart 1
    chart1 = os.path.join(figures_dir, "scalability_workload_tiers.png")
    if os.path.exists(chart1):
        story.append(Image(chart1, width=460, height=220))
        story.append(Spacer(1, 10))

    story.append(PageBreak())

    # Baseline Comparison & Consistent Hashing
    story.append(Paragraph("4. Architectural Baseline Comparison & Consistent Hashing Churn", h2_style))
    story.append(Paragraph(
        "Comparing unoptimized regex search against indexed catalogue search demonstrated an 8.6% latency reduction "
        "and a +9.7% throughput improvement. Furthermore, consistent hashing achieved near-optimal remapping (28.3% keys churned) "
        "compared to modulo hashing's catastrophic 75.1% invalidation.",
        body_style
    ))

    chart2 = os.path.join(figures_dir, "baseline_vs_optimized_search.png")
    chart3 = os.path.join(figures_dir, "consistent_hashing_churn.png")
    if os.path.exists(chart2) and os.path.exists(chart3):
        comp_table = [
            [Image(chart2, width=240, height=155), Image(chart3, width=240, height=155)]
        ]
        tc = Table(comp_table, colWidths=[260, 260])
        tc.setStyle(TableStyle([('ALIGN', (0,0), (-1,-1), 'CENTER')]))
        story.append(tc)
        story.append(Spacer(1, 12))

    # Concurrency & Idempotency Guarantees
    story.append(Paragraph("5. Concurrency Correctness & Transaction Guarantees", h2_style))
    story.append(Paragraph(
        "• <b>Atomic Copy Claim</b>: MongoDB atomic <i>findAndModify</i> with status predicates guarantees exactly one member acquires a scarce copy. In automated tests with 20 concurrent threads, 1 succeeded and 19 received HTTP 409 Conflict.<br/>"
        "• <b>Idempotent Payments</b>: Client-supplied UUID idempotency keys recorded in MongoDB guarantee that network retries return identical receipts (<i>isDuplicateReplay=true</i>) without double-debiting.<br/>"
        "• <b>Idempotent Returns</b>: Duplicate return submissions return <i>ALREADY_RETURNED</i> without generating duplicate overdue fines.",
        body_style
    ))
    story.append(Spacer(1, 10))

    # Verification Summary
    story.append(Paragraph("6. Final Acceptance Summary", h2_style))
    story.append(Paragraph(
        "All 13 core functional and non-functional workflows passed automated verification. The system is fully operational "
        "with active Spring Boot backend (:8080), MongoDB (:27017), and React TypeScript frontend (:5173).",
        body_style
    ))

    doc.build(story)
    print(f"PDF successfully generated at: {pdf_path}")

if __name__ == "__main__":
    generate_pdf()
