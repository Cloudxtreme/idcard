DROP SCHEMA IF EXISTS backup CASCADE;
ALTER SCHEMA public RENAME TO backup;
CREATE SCHEMA public AUTHORIZATION card42;
