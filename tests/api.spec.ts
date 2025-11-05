import { test, expect } from '@playwright/test';

const BASE_URL = 'http://localhost:8080/api/v1';

test.describe('Payments Ledger API', () => {

  test('health check endpoint returns healthy status', async ({ request }) => {
    const response = await request.get(`${BASE_URL}/health`);
    expect(response.ok()).toBeTruthy();
    const body = await response.json();
    expect(body.status).toBe('healthy');
  });

  test('create account with initial balance', async ({ request }) => {
    const response = await request.post(`${BASE_URL}/accounts`, {
      data: {
        initialBalance: '1000.00'
      }
    });

    expect(response.status()).toBe(201);
    const body = await response.json();
    expect(body.id).toMatch(/^acc_[a-z0-9]{10}$/);
    expect(body.balance).toBe(1000.00);
  });

  test('get account by id', async ({ request }) => {
    const createResponse = await request.post(`${BASE_URL}/accounts`, {
      data: { initialBalance: '500.00' }
    });
    const { id } = await createResponse.json();

    const getResponse = await request.get(`${BASE_URL}/accounts/${id}`);
    expect(getResponse.ok()).toBeTruthy();
    const body = await getResponse.json();
    expect(body.id).toBe(id);
    expect(body.balance).toBe(500.00);
  });

  test('get non-existent account returns 404', async ({ request }) => {
    const response = await request.get(`${BASE_URL}/accounts/acc_nonexistent`);
    expect(response.status()).toBe(404);
  });

  test('successful transfer between accounts', async ({ request }) => {
    const acc1Response = await request.post(`${BASE_URL}/accounts`, {
      data: { initialBalance: '1000.00' }
    });
    const acc1 = await acc1Response.json();

    const acc2Response = await request.post(`${BASE_URL}/accounts`, {
      data: { initialBalance: '500.00' }
    });
    const acc2 = await acc2Response.json();

    const transferResponse = await request.post(`${BASE_URL}/transactions`, {
      data: {
        fromAccountId: acc1.id,
        toAccountId: acc2.id,
        amount: '250.00'
      }
    });

    expect(transferResponse.status()).toBe(201);
    const transaction = await transferResponse.json();
    expect(transaction.id).toMatch(/^txn_[a-z0-9]{10}$/);
    expect(transaction.fromAccountId).toBe(acc1.id);
    expect(transaction.toAccountId).toBe(acc2.id);
    expect(transaction.amount).toBe(250.00);
    expect(transaction.status).toBe('COMPLETED');

    const acc1After = await (await request.get(`${BASE_URL}/accounts/${acc1.id}`)).json();
    const acc2After = await (await request.get(`${BASE_URL}/accounts/${acc2.id}`)).json();

    expect(acc1After.balance).toBe(750.00);
    expect(acc2After.balance).toBe(750.00);
  });

  test('transfer with insufficient funds returns 400', async ({ request }) => {
    const acc1Response = await request.post(`${BASE_URL}/accounts`, {
      data: { initialBalance: '100.00' }
    });
    const acc1 = await acc1Response.json();

    const acc2Response = await request.post(`${BASE_URL}/accounts`, {
      data: { initialBalance: '0.00' }
    });
    const acc2 = await acc2Response.json();

    const transferResponse = await request.post(`${BASE_URL}/transactions`, {
      data: {
        fromAccountId: acc1.id,
        toAccountId: acc2.id,
        amount: '150.00'
      }
    });

    expect(transferResponse.status()).toBe(400);
    const error = await transferResponse.json();
    expect(error.message).toContain('insufficient funds');
  });

  test('transfer with negative amount returns 400', async ({ request }) => {
    const acc1Response = await request.post(`${BASE_URL}/accounts`, {
      data: { initialBalance: '1000.00' }
    });
    const acc1 = await acc1Response.json();

    const acc2Response = await request.post(`${BASE_URL}/accounts`, {
      data: { initialBalance: '500.00' }
    });
    const acc2 = await acc2Response.json();

    const transferResponse = await request.post(`${BASE_URL}/transactions`, {
      data: {
        fromAccountId: acc1.id,
        toAccountId: acc2.id,
        amount: '-50.00'
      }
    });

    expect(transferResponse.status()).toBe(400);
  });

  test('transfer to non-existent account returns 404', async ({ request }) => {
    const acc1Response = await request.post(`${BASE_URL}/accounts`, {
      data: { initialBalance: '1000.00' }
    });
    const acc1 = await acc1Response.json();

    const transferResponse = await request.post(`${BASE_URL}/transactions`, {
      data: {
        fromAccountId: acc1.id,
        toAccountId: 'acc_nonexistent',
        amount: '100.00'
      }
    });

    expect(transferResponse.status()).toBe(404);
  });

  test('get transaction by id', async ({ request }) => {
    const acc1Response = await request.post(`${BASE_URL}/accounts`, {
      data: { initialBalance: '1000.00' }
    });
    const acc1 = await acc1Response.json();

    const acc2Response = await request.post(`${BASE_URL}/accounts`, {
      data: { initialBalance: '500.00' }
    });
    const acc2 = await acc2Response.json();

    const transferResponse = await request.post(`${BASE_URL}/transactions`, {
      data: {
        fromAccountId: acc1.id,
        toAccountId: acc2.id,
        amount: '100.00'
      }
    });
    const transaction = await transferResponse.json();

    const getResponse = await request.get(`${BASE_URL}/transactions/${transaction.id}`);
    expect(getResponse.ok()).toBeTruthy();
    const body = await getResponse.json();
    expect(body.id).toBe(transaction.id);
    expect(body.status).toBe('COMPLETED');
  });

  test('create account with negative balance returns 400', async ({ request }) => {
    const response = await request.post(`${BASE_URL}/accounts`, {
      data: { initialBalance: '-100.00' }
    });

    expect(response.status()).toBe(400);
  });

  test('exact balance transfer leaves account with zero', async ({ request }) => {
    const acc1Response = await request.post(`${BASE_URL}/accounts`, {
      data: { initialBalance: '100.00' }
    });
    const acc1 = await acc1Response.json();

    const acc2Response = await request.post(`${BASE_URL}/accounts`, {
      data: { initialBalance: '0.00' }
    });
    const acc2 = await acc2Response.json();

    const transferResponse = await request.post(`${BASE_URL}/transactions`, {
      data: {
        fromAccountId: acc1.id,
        toAccountId: acc2.id,
        amount: '100.00'
      }
    });

    expect(transferResponse.status()).toBe(201);

    const acc1After = await (await request.get(`${BASE_URL}/accounts/${acc1.id}`)).json();
    const acc2After = await (await request.get(`${BASE_URL}/accounts/${acc2.id}`)).json();

    expect(acc1After.balance).toBe(0.00);
    expect(acc2After.balance).toBe(100.00);
  });
});
