import useStore from './useStore';
import api from '../services/api';

jest.mock('../services/api', () => ({
  __esModule: true,
  default: {
    post: jest.fn(),
    get: jest.fn(),
  },
  getApiErrorMessage: jest.fn(() => 'request failed'),
}));

describe('useStore order operations', () => {
  beforeEach(() => {
    useStore.setState({
      cartItems: [],
      orderLoading: false,
      orders: [],
      ordersLoading: false,
      ordersError: '',
    });
    jest.clearAllMocks();
  });

  test('placeOrder posts SKU/quantity payload and clears cart', async () => {
    useStore.setState({
      cartItems: [
        { sku: 'P1', quantity: 2, price: 10 },
        { sku: 'P2', quantity: 1, price: 20 },
      ],
    });
    api.post.mockResolvedValue({ data: { id: 'o-1' } });

    const response = await useStore.getState().placeOrder();

    expect(api.post).toHaveBeenCalledWith('/api/orders', {
      items: [
        { sku: 'P1', quantity: 2 },
        { sku: 'P2', quantity: 1 },
      ],
      currency: 'USD',
    });
    expect(response).toEqual({ id: 'o-1' });
    expect(useStore.getState().cartItems).toEqual([]);
    expect(useStore.getState().orderLoading).toBe(false);
  });

  test('fetchOrders stores array response from gateway', async () => {
    api.get.mockResolvedValue({ data: [{ id: 'o-1' }, { id: 'o-2' }] });

    await useStore.getState().fetchOrders();

    expect(api.get).toHaveBeenCalledWith('/api/orders/me');
    expect(useStore.getState().orders).toHaveLength(2);
    expect(useStore.getState().ordersLoading).toBe(false);
  });
});
