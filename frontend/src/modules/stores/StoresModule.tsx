import { Route, Routes } from 'react-router-dom';
import { StoreForm } from './StoreForm';
import { StoresList } from './StoresList';
import './module.css';

export function StoresModule() {
  return (
    <Routes>
      <Route index element={<StoresList />} />
      <Route path="new" element={<StoreForm />} />
      <Route path=":id/edit" element={<StoreForm />} />
    </Routes>
  );
}
