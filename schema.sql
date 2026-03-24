
    create sequence Celebrant_SEQ start with 1 increment by 50;

    create sequence dashboard_user_seq start with 1 increment by 50;

    create sequence reporter_link_seq start with 1 increment by 50;

    create sequence ServiceInfoItem_SEQ start with 1 increment by 50;

    create sequence ServiceInfoItemResponse_SEQ start with 1 increment by 50;

    create sequence ServiceInstance_SEQ start with 1 increment by 50;

    create sequence ServiceTemplate_SEQ start with 1 increment by 50;

    create table Celebrant (
        id bigint not null,
        name varchar(255) not null,
        primary key (id)
    );

    create table celebrant_service (
        celebrants_id bigint not null,
        servicesCelebrated_id bigint not null,
        primary key (celebrants_id, servicesCelebrated_id)
    );

    create table Church (
        name varchar(255) not null,
        location varchar(255),
        main_celebrant_id bigint unique,
        primary key (name)
    );

    create table dashboard_user_church (
        dashboard_user_id bigint not null,
        church_name varchar(255) not null,
        primary key (dashboard_user_id, church_name)
    );

    create table DashboardUser (
        id bigint not null,
        enabled boolean not null,
        passwordHash varchar(255) not null,
        role varchar(255) not null check (role in ('ADMIN','REPORTER')),
        username varchar(255) not null unique,
        primary key (id)
    );

    create table ReporterLink (
        id bigint not null,
        token varchar(255) not null unique,
        church_name varchar(255) not null,
        reporter_id bigint not null,
        service_template_id bigint not null,
        primary key (id)
    );

    create table ServiceInfoItem (
        id bigint not null,
        questionId varchar(255) not null unique,
        required boolean,
        serviceInfoItemType smallint not null check (serviceInfoItemType between 0 and 3),
        serviceTemplate_id bigint not null,
        primary key (id)
    );

    create table ServiceInfoItemResponse (
        id bigint not null,
        responseValue varchar(255),
        service_info_item_id bigint not null,
        service_instance_id bigint not null,
        primary key (id)
    );

    create table ServiceInstance (
        id bigint not null,
        serviceDate date,
        church_id varchar(255) not null,
        template_id bigint not null,
        primary key (id)
    );

    create table ServiceTemplate (
        id bigint not null,
        serviceTemplateName varchar(255) not null unique,
        primary key (id)
    );

    alter table if exists celebrant_service 
       add constraint FKlwilfj4a5xx6aker34vgnjpt7 
       foreign key (servicesCelebrated_id) 
       references ServiceInstance;

    alter table if exists celebrant_service 
       add constraint FKdn9v3f8ff23s47f2xpno63k9c 
       foreign key (celebrants_id) 
       references Celebrant;

    alter table if exists Church 
       add constraint FKa8l0bw0796lsiey3no3kld0hx 
       foreign key (main_celebrant_id) 
       references Celebrant;

    alter table if exists dashboard_user_church 
       add constraint FKakvq1g5c74960trsumf3cddab 
       foreign key (church_name) 
       references Church;

    alter table if exists dashboard_user_church 
       add constraint FKtpcwvqv861illkcwkdwl6ckpm 
       foreign key (dashboard_user_id) 
       references DashboardUser;

    alter table if exists ReporterLink 
       add constraint FKdf4ks8bt8agg2c73vfgrx33ln 
       foreign key (church_name) 
       references Church;

    alter table if exists ReporterLink 
       add constraint FKgj0rnvgxnpkcebaq5eo9skeuk 
       foreign key (reporter_id) 
       references DashboardUser;

    alter table if exists ReporterLink 
       add constraint FKj23c9081e2vwis25o01nq0kfy 
       foreign key (service_template_id) 
       references ServiceTemplate;

    alter table if exists ServiceInfoItem 
       add constraint FKbpqcjnhs5edkblj7nvmtwgwvw 
       foreign key (serviceTemplate_id) 
       references ServiceTemplate;

    alter table if exists ServiceInfoItemResponse 
       add constraint FKgai27ytwv2at6mno53jul6vi9 
       foreign key (service_info_item_id) 
       references ServiceInfoItem;

    alter table if exists ServiceInfoItemResponse 
       add constraint FKp0mouju1n2f9aw4v4cav4c1gd 
       foreign key (service_instance_id) 
       references ServiceInstance;

    alter table if exists ServiceInstance 
       add constraint FK99iknk0oiobjlsewcp086sybq 
       foreign key (church_id) 
       references Church;

    alter table if exists ServiceInstance 
       add constraint FKhnfh5nf16lvrtafq78pbw8vs4 
       foreign key (template_id) 
       references ServiceTemplate;
