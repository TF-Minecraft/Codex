# Codex

> Lore discoveries for TF-Minecraft, maintained from [Ajneb97/Codex](https://github.com/Ajneb97/Codex).

Codex gives players a journal of discoveries: places they enter, creatures they
kill and lore that commands unlock. `/codex` opens the journal, and each entry
shows its text once it has been found.

Each roleplay character keeps its own journal, so a new character discovers the
world afresh and earns the discovery rewards again. What an account found before
this goes to the character that inherited its belongings. This copy tracks upstream
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
