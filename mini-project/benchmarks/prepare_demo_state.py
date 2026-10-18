"""
Demo-State Preparation for Report Screenshots
=============================================
Creates a realistic, coherent member state for the demo account
(member@ssn.edu.in) so that report screenshots show live features:

  1. At least one ACTIVE loan            -> My Library page (Renew / Return)
  2. One genuinely overdue return        -> an UNPAID fine via the real
                                            CirculationService overdue math
  3. One PENDING reservation             -> reservation queue card

Everything is produced through the public REST API, so the state reflects
real business rules (idempotent returns, fine caps, FIFO queues).
"""
import json
import time
from datetime import datetime, timedelta, timezone

import requests
from pymongo import MongoClient

BASE = "http://localhost:8080/api"
MONGO_URI = "mongodb://localhost:27017/librarydb"

MEMBER = {"email": "member@ssn.edu.in", "password": "member123"}

# Preferred titles for a nice-looking screenshot set (fallback: any available book)
LOAN_TITLE_PREFERENCE = "Designing Data-Intensive Applications"
OVERDUE_TITLE_PREFERENCE = "Site Reliability Engineering"
RESERVATION_TITLE_PREFERENCE = "System Design Interview"


def login():
    r = requests.post(f"{BASE}/auth/login", json=MEMBER, timeout=15)
    r.raise_for_status()
    data = r.json()["data"]
    return data["token"], data["id"]


def headers(token):
    return {"Authorization": f"Bearer {token}", "Content-Type": "application/json"}


def find_book(token, title_preference, exclude_ids):
    r = requests.get(f"{BASE}/books?size=100", timeout=15)
    books = r.json()["data"]["content"]
    for b in books:
        if title_preference.lower() in (b.get("title") or "").lower() \
                and b["id"] not in exclude_ids and b.get("availableCopies", 0) > 0:
            return b
    for b in books:
        if b["id"] not in exclude_ids and b.get("availableCopies", 0) > 0:
            return b
    return None


def main():
    token, member_id = login()
    hdrs = headers(token)
    summary = {"memberId": member_id}

    loans = requests.get(f"{BASE}/circulation/my-loans", headers=hdrs, timeout=15).json().get("data", [])
    active = [l for l in loans if l.get("status") == "ACTIVE"]
    summary["active_loans_before"] = len(active)

    used_book_ids = set()

    # ---- 1. Ensure an active loan ---------------------------------------
    if not active:
        book = find_book(token, LOAN_TITLE_PREFERENCE, used_book_ids)
        if book:
            r = requests.post(f"{BASE}/circulation/borrow", json={"bookId": book["id"]}, headers=hdrs, timeout=15)
            used_book_ids.add(book["id"])
            summary["active_loan_created"] = {
                "book": book["title"], "http": r.status_code,
                "loanId": (r.json().get("data") or {}).get("loanId")}
            print(f"[1] Borrowed '{book['title']}' -> HTTP {r.status_code}")
        else:
            print("[1] WARNING: no borrowable book found")
    else:
        print(f"[1] Member already has {len(active)} active loan(s)")
        for l in active:
            used_book_ids.add(l.get("bookId"))

    # ---- 2. Ensure an unpaid overdue fine --------------------------------
    fines_payload = requests.get(f"{BASE}/fines/my-fines", headers=hdrs, timeout=15).json().get("data", {})
    unpaid = fines_payload.get("unpaidFines", []) if isinstance(fines_payload, dict) else []
    summary["unpaid_fines_before"] = len(unpaid)

    if not unpaid:
        book = find_book(token, OVERDUE_TITLE_PREFERENCE, used_book_ids)
        if book:
            borrow = requests.post(f"{BASE}/circulation/borrow", json={"bookId": book["id"]},
                                   headers=hdrs, timeout=15).json().get("data", {})
            used_book_ids.add(book["id"])
            loan_id = borrow.get("loanId")
            copy_id = borrow.get("copyId")
            time.sleep(0.3)
            # Backdate the due date so the return crosses into overdue
            client = MongoClient(MONGO_URI, serverSelectionTimeoutMS=5000)
            db = client.get_database("librarydb")
            past_due = datetime.now(timezone.utc) - timedelta(days=16)
            db.loans.update_one({"loanId": loan_id}, {"$set": {"dueDate": past_due}})
            ret = requests.post(f"{BASE}/circulation/return", json={"copyIdOrBarcode": copy_id},
                                headers=hdrs, timeout=15)
            ret_data = ret.json().get("data") or {}
            print(f"[2] Overdue return of '{book['title']}' -> HTTP {ret.status_code}, "
                  f"overdueDays={ret_data.get('overdueDays')}, fineAmount={ret_data.get('fineAmount')}")
            summary["overdue_fine_created"] = {
                "book": book["title"], "overdueDays": ret_data.get("overdueDays"),
                "fineAmount": ret_data.get("fineAmount")}
        else:
            print("[2] WARNING: no book available for overdue simulation")
    else:
        print(f"[2] Member already has {len(unpaid)} unpaid fine(s)")

    # ---- 3. Ensure a pending reservation ---------------------------------
    reservations = requests.get(f"{BASE}/reservations/my-reservations", headers=hdrs, timeout=15)
    if reservations.status_code == 404:
        # Fallback endpoint name
        reservations = requests.get(f"{BASE}/reservations/me", headers=hdrs, timeout=15)
    pending = []
    if reservations.status_code == 200:
        pending = [r for r in reservations.json().get("data", []) if r.get("status") == "PENDING"]
    summary["pending_reservations_before"] = len(pending)

    if not pending:
        book = find_book(token, RESERVATION_TITLE_PREFERENCE, used_book_ids)
        if book:
            r = requests.post(f"{BASE}/reservations", json={"bookId": book["id"]}, headers=hdrs, timeout=15)
            body = r.json()
            print(f"[3] Reservation on '{book['title']}' -> HTTP {r.status_code}: {body.get('message')}")
            summary["reservation_created"] = {"book": book["title"], "http": r.status_code,
                                              "queuePosition": (body.get("data") or {}).get("queuePosition")}
        else:
            print("[3] WARNING: no book available to reserve")
    else:
        print(f"[3] Member already has {len(pending)} pending reservation(s)")

    print("\n" + json.dumps(summary, indent=2))


if __name__ == "__main__":
    main()
