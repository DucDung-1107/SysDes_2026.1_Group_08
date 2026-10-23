"""Render the MEDORA functional decomposition as a fixed grid.

Run from any directory:  python functional-decomposition.py
Output: ../figure/functional-decomposition.png (page-width figure, 400 dpi).
"""
from pathlib import Path

import matplotlib

matplotlib.use("Agg")
import matplotlib.pyplot as plt
from matplotlib.patches import FancyBboxPatch

NAVY, BLUE, LIGHT, PALE, TEXT = "#163A5F", "#2563A6", "#EAF3FB", "#F5F9FE", "#183247"

GROUPS = [
    ("Identity and Access", [
        "Authenticate and\nManage Sessions", "Manage Patient Identity\nand Duplicate Review",
        "Search Longitudinal\nRecords", "Enforce Role and\nScope Control"]),
    ("Scheduling and Reception", [
        "Manage Appointments", "Check In and\nManage Queue",
        "Validate Resource\nAvailability", "Send Operational\nNotifications (Phase 2)"]),
    ("Clinical Care", [
        "Record Screening and\nVital Signs", "Document Encounter",
        "Record Diagnosis, Closure\nand Follow-up"]),
    ("Orders and Execution", [
        "Validate and Issue\nPrescription", "Record Medicine Event",
        "Create Service or\nProcedure Order", "Allocate Resources and\nRecord Execution Result"]),
    ("Finance and Operations", [
        "Create Invoice and\nRecord Payment", "Record Authorized\nRefund",
        "Manage Service and\nPrice Catalog", "View Dashboard\nand Reports"]),
    ("Governance", [
        "Administer Accounts,\nFacilities and Catalogs", "Control Attachments\nand Exports",
        "Review Audit Trail"]),
]

W, H = 6.3, 4.6          # inches: report text width
fig = plt.figure(figsize=(W, H), dpi=400, facecolor=PALE)
ax = fig.add_axes([0, 0, 1, 1])
ax.set_xlim(0, W)
ax.set_ylim(0, H)
ax.axis("off")

ax.text(W / 2, H - 0.16, "MEDORA: Functional Decomposition", ha="center", va="center",
        fontsize=9, fontweight="bold", color=NAVY, family="Arial")

margin, gap = 0.12, 0.12
top = H - 0.34
cols, rows = 2, 3
pw = (W - 2 * margin - gap) / cols
ph = (top - margin - gap * (rows - 1)) / rows

for i, (title, items) in enumerate(GROUPS):
    r, c = divmod(i, cols)
    x = margin + c * (pw + gap)
    y = top - (r + 1) * ph - r * gap
    ax.add_patch(FancyBboxPatch((x, y), pw, ph, boxstyle="round,pad=0,rounding_size=0.05",
                                fc="white", ec=NAVY, lw=0.8))
    band = 0.22
    ax.add_patch(FancyBboxPatch((x, y + ph - band), pw, band, boxstyle="round,pad=0,rounding_size=0.05",
                                fc=NAVY, ec=NAVY, lw=0.8))
    ax.text(x + pw / 2, y + ph - band / 2, title, ha="center", va="center", fontsize=7.5,
            fontweight="bold", color="white", family="Arial")
    inner_top = y + ph - band - 0.07
    inner_h = ph - band - 0.14
    n_rows = 2
    bw = (pw - 0.21) / 2
    bh = (inner_h - 0.07) / n_rows
    for k, label in enumerate(items):
        kr, kc = divmod(k, 2)
        bx = x + 0.07 + kc * (bw + 0.07)
        by = inner_top - (kr + 1) * bh - kr * 0.07
        ax.add_patch(FancyBboxPatch((bx, by), bw, bh, boxstyle="round,pad=0,rounding_size=0.04",
                                    fc=LIGHT, ec=BLUE, lw=0.6))
        ax.text(bx + bw / 2, by + bh / 2, label, ha="center", va="center", fontsize=7,
                color=TEXT, family="Arial", linespacing=1.15)

out = Path(__file__).resolve().parent.parent / "figure" / "functional-decomposition.png"
fig.savefig(out, dpi=400, facecolor=PALE)
print(out)
