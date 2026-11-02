/** The time an alert was sent, with the date unless it was today, in the destination's language. */
export function formatTime(timestamp: string, locale: string, now: Date = new Date()): string {
  const sent = new Date(timestamp);
  const today = sent.toDateString() === now.toDateString();
  return new Intl.DateTimeFormat(locale, today
    ? { hour: '2-digit', minute: '2-digit' }
    : { day: 'numeric', month: 'numeric', hour: '2-digit', minute: '2-digit' }).format(sent);
}
