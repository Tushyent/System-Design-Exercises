"""
Test-Artifact Cleanup Utility
=============================
Removes artifacts left behind by the automated verification suites
(verification books, synthetic users, and their dependent loans / fines /
payments / reservations) so that demos and screenshots show only the
curated catalogue.

Kept intentionally: the seeded catalogue, staff accounts, crawler jobs and
short URLs (legitimate demo data).

Run:  python benchmarks/cleanup_test_artifacts.py
"""
import json
import os
import re
from pymongo import MongoClient

MONGO_URI = "mongodb://localhost:27017/librarydb"
SUMMARY_PATH = os.path.join(os.path.dirname(__file__), "..", "results", "verification",
                            "cleanup_summary.json")

BOOK_TITLE_RE = re.compile(
    r"^(Strict Concurrency Verification Book"
    r"|Loan Renewal Lifecycle Verification Book"
    r"|Concurrency Test Book \d+)$"
)
BOOK_ISBN_PREFIXES = ("CONC-", "RENEW-", "TEST-ISBN-")
MEMBER_EMAIL_RE = re.compile(r"^(testuser|conc_user|user_a|user_b)[\w.]*@ssn\.edu\.in$", re.IGNORECASE)


def main():
    client = MongoClient(MONGO_URI, serverSelectionTimeoutMS=5000)
    db = client.get_database("librarydb")

    summary = {}

    # ---- 1. Identify synthetic books and members -------------------------
    test_books = [b for b in db.books.find({}, {"_id": 1, "title": 1, "isbn": 1})
                  if BOOK_TITLE_RE.match(b.get("title") or "")
                  or str(b.get("isbn") or "").startswith(BOOK_ISBN_PREFIXES)]
    test_book_ids = [b["_id"] for b in test_books]

    test_members = [m for m in db.members.find({}, {"_id": 1, "email": 1})
                    if MEMBER_EMAIL_RE.match(m.get("email") or "")]
    test_member_ids = [m["_id"] for m in test_members]

    summary["books_matched"] = [b["title"] for b in test_books]
    summary["members_matched"] = len(test_members)

    # ---- 2. Collect dependent loans, fines, payments ---------------------
    loan_filter = {"$or": [{"bookId": {"$in": test_book_ids}},
                           {"memberId": {"$in": test_member_ids}}]}
    loans = list(db.loans.find(loan_filter))
    loan_ids = [l.get("loanId") for l in loans if l.get("loanId")]
    active_copy_ids = [l.get("copyId") for l in loans
                       if l.get("status") == "ACTIVE" and l.get("copyId")]
    real_book_ids_touched = sorted({l.get("bookId") for l in loans
                                    if l.get("bookId") and l.get("bookId") not in test_book_ids})

    fine_filter = {"$or": [{"memberId": {"$in": test_member_ids}},
                           {"loanId": {"$in": loan_ids}}]}
    fines = list(db.fines.find(fine_filter))
    fine_ids = [f.get("fineId") for f in fines if f.get("fineId")]

    # ---- 3. Delete in dependency order -----------------------------------
    payments_deleted = db.payments.delete_many({"fineId": {"$in": fine_ids}}).deleted_count
    fines_deleted = db.fines.delete_many(fine_filter).deleted_count
    loans_deleted = db.loans.delete_many(loan_filter).deleted_count
    reservations_deleted = db.reservations.delete_many(
        {"$or": [{"bookId": {"$in": test_book_ids}}, {"memberId": {"$in": test_member_ids}}]}
    ).deleted_count
    copies_deleted = db.book_copies.delete_many({"bookId": {"$in": test_book_ids}}).deleted_count
    books_deleted = db.books.delete_many({"_id": {"$in": test_book_ids}}).deleted_count
    members_deleted = db.members.delete_many({"_id": {"$in": test_member_ids}}).deleted_count

    # ---- 4. Release copies held by removed loans on real books -----------
    released = 0
    if active_copy_ids:
        released = db.book_copies.update_many(
            {"id": {"$in": active_copy_ids}, "status": "BORROWED"},
            {"$set": {"status": "AVAILABLE", "currentLoanId": None, "currentHolderId": None}}
        ).modified_count

    # ---- 5. Recompute copy counters for touched real books ---------------
    books_recounted = 0
    for book_id in real_book_ids_touched:
        total = db.book_copies.count_documents({"bookId": book_id})
        available = db.book_copies.count_documents({"bookId": book_id, "status": "AVAILABLE"})
        db.books.update_one({"_id": book_id}, {"$set": {"totalCopies": total, "availableCopies": available}})
        books_recounted += 1

    summary.update({
        "payments_deleted": payments_deleted,
        "fines_deleted": fines_deleted,
        "loans_deleted": loans_deleted,
        "reservations_deleted": reservations_deleted,
        "copies_deleted": copies_deleted,
        "books_deleted": books_deleted,
        "members_deleted": members_deleted,
        "copies_released": released,
        "real_books_recounted": books_recounted,
        "books_remaining": db.books.count_documents({}),
        "members_remaining": db.members.count_documents({}),
    })

    os.makedirs(os.path.dirname(SUMMARY_PATH), exist_ok=True)
    with open(SUMMARY_PATH, "w", encoding="utf-8") as fh:
        json.dump(summary, fh, indent=2)

    print(json.dumps(summary, indent=2))
    print(f"\nCleanup summary written to {os.path.abspath(SUMMARY_PATH)}")


if __name__ == "__main__":
    main()
