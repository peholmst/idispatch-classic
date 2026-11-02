import type { Notification, Unit } from './api';

/**
 * Whether an alert still tells the unit where to go: `replaced` when a later alert was sent for the same assignment,
 * as when the dispatcher changed the address and dispatched the unit again, and `obsolete` when the unit is no longer
 * on its assignment. An alert is `current` while the app doesn't know yet which assignment the unit is on.
 */
export type AlertState = 'current' | 'replaced' | 'obsolete';

export function alertState(alert: Notification, alerts: Notification[], unit: Unit | null): AlertState {
  if (alert.assignmentId === undefined) {
    return 'current';
  }
  const later = alerts.some(other => other.assignmentId === alert.assignmentId && isLater(other, alert));
  if (later) {
    return 'replaced';
  }
  if (unit !== null && unit.assignmentId !== alert.assignmentId) {
    return 'obsolete';
  }
  return 'current';
}

function isLater(a: Notification, b: Notification) {
  return a.timestamp > b.timestamp || (a.timestamp === b.timestamp && a.id > b.id);
}
