import { Route, Routes } from 'react-router-dom';
import { ClientForm } from './ClientForm';
import { ClientsList } from './ClientsList';
import './module.css';

export function ClientsModule() {
  return (
    <Routes>
      <Route index element={<ClientsList />} />
      <Route path="new" element={<ClientForm />} />
      <Route path=":id/edit" element={<ClientForm />} />
    </Routes>
  );
}
