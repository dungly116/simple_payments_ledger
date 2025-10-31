# Simple Payments Ledger API

A lightweight, in-memory transactional ledger system built with Python and FastAPI for managing accounts and fund transfers with strict atomicity guarantees.

## Quick Start

**Prerequisites:** Python 3.10+

```bash
# Setup
python -m venv venv
source venv/bin/activate  # Windows: venv\Scripts\activate
pip install -r requirements.txt

# Run server
uvicorn app.main:app --reload

# Run tests
pytest app/tests/                # Unit tests
npx playwright test              # E2E tests (all browsers)
pytest --cov=app app/tests/      # With coverage
```

**API Docs:** `http://localhost:8000/docs`

---

## For AI Agents

**Read Order:**
1. This file (orientation)
2. `claude/requirements.md` (specs)
3. `claude/design-decisions.md` (architecture)
4. `claude/testing-strategy.md` (validation)
5. `claude/conversations/` (if needed)

**File Map:**

| File | Use Case |
|------|----------|
| `claude/requirements.md` | Adding features, API contracts |
| `claude/design-decisions.md` | Architecture changes, tradeoff analysis |
| `claude/testing-strategy.md` | Writing tests, edge cases |
| `claude/coding-conventions.md` | Naming, comments, error handling |
| `claude/ai-insights.md` | AI collaboration patterns |
| `claude/conversations/` | Historical context |

**Project Rules:**
- In-memory storages (thread-safe)
- Decimal for money (never float)
- Account-level locking (not global lock)
- Lock ordering by account ID (deadlock prevention)
- Repository + Service pattern

**Constraints:**
- No external DB
- Account balance never negative
- Amounts max 2 decimal places
- Transfers must be atomic
- Tests must fail on broken code

---

## Architecture

**Key Decisions:**

1. **Account-Level Locking** (not global lock)
   - Allows parallel transfers on different accounts
   - Locks acquired in sorted order (deadlock prevention)
   - Code: `app/services/transfer_service.py:67-90`
   - Details: `claude/design-decisions.md`

2. **Decimal for Money** (not float)
   - Exact precision for financial calculations
   - Max 2 decimal places enforced

3. **Repository Pattern**
   - Easy DB migration without changing business logic
   - Dependency injection for testability

---

## Testing

**Coverage:**
- Unit tests: `app/tests/test_transfer_service.py`
- E2E tests: `app/tests/e2e.spec.ts` (24 tests, 3 browsers)
- All tests prove thread-safety
- Full strategy: `claude/testing-strategy.md`

**Run:**
```bash
pytest -v                 # Unit tests
npx playwright test       # E2E tests
```

---

## Production Gaps

- [ ] Database persistence
- [ ] API authentication
- [ ] Rate limiting
- [ ] Idempotency
- [ ] Transaction reversal
- [ ] Pagination
- [ ] Observability

---

## Project Structure

```
app/
├── models/          # Account, Transaction
├── repositories/    # Storage abstraction
├── services/        # Business logic + locking
├── api/            # FastAPI routes
└── tests/
    ├── test_transfer_service.py  # Unit tests
    └── e2e.spec.ts               # Playwright E2E

claude/              # AI context & docs
├── requirements.md
├── design-decisions.md
├── testing-strategy.md
└── coding-conventions.md
```