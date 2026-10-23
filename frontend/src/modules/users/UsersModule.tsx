import { Route, Routes } from 'react-router-dom';
import { UserForm } from './UserForm';
import { UsersList } from './UsersList';
import './module.css';

export function UsersModule() {
  return (
    <Routes>
      <Route index element={<UsersList />} />
      <Route path="new" element={<UserForm />} />
      <Route path=":username/edit" element={<UserForm />} />
    </Routes>
  );
}
