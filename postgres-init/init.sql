CREATE USER okapi_oscar_user WITH PASSWORD 'okapi_oscar_password';
CREATE SCHEMA okapi_oscar;
GRANT CREATE ON SCHEMA okapi_oscar TO okapi_oscar_user;
GRANT USAGE ON SCHEMA okapi_oscar TO okapi_oscar_user;
GRANT USAGE ON ALL TABLES IN SCHEMA okapi_oscar to okapi_oscar_user;