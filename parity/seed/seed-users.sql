-- Identical seed data loaded into BOTH the legacy and the migrated database for parity runs.
-- Passwords are plaintext here because that is how the legacy system stores them.
INSERT INTO `user` (`username`, `password`) VALUES
  ('tarang',       'parisdkh'),
  ('pharmacist01', 'Secure#2024'),
  ('storemgr',     'warehouse88'),
  ('accounts',     'ledger2024');
