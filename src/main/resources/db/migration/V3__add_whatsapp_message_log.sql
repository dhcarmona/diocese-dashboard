create sequence whatsapp_message_log_seq start with 1 increment by 50;

create table whatsapp_message_log (
    id bigint not null,
    sent_at timestamp with time zone not null,
    recipient_username varchar(255) not null,
    body text,
    otp boolean not null,
    primary key (id)
);
