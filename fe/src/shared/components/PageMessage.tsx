type PageMessageProps = {
  tone?: 'error' | 'success' | 'info'
  children: string
}

export function PageMessage({ tone = 'info', children }: PageMessageProps) {
  return <p className={`page-message page-message--${tone}`} role={tone === 'error' ? 'alert' : 'status'}>{children}</p>
}
