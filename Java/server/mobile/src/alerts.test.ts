import { describe, expect, it } from 'vitest';
import { alertState } from './alerts';
import type { Notification, Unit } from './api';

function alert(id: number, assignmentId: number | undefined, timestamp: string): Notification {
  return {
    id, assignmentId, timestamp, urgency: 'A', typeCode: '402', type: 'Rakennuspalo', municipality: 'Parainen',
    address: `Rantatie ${id}`, description: '', resources: ['RVS911'],
  };
}

function unitOn(assignmentId: number | null): Unit {
  return { callSign: 'RVS911', locale: 'fi', status: null, nextStates: [], assignmentId };
}

describe('whether an alert is current', () => {
  const first = alert(1, 100, '2026-11-01T10:00:00Z');
  const again = alert(2, 100, '2026-11-01T10:05:00Z');
  const other = alert(3, 200, '2026-11-01T09:00:00Z');
  const all = [again, first, other];

  it('is current for the latest alert of the assignment the unit is on', () => {
    expect(alertState(again, all, unitOn(100))).toBe('current');
  });

  it('is replaced when the unit was dispatched to the same assignment again', () => {
    expect(alertState(first, all, unitOn(100))).toBe('replaced');
  });

  it('is obsolete when the unit is no longer on the assignment', () => {
    expect(alertState(other, all, unitOn(100))).toBe('obsolete');
    expect(alertState(again, all, unitOn(null))).toBe('obsolete');
  });

  it('is replaced rather than obsolete when both are true', () => {
    expect(alertState(first, all, unitOn(null))).toBe('replaced');
  });

  it('tells a later alert sent at the same moment by its ID', () => {
    const same = alert(4, 100, again.timestamp);

    expect(alertState(again, [same, again], unitOn(100))).toBe('replaced');
    expect(alertState(same, [same, again], unitOn(100))).toBe('current');
  });

  it('is current while the app does not know the unit yet, or for an alert without an assignment', () => {
    expect(alertState(other, all, null)).toBe('current');
    expect(alertState(alert(5, undefined, '2026-11-01T08:00:00Z'), all, unitOn(100))).toBe('current');
  });
});
