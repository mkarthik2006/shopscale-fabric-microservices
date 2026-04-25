import React from 'react';
import Icon from './Icon';

function StatsCard({ icon = 'package', tone = 'brand', label, value, delta, deltaTone }) {
  return (
    <div className="stat-card">
      <span className={`stat-card__icon stat-card__icon--${tone}`}>
        <Icon name={icon} size={20} />
      </span>
      <div className="stat-card__body">
        <div className="stat-card__label">{label}</div>
        <div className="stat-card__value">{value}</div>
        {delta && (
          <div className={`stat-card__delta stat-card__delta--${deltaTone || 'up'}`}>
            <Icon name={deltaTone === 'down' ? 'trending' : 'trending'} size={14} />
            <span>{delta}</span>
          </div>
        )}
      </div>
    </div>
  );
}

export default StatsCard;
