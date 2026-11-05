-- Who created and changed the records, and when. One row per event, kept forever. The entity is told by its type and
-- ID; a report's ID is the ID of its assignment. The part tells which part of the entity an event is about, such as the
-- scene of a report (SCENE) or a unit's report (UNIT:<resource ID>), and is null when it is about the whole. The
-- subject is the user's ID in Keycloak; the names are copied from the ID token when the event happens, and are null if
-- the token had none.
create sequence audit_events_seq start with 1 increment by 50;

create table audit_events (
    id              bigint                      not null,
    entity_type     varchar(50)                 not null,
    entity_id       bigint                      not null,
    kind            varchar(50)                 not null,
    part            varchar(50),
    user_subject    varchar(255)                not null,
    user_first_name varchar(255),
    user_last_name  varchar(255),
    occurred_at     timestamp(6) with time zone not null,
    constraint audit_events_pk primary key (id)
);

create index audit_events_entity_ix on audit_events (entity_type, entity_id, occurred_at);

-- The reports' edit and close stamps move into the audit log
insert into audit_events (id, entity_type, entity_id, kind, part, user_subject, user_first_name, user_last_name,
                          occurred_at)
select nextval('audit_events_seq'), 'ASSIGNMENT_REPORT', assignment_id, 'EDITED', 'SCENE', last_edited_by_subject,
       last_edited_by_first_name, last_edited_by_last_name, last_edited_at
from assignment_reports
where last_edited_by_subject is not null
  and last_edited_at is not null;

insert into audit_events (id, entity_type, entity_id, kind, part, user_subject, user_first_name, user_last_name,
                          occurred_at)
select nextval('audit_events_seq'), 'ASSIGNMENT_REPORT', r.assignment_id, 'EDITED', 'UNIT:' || u.resource_id,
       u.last_edited_by_subject, u.last_edited_by_first_name, u.last_edited_by_last_name, u.last_edited_at
from assignment_report_units u
         join assignment_reports r on r.id = u.report_id
where u.last_edited_by_subject is not null
  and u.last_edited_at is not null;

insert into audit_events (id, entity_type, entity_id, kind, part, user_subject, user_first_name, user_last_name,
                          occurred_at)
select nextval('audit_events_seq'), 'ASSIGNMENT_REPORT', assignment_id, 'CLOSED', null, closed_by_subject,
       closed_by_first_name, closed_by_last_name, closed_at
from assignment_reports
where closed_by_subject is not null
  and closed_at is not null;

-- Whether a report is closed is all that stays on the report itself
alter table assignment_reports add column closed boolean not null default false;
update assignment_reports set closed = closed_by_subject is not null;

alter table assignment_reports drop column last_edited_by_subject;
alter table assignment_reports drop column last_edited_by_first_name;
alter table assignment_reports drop column last_edited_by_last_name;
alter table assignment_reports drop column last_edited_at;
alter table assignment_reports drop column closed_by_subject;
alter table assignment_reports drop column closed_by_first_name;
alter table assignment_reports drop column closed_by_last_name;
alter table assignment_reports drop column closed_at;

alter table assignment_report_units drop column last_edited_by_subject;
alter table assignment_report_units drop column last_edited_by_first_name;
alter table assignment_report_units drop column last_edited_by_last_name;
alter table assignment_report_units drop column last_edited_at;
