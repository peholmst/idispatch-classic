-- Reviewers close a report once they have checked it, and nobody can change it until they re-open it. All null while
-- the report is open; otherwise who closed it (Keycloak subject and the names from the ID token) and when.
alter table assignment_reports add column closed_by_subject varchar(255);
alter table assignment_reports add column closed_by_first_name varchar(255);
alter table assignment_reports add column closed_by_last_name varchar(255);
alter table assignment_reports add column closed_at timestamp(6) with time zone;
