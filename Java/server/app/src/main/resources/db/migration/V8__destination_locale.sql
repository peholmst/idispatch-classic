-- Each destination receives notifications in its own language. Runboards used to get the language of the whole
-- installation from idispatch.runboard.language, which was Finnish unless set, so every destination starts in Finnish.
alter table destinations add column locale varchar(35) not null default 'fi';
alter table destinations alter column locale drop default;
