alter table dashboard_user
  add column preferred_language varchar(5) not null default 'es';

alter table dashboard_user
  alter column preferred_language drop default;
