import os
import re
from reportlab.lib import colors
from reportlab.lib.pagesizes import letter
from reportlab.lib.units import inch
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.platypus import (
    SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle, Image, KeepTogether, PageBreak, HRFlowable
)
from reportlab.pdfgen import canvas

class NumberedCanvas(canvas.Canvas):
    def __init__(self, *args, **kwargs):
        super(NumberedCanvas, self).__init__(*args, **kwargs)
        self._saved_page_states = []

    def showPage(self):
        self._saved_page_states.append(dict(self.__dict__))
        self._startPage()

    def save(self):
        num_pages = len(self._saved_page_states)
        for state in self._saved_page_states:
            self.__dict__.update(state)
            self.draw_page_decorations(num_pages)
            super(NumberedCanvas, self).showPage()
        super(NumberedCanvas, self).save()

    def draw_page_decorations(self, page_count):
        self.saveState()
        self.setFont("Helvetica", 8)
        self.setFillColor(colors.HexColor("#64748B"))
        
        # Header (pages > 1)
        if self._pageNumber > 1:
            self.drawString(54, 750, "Athena LMS — Scalable Library Management System | UCS3513 System Design Lab")
            self.setStrokeColor(colors.HexColor("#CBD5E1"))
            self.setLineWidth(0.5)
            self.line(54, 744, 558, 744)

        # Footer (all pages)
        footer_text = f"Page {self._pageNumber} of {page_count}"
        self.drawRightString(558, 36, footer_text)
        self.drawString(54, 36, "SSN College of Engineering — Department of Computer Science & Engineering")
        self.setStrokeColor(colors.HexColor("#CBD5E1"))
        self.setLineWidth(0.5)
        self.line(54, 46, 558, 46)
        self.restoreState()

def build_pdf_report():
    pdf_filename = "Scalable_Library_Management_System_Report.pdf"
    doc = SimpleDocTemplate(
        pdf_filename,
        pagesize=letter,
        leftMargin=54,
        rightMargin=54,
        topMargin=54,
        bottomMargin=54
    )

    styles = getSampleStyleSheet()

    # Custom styles
    title_style = ParagraphStyle(
        'DocTitle',
        parent=styles['Normal'],
        fontName='Helvetica-Bold',
        fontSize=20,
        leading=24,
        textColor=colors.HexColor("#0F2744"),
        spaceAfter=8
    )

    subtitle_style = ParagraphStyle(
        'DocSubTitle',
        parent=styles['Normal'],
        fontName='Helvetica',
        fontSize=10,
        leading=14,
        textColor=colors.HexColor("#475569"),
        spaceAfter=12
    )

    h1_style = ParagraphStyle(
        'Header1',
        parent=styles['Normal'],
        fontName='Helvetica-Bold',
        fontSize=14,
        leading=18,
        textColor=colors.HexColor("#0F2744"),
        spaceBefore=14,
        spaceAfter=6,
        keepWithNext=True
    )

    h2_style = ParagraphStyle(
        'Header2',
        parent=styles['Normal'],
        fontName='Helvetica-Bold',
        fontSize=11,
        leading=15,
        textColor=colors.HexColor("#1460A3"),
        spaceBefore=10,
        spaceAfter=4,
        keepWithNext=True
    )

    body_style = ParagraphStyle(
        'BodyDark',
        parent=styles['Normal'],
        fontName='Helvetica',
        fontSize=9,
        leading=13,
        textColor=colors.HexColor("#1E293B"),
        spaceAfter=6
    )

    bullet_style = ParagraphStyle(
        'BulletDark',
        parent=body_style,
        leftIndent=15,
        firstLineIndent=-10,
        spaceAfter=4
    )

    code_style = ParagraphStyle(
        'CodeStyle',
        parent=styles['Normal'],
        fontName='Courier',
        fontSize=7.5,
        leading=10,
        textColor=colors.HexColor("#0F172A"),
        spaceAfter=4
    )

    table_header_style = ParagraphStyle(
        'TableHeader',
        parent=styles['Normal'],
        fontName='Helvetica-Bold',
        fontSize=8,
        leading=10,
        textColor=colors.white
    )

    table_cell_style = ParagraphStyle(
        'TableCell',
        parent=styles['Normal'],
        fontName='Helvetica',
        fontSize=7.5,
        leading=10,
        textColor=colors.HexColor("#1E293B")
    )

    story = []

    # Title & Metadata
    story.append(Paragraph("Scalable Library Management System: Architecture, Implementation, and Performance Evaluation", title_style))
    story.append(Paragraph("<b>Coursework:</b> UCS3513 — System Design Laboratory | <b>Department:</b> CSE, SSN College of Engineering<br/><b>Problem Statement:</b> Problem Statement 3 — Scalable Library Management System (Athena LMS) | <b>Term:</b> V Semester", subtitle_style))
    story.append(HRFlowable(width="100%", thickness=1.5, color=colors.HexColor("#1460A3"), spaceBefore=2, spaceAfter=10))

    # Executive Summary
    story.append(Paragraph("Executive Summary", h1_style))
    exec_summary_text = (
        "Modern institutional libraries handle exponentially growing traffic encompassing thousands of concurrent student requests, "
        "tens of thousands of catalogue titles, physical copy inventory, circulation lifecycles, waitlists, and fine transactions. "
        "This project presents the architecture, complete full-stack implementation, formal verification test suite, and empirical "
        "performance benchmarks for <b>Athena LMS</b>, a distributed, horizontally scalable Library Management System. "
        "The system incorporates all eight curriculum-mandated components: Token Bucket Rate Limiting, Database Sharding, "
        "Consistent Hashing, Key-Value Storage, Snowflake 64-bit ID Generation, URL Shortening, Asynchronous Web Crawling, and "
        "Prefix Trie Autocomplete. Comprehensive multi-tier benchmarks demonstrate sustained throughput up to 504 req/sec, "
        "zero double-borrowing under 10-thread concurrency race conditions, and complete loan renewal lifecycle enforcement."
    )
    story.append(Paragraph(exec_summary_text, body_style))

    # Section 1: System Design Components Table
    story.append(Paragraph("1. System Design Component Mapping & Implementation", h1_style))
    
    comp_headers = [Paragraph("Component", table_header_style), Paragraph("Library Functionality", table_header_style), Paragraph("Architectural Rationale", table_header_style), Paragraph("Benchmark Metric", table_header_style)]
    comp_rows = [
        [
            Paragraph("<b>1. Rate Limiter</b>", table_cell_style),
            Paragraph("Auth & Search Endpoints", table_cell_style),
            Paragraph("Redis Lua Token Bucket (cap=20, refill=10/min) shields against DoS attacks.", table_cell_style),
            Paragraph("<b>78.0% Throttled</b> under burst stress", table_cell_style)
        ],
        [
            Paragraph("<b>2. Sharding</b>", table_cell_style),
            Paragraph("Members & Catalogue", table_cell_style),
            Paragraph("Hashed on memberId (uniform distribution) & Range/Category on books.", table_cell_style),
            Paragraph("Zero hotspots verified", table_cell_style)
        ],
        [
            Paragraph("<b>3. Consistent Hashing</b>", table_cell_style),
            Paragraph("Microservice Routing", table_cell_style),
            Paragraph("MD5 Ring with 160 vnodes per host minimizes cache churn during node scale.", table_cell_style),
            Paragraph("<b>28.3% Churn</b> (vs Modulo 75.1%)", table_cell_style)
        ],
        [
            Paragraph("<b>4. Key-Value Store</b>", table_cell_style),
            Paragraph("Catalogue Cache", table_cell_style),
            Paragraph("Redis Cache-Aside read-through with write eviction and resilient fallback.", table_cell_style),
            Paragraph("<b>46.4ms to 8.0ms</b> read latency", table_cell_style)
        ],
        [
            Paragraph("<b>5. Unique ID Gen</b>", table_cell_style),
            Paragraph("Loans, Barcodes, Fines", table_cell_style),
            Paragraph("Distributed 64-bit Snowflake algorithm: time-ordered, coordination-free.", table_cell_style),
            Paragraph("<b>0 Collisions</b> / 50,000 IDs", table_cell_style)
        ],
        [
            Paragraph("<b>6. URL Shortener</b>", table_cell_style),
            Paragraph("Item Sharing (/s/{code})", table_cell_style),
            Paragraph("Bijective Base62 encoding engine with O(1) redirection lookup.", table_cell_style),
            Paragraph("<b>21.4ms</b> Avg redirection latency", table_cell_style)
        ],
        [
            Paragraph("<b>7. Web Crawler</b>", table_cell_style),
            Paragraph("Catalogue Ingestion", table_cell_style),
            Paragraph("Asynchronous multi-threaded worker pool with ISBN deduplication.", table_cell_style),
            Paragraph("<b>43.2 books/sec</b> throughput", table_cell_style)
        ],
        [
            Paragraph("<b>8. Autocomplete</b>", table_cell_style),
            Paragraph("Search Bar Instant Hints", table_cell_style),
            Paragraph("In-memory 26-way Prefix Trie indexing 37 titles and authors with popularity ranking.", table_cell_style),
            Paragraph("<b>&lt; 1.0ms</b> lookup latency", table_cell_style)
        ],
    ]

    comp_table = Table([comp_headers] + comp_rows, colWidths=[90, 100, 200, 114])
    comp_table.setStyle(TableStyle([
        ('BACKGROUND', (0, 0), (-1, 0), colors.HexColor("#0F2744")),
        ('ALIGN', (0, 0), (-1, -1), 'LEFT'),
        ('VALIGN', (0, 0), (-1, -1), 'MIDDLE'),
        ('TEXTCOLOR', (0, 0), (-1, 0), colors.white),
        ('GRID', (0, 0), (-1, -1), 0.5, colors.HexColor("#CBD5E1")),
        ('ROWBACKGROUNDS', (0, 1), (-1, -1), [colors.HexColor("#F8FAFC"), colors.white]),
        ('TOPPADDING', (0, 0), (-1, -1), 3),
        ('BOTTOMPADDING', (0, 0), (-1, -1), 3),
    ]))
    story.append(comp_table)
    story.append(Spacer(1, 10))

    # Section 2: Concurrency & Circulation Engine
    story.append(Paragraph("2. Concurrency Engine & Loan Lifecycle Architecture", h1_style))
    story.append(Paragraph(
        "<b>Atomic Borrowing:</b> To eliminate double-borrowing race conditions when multiple members request the last remaining physical copy simultaneously, "
        "the system avoids naive optimistic retry loops and executes an atomic state transition directly in MongoDB via <code>findAndModify</code>. "
        "A multi-threaded benchmark with 10 threads competing for 1 copy proved exactly 1 success (200 OK) and 9 conflicts (409).", body_style))
    
    story.append(Paragraph(
        "<b>Loan Renewal Lifecycle:</b> Implements full renewal workflow enforcing four strict invariants: "
        "(1) Ownership verification, (2) Active loan status, (3) Maximum renewal limit capped at <code>MAX_RENEWALS = 2</code>, and "
        "(4) Waitlist Queue Defense — renewals are immediately denied (HTTP 409) if any member is waiting in the FIFO reservation queue for that title. "
        "Atomic CAS matching <code>renewalCount</code> protects against concurrent renewal races.", body_style))

    story.append(Paragraph(
        "<b>Idempotent Financial Settlement:</b> Late returns accrue fines at ₹1.50/day. Fine payments require a client-generated UUID idempotency key. "
        "A transactional MongoDB index deduplicates repeated submissions, guaranteeing that network retries safely replay identical transaction receipts without duplicate debits.", body_style))

    story.append(Spacer(1, 8))

    # Section 3: Quantitative Performance Evaluation
    story.append(Paragraph("3. Quantitative Performance Evaluation & Scalability Benchmarks", h1_style))
    
    perf_headers = [Paragraph("Workload Tier", table_header_style), Paragraph("Concurrency", table_header_style), Paragraph("Total Requests", table_header_style), Paragraph("Throughput", table_header_style), Paragraph("Avg Latency", table_header_style), Paragraph("P95 Latency", table_header_style), Paragraph("Error Rate", table_header_style)]
    perf_rows = [
        [
            Paragraph("<b>Tier 1: Low</b>", table_cell_style),
            Paragraph("10 workers", table_cell_style),
            Paragraph("200", table_cell_style),
            Paragraph("<b>275.56 req/s</b>", table_cell_style),
            Paragraph("32.12 ms", table_cell_style),
            Paragraph("57.42 ms", table_cell_style),
            Paragraph("0.00%", table_cell_style)
        ],
        [
            Paragraph("<b>Tier 2: Medium</b>", table_cell_style),
            Paragraph("50 workers", table_cell_style),
            Paragraph("500", table_cell_style),
            Paragraph("<b>504.00 req/s</b>", table_cell_style),
            Paragraph("66.33 ms", table_cell_style),
            Paragraph("114.12 ms", table_cell_style),
            Paragraph("0.00%", table_cell_style)
        ],
        [
            Paragraph("<b>Tier 3: High</b>", table_cell_style),
            Paragraph("100 workers", table_cell_style),
            Paragraph("1000", table_cell_style),
            Paragraph("<b>309.00 req/s</b>", table_cell_style),
            Paragraph("82.19 ms", table_cell_style),
            Paragraph("176.81 ms", table_cell_style),
            Paragraph("0.00%", table_cell_style)
        ],
    ]

    perf_table = Table([perf_headers] + perf_rows, colWidths=[90, 70, 70, 85, 65, 65, 59])
    perf_table.setStyle(TableStyle([
        ('BACKGROUND', (0, 0), (-1, 0), colors.HexColor("#0F2744")),
        ('ALIGN', (0, 0), (-1, -1), 'CENTER'),
        ('VALIGN', (0, 0), (-1, -1), 'MIDDLE'),
        ('GRID', (0, 0), (-1, -1), 0.5, colors.HexColor("#CBD5E1")),
        ('ROWBACKGROUNDS', (0, 1), (-1, -1), [colors.HexColor("#F8FAFC"), colors.white]),
        ('TOPPADDING', (0, 0), (-1, -1), 3),
        ('BOTTOMPADDING', (0, 0), (-1, -1), 3),
    ]))
    story.append(perf_table)
    story.append(Spacer(1, 10))

    # Add Charts
    fig_dir = "results/figures"
    chart1_path = os.path.join(fig_dir, "scalability_workload_tiers.png")
    chart2_path = os.path.join(fig_dir, "baseline_vs_optimized_search.png")
    chart3_path = os.path.join(fig_dir, "consistent_hashing_churn.png")
    chart4_path = os.path.join(fig_dir, "ratelimiter_rejection_pie.png")

    if os.path.exists(chart1_path) and os.path.exists(chart2_path):
        story.append(Paragraph("<b>Empirical Performance Visualizations:</b>", h2_style))
        img_table = Table([
            [Image(chart1_path, width=3.3*inch, height=2.1*inch), Image(chart2_path, width=3.3*inch, height=2.1*inch)],
            [Paragraph("<i>Figure 1: Scalability across Workload Tiers</i>", subtitle_style), Paragraph("<i>Figure 2: Search Baseline vs Optimized</i>", subtitle_style)]
        ], colWidths=[252, 252])
        img_table.setStyle(TableStyle([
            ('ALIGN', (0, 0), (-1, -1), 'CENTER'),
            ('VALIGN', (0, 0), (-1, -1), 'MIDDLE'),
            ('BOTTOMPADDING', (0, 0), (-1, -1), 2),
            ('TOPPADDING', (0, 0), (-1, -1), 2),
        ]))
        story.append(img_table)
        story.append(Spacer(1, 6))

    if os.path.exists(chart3_path) and os.path.exists(chart4_path):
        img_table2 = Table([
            [Image(chart3_path, width=3.3*inch, height=2.1*inch), Image(chart4_path, width=3.3*inch, height=2.1*inch)],
            [Paragraph("<i>Figure 3: Consistent Hashing Key Churn</i>", subtitle_style), Paragraph("<i>Figure 4: Rate Limiter Rejection Breakdown</i>", subtitle_style)]
        ], colWidths=[252, 252])
        img_table2.setStyle(TableStyle([
            ('ALIGN', (0, 0), (-1, -1), 'CENTER'),
            ('VALIGN', (0, 0), (-1, -1), 'MIDDLE'),
            ('BOTTOMPADDING', (0, 0), (-1, -1), 2),
            ('TOPPADDING', (0, 0), (-1, -1), 2),
        ]))
        story.append(img_table2)
        story.append(Spacer(1, 8))

    # Section 4: Workflow Verification Results
    story.append(Paragraph("4. End-to-End Workflow Verification Suite (14 / 14 Passed)", h1_style))
    wf_text = (
        "All user workflows were automated and executed against the live Spring Boot server via <code>benchmarks/verify_workflows.py</code>. "
        "Every single step succeeded with 100% assertions satisfied:<br/>"
        "• <b>Step 1–3:</b> Member Registration, JWT Login, and Authenticated Access (/api/auth/me) [PASS]<br/>"
        "• <b>Step 4–6:</b> 37-Title Catalogue Listing, Multi-Discipline Search, Physical Copy Availability [PASS]<br/>"
        "• <b>Step 7–10:</b> Physical Copy Borrowing, Active Loan Tracking, Return, and Idempotent Replay Defense [PASS]<br/>"
        "• <b>Step 11–12:</b> FIFO Reservation Queue Placement & Cancellation, Idempotent Fine Payments [PASS]<br/>"
        "• <b>Step 13:</b> 10-Thread Single-Copy Concurrency Race Condition (Zero Double-Borrowing) [PASS]<br/>"
        "• <b>Step 14:</b> Loan Renewal Lifecycle (+14d extension, Waitlist queue defense lock, Max 2 cap rejection) [PASS]"
    )
    story.append(Paragraph(wf_text, body_style))
    story.append(Spacer(1, 8))

    # Section 5: Frontend & Accessibility
    story.append(Paragraph("5. Frontend Design, Accessibility & Cover Imagery", h1_style))
    fe_text = (
        "The Athena LMS frontend was refined to deliver a dignified, high-contrast academic experience:<br/>"
        "• <b>Contrast & Visual Hierarchy:</b> Adopted Athena Navy (#0F2744), Deep Blue (#1460A3), and dark slate typography (#0F172A, #334155), achieving full WCAG AA contrast compliance.<br/>"
        "• <b>Real Cover Imagery:</b> Enriched 28 catalogue titles with verified high-resolution Open Library ISBN covers. Remaining titles feature procedurally generated SVG category-themed covers.<br/>"
        "• <b>Multi-Discipline Breadth:</b> Expanded catalogue across 11 disciplines: Distributed Systems, System Design, Software Engineering, Computer Science, Electrical Engineering, Mechanical Engineering, Civil Engineering, Mathematics, Data Science, Humanities, and General Reading.<br/>"
        "• <b>Production Readiness:</b> Vite production build compiles with zero TypeScript errors into optimized static bundles."
    )
    story.append(Paragraph(fe_text, body_style))

    # Build PDF
    doc.build(story, canvasmaker=NumberedCanvas)
    print(f"Successfully generated publication-grade PDF report: {pdf_filename}")

if __name__ == "__main__":
    build_pdf_report()
