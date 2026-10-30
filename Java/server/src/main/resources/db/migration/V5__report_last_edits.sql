-- Who last changed the situation and counts of a report, and who last changed each unit's report. The subject is the
-- user's ID in Keycloak; the names are copied from the ID token when the edit is made. All null until someone edits.
alter table assignment_reports add column last_edited_by_subject varchar(255);
alter table assignment_reports add column last_edited_by_first_name varchar(255);
alter table assignment_reports add column last_edited_by_last_name varchar(255);
alter table assignment_reports add column last_edited_at timestamp(6) with time zone;

alter table assignment_report_units add column last_edited_by_subject varchar(255);
alter table assignment_report_units add column last_edited_by_first_name varchar(255);
alter table assignment_report_units add column last_edited_by_last_name varchar(255);
alter table assignment_report_units add column last_edited_at timestamp(6) with time zone;
