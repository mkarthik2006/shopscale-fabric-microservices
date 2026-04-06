import { create } from 'zustand';
import api from '../services/api';

const useStore = create((set, get) => ({
  // Products
  products: [],
  productsLoading: false,
  fetchProducts: async () => {
    set({ productsLoading: true });
    try {
      const res = await api.get('/api/products');
      set({ products: res.data, productsLoading: false });
    } catch (err) {
      console.error('Failed to fetch products:', err);
      set({ productsLoading: false });
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
  },
  removeFromCart: (sku) => {
    set({ cartItems: get().cartItems.filter(i => i.sku !== sku) });
  },
  clearCart: () => set({ cartItems: [] }),

  // Orders
  orders: [],
  orderLoading: false,
  placeOrder: async (userId) => {
    set({ orderLoading: true });
    const items = get().cartItems.map(i => ({
      sku: i.sku,
      quantity: i.quantity,
      unitPrice: i.price,
    }));
    const totalAmount = items.reduce((sum, i) => sum + i.unitPrice * i.quantity, 0);
    try {
      const res = await api.post('/api/orders', {
        userId,
        items,
        totalAmount,
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
  fetchOrders: async (userId) => {
    try {
      const res = await api.get(`/api/orders?userId=${userId}`);
      set({ orders: res.data });
    } catch (err) {
      console.error('Failed to fetch orders:', err);
    }
  },
}));

export default useStore;