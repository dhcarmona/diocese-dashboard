alter table reporter_link add column active_date date not null default current_date;
alter table reporter_link alter column active_date drop default;
