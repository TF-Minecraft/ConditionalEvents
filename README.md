# ConditionalEvents

> Event triggers for TF-Minecraft, maintained from [Ajneb97/ConditionalEvents](https://github.com/Ajneb97/ConditionalEvents).

ConditionalEvents runs actions when Minecraft events meet configured conditions:
commands, item use, joins, deaths, regions and more. Events can run once only or
on a cooldown. Once-only and cooldown data belong to the roleplay character in
play, so such events run only while a character is in play; `data_scope: account`
keeps an event's data per account instead. Events without that data run as
upstream. This copy tracks upstream 4.80.3, builds for
Paper 1.21.10 and needs TLibs 2.6.1 with RPCharacters. It has no update checker or
bStats reporting.

## Documentation

[Project documentation](https://github.com/TF-Minecraft/Docs/blob/main/projects/ConditionalEvents/README.md)

The upstream [ConditionalEvents wiki](https://ajneb97.gitbook.io/conditionalevents)
covers event types, conditions, actions and variables.

## Tests

With Java 21 and the private build inputs prepared by
`.github/scripts/prepare-release.sh`, run `mvn clean verify`.
JUnit and Mockito cover the per-character data, which must keep 100% line
coverage in `net.tfminecraft.conditionalevents.character`. Upstream
ConditionalEvents has no tests.

## License

[MIT](LICENSE), copyright Ajneb97. TF-Minecraft changes are released under the
same license.
