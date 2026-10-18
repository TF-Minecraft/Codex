# Codex

> Lore discoveries for TF-Minecraft, maintained from [Ajneb97/Codex](https://github.com/Ajneb97/Codex).

Codex gives players a journal of discoveries: places they enter, creatures they
kill and lore that commands unlock. `/codex` opens the journal, and each entry
shows its text once it has been found.

Each roleplay character keeps its own journal. Exploration belongs to the account:
a character gets the discoveries its account's other characters made, with their
Player EXP, when it starts playing. Research stays with whoever found it, so a new
character researches afresh and earns those rewards again. An entry any of the account's
characters has found stays readable for out-of-character reference, and its hover
text names the living characters who can use it in character. What an account
found before journals were per character stays readable this way, but no
character knows it in character until it finds it again. This copy tracks upstream
2.12.1, builds for Paper 1.21.10 and needs TLibs 2.6.1 with RPCharacters. It stores
journals in files only, and has no update checker or bStats reporting.

## Documentation

[Project documentation](https://github.com/TF-Minecraft/Docs/blob/main/projects/Codex/README.md)

The upstream [Codex wiki](https://ajneb97.gitbook.io/codex) covers categories,
discoveries, actions and placeholders.

## Tests

With Java 21 and the pinned plugin dependencies installed, run `mvn clean verify`.
JUnit and Mockito cover the per-character journals, which must keep 100% line
coverage. Upstream Codex has no tests.

## License

[MIT](LICENSE), copyright Ajneb97. TF-Minecraft changes are released under the
same license.
