import { Route, Routes } from 'react-router-dom';
import { InvoiceCreate } from './InvoiceCreate';
import { InvoiceReceipt } from './InvoiceReceipt';
import { InvoicesList } from './InvoicesList';
import './module.css';

export function InvoicesModule() {
  return (
    <Routes>
      <Route index element={<InvoicesList />} />
      <Route path="new" element={<InvoiceCreate />} />
      <Route path=":id" element={<InvoiceReceipt />} />
    </Routes>
  );
}
