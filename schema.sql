create sequence celebrant_seq start with 1 increment by 50;

create sequence dashboard_user_seq start with 1 increment by 50;

create sequence reporter_link_seq start with 1 increment by 50;

create sequence service_info_item_seq start with 1 increment by 50;

create sequence service_info_item_response_seq start with 1 increment by 50;

create sequence service_instance_seq start with 1 increment by 50;

create sequence service_template_seq start with 1 increment by 50;

create table celebrant (
    id bigint not null,
    name varchar(255) not null,
    primary key (id)
);

create table celebrant_service (
    celebrants_id bigint not null,
    services_celebrated_id bigint not null,
    primary key (celebrants_id, services_celebrated_id)
);

create table church (
    name varchar(255) not null,
    location varchar(255),
    main_celebrant_id bigint unique,
    primary key (name)
);

create table dashboard_user (
  id bigint not null,
  enabled boolean not null,
  full_name varchar(255),
  phone_number varchar(50),
  password_hash varchar(255),
  preferred_language varchar(5) not null,
  role varchar(255) not null check (role in ('ADMIN', 'REPORTER')),
  username varchar(255) not null unique,
  primary key (id)
);

create table dashboard_user_church (
    dashboard_user_id bigint not null,
    church_name varchar(255) not null,
    primary key (dashboard_user_id, church_name)
);

create table reporter_link (
    id bigint not null,
    church_name varchar(255) not null,
    reporter_id bigint not null,
    service_template_id bigint not null,
    token varchar(255) not null unique,
    primary key (id)
);

create table service_info_item (
    required boolean,
    sort_order integer,
    service_info_item_type varchar(255) not null,
    id bigint not null,
    service_template_id bigint not null,
    title varchar(255) not null,
    description varchar(1000),
    primary key (id)
);

create table service_info_item_response (
    id bigint not null,
    service_info_item_id bigint not null,
    service_instance_id bigint not null,
    response_value varchar(255),
    primary key (id)
);

create table service_instance (
    service_date date,
    church_id varchar(255) not null,
    id bigint not null,
    template_id bigint not null,
    primary key (id)
);

create table service_template (
    id bigint not null,
    service_template_name varchar(255) not null unique,
    template_type varchar(255),
    primary key (id)
);

alter table if exists celebrant_service
   add constraint FKlwilfj4a5xx6aker34vgnjpt7
   foreign key (services_celebrated_id)
   references service_instance;

alter table if exists celebrant_service
   add constraint FKdn9v3f8ff23s47f2xpno63k9c
   foreign key (celebrants_id)
   references celebrant;

alter table if exists church
   add constraint FKa8l0bw0796lsiey3no3kld0hx
   foreign key (main_celebrant_id)
   references celebrant;

alter table if exists dashboard_user_church
   add constraint FKakvq1g5c74960trsumf3cddab
   foreign key (church_name)
   references church;

alter table if exists dashboard_user_church
   add constraint FKtpcwvqv861illkcwkdwl6ckpm
   foreign key (dashboard_user_id)
   references dashboard_user;

alter table if exists reporter_link
   add constraint FKdf4ks8bt8agg2c73vfgrx33ln
   foreign key (church_name)
   references church;

alter table if exists reporter_link
   add constraint FKgj0rnvgxnpkcebaq5eo9skeuk
   foreign key (reporter_id)
   references dashboard_user;

alter table if exists reporter_link
   add constraint FKj23c9081e2vwis25o01nq0kfy
   foreign key (service_template_id)
   references service_template;

alter table if exists service_info_item
   add constraint FKbpqcjnhs5edkblj7nvmtwgwvw
   foreign key (service_template_id)
   references service_template;

alter table if exists service_info_item_response
   add constraint FKgai27ytwv2at6mno53jul6vi9
   foreign key (service_info_item_id)
   references service_info_item;

alter table if exists service_info_item_response
   add constraint FKp0mouju1n2f9aw4v4cav4c1gd
   foreign key (service_instance_id)
   references service_instance;

alter table if exists service_instance
   add constraint FK99iknk0oiobjlsewcp086sybq
   foreign key (church_id)
   references church;

alter table if exists service_instance
   add constraint FKhnfh5nf16lvrtafq78pbw8vs4
   foreign key (template_id)
   references service_template;
