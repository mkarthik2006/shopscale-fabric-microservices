import React, { useEffect } from 'react';
import useStore from '../store/useStore';

const card = {
  border: '1px solid #ddd', borderRadius: '8px', padding: '16px',
  background: '#fff', boxShadow: '0 2px 4px rgba(0,0,0,0.1)',
};
const btn = {
  padding: '8px 16px', background: '#e94560', color: '#fff',
  border: 'none', borderRadius: '4px', cursor: 'pointer', fontWeight: 'bold',
};

function ProductsPage() {
  const { products, productsLoading, fetchProducts, addToCart } = useStore();

  useEffect(() => { fetchProducts(); }, [fetchProducts]);

  if (productsLoading) return <p>Loading products...</p>;

  return (
    <div>
      <h2 style={{ marginBottom: '16px' }}>Product Catalogue</h2>
      {products.length === 0 && <p>No products available. Add some via the API.</p>}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(280px, 1fr))', gap: '16px' }}>
        {products.map((p) => (
          <div key={p.id} style={card}>
            <h3>{p.name}</h3>
            <p>SKU: {p.sku}</p>
            <p style={{ fontSize: '1.4rem', fontWeight: 'bold', color: '#e94560' }}>
              ${p.price?.toFixed?.(2) ?? p.price}
            </p>
            <p>Stock: {p.stock ?? 'N/A'}</p>
            <button style={btn} onClick={() => addToCart(p)}>Add to Cart</button>
          </div>
        ))}
      </div>
    </div>
  );
}

export default ProductsPage;