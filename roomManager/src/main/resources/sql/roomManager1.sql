
-- user_info --------------------------------------------------------------------------
alter table USER_INFO
    add uid bigint default 100201 auto_increment;

alter table USER_INFO
    add user_name varchar(100) not null;

alter table USER_INFO
    add password varchar(100) not null;

alter table USER_INFO
    add roomId bigint default 1000201 auto_increment;

alter table USER_INFO
    add token varchar(63) default '' not null;

alter table USER_INFO
    add token_create_time bigint not null;

alter table USER_INFO
    add head_img varchar(256) default '' not null;

alter table USER_INFO
    add cover_img varchar(256) default '' not null;

alter table USER_INFO
    add email varchar(256) default '' not null;

alter table USER_INFO
    add create_time bigint not null;

alter table USER_INFO
    add rtmp_token varchar(256) default '' not null;

alter table USER_INFO
    add sealed boolean default false not null;

alter table USER_INFO
    add sealed_util_time bigint default 0 not null;

alter table USER_INFO
    add allow_anchor boolean default true not null;

alter table USER_INFO
    add constraint USER_INFO_pk
        primary key (uid);

alter table USER_INFO alter column UID set default 100201;

alter table USER_INFO alter column ROOMID set default 1000201;

alter table USER_INFO alter column ROOMID BIGINT default 1000201 auto_increment;



-- record ----------------------------------------------------------

create table record;

alter table RECORD
    add id bigint default 1 auto_increment;

alter table RECORD
    add constraint RECORD_pk
        primary key (id);

alter table RECORD
    add roomid bigint;

alter table RECORD
    add start_time bigint;

alter table RECORD
    add cover_img varchar(256);

alter table RECORD
    add record_name varchar(10485760);

alter table RECORD
    add record_des varchar(10485760);

alter table RECORD
    add view_num integer;

alter table RECORD
    add like_num integer;

alter table RECORD
    add duration varchar(100) default ''::character varying;

alter table RECORD
    add record_addr varchar(100) default ''::character varying;

alter table RECORD alter column ROOMID set not null;

alter table RECORD alter column START_TIME set not null;

alter table RECORD alter column COVER_IMG set not null;

alter table RECORD alter column RECORD_NAME set not null;

alter table RECORD alter column RECORD_DES set not null;

alter table RECORD alter column VIEW_NUM set not null;

alter table RECORD alter column LIKE_NUM set not null;

alter table RECORD alter column DURATION set not null;

alter table RECORD alter column RECORD_ADDR set not null;



-- record_comment --------------------------------------------------

create table record_comment;

alter table RECORD_COMMENT
    add comment_id bigint default 1 auto_increment;

alter table RECORD_COMMENT
    add room_id bigint not null;

alter table RECORD_COMMENT
    add record_time bigint not null;

alter table RECORD_COMMENT
    add comment varchar default '' not null;

alter table RECORD_COMMENT
    add comment_time bigint not null;

alter table RECORD_COMMENT
    add comment_uid bigint not null;

alter table RECORD_COMMENT
    add author_uid bigint;

alter table RECORD_COMMENT
    add relative_time bigint default 0 not null;

alter table RECORD_COMMENT
    add constraint RECORD_COMMENT_pk
        primary key (comment_id);


-- comment_authority -----------------------------------------------------

create table comment_access;

alter table comment_access
    add id bigint default 1 auto_increment;

alter table comment_access
    add constraint comment_permission_pk
        primary key (id);

alter table comment_access
    add record_id bigint not null ;

alter table comment_access
    add host_id bigint not null ;

alter table comment_access
    add room_id bigint not null ;

alter table comment_access
    add allow_uid bigint not null ;

alter table comment_access
    add add_time bigint not null ;
