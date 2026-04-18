create sequence section_header_seq start with 1 increment by 50;

create table section_header (
  id bigint not null,
  title varchar(255) not null,
  sort_order integer,
  service_template_id bigint not null,
  primary key (id)
);

alter table if exists section_header
  add constraint fk_section_header_service_template
  foreign key (service_template_id)
  references service_template;
