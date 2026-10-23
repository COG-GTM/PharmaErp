import { Route, Routes } from 'react-router-dom';
import { InventoryForm } from './InventoryForm';
import { InventoryList } from './InventoryList';
import './module.css';

export function InventoryModule() {
  return (
    <Routes>
      <Route index element={<InventoryList />} />
      <Route path="new" element={<InventoryForm />} />
      <Route path=":id/edit" element={<InventoryForm />} />
    </Routes>
  );
}
