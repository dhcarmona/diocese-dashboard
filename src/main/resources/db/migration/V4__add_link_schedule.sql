create sequence link_schedule_seq start with 1 increment by 50;

-- Stores a recurring schedule for bulk reporter link creation.
-- send_hour is in Costa Rica local time (America/Costa_Rica, UTC-6, no DST), 0–23.
create table link_schedule (
    id                  bigint not null,
    service_template_id bigint not null,
    send_hour           integer not null,
    last_triggered_date date,
    created_at          timestamp with time zone not null,
    primary key (id),
    constraint fk_link_schedule_template
        foreign key (service_template_id) references service_template (id)
);

-- One row per day-of-week for a schedule (e.g. 'MONDAY', 'FRIDAY').
create table link_schedule_day (
    schedule_id bigint       not null,
    day_of_week varchar(20)  not null,
    primary key (schedule_id, day_of_week),
    constraint fk_link_schedule_day_schedule
        foreign key (schedule_id) references link_schedule (id)
);

-- One row per church for a schedule.
create table link_schedule_church (
    schedule_id bigint       not null,
    church_name varchar(255) not null,
    primary key (schedule_id, church_name),
    constraint fk_link_schedule_church_schedule
        foreign key (schedule_id) references link_schedule (id),
    constraint fk_link_schedule_church_name
        foreign key (church_name) references church (name)
);
