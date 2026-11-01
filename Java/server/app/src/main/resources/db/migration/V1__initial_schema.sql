-- Initial schema, matching the JPA mappings in net.pkhapps.idispatch.server.entity.
-- Hibernate allocates ids from one sequence per entity hierarchy, 50 ids at a time.

create sequence assignment_types_seq start with 1 increment by 50;
create sequence assignments_seq start with 1 increment by 50;
create sequence destinations_seq start with 1 increment by 50;
create sequence dispatch_notifications_seq start with 1 increment by 50;
create sequence municipalities_seq start with 1 increment by 50;
create sequence receipts_seq start with 1 increment by 50;
create sequence resource_status_archive_seq start with 1 increment by 50;
create sequence resource_status_seq start with 1 increment by 50;
create sequence resource_types_seq start with 1 increment by 50;
create sequence resources_seq start with 1 increment by 50;
create sequence users_seq start with 1 increment by 50;

create table users (
    id               bigint       not null,
    version          bigint       not null,
    username         varchar(255) not null,
    encoded_password varchar(255) not null,
    is_dispatcher    boolean      not null,
    is_admin         boolean      not null,
    is_report_reader boolean      not null,
    is_enabled       boolean      not null,
    constraint users_pk primary key (id),
    constraint users_username_uk unique (username)
);

create table municipalities (
    id      bigint       not null,
    version bigint       not null,
    name    varchar(255) not null,
    active  boolean      not null,
    constraint municipalities_pk primary key (id),
    constraint municipalities_name_uk unique (name)
);

create table assignment_types (
    id          bigint       not null,
    version     bigint       not null,
    code        varchar(255) not null,
    description varchar(255) not null,
    active      boolean      not null,
    constraint assignment_types_pk primary key (id),
    constraint assignment_types_code_uk unique (code)
);

create table resource_types (
    id          bigint       not null,
    version     bigint       not null,
    code        varchar(255) not null,
    description varchar(255) not null,
    active      boolean      not null,
    constraint resource_types_pk primary key (id),
    constraint resource_types_code_uk unique (code)
);

create table resources (
    id        bigint       not null,
    version   bigint       not null,
    call_sign varchar(255) not null,
    type_id   bigint       not null,
    active    boolean      not null,
    constraint resources_pk primary key (id),
    constraint resources_call_sign_uk unique (call_sign),
    constraint resources_type_fk foreign key (type_id) references resource_types
);

create table assignments (
    id              bigint                      not null,
    version         bigint                      not null,
    opened          timestamp(6) with time zone not null,
    closed          timestamp(6) with time zone,
    urgency         varchar(255)                not null,
    type_id         bigint,
    description     varchar(255)                not null,
    municipality_id bigint,
    address         varchar(255)                not null,
    constraint assignments_pk primary key (id),
    constraint assignments_urgency_ck check (urgency in ('A', 'B', 'C', 'D', 'N')),
    constraint assignments_type_fk foreign key (type_id) references assignment_types,
    constraint assignments_municipality_fk foreign key (municipality_id) references municipalities
);

create table resource_status (
    id            bigint                      not null,
    version       bigint                      not null,
    ts            timestamp(6) with time zone not null,
    state         varchar(255)                not null,
    assignment_id bigint,
    resource_id   bigint                      not null,
    available     boolean                     not null,
    assigned      boolean                     not null,
    constraint resource_status_pk primary key (id),
    constraint resource_status_resource_uk unique (resource_id),
    constraint resource_status_state_ck check (state in
        ('RESERVED', 'DISPATCHED', 'EN_ROUTE', 'ON_SCENE', 'AVAILABLE', 'AT_STATION', 'OUT_OF_SERVICE')),
    constraint resource_status_assignment_fk foreign key (assignment_id) references assignments,
    constraint resource_status_resource_fk foreign key (resource_id) references resources
);

create index resource_status_ts_idx on resource_status (ts);

create table resource_status_archive (
    id            bigint                      not null,
    ts            timestamp(6) with time zone not null,
    state         varchar(255)                not null,
    assignment_id bigint,
    resource_id   bigint                      not null,
    constraint resource_status_archive_pk primary key (id),
    constraint resource_status_archive_state_ck check (state in
        ('RESERVED', 'DISPATCHED', 'EN_ROUTE', 'ON_SCENE', 'AVAILABLE', 'AT_STATION', 'OUT_OF_SERVICE')),
    constraint resource_status_archive_assignment_fk foreign key (assignment_id) references assignments,
    constraint resource_status_archive_resource_fk foreign key (resource_id) references resources
);

create index resource_status_archive_ts_idx on resource_status_archive (ts);

create table destinations (
    id      bigint  not null,
    version bigint  not null,
    active  boolean not null,
    constraint destinations_pk primary key (id)
);

create table destination_resources (
    destination_id bigint not null,
    resource_id    bigint not null,
    constraint destination_resources_pk primary key (destination_id, resource_id),
    constraint destination_resources_destination_fk foreign key (destination_id) references destinations,
    constraint destination_resources_resource_fk foreign key (resource_id) references resources
);

create table runboard_destinations (
    id           bigint       not null,
    runboard_key varchar(255) not null,
    constraint runboard_destinations_pk primary key (id),
    constraint runboard_destinations_runboard_key_uk unique (runboard_key),
    constraint runboard_destinations_destination_fk foreign key (id) references destinations
);

create table sms_destinations (
    id bigint not null,
    constraint sms_destinations_pk primary key (id),
    constraint sms_destinations_destination_fk foreign key (id) references destinations
);

create table sms_destination_phone_numbers (
    sms_destination_id bigint not null,
    phone_numbers      varchar(255),
    constraint sms_destination_phone_numbers_uk unique (sms_destination_id, phone_numbers),
    constraint sms_destination_phone_numbers_destination_fk foreign key (sms_destination_id) references sms_destinations
);

create table dispatch_notifications (
    id                         bigint                      not null,
    ts                         timestamp(6) with time zone not null,
    assignment_id              bigint                      not null,
    assignment_municipality_id bigint                      not null,
    assignment_address         varchar(255)                not null,
    assignment_type_id         bigint                      not null,
    assignment_urgency         varchar(255)                not null,
    assignment_description     varchar(255)                not null,
    constraint dispatch_notifications_pk primary key (id),
    constraint dispatch_notifications_urgency_ck check (assignment_urgency in ('A', 'B', 'C', 'D', 'N')),
    constraint dispatch_notifications_assignment_fk foreign key (assignment_id) references assignments,
    constraint dispatch_notifications_municipality_fk foreign key (assignment_municipality_id) references municipalities,
    constraint dispatch_notifications_type_fk foreign key (assignment_type_id) references assignment_types
);

create table dispatch_notification_assigned_resources (
    dispatch_notification_id bigint not null,
    resource_id              bigint not null,
    constraint dispatch_notification_assigned_resources_pk primary key (dispatch_notification_id, resource_id),
    constraint dispatch_notification_assigned_resources_notification_fk
        foreign key (dispatch_notification_id) references dispatch_notifications,
    constraint dispatch_notification_assigned_resources_resource_fk foreign key (resource_id) references resources
);

create table dispatch_notification_destinations (
    dispatch_notification_id bigint not null,
    destination_id           bigint not null,
    constraint dispatch_notification_destinations_pk primary key (destination_id, dispatch_notification_id),
    constraint dispatch_notification_destinations_notification_fk
        foreign key (dispatch_notification_id) references dispatch_notifications,
    constraint dispatch_notification_destinations_destination_fk foreign key (destination_id) references destinations
);

create table receipts (
    id              bigint                      not null,
    ts              timestamp(6) with time zone not null,
    destination_id  bigint                      not null,
    notification_id bigint                      not null,
    constraint receipts_pk primary key (id),
    constraint receipts_destination_notification_uk unique (destination_id, notification_id),
    constraint receipts_destination_fk foreign key (destination_id) references destinations,
    constraint receipts_notification_fk foreign key (notification_id) references dispatch_notifications
);
