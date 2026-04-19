alter table if exists service_template
  add column link_only boolean not null default false;

alter table if exists service_template
  alter column link_only drop default;
