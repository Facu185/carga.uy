import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
// Tipografías incluidas en el sitio (no se piden a Google Fonts): cargan sin parpadeo y sin servicios externos
import '@fontsource/atkinson-hyperlegible/400.css'
import '@fontsource/atkinson-hyperlegible/700.css'
import '@fontsource/barlow-semi-condensed/700.css'
import './index.css'
import App from './App.tsx'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>,
)
