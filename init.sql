drop table if exists auxiliary_text;
drop table if exists quote_to_category;
drop table if exists category;
drop table if exists quote;


create table quote (
	id serial primary key,
	title varchar(256) not null,
	create_timestamp timestamp not null,
	update_timestamp timestamp not null,
	wiki varchar(32) not null,
	language varchar(32) not null
);

create unique index title_lower on quote (lower(title));

create table category (
	id serial primary key,
	name varchar(256) not null
);

create unique index name_lower on category (lower(name));

create table quote_to_category (
	quote_id integer not null references quote on delete cascade,
	category_id integer not null references category on delete cascade,
	primary key (quote_id, category_id)
);

create table auxiliary_text (
	quote_id integer not null references quote on delete cascade,
	create_timestamp timestamp not null,
	aux_text varchar not null
);