import React, { useState } from 'react';
import api, { getApiErrorMessage } from '../services/api';
import { showErrorToast, showSuccessToast } from '../utils/toast';
import useStore from '../store/useStore';

const initialForm = {
  sku: '',
  name: '',
  price: '',
  stock: '',
  active: true,
};

function CreateProductPage() {
  const { fetchProducts } = useStore();
  const [form, setForm] = useState(initialForm);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const updateField = (key, value) => setForm((prev) => ({ ...prev, [key]: value }));

  const onSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setSuccess('');
    setSubmitting(true);
    try {
      await api.post('/api/products', {
        sku: form.sku.trim(),
        name: form.name.trim(),
        price: Number(form.price),
        stock: Number(form.stock),
        active: Boolean(form.active),
      });
      setSuccess('Product created successfully.');
      showSuccessToast('Product created successfully.');
      setForm(initialForm);
      await fetchProducts();
    } catch (err) {
      const message = getApiErrorMessage(err);
      setError(message);
      showErrorToast(message);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="card">
      <div className="page-header">
        <h2 style={{ margin: 0 }}>Create Product</h2>
      </div>
      <p className="muted">Creates product via gateway endpoint `POST /api/products`.</p>
      {error && <div className="status status-error">{error}</div>}
      {success && <div className="status status-success">{success}</div>}
      <form onSubmit={onSubmit}>
        <div className="form-grid">
          <label>
            SKU (unique code)
            <input
              className="input"
              value={form.sku}
              onChange={(e) => updateField('sku', e.target.value)}
              required
            />
          </label>
          <label>
            Product Name
            <input
              className="input"
              value={form.name}
              onChange={(e) => updateField('name', e.target.value)}
              required
            />
          </label>
          <label>
            Price (INR)
            <input
              className="input"
              type="number"
              min="0"
              step="0.01"
              value={form.price}
              onChange={(e) => updateField('price', e.target.value)}
              required
            />
          </label>
          <label>
            Available Stock
            <input
              className="input"
              type="number"
              min="0"
              step="1"
              value={form.stock}
              onChange={(e) => updateField('stock', e.target.value)}
              required
            />
          </label>
        </div>
        <label style={{ display: 'inline-flex', gap: 8, alignItems: 'center', marginTop: 12 }}>
          <input
            type="checkbox"
            checked={form.active}
            onChange={(e) => updateField('active', e.target.checked)}
          />
          Active
        </label>
        <div style={{ marginTop: 14 }}>
          <button className="btn btn-primary" disabled={submitting}>
            {submitting ? 'Creating...' : 'Create Product'}
          </button>
        </div>
      </form>
    </div>
  );
}

export default CreateProductPage;
