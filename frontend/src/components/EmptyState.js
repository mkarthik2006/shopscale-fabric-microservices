import React from 'react';
import Icon from './Icon';

function EmptyState({ icon = 'package', title, description, action }) {
  return (
    <div className="empty-state">
      <span className="empty-state__icon">
        <Icon name={icon} size={28} strokeWidth={1.6} />
      </span>
      {title && <h3 className="empty-state__title">{title}</h3>}
      {description && <p className="empty-state__desc">{description}</p>}
      {action && <div>{action}</div>}
    </div>
  );
}

export default EmptyState;
