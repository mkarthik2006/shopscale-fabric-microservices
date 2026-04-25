import React, { useState } from 'react';
import api, { getApiErrorMessage } from '../services/api';
import { showErrorToast, showSuccessToast } from '../utils/toast';
import useStore from '../store/useStore';
import Icon from '../components/Icon';
import Badge from '../components/Badge';

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
    <div>
      <div className="page-header">
        <div className="page-header__title">
          <span className="page-header__eyebrow">Catalogue · Admin</span>
          <h1 className="page-header__heading">Create Product</h1>
          <p className="page-header__subtitle">
            Publish a new SKU to the catalogue. Persisted via the API gateway endpoint{' '}
            <code className="mono">POST /api/products</code> and broadcast to the storefront.
          </p>
        </div>
        <div className="page-header__actions">
          <Badge tone="accent">Admin only</Badge>
        </div>
      </div>

      {error && (
        <div className="alert alert--danger" role="alert">
          <Icon name="warning" size={18} className="alert__icon" />
          <span>{error}</span>
        </div>
      )}
      {success && (
        <div className="alert alert--success" role="status">
          <Icon name="check" size={18} className="alert__icon" />
          <span>{success}</span>
        </div>
      )}

      <div className="card">
        <form onSubmit={onSubmit}>
          <div className="card__body">
            {/* Identity section */}
            <div className="form-section" style={{ paddingTop: 0 }}>
              <div className="form-section__title">Identity</div>
              <div className="form-section__subtitle">SKU and product name as it appears in the storefront.</div>

              <div className="form-grid mt-3">
                <div className="field">
                  <label className="field__label" htmlFor="sku-input">SKU</label>
                  <input
                    id="sku-input"
                    className="input mono"
                    placeholder="e.g. P100"
                    value={form.sku}
                    onChange={(e) => updateField('sku', e.target.value)}
                    required
                  />
                  <span className="field__hint">Unique stock-keeping unit. Must not collide with existing SKUs.</span>
                </div>
                <div className="field">
                  <label className="field__label" htmlFor="name-input">Product name</label>
                  <input
                    id="name-input"
                    className="input"
                    placeholder="e.g. Wireless Headphones"
                    value={form.name}
                    onChange={(e) => updateField('name', e.target.value)}
                    required
                  />
                  <span className="field__hint">Displayed to customers in the catalogue grid.</span>
                </div>
              </div>
            </div>

            {/* Pricing & stock */}
            <div className="form-section">
              <div className="form-section__title">Pricing &amp; stock</div>
              <div className="form-section__subtitle">Initial price and on-hand quantity. Both can be updated later.</div>

              <div className="form-grid mt-3">
                <div className="field">
                  <label className="field__label" htmlFor="price-input">Price (USD)</label>
                  <div className="field__prefix-wrap">
                    <span className="field__prefix">$</span>
                    <input
                      id="price-input"
                      className="input input--with-prefix"
                      type="number"
                      min="0"
                      step="0.01"
                      placeholder="0.00"
                      value={form.price}
                      onChange={(e) => updateField('price', e.target.value)}
                      required
                    />
                  </div>
                </div>
                <div className="field">
                  <label className="field__label" htmlFor="stock-input">Available stock</label>
                  <input
                    id="stock-input"
                    className="input"
                    type="number"
                    min="0"
                    step="1"
                    placeholder="0"
                    value={form.stock}
                    onChange={(e) => updateField('stock', e.target.value)}
                    required
                  />
                </div>
              </div>
            </div>

            {/* Status */}
            <div className="form-section">
              <div className="form-section__title">Visibility</div>
              <div className="form-section__subtitle">Inactive products are hidden from the storefront grid.</div>
              <label className="checkbox mt-3">
                <input
                  type="checkbox"
                  checked={form.active}
                  onChange={(e) => updateField('active', e.target.checked)}
                />
                Active &nbsp;
                <Badge tone={form.active ? 'success' : 'neutral'}>
                  {form.active ? 'Visible to customers' : 'Hidden'}
                </Badge>
              </label>
            </div>
          </div>

          <div className="card__footer" style={{ display: 'flex', justifyContent: 'flex-end', gap: 8 }}>
            <button
              type="button"
              className="btn btn-ghost"
              onClick={() => setForm(initialForm)}
              disabled={submitting}
            >
              Reset
            </button>
            <button type="submit" className="btn btn-primary" disabled={submitting}>
              {submitting ? (
                <>
                  <span className="spinner" /> Creating…
                </>
              ) : (
                <>
                  <Icon name="add" size={14} /> Create product
                </>
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

export default CreateProductPage;
