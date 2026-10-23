import { Route, Routes } from 'react-router-dom';
import { StaffForm } from './StaffForm';
import { StaffList } from './StaffList';
import './module.css';

export function StaffModule() {
  return (
    <Routes>
      <Route index element={<StaffList />} />
      <Route path="new" element={<StaffForm />} />
      <Route path=":id/edit" element={<StaffForm />} />
    </Routes>
  );
}
