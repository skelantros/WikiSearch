drop table if exists auxiliary_text;
drop table if exists article_to_category;
drop table if exists category;
drop table if exists article;


create table article (
	id serial primary key,
	title varchar(256) not null,
	create_timestamp timestamp not null,
	update_timestamp timestamp not null,
	wiki varchar(32) not null,
	language varchar(32) not null
);

create unique index title_lower on article (lower(title));

create table category (
	id serial primary key,
	name varchar(256) not null
);

create unique index name_lower on category (lower(name));

create table article_to_category (
	article_id integer not null references article on delete cascade,
	category_id integer not null references category on delete cascade,
	primary key (article_id, category_id)
);

create table auxiliary_text (
	article_id integer not null references article on delete cascade,
	create_timestamp timestamp not null,
	aux_text varchar not null
);