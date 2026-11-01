-- A mobile destination is the mobile app of one unit. The app authenticates with a generated key, of which only the
-- SHA-256 hash is stored (hex), so the key can be shown to the administrator only when it is generated.
create table mobile_destinations (
    id       bigint      not null,
    key_hash varchar(64) not null,
    constraint mobile_destinations_pk primary key (id),
    constraint mobile_destinations_key_hash_uk unique (key_hash),
    constraint mobile_destinations_destination_fk foreign key (id) references destinations
);
