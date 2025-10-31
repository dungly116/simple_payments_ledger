import { test, expect } from '@playwright/test';

const API_KEY = '7f8c9d2e-4b5a-6c3d-8e1f-9a2b3c4d5e6f';

test.describe('Payments Ledger API E2E Tests', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/');
  });

  test('health check endpoint returns ok', async ({ request }) => {
    const response = await request.get('/health');
    expect(response.ok()).toBeTruthy();

    const data = await response.json();
    expect(data.status).toBe('ok');
    expect(data.service).toBe('payments-ledger');
  });

  test('create account with valid initial balance', async ({ request }) => {
    const response = await request.post('/accounts', {
      headers: {
        'X-API-Key': API_KEY,
        'Content-Type': 'application/json',
      },
      data: {
        initial_balance: 1000.50
      }
    });

    expect(response.status()).toBe(201);
    const account = await response.json();

    expect(account).toHaveProperty('id');
    expect(account.balance).toBe('1000.50');
    expect(account).toHaveProperty('created_at');
  });

  test('get account by id returns account details', async ({ request }) => {
    const createResponse = await request.post('/accounts', {
      headers: {
        'X-API-Key': API_KEY,
        'Content-Type': 'application/json',
      },
      data: {
        initial_balance: 500.00
      }
    });
    const createdAccount = await createResponse.json();

    const getResponse = await request.get(`/accounts/${createdAccount.id}`, {
      headers: {
        'X-API-Key': API_KEY,
      }
    });

    expect(getResponse.status()).toBe(200);
    const account = await getResponse.json();
    expect(account.id).toBe(createdAccount.id);
    expect(account.balance).toBe('500.00');
  });

  test('get non-existent account returns 404', async ({ request }) => {
    const response = await request.get('/accounts/non_existent_account', {
      headers: {
        'X-API-Key': API_KEY,
      }
    });

    expect(response.status()).toBe(404);
    const error = await response.json();
    expect(error.detail).toContain('not found');
  });

  test('transfer funds between accounts successfully', async ({ request }) => {
    const account1 = await request.post('/accounts', {
      headers: { 'X-API-Key': API_KEY, 'Content-Type': 'application/json' },
      data: { initial_balance: 1000.00 }
    }).then(res => res.json());

    const account2 = await request.post('/accounts', {
      headers: { 'X-API-Key': API_KEY, 'Content-Type': 'application/json' },
      data: { initial_balance: 500.00 }
    }).then(res => res.json());

    const transferResponse = await request.post('/transactions', {
      headers: {
        'X-API-Key': API_KEY,
        'Content-Type': 'application/json',
      },
      data: {
        from_account_id: account1.id,
        to_account_id: account2.id,
        amount: 250.00
      }
    });

    expect(transferResponse.status()).toBe(201);
    const transaction = await transferResponse.json();

    expect(transaction).toHaveProperty('id');
    expect(transaction.from_account_id).toBe(account1.id);
    expect(transaction.to_account_id).toBe(account2.id);
    expect(transaction.amount).toBe('250.00');

    const updatedAccount1 = await request.get(`/accounts/${account1.id}`, {
      headers: { 'X-API-Key': API_KEY }
    }).then(res => res.json());
    expect(updatedAccount1.balance).toBe('750.00');

    const updatedAccount2 = await request.get(`/accounts/${account2.id}`, {
      headers: { 'X-API-Key': API_KEY }
    }).then(res => res.json());
    expect(updatedAccount2.balance).toBe('750.00');
  });

  test('transfer with insufficient funds returns 400', async ({ request }) => {
    const account1 = await request.post('/accounts', {
      headers: { 'X-API-Key': API_KEY, 'Content-Type': 'application/json' },
      data: { initial_balance: 100.00 }
    }).then(res => res.json());

    const account2 = await request.post('/accounts', {
      headers: { 'X-API-Key': API_KEY, 'Content-Type': 'application/json' },
      data: { initial_balance: 0.00 }
    }).then(res => res.json());

    const transferResponse = await request.post('/transactions', {
      headers: {
        'X-API-Key': API_KEY,
        'Content-Type': 'application/json',
      },
      data: {
        from_account_id: account1.id,
        to_account_id: account2.id,
        amount: 200.00
      }
    });

    expect(transferResponse.status()).toBe(400);
    const error = await transferResponse.json();
    expect(error.detail).toContain('insufficient');
  });


  test('transfer to non-existent account returns 404', async ({ request }) => {
    const account1 = await request.post('/accounts', {
      headers: { 'X-API-Key': API_KEY, 'Content-Type': 'application/json' },
      data: { initial_balance: 1000.00 }
    }).then(res => res.json());

    const transferResponse = await request.post('/transactions', {
      headers: {
        'X-API-Key': API_KEY,
        'Content-Type': 'application/json',
      },
      data: {
        from_account_id: account1.id,
        to_account_id: 'non_existent_account',
        amount: 100.00
      }
    });

    expect(transferResponse.status()).toBe(404);
  });


  test('complete user journey: create accounts, multiple transfers, verify balances', async ({ request }) => {
    const alice = await request.post('/accounts', {
      headers: { 'X-API-Key': API_KEY, 'Content-Type': 'application/json' },
      data: { initial_balance: 1000.00 }
    }).then(res => res.json());

    const bob = await request.post('/accounts', {
      headers: { 'X-API-Key': API_KEY, 'Content-Type': 'application/json' },
      data: { initial_balance: 500.00 }
    }).then(res => res.json());

    const charlie = await request.post('/accounts', {
      headers: { 'X-API-Key': API_KEY, 'Content-Type': 'application/json' },
      data: { initial_balance: 0.00 }
    }).then(res => res.json());

    await request.post('/transactions', {
      headers: { 'X-API-Key': API_KEY, 'Content-Type': 'application/json' },
      data: {
        from_account_id: alice.id,
        to_account_id: bob.id,
        amount: 200.00
      }
    });

    await request.post('/transactions', {
      headers: { 'X-API-Key': API_KEY, 'Content-Type': 'application/json' },
      data: {
        from_account_id: bob.id,
        to_account_id: charlie.id,
        amount: 300.00
      }
    });

    await request.post('/transactions', {
      headers: { 'X-API-Key': API_KEY, 'Content-Type': 'application/json' },
      data: {
        from_account_id: alice.id,
        to_account_id: charlie.id,
        amount: 100.00
      }
    });

    const finalAlice = await request.get(`/accounts/${alice.id}`, {
      headers: { 'X-API-Key': API_KEY }
    }).then(res => res.json());
    expect(finalAlice.balance).toBe('700.00');

    const finalBob = await request.get(`/accounts/${bob.id}`, {
      headers: { 'X-API-Key': API_KEY }
    }).then(res => res.json());
    expect(finalBob.balance).toBe('400.00');

    const finalCharlie = await request.get(`/accounts/${charlie.id}`, {
      headers: { 'X-API-Key': API_KEY }
    }).then(res => res.json());
    expect(finalCharlie.balance).toBe('400.00');

    const totalBalance = parseFloat(finalAlice.balance) +
                        parseFloat(finalBob.balance) +
                        parseFloat(finalCharlie.balance);
    expect(totalBalance).toBe(1500.00);
  });
});
