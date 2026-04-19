alter table if exists service_template
  add column link_only boolean not null default false;
