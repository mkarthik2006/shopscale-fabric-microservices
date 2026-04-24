import { create } from 'zustand';
import api, { getApiErrorMessage } from '../services/api';
import { showSuccessToast } from '../utils/toast';

const useStore = create((set, get) => ({
  // Products
  products: [],
  productsLoading: false,
  productsError: '',
  fetchProducts: async () => {
    set({ productsLoading: true, productsError: '' });
    try {
      const res = await api.get('/api/products');
      const products = Array.isArray(res.data) ? res.data : [];
      set({ products, productsLoading: false });
    } catch (err) {
      console.error('Failed to fetch products:', err);
      set({ productsLoading: false, productsError: getApiErrorMessage(err) });
    }
  },

  // Cart
  cartItems: [],
  addToCart: (product) => {
    const items = get().cartItems;
    const existing = items.find(i => i.sku === product.sku);
    if (existing) {
      set({ cartItems: items.map(i => i.sku === product.sku ? { ...i, quantity: i.quantity + 1 } : i) });
    } else {
      set({ cartItems: [...items, { ...product, quantity: 1 }] });
    }
    showSuccessToast(`${product.name} added to cart`);
  },
  removeFromCart: (sku) => {
    const removed = get().cartItems.find(i => i.sku === sku);
    set({ cartItems: get().cartItems.filter(i => i.sku !== sku) });
    showSuccessToast(`${removed?.name || sku} removed from cart`);
  },
  clearCart: () => set({ cartItems: [] }),

  // Orders
  orders: [],
  ordersLoading: false,
  ordersError: '',
  orderLoading: false,
  placeOrder: async () => {
    set({ orderLoading: true });
    const items = get().cartItems.map(i => ({
      sku: i.sku,
      quantity: i.quantity,
    }));
    try {
      const res = await api.post('/api/orders', {
        items,
        currency: 'USD',
      });
      set({ orderLoading: false });
      get().clearCart();
      return res.data;
    } catch (err) {
      console.error('Failed to place order:', err);
      set({ orderLoading: false });
      throw err;
    }
  },
  fetchOrders: async () => {
    set({ ordersLoading: true, ordersError: '' });
    try {
      const res = await api.get('/api/orders/me');
      const orders = Array.isArray(res.data) ? res.data : [];
      set({ orders, ordersLoading: false });
    } catch (err) {
      console.error('Failed to fetch orders:', err);
      set({ ordersLoading: false, ordersError: getApiErrorMessage(err) });
    }
  },
}));

export default useStore;