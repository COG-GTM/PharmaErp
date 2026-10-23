import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import { App } from './app/App';
import { routerFuture } from './app/routerFuture';
import { AuthProvider } from './auth';
import './styles/base.css';

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <AuthProvider>
      <BrowserRouter future={routerFuture}>
        <App />
      </BrowserRouter>
    </AuthProvider>
  </StrictMode>,
);
