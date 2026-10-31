-- Municipality names and type descriptions are in Finnish and Swedish. The existing texts become the Finnish ones.
-- At least one of the two is required, which the application checks, so either may be empty.

alter table municipalities rename column name to name_fi;
alter table municipalities add column name_sv varchar(255) not null default '';
alter table municipalities alter column name_sv drop default;
-- A name may now be empty, so it is unique only among the municipalities that have one in the same language
alter table municipalities drop constraint municipalities_name_uk;
create unique index municipalities_name_fi_uk on municipalities (name_fi) where name_fi <> '';
create unique index municipalities_name_sv_uk on municipalities (name_sv) where name_sv <> '';

alter table assignment_types rename column description to description_fi;
alter table assignment_types add column description_sv varchar(255) not null default '';
alter table assignment_types alter column description_sv drop default;

alter table resource_types rename column description to description_fi;
alter table resource_types add column description_sv varchar(255) not null default '';
alter table resource_types alter column description_sv drop default;
