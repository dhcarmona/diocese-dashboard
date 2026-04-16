create sequence reporter_login_token_seq start with 1 increment by 50;

create table reporter_login_token (
  id bigint not null,
  token varchar(255) not null unique,
  username varchar(255) not null,
  created_at timestamp with time zone not null,
  expires_at timestamp with time zone not null,
  primary key (id)
);

create index reporter_login_token_expires_at_idx on reporter_login_token (expires_at);
