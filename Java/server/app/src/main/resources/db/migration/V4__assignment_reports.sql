-- What happened on scene of a closed assignment, and what each responding unit found and did there.
create sequence assignment_reports_seq start with 1 increment by 50;

create table assignment_reports (
    id                    bigint         not null,
    version               bigint         not null,
    assignment_id         bigint         not null,
    situation_development varchar(4000)  not null,
    people_involved       integer,
    lightly_injured       integer,
    seriously_injured     integer,
    deceased              integer,
    constraint assignment_reports_pk primary key (id),
    constraint assignment_reports_assignment_uk unique (assignment_id),
    constraint assignment_reports_assignment_fk foreign key (assignment_id) references assignments,
    constraint assignment_reports_counts_ck check (people_involved >= 0 and lightly_injured >= 0
        and seriously_injured >= 0 and deceased >= 0)
);

create table assignment_report_units (
    report_id            bigint        not null,
    resource_id          bigint        not null,
    situation_on_arrival varchar(4000) not null,
    actions_taken        varchar(4000) not null,
    constraint assignment_report_units_pk primary key (report_id, resource_id),
    constraint assignment_report_units_report_fk foreign key (report_id) references assignment_reports,
    constraint assignment_report_units_resource_fk foreign key (resource_id) references resources
);
