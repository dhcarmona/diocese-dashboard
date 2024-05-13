
    create sequence Celebrant_SEQ start with 1 increment by 50;

    create sequence ServiceInfoItem_SEQ start with 1 increment by 50;

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

    create table ServiceInfoItem (
        id bigint not null,
        questionId varchar(255) not null unique,
        required boolean,
        serviceInfoItemType smallint not null check (serviceInfoItemType between 0 and 3),
        serviceTemplate_id bigint not null,
        primary key (id)
    );

    create table ServiceInstance (
        id bigint not null,
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

    alter table if exists ServiceInfoItem 
       add constraint FKbpqcjnhs5edkblj7nvmtwgwvw 
       foreign key (serviceTemplate_id) 
       references ServiceTemplate;

    alter table if exists ServiceInstance 
       add constraint FK99iknk0oiobjlsewcp086sybq 
       foreign key (church_id) 
       references Church;

    alter table if exists ServiceInstance 
       add constraint FKhnfh5nf16lvrtafq78pbw8vs4 
       foreign key (template_id) 
       references ServiceTemplate;
