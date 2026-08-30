import { Moon, Sun } from 'lucide-react'
import { useTheme } from '../../app/providers/useTheme'

export function ThemeToggle() {
  const { theme, toggleTheme } = useTheme()
  const label = theme === 'light' ? 'Switch to dark theme' : 'Switch to light theme'
  return <button type="button" className="icon-button" title={label} aria-label={label} onClick={toggleTheme}>
    {theme === 'light' ? <Moon size={17} /> : <Sun size={17} />}
  </button>
}
