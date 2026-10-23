import { Route, Routes } from 'react-router-dom';
import { ManufacturerForm } from './ManufacturerForm';
import { ManufacturersList } from './ManufacturersList';
import './module.css';

export function ManufacturersModule() {
  return (
    <Routes>
      <Route index element={<ManufacturersList />} />
      <Route path="new" element={<ManufacturerForm />} />
      <Route path=":id/edit" element={<ManufacturerForm />} />
    </Routes>
  );
}
